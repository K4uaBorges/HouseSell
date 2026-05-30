package main.app

import main.api.httpServer.HousesDataMem
import main.app.config.getOptionalSetting
import main.app.config.getRequiredSetting
import main.app.config.loadDotEnv
import main.api.httpServer.HousesRouter
import main.api.httpServer.HousesWebApi

fun main() {
    val dotEnv = loadDotEnv()
    val jdbcDatabaseUrl = getRequiredSetting("JDBC_DATABASE_URL", dotEnv)
    val databaseUser =
        getOptionalSetting("DATABASE_USER", dotEnv)
            ?: getOptionalSetting("DATABASE_NAME", dotEnv)
    val databasePass = getOptionalSetting("DATABASE_PASS", dotEnv)
    val rawPort = getOptionalSetting("PORT", dotEnv)

    val port =
        rawPort
            ?.toIntOrNull()
            ?: 8080

    val usingDatabase = jdbcDatabaseUrl.isNotBlank()
    val services = HousesDataMem.services(jdbcDatabaseUrl, databaseUser, databasePass)

    if (usingDatabase) {
        println("Starting with PostgreSQL persistence at $jdbcDatabaseUrl")
    } else {
        println("Starting with in-memory persistence. Define JDBC_DATABASE_URL to persist data in PostgreSQL.")
    }

    val server = HousesRouter(HousesWebApi(services), port).start()
    println("Houses server running on port ${server.port()}")
}
