package ch.bziswiler.pvlogger.dataaccessor.database

interface DatabaseFactory {
    fun connect()
    fun close()
}
