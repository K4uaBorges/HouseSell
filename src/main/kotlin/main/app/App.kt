package main.app

import main.api.http_server.HousesDataMem
import main.api.http_server.HousesRouter
import main.api.http_server.HousesWebApi
import java.net.URI

private const val DEFAULT_HTTP_PORT = 8080

fun main() {
    val jdbcDatabaseUrl = System.getenv("JDBC_DATABASE_URL")
    val port = resolveHttpPort(System.getenv("PORT"), jdbcDatabaseUrl)

    val services = HousesDataMem.services(jdbcDatabaseUrl)

    val server = HousesRouter(HousesWebApi(services), port)
    server.start()
    println("Houses server running on port $port")

}

private fun resolveHttpPort(
    portRaw: String?,
    jdbcDatabaseUrl: String?,
): Int {
    val configuredPort = portRaw?.toIntOrNull() ?: DEFAULT_HTTP_PORT
    val databasePort = extractJdbcPort(jdbcDatabaseUrl)

    if (databasePort == null || databasePort != configuredPort) {
        return configuredPort
    }

    if (configuredPort != DEFAULT_HTTP_PORT) {
        System.err.println(
            "Warning: PORT=$configuredPort conflicts with JDBC_DATABASE_URL port $databasePort. " +
                "Falling back to PORT=$DEFAULT_HTTP_PORT.",
        )
        return DEFAULT_HTTP_PORT
    }

    System.err.println(
        "Warning: PORT=$configuredPort matches JDBC_DATABASE_URL port $databasePort. " +
            "Adjust environment variables to avoid port conflict.",
    )
    return configuredPort
}

private fun extractJdbcPort(jdbcDatabaseUrl: String?): Int? {
    val raw = jdbcDatabaseUrl?.trim().orEmpty()
    if (raw.isEmpty()) return null

    val uriRaw = raw.removePrefix("jdbc:")
    val parsed = runCatching { URI.create(uriRaw) }.getOrNull() ?: return null
    return parsed.port.takeIf { it > 0 }
}
