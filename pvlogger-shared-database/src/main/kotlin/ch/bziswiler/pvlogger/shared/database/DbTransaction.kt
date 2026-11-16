package ch.bziswiler.pvlogger.shared.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Runs [block] in a JDBC transaction on [Dispatchers.IO] (backlog 60). JDBC
 * blocks its thread; called directly from a route it would hold one of Ktor's
 * few request threads for the whole query.
 */
suspend fun <T> dbTransaction(block: JdbcTransaction.() -> T): T = withContext(Dispatchers.IO) { transaction { block() } }
