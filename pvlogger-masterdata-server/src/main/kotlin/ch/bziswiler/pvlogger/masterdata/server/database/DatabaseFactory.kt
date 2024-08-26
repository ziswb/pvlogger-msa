package ch.bziswiler.pvlogger.masterdata.server.database

interface DatabaseFactory {
    fun connect()

    fun close()
}
