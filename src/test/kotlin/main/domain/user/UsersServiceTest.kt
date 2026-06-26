package main.domain.user

import main.data.impl.mem.InMemoryUsersRepository
import main.errors.UnauthorizedException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class UsersServiceTest {
    private val repo = InMemoryUsersRepository
    private val service = UsersService(repo)
    private val password = "Secret123"

    @BeforeTest
    fun setup() {
        repo.clear()
    }

    @Test
    fun `create user with valid name and email`() {
        val user = service.createUser("Alice", "alice@example.com", password)

        assertEquals("Alice", user.name.value)
        assertEquals("alice@example.com", user.email.value)
    }

    @Test
    fun `reject duplicate email`() {
        service.createUser("Alice", "alice@example.com", password)

        assertFailsWith<IllegalArgumentException> {
            service.createUser("Alicia", "alice@example.com", password)
        }
    }

    @Test
    fun `update user keeps same email for same user`() {
        val user = service.createUser("Alice", "alice@example.com", password)

        val updated = service.updateUser(user, "Alice Cooper", "alice@example.com")

        assertEquals(user.id, updated.id)
        assertEquals("Alice Cooper", updated.name.value)
        assertEquals("alice@example.com", updated.email.value)
    }

    @Test
    fun `update user by id rejects invalid uuid`() {
        assertFailsWith<IllegalArgumentException> {
            service.updateUserById("invalid-uuid", "Alice", "alice@example.com")
        }
    }

    @Test
    fun `list users sorted by name`() {
        service.createUser("Bruno", "bruno@example.com", password)
        service.createUser("Ana", "ana@example.com", password)

        val users = service.listUsers()

        assertEquals(2, users.size)
        assertEquals("Ana", users[0].name.value)
        assertEquals("Bruno", users[1].name.value)
    }

    @Test
    fun `get user by id and token returns same user`() {
        val created = service.createUser("Alice", "alice@example.com", password)

        val byId = service.getUserById(created.id)
        val byToken = service.getUserByToken(created.token)

        assertEquals(created.id, byId.id)
        assertEquals(created.id, byToken.id)
    }

    @Test
    fun `delete user by id removes user`() {
        val created = service.createUser("Alice", "alice@example.com", password)

        service.deleteUserById(created.id.toString())

        assertFailsWith<IllegalArgumentException> {
            service.getUserById(created.id)
        }
    }

    @Test
    fun `delete user by id rejects invalid uuid`() {
        assertFailsWith<IllegalArgumentException> {
            service.deleteUserById("not-an-id")
        }
    }

    @Test
    fun `authenticate user accepts correct password`() {
        service.createUser("Alice", "alice@example.com", password)

        val authenticated = service.authenticateUser("alice@example.com", password)

        assertEquals("alice@example.com", authenticated.email.value)
    }

    @Test
    fun `authenticate user rejects incorrect password`() {
        service.createUser("Alice", "alice@example.com", password)

        val error =
            assertFailsWith<UnauthorizedException> {
                service.authenticateUser("alice@example.com", "Wrongpass1")
            }

        assertEquals("Invalid credentials.", error.message)
    }

    @Test
    fun `create user rejects weak password`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                service.createUser("Alice", "alice@example.com", "secret123")
            }

        assertEquals("Password must include an uppercase letter.", error.message)
    }
}
