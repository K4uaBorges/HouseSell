package main.app

import main.api.http_server.HousesDataMem
import main.api.http_server.HousesRouter
import main.api.http_server.HousesWebApi
import org.http4k.routing.ResourceLoader
import org.http4k.routing.singlePageApp

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val jdbcDatabaseUrl = System.getenv("JDBC_DATABASE_URL")

    val services = HousesDataMem.services(jdbcDatabaseUrl)

    val server = HousesRouter(HousesWebApi(services), port)
    server.start()
    println("Houses server running on port $port")
//    singlePageApp(ResourceLoader()) Iniciação da segunda fase

}
