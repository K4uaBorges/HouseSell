package domain.user.repository

import domain.user.User
import kotlin.uuid.ExperimentalUuidApi


@OptIn(ExperimentalUuidApi::class)
object InMemoryUsersRepository : UsersRepository {
    private val usersById = mutableMapOf<String, User>()
    private val usersByToken = mutableMapOf<String, User>()
    private val userIdByEmail = mutableMapOf<String, String>()

    override fun create(user: User): User {
        index(user)
        return user
    }

    override fun update(user: User): User {
        val id = user.id.toString()
        usersById[id]?.let { removeIndexes(it) }
        index(user)
        return user
    }


    override fun delete(user: User) {
        val removed = usersById.remove(user.id.toString()) ?: return
        removeIndexes(removed)
    }

    override fun getByEmail(email: String): User? = userIdByEmail[email]?.let(usersById::get)
    override fun getById(id: String) = usersById[id]
    override fun getByToken(token: String) = usersByToken[token]
    override fun getAll(): List<User> = usersById.values.toList()

    private fun index(user: User) {
        val id = user.id.toString()
        usersById[id] = user
        usersByToken[user.token.toString()] = user
        userIdByEmail[user.email.value] = id
    }

    private fun removeIndexes(user: User) {
        usersByToken.remove(user.token.toString())
        userIdByEmail.remove(user.email.value)
    }

    fun clear() {
        usersById.clear()
        usersByToken.clear()
        userIdByEmail.clear()
    }
}
