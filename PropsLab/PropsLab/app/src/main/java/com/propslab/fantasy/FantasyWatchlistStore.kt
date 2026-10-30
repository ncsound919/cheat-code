package com.propslab.fantasy

import android.content.Context

/** The watchlist on this phone. One private preference, encoded by [FantasyWatchlist]. */
class FantasyWatchlistStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("fantasy_desk", Context.MODE_PRIVATE)

    fun load(): Set<String> = FantasyWatchlist.decode(prefs.getString(KEY, ""))

    fun save(keys: Set<String>) {
        prefs.edit().putString(KEY, FantasyWatchlist.encode(keys)).apply()
    }

    private companion object {
        const val KEY = "watchlist"
    }
}
