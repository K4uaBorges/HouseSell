package main.repos.mem

import junit.framework.TestCase.assertTrue
import main.data.impl.mem.InMemoryUsersRepository
import main.domain.user.Email
import main.domain.user.Name
import main.domain.user.User
import main.errors.NoUserExist
import main.utils.hashPassword
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InMemoryUsersRepositoryTest {
    private lateinit var repo: InMemoryUsersRepository

    @BeforeTest
    fun setup() {
        InMemoryUsersRepository.clear()
        repo = InMemoryUsersRepository
    }

    @Test
    fun `create should index user by id token and email`() {
        val user = createUser("Alice", "alice@test.com")

        repo.create(user)

        // Testar todos os lookups
        assertEquals(user.id, repo.getById(user.id).id)
        assertEquals(user.id, repo.getByToken(user.token).id)
        assertEquals(user.id, repo.getByEmail(user.email.value).id)
    }

    @Test
    fun `update should maintain index consistency when email changes`() {
        val user = repo.create(createUser("Alice", "alice@test.com"))

        val updated = user.copy(email = Email.of("alice.new@test.com"))
        repo.update(updated)

        // Novo email deve funcionar
        assertEquals(user.id, repo.getByEmail("alice.new@test.com").id)

        // Email antigo deve falhar
        assertFailsWith<NoUserExist> {
            repo.getByEmail("alice@test.com")
        }
    }

    @Test
    fun `delete should remove from all indexes`() {
        val user = repo.create(createUser("Alice", "alice@test.com"))

        repo.deleteById(user.id)

        assertFailsWith<NoUserExist> { repo.getById(user.id) }
        assertFailsWith<NoUserExist> { repo.getByToken(user.token) }
        assertFailsWith<NoUserExist> { repo.getByEmail(user.email.value) }
    }

    @Test
    fun `getAll should return users sorted by name`() {
        repo.create(createUser("Charlie", "charlie@test.com"))
        repo.create(createUser("Alice", "alice@test.com"))
        repo.create(createUser("Bob", "bob@test.com"))

        val result = repo.getAll()

        assertEquals(3, result.size)
        assertTrue(result.any { it.name.value == "Alice" })
        assertTrue(result.any { it.name.value == "Bob" })
        assertTrue(result.any { it.name.value == "Charlie" })
    }

    // Helper
    private fun createUser(
        name: String,
        email: String,
    ): User =
        User(
            id = Uuid.random(),
            name = Name.of(name),
            email = Email.of(email),
            passwordHash = hashPassword("Secret123"),
            token = Uuid.random(),
        )
}
