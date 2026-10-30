package com.propslab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.propslab.data.FantasyLeagueWithData
import com.propslab.data.FantasyRosterEntity
import com.propslab.fantasy.FantasyAssets
import com.propslab.fantasy.FantasyCall
import com.propslab.fantasy.FantasyDeskUi
import com.propslab.fantasy.FantasyInjury
import com.propslab.fantasy.FantasyLeagueDesk
import com.propslab.fantasy.FantasyPlayer
import com.propslab.fantasy.LeaguePlayer
import com.propslab.fantasy.LeaguePlayerWriteUp
import com.propslab.fantasy.ProTeams
import com.propslab.feed.Names
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.Panel
import com.propslab.ui.components.RemoteImage
import com.propslab.ui.components.TeamAvatar
import com.propslab.ui.initials
import com.propslab.ui.theme.Palette
import java.util.Locale

/**
 * The captured league sits above the generic ESPN desk.
 *
 * The two are deliberately separate: the desk is form across the whole sport, while this
 * block knows the user's slots, bench and opponent. The screenshot's projections and the
 * public ESPN form are labelled where each one is used, so a missing number is visible
 * instead of being filled with a confident-looking guess.
 */
@Composable
fun FantasyLeagueSection(
    data: FantasyLeagueWithData?,
    desk: FantasyDeskUi,
    onCapture: () -> Unit,
) {
    Column(Modifier.padding(top = 18.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("YOUR LEAGUE", style = MaterialTheme.typography.labelSmall, color = Palette.Over)
                Spacer(Modifier.height(3.dp))
                Text(
                    data?.league?.name?.ifBlank { "Captured league" } ?: "Capture once, use all season",
                    style = MaterialTheme.typography.titleMedium,
                    color = Palette.Chalk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (data != null) {
                Badge("WEEK ${data.league.week}", Palette.Over, filled = true)
            }
        }
        Spacer(Modifier.height(9.dp))

        if (data == null) {
            Panel(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "Screenshot your lineup and your opponent's. Cheat Code reads the slots, names, pro teams " +
                        "and any projected points, then gives every player a start/sit call with the reason.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.Dust,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Import the league table once and the app keeps the standings and each week's lineup " +
                        "as the season moves. The screenshot stays on your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Faint,
                )
                Spacer(Modifier.height(14.dp))
                ActionButton("Capture my league", onCapture, tone = Palette.Over)
            }
            return@Column
        }

        val myTeam = data.league.myTeam.ifBlank {
            data.teams.firstOrNull { it.isMine }?.name.orEmpty()
        }.ifBlank {
            data.roster.maxByOrNull { it.week }?.teamName.orEmpty()
        }
        val week = data.league.week
        val snapshot = data.weeks.firstOrNull { it.week == week }
        val opponentName = snapshot?.opponent.orEmpty()
        val roster = if (myTeam.isBlank()) emptyList() else data.rosterFor(week, myTeam)
        val opponentRoster = if (opponentName.isBlank()) emptyList() else {
            data.rosterFor(week, opponentName)
        }
        if (roster.isEmpty()) {
            Panel(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    if (myTeam.isBlank()) {
                        "The league is saved. Capture your lineup so the desk can read your slots and bench."
                    } else {
                        "No players are stored for $myTeam in week $week. Capture that lineup or correct the team name."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.Dust,
                )
                Spacer(Modifier.height(14.dp))
                ActionButton("Capture my lineup", onCapture, tone = Palette.Over)
            }
            if (data.teams.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Standings(data)
            }
            return@Column
        }

        val myPlayers = leaguePlayers(roster, desk, data.league.sport)
        val opponentPlayers = leaguePlayers(opponentRoster, desk, data.league.sport)
        val brief = FantasyLeagueDesk.build(week, myPlayers, opponentPlayers, opponentName)

        Panel(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(myTeam, style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
                    Text(
                        "${myPlayers.count { it.isStarter }} starters · ${myPlayers.count { !it.isStarter }} bench",
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.Faint,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        brief.startersProjected?.let { "%.1f".format(it) } ?: "—",
                        style = MaterialTheme.typography.displaySmall,
                        color = Palette.Over,
                    )
                    Text("projected starters", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Palette.Hairline)
            Spacer(Modifier.height(10.dp))
            if (opponentName.isNotBlank()) {
                val opp = brief.opponentProjected
                val edge = brief.edge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "vs $opponentName",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        color = Palette.Chalk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (opp != null) "%.1f".format(opp) else "no projection",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (opp != null) Palette.Over else Palette.Faint,
                    )
                }
                if (edge != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "%s%.1f on the current projections".format(if (edge >= 0) "+" else "", edge),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (edge >= 0) Palette.Gain else Palette.Loss,
                    )
                }
            } else {
                Text(
                    "No opponent lineup for week $week yet. Capture it to compare the two projected totals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Dust,
                )
            }
            Spacer(Modifier.height(12.dp))
            ActionButton("Capture or refresh lineup", onCapture, tone = Palette.Over)
        }

        Spacer(Modifier.height(8.dp))
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                "EVERY PLAYER, EVERY CALL",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.Over,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "The short reason under each name is generated from the slot, the projection and the " +
                    "public box-score form — not from a promise about the game.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Faint,
            )
        }
        Spacer(Modifier.height(4.dp))
        brief.writeUps.forEach { writeUp ->
            LeaguePlayerCard(writeUp)
        }

        if (brief.insights.isNotEmpty()) {
            Panel(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), tone = Palette.Raised) {
                Text("WEEK $week READ", style = MaterialTheme.typography.labelSmall, color = Palette.Over)
                Spacer(Modifier.height(6.dp))
                brief.insights.forEach { insight ->
                    Text(
                        "•  $insight",
                        Modifier.padding(vertical = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Dust,
                    )
                }
            }
        }

        if (data.teams.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Standings(data)
        }
        if (data.weeks.size > 1) {
            Spacer(Modifier.height(8.dp))
            SeasonTrack(data)
        }
        if (opponentName.isBlank() || opponentRoster.isEmpty()) {
            Text(
                "Tip: capture the opponent's lineup in the same week to turn the single-team calls into a matchup.",
                Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Faint,
            )
        }
    }
}

private fun leaguePlayers(
    rows: List<FantasyRosterEntity>,
    desk: FantasyDeskUi,
    sport: String,
): List<LeaguePlayer> {
    val recent = desk.players.associateBy { it.key }
    val injuries = (desk.injuries + desk.players.mapNotNull { it.injury })
        .associateBy { Names.key(it.playerName) }
    val fantasySport = com.propslab.fantasy.FantasySport.fromSlug(sport)
    return rows.map { row ->
        val player = recent[row.playerKey]
        val injury = injuries[row.playerKey] ?: player?.injury
        LeaguePlayer(
            key = row.playerKey,
            name = row.playerName,
            slot = row.slot,
            position = row.position,
            proTeam = row.proTeam,
            opponent = row.opponent,
            projected = row.projected,
            seasonAverage = row.seasonPoints,
            recent = player,
            injury = injury,
            headshotUrl = row.headshotUrl.ifBlank {
                if (fantasySport != null && row.athleteId.isNotBlank()) {
                    FantasyAssets.headshotUrl(fantasySport, row.athleteId)
                } else {
                    ""
                }
            },
            teamLogoUrl = row.teamLogoUrl.ifBlank {
                if (fantasySport != null && row.proTeam.isNotBlank()) {
                    ProTeams.logoUrl(row.proTeam, fantasySport)
                } else {
                    ""
                }
            },
        )
    }
}

@Composable
private fun LeaguePlayerCard(writeUp: LeaguePlayerWriteUp) {
    val player = writeUp.player
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Palette.Raised)
                    .border(1.dp, Palette.Hairline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                RemoteImage(
                    url = player.headshotUrl,
                    contentDescription = "${player.name} headshot",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    fallback = {
                        Text(
                            initials(player.name),
                            style = MaterialTheme.typography.labelMedium,
                            color = Palette.Dust,
                        )
                    },
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        player.name,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = Palette.Chalk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(6.dp))
                    Badge(writeUp.call.label.uppercase(Locale.ROOT), callColor(writeUp.call), filled = true)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (player.teamLogoUrl.isNotBlank()) {
                        RemoteImage(
                            url = player.teamLogoUrl,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp),
                            fallback = {},
                        )
                    }
                    Text(
                        listOf(player.slot.ifBlank { "slot not read" }, player.proTeam, player.opponent)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.Faint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        writeUp.projected?.let { "%.1f".format(it) } ?: "—",
                        style = MaterialTheme.typography.titleSmall,
                        color = Palette.Over,
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    writeUp.headline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.Chalk,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    writeUp.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Dust,
                )
            }
        }
    }
}

@Composable
private fun Standings(data: FantasyLeagueWithData) {
    val teams = data.teams.sortedWith(
        compareBy<com.propslab.data.FantasyTeamEntity> { if (it.rank > 0) it.rank else 99 }
            .thenByDescending { it.wins }
            .thenBy { it.name },
    ).take(8)
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("STANDINGS", style = MaterialTheme.typography.labelSmall, color = Palette.Over)
        Spacer(Modifier.height(3.dp))
        Text(
            "Read from the league table screenshot. A record is shown as printed; nothing is recomputed.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.Faint,
        )
        Spacer(Modifier.height(6.dp))
        teams.forEachIndexed { index, team ->
            if (index > 0) HorizontalDivider(color = Palette.Hairline, modifier = Modifier.padding(vertical = 6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (team.rank > 0) "${team.rank}" else "${index + 1}",
                    Modifier.width(22.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.Faint,
                )
                TeamAvatar(team.name, team.name, 26.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        team.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (team.isMine) Palette.Over else Palette.Chalk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (team.pointsFor != null) {
                        Text(
                            "%.1f points for".format(team.pointsFor),
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.Faint,
                        )
                    }
                }
                Text(
                    team.record,
                    style = MaterialTheme.typography.labelMedium,
                    color = Palette.Dust,
                )
            }
        }
    }
}

@Composable
private fun SeasonTrack(data: FantasyLeagueWithData) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("SEASON TRACK", style = MaterialTheme.typography.labelSmall, color = Palette.Over)
        Spacer(Modifier.height(3.dp))
        Text(
            "Every imported week is kept. Projection is the number at capture time, not a result; " +
                "scores appear only when the screenshot or a later import supplied them.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.Faint,
        )
        Spacer(Modifier.height(6.dp))
        data.weeks.sortedByDescending { it.week }.take(5).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("WEEK ${week.week}", Modifier.width(66.dp), style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Text(
                    week.myTeam.ifBlank { "My team" },
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Chalk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        week.myProjected?.let { "%.1f".format(it) },
                        week.oppProjected?.let { "vs %.1f".format(it) },
                    ).joinToString(" ").ifBlank { "—" },
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.Dust,
                )
            }
        }
    }
}

private fun callColor(call: FantasyCall): androidx.compose.ui.graphics.Color = when (call) {
    FantasyCall.START -> Palette.Gain
    FantasyCall.WATCH -> Palette.Caution
    FantasyCall.SIT -> Palette.Under
    FantasyCall.OUT -> Palette.Loss
}
