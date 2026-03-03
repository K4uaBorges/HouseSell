package domain.user

import domain.user.repository.UsersRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class UsersService(private val repo: UsersRepository){

    fun createUser(nameRaw: String, emailRaw: String): User {
        val name = normalizeName(nameRaw)
        val email = Email.of(emailRaw)
        requireEmailAvailable(email.value)

        val user = User(
            id = Uuid.random(),
            name = name,
            email = email,
            token = Uuid.random()
        )

        return repo.create(user)
    }

    fun getUserById(id: Uuid): User? = repo.getById(id.toString())
    fun getUserByToken(token: Uuid): User? = repo.getByToken(token.toString())

    fun updateUser(user: User, nameRaw: String, emailRaw: String): User {
        val name = normalizeName(nameRaw)
        val email = Email.of(emailRaw)
        requireEmailAvailable(email.value, currentUserId = user.id.toString())
        return repo.update(user.copy(name = name, email = email))
    }

    fun deleteUser(user: User) = repo.delete(user)

    private fun normalizeName(nameRaw: String): String {
        val name = nameRaw.trim()
        require(name.length in 3..20) { "Name Invalid" }
        return name
    }

    private fun requireEmailAvailable(email: String, currentUserId: String? = null) {
        val found = repo.getByEmail(email) ?: return
        require(found.id.toString() == currentUserId) { "Email already exist." }
    }
}
