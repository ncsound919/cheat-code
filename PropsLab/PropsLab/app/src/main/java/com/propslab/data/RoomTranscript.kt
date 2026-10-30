package com.propslab.data

import com.propslab.agent.ConsoleTranscript

/**
 * The durable transcript.
 *
 * It writes through the same append-only DAO the audit log uses, which is the point: a
 * console turn is a record of what the system did, so it should survive a restart exactly
 * as written.
 */
class RoomTranscript(private val dao: ConsoleDao) : ConsoleTranscript {

    override suspend fun append(turn: ConsoleTurn): Long = dao.append(turn)

    override suspend fun recent(limit: Int): List<ConsoleTurn> = dao.recent(limit)

    override suspend fun clear() = dao.clear()
}
