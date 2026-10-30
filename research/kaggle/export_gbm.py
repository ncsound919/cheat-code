"""Export a fitted CalibratedEnsemble (4 HGB folds + 4 isotonic maps) to JSON.

The format is flat and versioned so a Kotlin tree-walker can load it without any
Python dependency. Every number needed to reproduce predict_proba on a phone is here.

Prediction (must match sklearn exactly):
  raw = baseline
  for each fold:
    fold_raw = baseline
    for each tree: fold_raw += learning_rate * leaf_value(walk(tree, x))
    prob = sigmoid(fold_raw)
    calibrated = isotonic(prob)        # isotonic maps probability -> probability
  p = mean(calibrated over folds)
  p = clip(p, 1e-6, 1-1e-6)

Tree walk (raw, non-binned path, matches sklearn _predict_one_from_raw_data):
  if leaf: return value
  if x[feature] is NaN: go left iff missing_go_left
  elif x[feature] <= node.threshold: go left
  else: go right

Isotonic lookup: largest X_thresholds_ <= p; clip to range.
"""
import bisect
import json
from pathlib import Path

import numpy as np


def _export_tree(tree):
    nodes = tree.nodes
    out = []
    for nd in nodes:
        if nd["is_leaf"]:
            out.append({"leaf": True, "value": float(nd["value"])})
        else:
            out.append({
                "leaf": False,
                "feature": int(nd["feature_idx"]),
                "threshold": float(nd["num_threshold"]),
                "missing_go_left": bool(nd["missing_go_to_left"]),
                "left": int(nd["left"]),
                "right": int(nd["right"]),
            })
    return {"nodes": out}


def _export_iso(iso):
    return {
        "x": [float(v) for v in iso.X_thresholds_],
        "y": [float(v) for v in iso.y_thresholds_],
    }


def export(calibrated_ensemble, feature_schema_version, features, out_path):
    model0 = calibrated_ensemble.parts[0][0]
    folds = []
    for model, iso in calibrated_ensemble.parts:
        folds.append({
            "baseline": float(np.asarray(model._baseline_prediction).ravel()[0]),
            "trees": [_export_tree(tr) for it in model._predictors for tr in it],
            "isotonic": _export_iso(iso),
        })
    bundle = {
        "kind": "propslab_gbm_bundle",
        "version": 1,
        "feature_schema_version": int(feature_schema_version),
        "features": list(features),
        "learning_rate": float(model0.learning_rate),
        "n_folds": len(folds),
        "folds": folds,
    }
    Path(out_path).write_text(json.dumps(bundle), encoding="utf-8")
    return bundle


def predict_bundle(bundle, X):
    features = bundle["features"]
    lr = bundle["learning_rate"]
    n = len(X)
    out = np.zeros(n, dtype=np.float64)
    for fi in range(n):
        x = X[fi]
        fold_preds = []
        for fold in bundle["folds"]:
            raw = fold["baseline"]
            for tree in fold["trees"]:
                nodes = tree["nodes"]
                i = 0
                while True:
                    nd = nodes[i]
                    if nd["leaf"]:
                        raw += nd["value"]   # learning_rate already baked in
                        break
                    f = nd["feature"]
                    thr = nd["threshold"]
                    xi = float(x[f])
                    if xi != xi:  # NaN
                        go_left = nd["missing_go_left"]
                    else:
                        go_left = xi <= thr
                    i = nd["left"] if go_left else nd["right"]
            prob = 1.0 / (1.0 + np.exp(-raw))
            iso = fold["isotonic"]
            xs = np.asarray(iso["x"]); ys = np.asarray(iso["y"])
            cal = float(np.interp(prob, xs, ys))
            fold_preds.append(cal)
        out[fi] = float(np.mean(fold_preds))
    return np.clip(out, 1e-6, 1.0 - 1e-6)


if __name__ == "__main__":
    import numpy as np
    from sklearn.ensemble import HistGradientBoostingClassifier
    from sklearn.isotonic import IsotonicRegression
    from sklearn.model_selection import GroupKFold

    rng = np.random.RandomState(0)
    X = rng.randn(2000, 4)
    y = (X[:, 0] + 0.5 * X[:, 2] > 0).astype(int)
    groups = np.repeat(np.arange(4), 500)
    FEATS = ["a", "b", "c", "d"]

    class CE:
        def __init__(self):
            self.parts = []

        def fit(self, X, y, groups):
            gkf = GroupKFold(n_splits=4)
            for tr, va in gkf.split(X, y, groups):
                m = HistGradientBoostingClassifier(max_iter=10, max_leaf_nodes=8,
                                                  learning_rate=0.1, random_state=0,
                                                  monotonic_cst=[1, 0, 1, 0])
                m.fit(X[tr], y[tr])
                p = m.predict_proba(X[va])[:, 1]
                iso = IsotonicRegression(out_of_bounds="clip", y_min=0.0, y_max=1.0)
                iso.fit(p, y[va])
                self.parts.append((m, iso))
            return self

        def predict(self, X):
            acc = np.zeros(len(X))
            for m, iso in self.parts:
                acc += iso.predict(m.predict_proba(X)[:, 1])
            return acc / len(self.parts)

    ce = CE().fit(X, y, groups)
    sk = ce.predict(X[:50])
    bundle = export(ce, 1, FEATS, r"C:\Users\tap45\AppData\Local\Temp\opencode\gbm_smoke.json")
    print("wrote bundle, folds:", bundle["n_folds"], "trees/fold:",
          len(bundle["folds"][0]["trees"]))
    bp = predict_bundle(bundle, X[:50])
    print("max abs diff vs sklearn:", np.abs(bp - sk).max())
    print("mean diff:", np.abs(bp - sk).mean())
