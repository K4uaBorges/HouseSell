package main.api.http_server

import main.api.dto.CreateUserRequest
import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.impl.mem.InMemoryLocationRepository
import main.data.impl.mem.InMemoryUsersRepository
import main.domain_model.user.Email
import main.domain_model.user.Name
import main.domain_model.user.User
import main.api.utils.Paging
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
        val created = services.createUser(CreateUserRequest("Alice", "alice@example.com"))
        val fetched = services.getUser(created.id)

        assertEquals("Alice", fetched.name)
        assertEquals("alice@example.com", fetched.email)
        assertNotEquals("", created.token)
    }

    @Test
    fun `listUsers applies paging`() {
        services.createUser(CreateUserRequest("Ana", "ana@example.com"))
        services.createUser(CreateUserRequest("Bruno", "bruno@example.com"))
        services.createUser(CreateUserRequest("Carla", "carla@example.com"))

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
