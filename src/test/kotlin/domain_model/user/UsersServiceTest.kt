package domain_model.user

import main.data.impl.mem.InMemoryUsersRepository
import main.domain_model.user.UsersService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class UsersServiceTest {
    private val repo = InMemoryUsersRepository
    private val service = UsersService(repo)

    @BeforeTest
    fun setup() {
        repo.clear()
    }

    @Test
    fun `create user with valid name and email`() {
        val user = service.createUser("Alice", "alice@example.com")

        assertEquals("Alice", user.name.value)
        assertEquals("alice@example.com", user.email.value)
    }

    @Test
    fun `reject duplicate email`() {
        service.createUser("Alice", "alice@example.com")

        assertFailsWith<IllegalArgumentException> {
            service.createUser("Alicia", "alice@example.com")
        }
    }

    @Test
    fun `update user keeps same email for same user`() {
        val user = service.createUser("Alice", "alice@example.com")

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
        service.createUser("Bruno", "bruno@example.com")
        service.createUser("Ana", "ana@example.com")

        val users = service.listUsers()

        assertEquals(2, users.size)
        assertEquals("Ana", users[0].name.value)
        assertEquals("Bruno", users[1].name.value)
    }

    @Test
    fun `get user by id and token returns same user`() {
        val created = service.createUser("Alice", "alice@example.com")

        val byId = service.getUserById(created.id)
        val byToken = service.getUserByToken(created.token)

        assertEquals(created.id, byId.id)
        assertEquals(created.id, byToken.id)
    }

    @Test
    fun `delete user by id removes user`() {
        val created = service.createUser("Alice", "alice@example.com")

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
}
