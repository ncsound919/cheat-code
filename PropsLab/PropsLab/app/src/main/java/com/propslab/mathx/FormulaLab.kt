package com.propslab.mathx

import com.propslab.domain.Odds
import com.propslab.domain.Pricing
import com.propslab.domain.Staking
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

data class FormulaStep(val operation: String, val from: String, val to: String)

/** A check is executable. Its name is the claim shown to the user. */
class FormulaCheck(val name: String, val block: () -> Boolean)

data class FormulaSpec(
    val id: String,
    val title: String,
    val mode: MathXMode,
    val statement: String,
    val symbols: List<String>,
    val steps: List<FormulaStep>,
    val checks: List<FormulaCheck>,
)

data class FormulaCheckResult(val name: String, val passed: Boolean)

data class FormulaReport(val spec: FormulaSpec, val results: List<FormulaCheckResult>) {
    val passed: Int get() = results.count { it.passed }
    val total: Int get() = results.size
    val allPassed: Boolean get() = passed == total
}

/**
 * Math X's Formula Lab, reduced to the part that can run offline and deterministically.
 *
 * The catalog holds the derivation Maths X would produce in `formula` or `probability`
 * mode, and every claim is executed against the same Kotlin functions the app ships.
 * No expression string is ever evaluated, so a model reply cannot become code.
 *
 * The independent SymPy reference lives in `research/mathx/verify_formulas.py`.
 */
object FormulaLab {

    private val prices = listOf(
        -1000, -500, -250, -200, -150, -125, -110, -105,
        100, 105, 110, 120, 150, 200, 300, 500, 1000,
    )
    private val probabilities = (5..95 step 5).map { it / 100.0 }
    private val pushes = listOf(0.0, 0.05, 0.12)

    val formulas: List<FormulaSpec> = listOf(noVig(), expectedValue(), kelly(), platt())

    fun report(): List<FormulaReport> = formulas.map { spec ->
        FormulaReport(
            spec = spec,
            results = spec.checks.map { check ->
                FormulaCheckResult(check.name, runCatching { check.block() }.getOrDefault(false))
            },
        )
    }

    private fun noVig() = FormulaSpec(
        id = "no_vig_hold",
        title = "No-vig price and hold",
        mode = MathXMode.FORMULA,
        statement = "Remove the book's margin from a two-way market, then measure the margin it charged.",
        symbols = listOf("dec_over", "dec_under", "i_over", "i_under"),
        steps = listOf(
            FormulaStep(
                operation = "definition",
                from = "i_over = 1 / dec_over,  i_under = 1 / dec_under",
                to = "hold = i_over + i_under - 1",
            ),
            FormulaStep(
                operation = "simplify",
                from = "p_over = i_over / (i_over + i_under)",
                to = "p_over = dec_under / (dec_over + dec_under)",
            ),
            FormulaStep(
                operation = "complement",
                from = "p_under = 1 - p_over",
                to = "p_over + p_under = 1",
            ),
        ),
        checks = listOf(
            FormulaCheck("No-vig probabilities sum to one") {
                prices.all { over ->
                    prices.all { under ->
                        abs(Odds.noVigOver(over, under) + Odds.noVigOver(under, over) - 1.0) < 1e-12
                    }
                }
            },
            FormulaCheck("Hold on -110 / -110 is 4.7619%") {
                abs(Pricing.hold(-110, -110) - (1.0 - 2.0 * 100.0 / 210.0)) < 1e-12
            },
            FormulaCheck("Edge is antisymmetric") {
                probabilities.all { p ->
                    probabilities.all { q -> abs(Pricing.edgePoints(p, q) + Pricing.edgePoints(q, p)) < 1e-12 }
                }
            },
            FormulaCheck("Fair price round-trips to the same quote") {
                prices.all { price ->
                    abs(Pricing.fairAmerican(Odds.impliedProbability(price)).toDouble() - price.toDouble()) <= 1.0
                }
            },
        ),
    )

    private fun expectedValue() = FormulaSpec(
        id = "push_aware_ev",
        title = "Expected value with push mass",
        mode = MathXMode.PROBABILITY,
        statement = "A push returns the stake, so it is a zero-profit outcome rather than a loss.",
        symbols = listOf("p", "t", "q", "b", "stake"),
        steps = listOf(
            FormulaStep("definition", "q = 1 - p - t", "q is the loss probability"),
            FormulaStep("expand", "EV = stake · (p · b - q)", "EV = stake · (p · b + p + t - 1)"),
            FormulaStep("break-even", "EV = 0", "dec_fair = 1 + q / p"),
        ),
        checks = listOf(
            FormulaCheck("EV matches the outcome sum") {
                probabilities.all { p ->
                    pushes.all { t ->
                        if (p + t >= 0.999) true
                        else prices.all { price ->
                            val expected = p * (Odds.americanToDecimal(price) - 1.0) - (1.0 - p - t)
                            abs(Staking.expectedValue(p, t, price) - expected) < 1e-12
                        }
                    }
                }
            },
            FormulaCheck("EV is zero at the fair price") {
                probabilities.all { p ->
                    pushes.all { t ->
                        p + t >= 0.999 || abs(Staking.expectedValue(p, t, Pricing.fairAmerican(p, t))) <= 0.02
                    }
                }
            },
            FormulaCheck("Push mass raises the break-even price") {
                Pricing.fairAmerican(0.5, 0.10) == -125
            },
            FormulaCheck("EV scales linearly with stake") {
                val one = Staking.expectedValue(0.55, 0.05, -110, 1.0)
                abs(Staking.expectedValue(0.55, 0.05, -110, 2.0) - 2.0 * one) < 1e-12
            },
        ),
    )

    private fun kelly() = FormulaSpec(
        id = "push_aware_kelly",
        title = "Kelly fraction with push mass",
        mode = MathXMode.PROBABILITY,
        statement = "Maximise expected log growth when a push returns the stake and contributes zero growth.",
        symbols = listOf("p", "t", "q", "b", "f"),
        steps = listOf(
            FormulaStep("objective", "g(f) = p · ln(1 + b·f) + q · ln(1 - f)", "growth rate of the bankroll"),
            FormulaStep("differentiate", "dg/df = p·b / (1 + b·f) - q / (1 - f)", "set to zero"),
            FormulaStep("solve", "p·b·(1 - f) = q·(1 + b·f)", "f* = (p·b - q) / (b·(1 - t))"),
        ),
        checks = listOf(
            FormulaCheck("Kelly is zero at the fair price") {
                probabilities.all { p ->
                    pushes.all { t ->
                        p + t >= 0.99 || abs(Staking.kellyFraction(p, t, Pricing.fairAmerican(p, t))) < 0.02
                    }
                }
            },
            FormulaCheck("Closed form matches a numeric growth maximum") {
                val cases = listOf(
                    Triple(0.55, 0.0, -110),
                    Triple(0.60, 0.0, -110),
                    Triple(0.70, 0.0, 120),
                    Triple(0.55, 0.08, -110),
                    Triple(0.62, 0.10, -125),
                    Triple(0.75, 0.05, 150),
                )
                cases.all { (p, t, price) ->
                    val fStar = Staking.kellyFraction(p, t, price)
                    if (fStar <= 0.0) return@all false
                    val b = Odds.americanToDecimal(price) - 1.0
                    val q = 1.0 - p - t
                    fun growth(f: Double): Double =
                        if (1.0 - f <= 0.0) Double.NEGATIVE_INFINITY
                        else p * ln(1.0 + b * f) + q * ln(1.0 - f)
                    val numeric = (0..995).map { it / 1000.0 }.maxByOrNull { growth(it) } ?: 0.0
                    abs(numeric - fStar) <= 0.002 && growth(fStar) > growth(0.0)
                }
            },
            FormulaCheck("Kelly is negative below the fair price") {
                probabilities.all { p ->
                    pushes.all { t ->
                        if (p + t >= 0.99) true
                        else {
                            val fair = Pricing.fairAmerican(p, t)
                            val worse = if (fair > 0) {
                                val candidate = fair - 20
                                if (candidate >= 100) candidate else -101
                            } else {
                                fair - 20
                            }
                            Staking.kellyFraction(p, t, worse) < 0.0
                        }
                    }
                }
            },
            FormulaCheck("Push mass increases the fraction") {
                Staking.kellyFraction(0.55, 0.08, -110) > Staking.kellyFraction(0.55, 0.0, -110)
            },
        ),
    )

    private fun platt() = FormulaSpec(
        id = "platt_calibration",
        title = "Platt calibration on the logit",
        mode = MathXMode.PROBABILITY,
        statement = "A linear correction on the logit stays inside the probability space and is monotone when a is positive.",
        symbols = listOf("p", "a", "b", "z"),
        steps = listOf(
            FormulaStep("definition", "logit(p) = ln(p / (1 - p))", "z = a · logit(p) + b"),
            FormulaStep("inverse link", "p_cal = sigmoid(z)", "p_cal = 1 / (1 + exp(-a · logit(p) - b))"),
        ),
        checks = listOf(
            FormulaCheck("Identity calibration returns the input") {
                probabilities.filter { it in 0.01..0.99 }.all { p ->
                    abs(sigmoid(logit(p)) - p) < 1e-12
                }
            },
            FormulaCheck("Calibrated output stays inside (0, 1)") {
                val slopes = listOf(0.5, 1.0, 1.5, 2.0)
                val intercepts = listOf(-2.0, -1.0, 0.0, 1.0, 2.0)
                slopes.all { a ->
                    intercepts.all { b ->
                        probabilities.all { p ->
                            val out = sigmoid(a * logit(p) + b)
                            out > 0.0 && out < 1.0
                        }
                    }
                }
            },
            FormulaCheck("Positive slope preserves order") {
                (1..90).all { i ->
                    val p1 = i / 100.0
                    val p2 = (i + 5) / 100.0
                    sigmoid(1.5 * logit(p1) + 0.3) < sigmoid(1.5 * logit(p2) + 0.3)
                }
            },
        ),
    )

    private fun logit(p: Double): Double = ln(p / (1.0 - p))

    private fun sigmoid(z: Double): Double = 1.0 / (1.0 + exp(-z))
}
