package domain.user.repository

import domain.user.Email
import domain.user.User
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class JdbcUsersRepository(
    private val dataSource: DataSource,
) : UsersRepository {

    override fun create(user: User): User {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                insert into users (uid, name, email, token)
                values (?, ?, ?, ?)
                """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, UUID.fromString(user.id.toString()))
                stmt.setString(2, user.name)
                stmt.setString(3, user.email.value)
                stmt.setObject(4, UUID.fromString(user.token.toString()))
                stmt.executeUpdate()
            }
        }
        return user
    }

    override fun update(user: User): User {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                update users
                set name = ?, email = ?, token = ?
                where uid = ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, user.name)
                stmt.setString(2, user.email.value)
                stmt.setObject(3, UUID.fromString(user.token.toString()))
                stmt.setObject(4, UUID.fromString(user.id.toString()))
                stmt.executeUpdate()
            }
        }
        return user
    }

    override fun delete(user: User) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("delete from users where uid = ?").use { stmt ->
                stmt.setObject(1, UUID.fromString(user.id.toString()))
                stmt.executeUpdate()
            }
        }
    }

    override fun getById(id: String): User? = querySingle(
        sql = "select uid, name, email, token from users where uid = ?",
        binder = { it.setObject(1, UUID.fromString(id.trim())) },
    )

    override fun getByToken(token: String): User? = querySingle(
        sql = "select uid, name, email, token from users where token = ?",
        binder = { it.setObject(1, UUID.fromString(token.trim())) },
    )

    override fun getByEmail(email: String): User? = querySingle(
        sql = "select uid, name, email, token from users where email = ?",
        binder = { it.setString(1, email.trim()) },
    )

    override fun getAll(): List<User> {
        dataSource.connection.use { conn ->
            conn.prepareStatement("select uid, name, email, token from users").use { stmt ->
                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<User>()
                    while (rs.next()) {
                        result += mapUser(rs)
                    }
                    return result
                }
            }
        }
    }

    private fun querySingle(sql: String, binder: (java.sql.PreparedStatement) -> Unit): User? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                binder(stmt)
                stmt.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return mapUser(rs)
                }
            }
        }
    }

    private fun mapUser(rs: ResultSet): User {
        return User(
            id = Uuid.parse(rs.getObject("uid", UUID::class.java).toString()),
            name = rs.getString("name"),
            email = Email.of(rs.getString("email")),
            token = Uuid.parse(rs.getObject("token", UUID::class.java).toString()),
        )
    }
}
