
import json, glob, math
import numpy as np
import pandas as pd

LENS = json.loads('{"claim": "their defense decides this game", "factor": "defense", "side": "UNDER_ONLY", "min_edge": 3.0, "tilt": -2.5}')

paths = glob.glob('C:/Users/tap45/Downloads/PropsLab/research/kaggle/dataset/**/propslab_training.parquet', recursive=True)
if not paths:
    raise SystemExit('no training parquet found')
df = pd.read_parquet(paths[0])
df = df.dropna(subset=['pts', 'mu', 't_sd', 'game_date']).copy()
df['game_date'] = pd.to_datetime(df['game_date'])
df = df.sort_values(['game_date', 'game_id', 'player_id']).reset_index(drop=True)
df['resid'] = df['pts'] - df['mu']

def welch(a, b):
    a = np.asarray(a, dtype=float); b = np.asarray(b, dtype=float)
    a = a[np.isfinite(a)]; b = b[np.isfinite(b)]
    if len(a) < 30 or len(b) < 30:
        return None, None
    va, vb = a.var(ddof=1) / len(a), b.var(ddof=1) / len(b)
    if va + vb <= 0:
        return None, None
    return float((a.mean() - b.mean()) / math.sqrt(va + vb)), float(a.mean() - b.mean())

def slope_t(x, y):
    x = np.asarray(x, dtype=float); y = np.asarray(y, dtype=float)
    ok = np.isfinite(x) & np.isfinite(y)
    x, y = x[ok], y[ok]
    if len(x) < 30 or np.std(x) == 0:
        return None, None
    X = np.column_stack([np.ones(len(x)), x])
    beta, *_ = np.linalg.lstsq(X, y, rcond=None)
    resid = y - X @ beta
    dof = max(len(x) - 2, 1)
    sigma2 = float(resid @ resid) / dof
    cov = sigma2 * np.linalg.inv(X.T @ X)
    se = math.sqrt(max(cov[1, 1], 1e-18))
    return float(beta[1]), float(beta[1] / se)

factor = LENS.get('factor', 'generic')
ft = {'name': None, 'supported': False, 'slope': None, 't': None, 'n': int(len(df)), 'note': ''}

if factor == 'defense':
    ft['name'] = 'opp_def100_trail'
    s, t = slope_t(df['opp_def100_trail'], df['resid'])
    ft.update(supported=True, slope=s, t=t,
              note='Points above baseline vs opponent defensive rating. Positive = a worse defense allows more than the baseline expects.')
elif factor == 'fatigue':
    ft['name'] = 'b2b'
    t, sh = welch(df.loc[df['b2b'] == 1, 'resid'], df.loc[df['b2b'] == 0, 'resid'])
    ft.update(supported=True, t=t, shift=sh,
              note='Second night of a back-to-back minus a normal night, in points above baseline.')
elif factor == 'rest':
    ft['name'] = 'rest_days'
    s, t = slope_t(df['rest_days'], df['resid'])
    ft.update(supported=True, slope=s, t=t, note='Points above baseline per extra rest day.')
elif factor == 'home':
    ft['name'] = 'is_home'
    t, sh = welch(df.loc[df['is_home'] == 1, 'resid'], df.loc[df['is_home'] == 0, 'resid'])
    ft.update(supported=True, t=t, shift=sh, note='Home minus road, in points above baseline.')
elif factor == 'pace':
    ft['name'] = 'own_pace_trail'
    s, t = slope_t(df['own_pace_trail'], df['resid'])
    ft.update(supported=True, slope=s, t=t, note='Points above baseline vs the player team pace.')
elif factor == 'minutes':
    ft['name'] = 't_min'
    s, t = slope_t(df['t_min'], df['resid'])
    ft.update(supported=True, slope=s, t=t, note='Points above baseline vs trailing minutes.')
else:
    ft['note'] = 'The training dataset is points-only and has no column for this idea.'

# Direction the claim predicts, so a huge t with the wrong sign is not a pass. The tilt is
# the claim's own lean, in percentage points of over chance; the side filter is only which
# bets the lens will take, so it must not decide what the claim believes.
tilt = float(LENS.get('tilt', 0.0))
expected_sign = 1 if tilt > 0.05 else (-1 if tilt < -0.05 else 0)
if factor in ('defense', 'pace', 'rest', 'minutes') and ft.get('slope') is not None:
    factor_supported = (ft['t'] is not None) and (abs(ft['t']) > 2.0) and \
        (expected_sign == 0 or ((ft['slope'] > 0) == (expected_sign > 0)))
elif factor in ('fatigue', 'home'):
    factor_supported = (ft.get('t') is not None) and (abs(ft['t']) > 2.0) and \
        (expected_sign == 0 or ((ft['t'] < 0) == (expected_sign < 0)))
else:
    factor_supported = False
ft['expected'] = 'no direction claimed' if expected_sign == 0 else ('lower points' if expected_sign < 0 else 'higher points')
ft['supported'] = bool(factor_supported)

# ---- lens replay. The line only knows the baseline; the claim supplies the projection.
mu = df['mu'].to_numpy(dtype=float)
proj = mu.copy()
col = ft.get('name')
if col and col in df.columns and (ft.get('slope') is not None or ft.get('shift') is not None):
    xs = df[col].to_numpy(dtype=float)
    m = float(np.nanmean(xs)) if np.isfinite(xs).any() else 0.0
    xs = np.where(np.isfinite(xs), xs, m)
    effect = ft['slope'] if ft.get('slope') is not None else ft['shift']
    proj = mu + effect * (xs - m)
sd = np.maximum.reduce([df['t_sd'].fillna(0).to_numpy(), 0.30 * mu, np.ones(len(df))])
# Centred on the baseline. round(mu)+0.5 sits systematically above mu, which would hand
# every under a free half point and fake a profit that belongs to the line, not the claim.
line = np.round(mu * 2.0) / 2.0
from math import erf, sqrt
z = (line - proj) / sd
cdf = 0.5 * (1.0 + np.vectorize(erf)(z / sqrt(2.0)))
p_over = np.clip(1.0 - cdf + tilt / 100.0, 0.01, 0.99)
# What the same line would say with no claim at all. A lens only earns a pass if it
# predicts better than the baseline it is built on, not just if a made-up price pays.
z0 = (line - mu) / sd
cdf0 = 0.5 * (1.0 + np.vectorize(erf)(z0 / sqrt(2.0)))
p_over_base = np.clip(1.0 - cdf0, 0.01, 0.99)
actual = df['pts'].to_numpy()
won_over = actual > line
edge = proj - line
flat = np.zeros(len(edge), dtype=bool)
in_over = LENS['side'] in ('OVER_ONLY', 'BOTH')
in_under = LENS['side'] in ('UNDER_ONLY', 'BOTH')
# One unit at -110 either way: a win pays 100/110, a loss costs 1.
profit = np.where(won_over, 100.0 / 110.0, -1.0)
profit_under = np.where(~won_over, 100.0 / 110.0, -1.0)
cut = int(len(df) * 0.8)
hold_mask = np.zeros(len(df), dtype=bool); hold_mask[cut:] = True

def replay_at(bar):
    over = (edge >= bar) if in_over else flat
    under = (-edge >= bar) if in_under else flat
    take = over | under
    pay = np.where(over, profit, profit_under)
    n = int(take.sum())
    if n == 0:
        return {'bets': 0, 'roi': 0.0, 't': 0.0, 'holdout_bets': 0, 'holdout_roi': None}
    r = float(pay[take].mean())
    s = float(pay[take].std(ddof=1)) if n > 1 else 0.0
    h = take & hold_mask
    hn = int(h.sum())
    hroi = float(pay[h].mean()) if hn else None
    return {
        'bets': n, 'roi': r, 't': (r / (s / math.sqrt(n))) if n > 1 and s > 0 else 0.0,
        'holdout_bets': hn, 'holdout_roi': hroi,
    }

primary = replay_at(LENS['min_edge'])
bets, roi, t_replay = primary['bets'], primary['roi'], primary['t']
hold_bets, hold_roi = primary['holdout_bets'], primary['holdout_roi']
# A realistic claim is worth a fraction of a point, so an absolute three-point bar
# often collects nothing at all. Report the bar it would need, not just an empty replay.
trials = {}
for bar in (1.0, 0.5):
    if bets < 50 and bar < LENS['min_edge']:
        trials[str(bar)] = replay_at(bar)
max_edge = float(np.max(edge)) if len(edge) else 0.0
max_under_edge = float(np.max(-edge)) if len(edge) else 0.0
brier = float(np.mean((p_over - won_over.astype(float)) ** 2))
brier_base = float(np.mean((p_over_base - won_over.astype(float)) ** 2))
skill = brier_base - brier

replay = {
    'bets': bets, 'roi': roi, 't': t_replay, 'brier': brier, 'brier_base': brier_base, 'skill': skill,
    'holdout_bets': hold_bets, 'holdout_roi': hold_roi,
    'max_edge': max_edge, 'max_under_edge': max_under_edge, 'trials': trials,
    'line': 'nearest half point to mu', 'price': -110, 'edge': 'claim projection minus line',
    'note': 'Constructed -110 line at the player own baseline; the claim supplies the projection, not a real market price.'
}

reach = max(max_edge, max_under_edge)
if not ft['supported']:
    verdict = 'UNTESTED'
    note = ft['note'] if factor not in ('defense', 'fatigue', 'rest', 'home', 'pace', 'minutes') else \
        'The idea did not move points in the claimed direction on 665k games (t ' + str(round(ft.get('t') or 0, 2)) + ').'
elif bets < 50:
    verdict = 'NO_EDGE_VOLUME'
    note = 'The claim only reaches ' + str(round(reach, 2)) + ' points of edge against a baseline line, ' + \
        'so it never clears the lens own ' + str(LENS['min_edge']) + '-point bar on 665k games.'
elif skill > 0 and roi > 0 and t_replay > 2.0 and (hold_roi is None or hold_roi >= 0):
    verdict = 'PASS'
    note = 'The factor moved points as claimed and beat the baseline at predicting the line ' + \
        '(Brier ' + str(round(brier, 4)) + ' vs ' + str(round(brier_base, 4)) + '), with a positive shadow return.'
elif skill <= 0:
    verdict = 'NO_SKILL'
    note = 'The claim moved points the way it said, but at the line it predicts no better than the ' + \
        'baseline itself (Brier ' + str(round(brier, 4)) + ' vs ' + str(round(brier_base, 4)) + ').'
else:
    verdict = 'FAIL'
    note = 'The factor check passed but the lens did not beat a flat -110 price out of sample ' + \
        '(replay t ' + str(round(t_replay, 2)) + ', holdout ' + str(None if hold_roi is None else round(hold_roi, 4)) + ').'

out = {
    'claim': LENS['claim'], 'factor': factor, 'rows': int(len(df)),
    'factor_test': ft, 'replay': replay, 'verdict': verdict, 'note': note,
}
json.dump(out, open('claim_result.json', 'w'), indent=1)
print(json.dumps({k: out[k] for k in ('factor', 'rows', 'verdict')}))
