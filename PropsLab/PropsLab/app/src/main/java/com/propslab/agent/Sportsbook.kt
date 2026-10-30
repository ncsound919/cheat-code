package com.propslab.agent

/**
 * The three books Cheat Code knows about.
 *
 * Researched 2026-10-29 (sources in `research/BETTING-INTEGRATION.md`): none of the
 * three publishes a public API that can place a wager, all three prohibit automated
 * wagering in their terms, and none documents a deep link that prefills a bet slip.
 * So the only honest handoff is the book's own official site, opened for the human.
 */
enum class Sportsbook(
    val slug: String,
    val displayName: String,
    val homeUrl: String,
) {
    DRAFTKINGS("draftkings", "DraftKings", "https://sportsbook.draftkings.com/"),
    PRIZEPICKS("prizepicks", "PrizePicks", "https://www.prizepicks.com/"),
    FANATICS("fanatics", "Fanatics Sportsbook", "https://sportsbook.fanatics.com/"),
    ;

    companion object {
        fun parse(raw: String): Sportsbook? {
            val value = raw.trim()
            return entries.firstOrNull {
                it.slug.equals(value, ignoreCase = true) ||
                    it.name.equals(value, ignoreCase = true) ||
                    it.displayName.equals(value, ignoreCase = true)
            }
        }
    }
}
