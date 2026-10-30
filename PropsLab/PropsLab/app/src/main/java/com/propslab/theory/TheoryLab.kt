package com.propslab.theory

import com.propslab.domain.Markets
import com.propslab.sim.Autopilot
import com.propslab.sim.SimProp
import java.time.LocalDate
import java.util.Locale

/**
 * A dataset a theory can be tested on.
 *
 * The requirement is deliberately strict: a row needs the two prices, the probability the
 * model gave *at the time*, and the result. Without the historical probability there is no
 * edge to measure, and a backtest would just be re-deriving today's opinion over yesterday's
 * games — which is the most common way a backtest lies.
 */
object HistoryCsv {

    data class Row(
        val id: String,
        val playedUtc: Long,
        val playerName: String,
        val market: String,
        val line: Double,
        val overOdds: Int,
        val underOdds: Int,
        val pOver: Double,
        val pPush: Double,
        val actual: Double,
    )

    data class Parsed(val rows: List<Row>, val skipped: Int, val problems: List<String>)

    private val ALIASES = mapOf(
        "date" to "date", "played" to "date", "game_date" to "date",
        "player" to "player", "name" to "player", "player_name" to "player",
        "market" to "market", "stat" to "market",
        "line" to "line",
        "over" to "over", "over_odds" to "over", "over_price" to "over",
        "under" to "under", "under_odds" to "under", "under_price" to "under",
        "p_over" to "p_over", "pover" to "p_over", "prob" to "p_over", "probability" to "p_over", "model_over" to "p_over",
        "p_push" to "p_push", "push" to "p_push",
        "actual" to "actual", "result" to "actual", "value" to "actual",
    )

    fun parse(text: String): Parsed {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty()) return Parsed(emptyList(), 0, listOf("The file is empty."))
        val header = split(lines.first()).map { ALIASES[it.lowercase(Locale.ROOT).replace(" ", "_")] ?: "" }
        val missing = listOf("date", "player", "line", "over", "under", "p_over", "actual").filter { it !in header }
        if (missing.isNotEmpty()) {
            return Parsed(emptyList(), 0, listOf("Missing column(s): ${missing.joinToString()}. Expected a header row."))
        }
        val rows = ArrayList<Row>()
        var skipped = 0
        val problems = ArrayList<String>()
        for ((index, line) in lines.drop(1).withIndex()) {
            val cells = split(line)
            val get = { name: String -> cells[header.indexOf(name)].trim() }
            val date = runCatching { LocalDate.parse(get("date")) }.getOrNull()
            val player = get("player")
            val market = marketOf(get("market"))
            val lineValue = get("line").toDoubleOrNull()
            val over = get("over").toIntOrNull()?.takeIf { it >= 100 || it <= -100 }
            val under = get("under").toIntOrNull()?.takeIf { it >= 100 || it <= -100 }
            val pOver = get("p_over").toDoubleOrNull()?.let { if (it > 1.0) it / 100.0 else it }
            val pPush = header.indexOf("p_push").takeIf { it >= 0 }?.let {
                cells.getOrNull(it)?.trim()?.toDoubleOrNull()?.let { p -> if (p > 1.0) p / 100.0 else p }
            } ?: 0.0
            val actual = get("actual").toDoubleOrNull()

            if (date == null || player.isEmpty() || market == null || lineValue == null ||
                over == null || under == null || pOver == null || actual == null ||
                pOver <= 0.0 || pOver >= 1.0 || pPush < 0.0 || pOver + pPush >= 1.0
            ) {
                skipped++
                if (problems.size < 4) problems += "Row ${index + 2} could not be read."
                continue
            }
            val key = "${get("date")}|$player|${market}|$lineValue"
            rows += Row(
                id = "hist-" + Integer.toHexString(key.lowercase(Locale.ROOT).hashCode()),
                playedUtc = date.toEpochDay() * 86_400_000L,
                playerName = player,
                market = market,
                line = lineValue,
                overOdds = over,
                underOdds = under,
                pOver = pOver,
                pPush = pPush,
                actual = actual,
            )
        }
        return Parsed(rows, skipped, problems)
    }

    /** Accepts either the app's market key or the word a person would type. */
    fun marketOf(raw: String): String? {
        val key = raw.trim().lowercase(Locale.ROOT)
        return when (key) {
            "", "points", "pts", "point" -> Markets.POINTS
            "rebounds", "reb", "rebs" -> Markets.REBOUNDS
            "assists", "ast", "asts" -> Markets.ASSISTS
            "threes", "3pm", "three" -> Markets.THREES
            "pra", "points+rebounds+assists" -> Markets.PRA
            else -> Markets.VALIDATED.firstOrNull { it == key }
        }
    }

    private fun split(line: String): List<String> = line.split(',').map { it.trim().removeSurrounding("\"") }
}

/**
 * The formula grid, run over history instead of a simulated season.
 *
 * Same engine, same gate, same numbers — the only difference is where the props came from.
 * If a formula passes here it passed on prices someone actually paid; if it fails, the
 * simulated promotion was about the simulator.
 */
object TheoryBench {

    data class Result(
        val rows: Int,
        val usable: Int,
        val candidates: List<Autopilot.Candidate>,
        val note: String,
    ) {
        val passed: List<Autopilot.Candidate> get() = candidates.filter { it.gate.allPassed }
    }

    fun toProps(rows: List<HistoryCsv.Row>): List<SimProp> = rows.mapNotNull { row ->
        val pUnder = 1.0 - row.pOver - row.pPush
        if (pUnder <= 0.0 || pUnder >= 1.0) return@mapNotNull null
        SimProp(
            id = row.id,
            playerName = row.playerName,
            line = row.line,
            overOdds = row.overOdds,
            underOdds = row.underOdds,
            pOver = row.pOver,
            pPush = row.pPush,
            pUnder = pUnder,
            startUtc = row.playedUtc,
            actual = row.actual,
            synthetic = false,
            // The market travels with the row so a learned market exclusion or focus is
            // tested against history instead of silently betting every market.
            market = row.market,
        )
    }

    fun run(rows: List<HistoryCsv.Row>, grid: List<com.propslab.sim.StrategyParams> = Autopilot.grid): Result {
        val props = toProps(rows)
        if (props.isEmpty()) {
            return Result(rows.size, 0, emptyList(), "None of the imported rows had a usable probability and result.")
        }
        val plan = Autopilot.plan(props, grid = grid)
        return Result(
            rows = rows.size,
            usable = props.size,
            candidates = plan.candidates,
            note = plan.note,
        )
    }

    /** Plain text, because the console prints it verbatim and a person has to read it. */
    fun summarise(result: Result, limit: Int = 6): String {
        if (result.candidates.isEmpty()) return "Backtest: ${result.note}"
        return buildString {
            append("Backtest over ${result.rows} imported rows (${result.usable} usable, ${result.candidates.size} formulas).\n")
            append("${result.passed.size} cleared the gate on this data.\n\n")
            result.candidates.take(limit).forEach { candidate ->
                append(candidate.params.label).append('\n')
                append("  ").append(candidate.stats.bets).append(" bets, ")
                append("ROI ").append("%+.2f%%".format(candidate.stats.roi * 100)).append(", ")
                append("t ").append("%.2f".format(candidate.stats.tStat)).append(", ")
                append("drawdown ").append("%.0f%%".format(candidate.stats.maxDrawdown * 100)).append(" — ")
                append(if (candidate.gate.allPassed) "PASS" else "FAIL: " + candidate.gate.summary).append('\n')
            }
            if (result.candidates.size > limit) {
                append("…and ${result.candidates.size - limit} more formulas.\n")
            }
            append("\nA gate pass here is evidence about this dataset, not about the next game.")
        }
    }
}
