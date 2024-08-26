package ch.bziswiler.pvlogger.heartbeat.database

interface DatabaseFactory {
    fun connect()
    fun close()
}
