package app

import api.http.HousesDataDb
import api.http.HousesDataMem
import api.http.HousesServer
import api.http.HousesWebApi

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val jdbcDatabaseUrl = System.getenv("JDBC_DATABASE_URL")

    val services = if (jdbcDatabaseUrl.isNullOrBlank()) {
        HousesDataMem.services
    } else {
        HousesDataDb.services(jdbcDatabaseUrl)
    }

    val server = HousesServer(HousesWebApi(services), port)
    server.start()
    println("Houses server running on port $port")
}
