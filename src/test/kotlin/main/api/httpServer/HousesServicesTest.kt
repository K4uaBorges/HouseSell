package main.api.httpServer

import main.api.dto.CreateUserRequest
import main.api.utils.Paging
import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.impl.mem.InMemoryLocationRepository
import main.data.impl.mem.InMemoryUsersRepository
import main.domain.user.Email
import main.domain.user.Name
import main.domain.user.User
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HousesServicesTest {
    private val services = HousesDataMem.services

    @BeforeTest
    fun clearRepos() {
        InMemoryUsersRepository.clear()
        InMemoryHouseRepository.clear()
        InMemoryBookingRepository.clear()
        InMemoryLocationRepository.clear()
    }

    @Test
    fun `createUser creates a user that can be fetched`() {
        val created = services.createUser(CreateUserRequest("Alice", "alice@example.com", "Secret123"))
        val fetched = services.getUser(created.id)

        assertEquals("Alice", fetched.name)
        assertEquals("alice@example.com", fetched.email)
        assertNotEquals("", created.token)
    }

    @Test
    fun `listUsers applies paging`() {
        services.createUser(CreateUserRequest("Ana", "ana@example.com", "Secret123"))
        services.createUser(CreateUserRequest("Bruno", "bruno@example.com", "Secret123"))
        services.createUser(CreateUserRequest("Carla", "carla@example.com", "Secret123"))

        val page = services.listUsers(Paging(skip = 1, limit = 1))

        assertEquals(1, page.users.size)
        assertEquals("Bruno", page.users.first().name)
    }

    private fun seedAuthUserToken(): Uuid {
        val authUser =
            User(
                id = Uuid.random(),
                name = Name.of("Zulu Auth"),
                email = Email.of("auth@example.com"),
                token = Uuid.random(),
            )
        InMemoryUsersRepository.create(authUser)
        return authUser.token
    }
}
