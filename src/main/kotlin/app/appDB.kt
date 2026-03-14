package app

import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.LoggerFactory
import utils.getOptionalSetting
import utils.getRequiredSetting
import utils.loadDotEnv


val logger = LoggerFactory.getLogger("main")

fun main() {
    val dotEnv = loadDotEnv()

    val dataSource = PGSimpleDataSource()
    dataSource.setURL(getRequiredSetting("JDBC_DATABASE_URL", dotEnv))

    val dbUser = getOptionalSetting("DATABASE_USER", dotEnv) ?: getOptionalSetting("DATABASE_NAME", dotEnv)
    dbUser?.let(dataSource::setUser)

    val dbPass = getOptionalSetting("DATABASE_PASS", dotEnv)
    dbPass?.let(dataSource::setPassword)

    dataSource.getConnection().use {
        val stm = it.prepareStatement("select * from users")
        val rs = stm.executeQuery()
        while (rs.next()) {
            logger.info(rs.getString("name"))
        }
    }
}
