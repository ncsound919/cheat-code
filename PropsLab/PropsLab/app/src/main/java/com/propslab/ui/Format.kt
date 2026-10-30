package com.propslab.ui

import com.propslab.domain.Odds
import com.propslab.settings.AppSettings
import com.propslab.settings.OddsFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val zone: ZoneId get() = ZoneId.systemDefault()

/** Built per call so changing the device locale repaints the next timestamp correctly. */
private fun formatter(pattern: String): DateTimeFormatter =
    DateTimeFormatter.ofPattern(pattern, Locale.getDefault())

/**
 * The price, in the format the settings screen selected. Decimal is the raw multiplier
 * (\u2212110 \u2192 1.91); American is the sportsbook's own voice. Read during composition, so
 * flipping the switch repaints every row that shows a price.
 */
fun fmtOdds(odds: Int?): String = when {
    odds == null -> "none"
    AppSettings.current.oddsFormat == OddsFormat.DECIMAL ->
        "%.2f".format(Odds.americanToDecimal(odds))
    odds > 0 -> "+$odds"
    else -> "\u2212${-odds}"
}

fun fmtPct(p: Double, digits: Int = 1): String = "%.${digits}f%%".format(p * 100)

fun fmtSigned(v: Double, digits: Int = 1): String {
    val body = "%.${digits}f".format(abs(v))
    return if (v >= 0) "+$body" else "\u2212$body"
}

fun fmtLine(line: Double?): String = line?.let { if (it % 1.0 == 0.0) "%.1f".format(it) else it.toString() } ?: "none"

fun fmtTime(utc: Long): String = formatter("h:mm a").format(Instant.ofEpochMilli(utc).atZone(zone))
fun fmtDay(utc: Long): String = formatter("EEE, MMM d").format(Instant.ofEpochMilli(utc).atZone(zone))
fun fmtStamp(utc: Long): String = formatter("MMM d, h:mm a").format(Instant.ofEpochMilli(utc).atZone(zone))

fun fmtAgo(utc: Long, now: Long = System.currentTimeMillis()): String {
    val m = (now - utc) / 60_000
    return when {
        m < 1 -> "just now"
        m < 60 -> "$m min ago"
        m < 24 * 60 -> "${m / 60} h ago"
        else -> fmtStamp(utc)
    }
}

fun initials(name: String): String = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
