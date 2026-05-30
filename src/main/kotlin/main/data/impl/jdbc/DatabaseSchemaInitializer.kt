package main.data.impl.jdbc

import java.sql.SQLException
import javax.sql.DataSource

object DatabaseSchemaInitializer {
    private const val SCHEMA_RESOURCE_PATH = "sql/createSchema.sql"

    fun ensureSchema(dataSource: DataSource) {
        val schemaSql =
            DatabaseSchemaInitializer::class.java.classLoader
                .getResource(SCHEMA_RESOURCE_PATH)
                ?.readText()
                ?: throw IllegalStateException("Schema resource not found: $SCHEMA_RESOURCE_PATH")

        try {
            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute(schemaSql)
                }
            }
        } catch (error: SQLException) {
            throw IllegalStateException("Failed to initialize database schema.", error)
        }
    }
}
