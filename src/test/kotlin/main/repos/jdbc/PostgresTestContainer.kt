package main.repos.jdbc

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class PostgresTestContainer {
    companion object {
        @Container
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(
                DockerImageName.parse("postgres:16-alpine"),
            )
                .withDatabaseName("test_db")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true) // Reutilizar container entre testes para performance

        @JvmStatic
        @BeforeAll
        fun startContainer() {
            postgres.start()
        }

        @JvmStatic
        @AfterAll
        fun stopContainer() {
            postgres.stop()
        }
    }

    protected lateinit var dataSource: PGSimpleDataSource

    @BeforeAll
    fun setupDataSource() {
        dataSource =
            PGSimpleDataSource().apply {
                setURL(postgres.jdbcUrl)
                user = postgres.username
                password = postgres.password
            }
        initSchema()
    }

    @BeforeEach
    fun cleanupTables() {
        // Limpar dados antes de cada teste para isolamento
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("TRUNCATE TABLE booking, houses, locations, users CASCADE")
            }
        }
    }

    private fun initSchema() {
        val schemaSql =
            javaClass.classLoader.getResource("sql/createSchema.sql")?.readText()
                ?: throw IllegalStateException("Schema file not found")

        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(schemaSql)
            }
        }
    }
}
