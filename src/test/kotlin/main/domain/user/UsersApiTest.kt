package main.domain.user

import main.api.httpServer.HousesDataMem
import main.api.httpServer.HousesWebApi
import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.impl.mem.InMemoryLocationRepository
import main.data.impl.mem.InMemoryUsersRepository
import main.utils.hashPassword
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class UsersApiTest {
    private val api = HousesWebApi(HousesDataMem.services)
    private lateinit var adminToken: String
    private val password = "Secret123"

    @BeforeTest
    fun setup() {
        InMemoryUsersRepository.clear()
        InMemoryHouseRepository.clear()
        InMemoryBookingRepository.clear()
        InMemoryLocationRepository.clear()
        adminToken = seedAuthUserToken().toString()
    }

    @Test
    fun `POST users creates user`() {
        val response =
            api.routes(
                userCreateRequest("Alice", "alice@example.com"),
            )

        assertEquals(Status.CREATED, response.status)
        assertTrue(response.bodyString().contains("alice@example.com"))
    }

    @Test
    fun `GET users returns list`() {
        api.routes(userCreateRequest("Alice", "alice@example.com"))
        api.routes(userCreateRequest("Alice2", "alice2@example.com"))

        val response = api.routes(Request(Method.GET, "/users"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("alice@example.com"))
        assertTrue(response.bodyString().contains("alice2@example.com"))
    }

    @Test
    fun `GET user with invalid id returns 400`() {
        val response = api.routes(Request(Method.GET, "/users/invalid-uuid"))

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `GET user with existing id returns 200`() {
        val createResponse = api.routes(userCreateRequest("Alice", "alice@example.com"))

        val id = extractField(createResponse.bodyString(), "id")
        val response = api.routes(Request(Method.GET, "/users/$id"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("alice@example.com"))
    }

    @Test
    fun `GET user with non existent id returns 404`() {
        val response = api.routes(Request(Method.GET, "/users/${UUID.randomUUID()}"))

        assertEquals(Status.NOT_FOUND, response.status)
    }

    @Test
    fun `PUT user should update if id already exists`() {
        val response1 = api.routes(userCreateRequest("Alicia", "alice@example.com"))

        val id =
            response1.body
                .toString()
                .substringAfter("id\":\"")
                .substringBefore("\"")

        val response2 =
            api.routes(
                Request(Method.PUT, "/users/$id")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Alice","email":"alice2@example.com"}"""),
            )

        assertEquals(Status.CREATED, response1.status)
        assertEquals(Status.OK, response2.status)
        assertTrue(response1.bodyString().contains("alice@example"))
        assertTrue(response2.bodyString().contains("alice2@example"))
    }

    @Test
    fun `PUT user to already exising email should return 400`() {
        api.routes(userCreateRequest("Alicia", "alice@example.com"))

        val response1 = api.routes(userCreateRequest("Alicia2", "alice2@example.com"))

        val id =
            response1.body
                .toString()
                .substringAfter("id\":\"")
                .substringBefore("\"")

        val response2 =
            api.routes(
                Request(Method.PUT, "/users/$id")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Alice","email":"alice@example.com"}"""),
            )

        assertEquals(Status.CREATED, response1.status)
        assertEquals(Status.BAD_REQUEST, response2.status)
    }

    @Test
    fun `PUT inexistent user returns 400`() {
        api.routes(userCreateRequest("Alicia", "alice@example.com"))

        val response =
            api.routes(
                Request(Method.PUT, "/users/invalid-uuid")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Alice","email":"alice2@example.com"}"""),
            )

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `POST duplicate user email returns 400`() {
        api.routes(userCreateRequest("Alice", "alice@example.com"))

        val response = api.routes(userCreateRequest("Alicia", "alice@example.com"))

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `DELETE user removes user`() {
        val createResponse = api.routes(userCreateRequest("Alice", "alice@example.com"))

        val id = extractField(createResponse.bodyString(), "id")
        val deleteResponse =
            api.routes(
                Request(Method.DELETE, "/users/$id")
                    .header("Authorization", "Bearer $adminToken"),
            )
        val getResponse = api.routes(Request(Method.GET, "/users/$id"))

        assertEquals(Status.OK, deleteResponse.status)
        assertEquals(Status.NOT_FOUND, getResponse.status)
    }

    @Test
    fun `DELETE user with mismatched body id returns 400`() {
        val createResponse = api.routes(userCreateRequest("Alice", "alice@example.com"))

        val id = extractField(createResponse.bodyString(), "id")
        val response =
            api.routes(
                Request(Method.DELETE, "/users/$id")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"id":"${UUID.randomUUID()}"}"""),
            )

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    private fun userCreateRequest(
        name: String,
        email: String,
    ): Request =
        Request(Method.POST, "/users")
            .header("Authorization", "Bearer $adminToken")
            .header("Content-Type", "application/json")
            .body("""{"name":"$name","email":"$email","password":"$password"}""")

    private fun extractField(
        json: String,
        field: String,
    ): String =
        "\"$field\":\"([^\"]+)\""
            .toRegex()
            .find(json)
            ?.groupValues
            ?.get(1)
            ?: error("Could not extract $field from response")

    private fun seedAuthUserToken(): Uuid {
        val authUser =
            User(
                id = Uuid.random(),
                name = Name.of("Zulu Admin"),
                email = Email.of("zulu-admin@example.com"),
                passwordHash = hashPassword(password),
                token = Uuid.random(),
            )
        InMemoryUsersRepository.create(authUser)
        return authUser.token
    }
}
