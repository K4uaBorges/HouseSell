package main.data.interfaces

import main.domain.user.User
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface UsersRepository : Repository<Uuid, User> {
    override fun create(value: User): User

    override fun getById(key: Uuid): User

    override fun getAll(): List<User>

    override fun deleteById(key: Uuid)

    override fun save(value: User): User

    override fun update(updated: User): User

    override fun clear()

    fun getByToken(token: Uuid): User

    fun getByEmail(email: String): User
}
