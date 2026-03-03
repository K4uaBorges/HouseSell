package domain.user.repository
import domain.user.User

interface UsersRepository {
    fun create(user: User): User
    fun update(user: User): User
    fun delete(user: User)
    fun getById(id: String): User?
    fun getByToken(token: String): User?
    fun getByEmail(email: String): User?
    fun getAll(): List<User>
}
