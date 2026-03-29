package main.data.impl.jdbc

import main.domain_model.user.Email
import main.domain_model.user.Name
import main.domain_model.user.User
import main.errors.NoUserExist
import main.errors.UsersRepositoryDatabaseException
import main.data.interfaces.UsersRepository
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import javax.sql.DataSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class JdbcUsersRepository(
    private val dataSource: DataSource,
) : UsersRepository {
    override fun create(value: User): User =
        withDatabaseHandling("creating user") {
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        insert into users (uid, name, email, token)
                        values (?, ?, ?, ?)
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setObject(1, toJavaUuid(value.id))
                        stmt.setString(2, value.name.value)
                        stmt.setString(3, value.email.value)
                        stmt.setObject(4, toJavaUuid(value.token))
                        stmt.executeUpdate()
                    }
            }
            value
        }

    override fun save(value: User): User =
        withDatabaseHandling("updating user") {
            getById(value.id)
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        update users
                        set name = ?, email = ?, token = ?
                        where uid = ?
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setString(1, value.name.value)
                        stmt.setString(2, value.email.value)
                        stmt.setObject(3, toJavaUuid(value.token))
                        stmt.setObject(4, toJavaUuid(value.id))
                        val updated = stmt.executeUpdate()
                        if (updated == 0) throw NoUserExist("User not found.")
                    }
            }
            value
        }

    override fun update(updated: User): User = save(updated)

    override fun deleteById(key: Uuid) {
        withDatabaseHandling("deleting user") {
            dataSource.connection.use { conn ->
                conn.prepareStatement("delete from users where uid = ?").use { stmt ->
                    stmt.setObject(1, toJavaUuid(key))
                    val deleted = stmt.executeUpdate()
                    if (deleted == 0) throw NoUserExist("User not found.")
                }
            }
        }
    }

    override fun getById(key: Uuid): User =
        querySingle(
            sql = "select uid, name, email, token from users where uid = ?",
            binder = { it.setObject(1, toJavaUuid(key)) },
        )

    override fun getByToken(token: Uuid): User =
        querySingle(
            sql = "select uid, name, email, token from users where token = ?",
            binder = { it.setObject(1, toJavaUuid(token)) },
        )

    override fun getByEmail(email: String): User =
        querySingle(
            sql = "select uid, name, email, token from users where email = ?",
            binder = { it.setString(1, email.trim()) },
        )

    override fun getAll(): List<User> =
        withDatabaseHandling("listing users") {
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                    |select uid, name, email, token from users
                        """.trimMargin(),
                    ).use { stmt ->
                        stmt.executeQuery().use { rs ->
                            val result = mutableListOf<User>()
                            while (rs.next()) {
                                result.plusAssign(mapUser(rs))
                            }
                            result
                        }
                    }
            }
        }

    override fun clear() {
        withDatabaseHandling("clearing users") {
            dataSource.connection.use { conn ->
                conn.prepareStatement("delete from users").use { stmt ->
                    stmt.executeUpdate()
                }
            }
        }
    }

    private fun querySingle(
        sql: String,
        binder: (PreparedStatement) -> Unit,
    ): User =
        withDatabaseHandling("querying user") {
            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    binder(stmt)
                    stmt.executeQuery().use { rs ->
                        if (!rs.next()) throw NoUserExist("User not found.")
                        mapUser(rs)
                    }
                }
            }
        }

    private fun mapUser(rs: ResultSet): User =
        User(
            id = Uuid.parse(rs.getObject("uid", UUID::class.java).toString()),
            name = Name.of(rs.getString("name")),
            email = Email.of(rs.getString("email")),
            token = Uuid.parse(rs.getObject("token", UUID::class.java).toString()),
        )

    private fun <T> withDatabaseHandling(
        operation: String,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (error: SQLException) {
            throw UsersRepositoryDatabaseException("Database error while $operation.", error)
        }

    private fun toJavaUuid(value: Uuid): UUID = UUID.fromString(value.toString())
}
