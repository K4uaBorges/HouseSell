package main.repos.jdbc

import main.data.impl.jdbc.JdbcUsersRepository
import main.domain.user.Email
import main.domain.user.Name
import main.domain.user.User
import main.errors.NoUserExist
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class JdbcUsersRepositoryTest : PostgresTestContainer() {
    private lateinit var repository: JdbcUsersRepository

    @BeforeAll
    fun setup() {
        repository = JdbcUsersRepository(dataSource)
    }

    @Test
    fun `create should insert user and return same user`() {
        val user = createTestUser()

        val result = repository.create(user)

        assertEquals(user.id, result.id)
        assertEquals(user.name, result.name)
        assertEquals(user.email.value, result.email.value)
        assertEquals(user.token, result.token)
    }

    @Test
    fun `create should persist user that can be retrieved by id`() {
        val user = createTestUser()
        repository.create(user)

        val retrieved = repository.getById(user.id)

        assertEquals(user.id, retrieved.id)
        assertEquals(user.name, retrieved.name)
        assertEquals(user.email.value, retrieved.email.value)
        assertEquals(user.token, retrieved.token)
    }

    @Test
    fun `create should persist user that can be retrieved by token`() {
        val user = createTestUser()
        repository.create(user)

        val retrieved = repository.getByToken(user.token)

        assertEquals(user.id, retrieved.id)
    }

    @Test
    fun `create should persist user that can be retrieved by email`() {
        val user = createTestUser()
        repository.create(user)

        val retrieved = repository.getByEmail(user.email.value)

        assertEquals(user.id, retrieved.id)
    }

    @Test
    fun `create should allow multiple users with different emails`() {
        val user1 = createTestUser(name = "Alice", email = "alice@test.com")
        val user2 = createTestUser(name = "Bob", email = "bob@test.com")

        repository.create(user1)
        repository.create(user2)

        assertEquals(2, repository.getAll().size)
    }

    @Test
    fun `create should fail when email already exists`() {
        val user1 = createTestUser(email = "duplicate@test.com")
        repository.create(user1)

        val user2 = createTestUser(email = "duplicate@test.com")

        // PostgreSQL deve lançar exceção de constraint violation
        assertThrows<Exception> {
            repository.create(user2)
        }
    }

    @Test
    fun `getById should throw NoUserExist when user not found`() {
        val randomId = Uuid.random()

        val exception =
            assertThrows<NoUserExist> {
                repository.getById(randomId)
            }

        assertTrue(exception.message!!.contains("not found"))
    }

    @Test
    fun `getByToken should throw NoUserExist when token not found`() {
        val randomToken = Uuid.random()

        assertThrows<NoUserExist> {
            repository.getByToken(randomToken)
        }
    }

    @Test
    fun `getByEmail should throw NoUserExist when email not found`() {
        assertThrows<NoUserExist> {
            repository.getByEmail("nonexistent@test.com")
        }
    }

    @Test
    fun `getAll should return empty list when no users`() {
        val result = repository.getAll()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAll should return all created users`() {
        val user1 = createTestUser(name = "Charlie", email = "charlie@test.com")
        val user2 = createTestUser(name = "Alice", email = "alice@test.com")
        val user3 = createTestUser(name = "Bob", email = "bob@test.com")

        repository.create(user1)
        repository.create(user2)
        repository.create(user3)

        val result = repository.getAll()

        assertEquals(3, result.size)
        assertTrue(result.any { it.name.value == "Alice" })
        assertTrue(result.any { it.name.value == "Bob" })
        assertTrue(result.any { it.name.value == "Charlie" })
    }

    @Test
    fun `update should modify user name and email`() {
        val user = createTestUser(name = "Old Name", email = "old@test.com")
        repository.create(user)

        val updated =
            user.copy(
                name = Name.of("New Name"),
                email = Email.of("new@test.com"),
            )

        val result = repository.update(updated)

        assertEquals("New Name", result.name.value)
        assertEquals("new@test.com", result.email.value)

        val retrieved = repository.getById(user.id)
        assertEquals("New Name", retrieved.name.value)
    }

    @Test
    fun `update should throw NoUserExist when user does not exist`() {
        val nonExistentUser = createTestUser()

        assertThrows<NoUserExist> {
            repository.update(nonExistentUser)
        }
    }

    @Test
    fun `deleteById should remove user`() {
        val user = createTestUser()
        repository.create(user)

        repository.deleteById(user.id)

        assertThrows<NoUserExist> {
            repository.getById(user.id)
        }
    }

    @Test
    fun `deleteById should throw NoUserExist when user not found`() {
        val randomId = Uuid.random()

        assertThrows<NoUserExist> {
            repository.deleteById(randomId)
        }
    }

    @Test
    fun `clear should remove all users`() {
        repository.create(createTestUser(email = "user1@test.com"))
        repository.create(createTestUser(email = "user2@test.com"))

        repository.clear()

        assertEquals(0, repository.getAll().size)
    }

    @Test
    fun `save should work as alias for update`() {
        val user = createTestUser(name = "Original")
        repository.create(user)

        val modified = user.copy(name = Name.of("Saved Name"))
        repository.save(modified)

        val retrieved = repository.getById(user.id)
        assertEquals("Saved Name", retrieved.name.value)
    }

    // Helper methods
    private fun createTestUser(
        name: String = "Test User",
        email: String = "test${Uuid.random()}@test.com",
    ): User =
        User(
            id = Uuid.random(),
            name = Name.of(name),
            email = Email.of(email),
            token = Uuid.random(),
        )
}
