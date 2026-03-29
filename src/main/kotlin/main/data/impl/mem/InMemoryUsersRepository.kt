package main.data.impl.mem

import main.data.interfaces.UsersRepository
import main.domain_model.user.User
import main.errors.NoUserExist
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object InMemoryUsersRepository : UsersRepository {
    private val usersById = mutableMapOf<String, User>()
    private val usersByToken = mutableMapOf<Uuid, User>()
    private val userIdByEmail = mutableMapOf<String, String>()

    override fun create(value: User): User {
        index(value)
        return value
    }

    override fun save(value: User): User {
        val id = value.id.toString()
        usersById[id]?.let { removeIndexes(it) } ?: throw NoUserExist("User not found.")
        index(value)
        return value
    }

    override fun update(updated: User): User =
        save(updated)

    override fun deleteById(key: Uuid) {
        val removed = usersById.remove(key.toString()) ?: throw NoUserExist("User not found.")
        removeIndexes(removed)
    }

    override fun getByEmail(email: String): User =
        userIdByEmail[email]?.let(usersById::get) ?: throw NoUserExist("User not found.")

    override fun getById(key: Uuid): User = usersById[key.toString()] ?: throw NoUserExist("User not found.")

    override fun getByToken(token: Uuid): User = usersByToken[token] ?: throw NoUserExist("User not found.")

    override fun getAll(): List<User> = usersById.values.toList()

    private fun index(user: User) {
        val id = user.id.toString()
        usersById[id] = user
        usersByToken[user.token] = user
        userIdByEmail[user.email.value] = id
    }

    private fun removeIndexes(user: User) {
        usersByToken.remove(user.token)
        userIdByEmail.remove(user.email.value)
    }

    override fun clear() {
        usersById.clear()
        usersByToken.clear()
        userIdByEmail.clear()
    }
}
