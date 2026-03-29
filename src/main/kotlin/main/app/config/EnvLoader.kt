package main.app.config

import org.postgresql.ds.PGSimpleDataSource
import java.io.File
import java.util.*

fun loadDotEnv(): Properties? {
    val projectDir = System.getProperty("user.dir")
    val envFile = File(projectDir, ".env")

    if (!envFile.exists()) {
        println(".env não encontrado em $projectDir")
        return null
    }

    val properties = Properties()
    envFile.inputStream().use(properties::load)
    return properties
}

fun getOptionalSetting(
    name: String,
    dotEnv: Properties?,
): String? =
    System.getenv(name)?.trim()?.takeIf { it.isNotBlank() }
        ?: dotEnv?.getProperty(name)?.trim()?.takeIf { it.isNotBlank() }

fun getRequiredSetting(
    name: String,
    dotEnv: Properties?,
): String =
    getOptionalSetting(name, dotEnv)
        ?: error("$name is not set. Define it in the environment or in .env.")

fun createDataSource(dotEnv: Properties? = loadDotEnv()): PGSimpleDataSource {
    val dataSource = PGSimpleDataSource()
    dataSource.setURL(getRequiredSetting("JDBC_DATABASE_URL", dotEnv))

    val dbUser =
        getOptionalSetting("DATABASE_USER", dotEnv)
            ?: getOptionalSetting("DATABASE_NAME", dotEnv)
            ?: error("DATABASE_USER or DATABASE_NAME must be set")
    dataSource.setUser(dbUser)

    val dbPass =
        getOptionalSetting("DATABASE_PASS", dotEnv)
            ?: error("DATABASE_PASS must be set")
    dataSource.setPassword(dbPass)

    return dataSource
}
