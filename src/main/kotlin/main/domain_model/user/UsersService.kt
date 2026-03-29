package main.domain_model.user

import main.api.dto.GetUserResponse
import main.data.interfaces.UsersRepository
import main.errors.NoUserExist
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class UsersService(
    private val repo: UsersRepository,
) {
    fun createUser(
        nameRaw: String,
        emailRaw: String,
    ): User {
        val name = normalizeName(nameRaw)
        val email = Email.of(emailRaw)
        requireEmailAvailable(email.value)

        val user =
            User(
                id = Uuid.random(),
                name = name,
                email = email,
                token = Uuid.random(),
            )

        return repo.create(user)
    }

    fun getUserById(id: Uuid): User = repo.getById(id)

    fun getUserByToken(token: Uuid): User = repo.getByToken(token)

    fun listUsers(): List<User> = repo.getAll().sortedBy { it.name.value }

    fun updateUser(
        user: User,
        nameRaw: String,
        emailRaw: String,
    ): User {
        val name = normalizeName(nameRaw)
        val email = Email.of(emailRaw)
        requireEmailAvailable(email.value, currentUserId = user.id.toString())
        return repo.update(user.copy(name = name, email = email))
    }

    fun deleteUser(user: User) = repo.deleteById(user.id)

    fun updateUserById(idRaw: String, nameRaw: String, emailRaw: String): User {
        val user = requireExistingUser(idRaw)
        return updateUser(user, nameRaw, emailRaw)
    }

    fun deleteUserById(idRaw: String) {
        val user = requireExistingUser(idRaw)
        deleteUser(user)
    }

    private fun normalizeName(nameRaw: String): Name = Name.of(nameRaw)

    private fun requireEmailAvailable(email: String, currentUserId: String? = null) {
        val found = runCatching { repo.getByEmail(email) }
            .getOrElse {
                if (it is NoUserExist) return
                throw it
            }
        require(found.id.toString() == currentUserId) { "Email already exist." }
    }

    private fun requireExistingUser(idRaw: String): User {
        val id = parseUuid(idRaw)
        return repo.getById(id)
    }

    private fun parseUuid(raw: String): Uuid =
        runCatching { Uuid.parse(raw.trim()) }
            .getOrElse { throw IllegalArgumentException("Invalid user id.") }
}

@OptIn(ExperimentalUuidApi::class)
fun User.toGetUserResponse() = GetUserResponse(
    id = id.toString(),
    name = name.value,
    email = email.value,
)
