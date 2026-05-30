package main.api.httpServer

import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.impl.mem.InMemoryLocationRepository
import main.data.impl.mem.InMemoryUsersRepository
import main.domain.user.Email
import main.domain.user.Name
import main.domain.user.User
import main.utils.hashPassword
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import java.time.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HousesWebApiTest {
    private val api = HousesWebApi(HousesDataMem.services)
    private lateinit var adminToken: String
    private val password = "Secret123"

    @BeforeTest
    fun clearRepos() {
        InMemoryUsersRepository.clear()
        InMemoryHouseRepository.clear()
        InMemoryBookingRepository.clear()
        InMemoryLocationRepository.clear()
        adminToken = seedAuthUserToken().toString()
    }

    @Test
    fun `POST users with invalid json returns 400`() {
        val response =
            api.routes(
                Request(Method.POST, "/users")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("{invalid"),
            )

        assertEquals(Status.BAD_REQUEST, response.status)
        assertTrue(response.bodyString().contains("Invalid JSON body."))
    }

    @Test
    fun `POST users creates and GET users lists it`() {
        val createResponse =
            api.routes(
                Request(Method.POST, "/users")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Alice","email":"alice@example.com","password":"$password"}"""),
            )
        val listResponse = api.routes(Request(Method.GET, "/users"))

        assertEquals(Status.CREATED, createResponse.status)
        assertEquals(Status.OK, listResponse.status)
        assertTrue(listResponse.bodyString().contains("alice@example.com"))
    }

    @Test
    fun `POST session login returns token for existing account`() {
        val createResponse =
            api.routes(
                Request(Method.POST, "/users")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Alice","email":"alice@example.com","password":"$password"}"""),
            )
        val createdBody = createResponse.bodyString()
        val createdUserId = """"id":"([^"]+)"""".toRegex().find(createdBody)!!.groupValues[1]
        val createdToken = """"token":"([^"]+)"""".toRegex().find(createdBody)!!.groupValues[1]

        val loginResponse =
            api.routes(
                Request(Method.POST, "/session/login")
                    .header("Content-Type", "application/json")
                    .body("""{"email":"alice@example.com","password":"$password"}"""),
            )

        assertEquals(Status.OK, loginResponse.status)
        val loginBody = loginResponse.bodyString()
        assertTrue(loginBody.contains(""""id":"$createdUserId""""))
        assertTrue(loginBody.contains(""""token":"$createdToken""""))
    }

    @Test
    fun `POST session login returns 404 when account does not exist`() {
        val loginResponse =
            api.routes(
                Request(Method.POST, "/session/login")
                    .header("Content-Type", "application/json")
                    .body("""{"email":"missing@example.com","password":"$password"}"""),
            )

        assertEquals(Status.NOT_FOUND, loginResponse.status)
        assertTrue(loginResponse.bodyString().contains("User not found."))
    }

    @Test
    fun `DELETE users returns delete response and removes user`() {
        val createResponse =
            api.routes(
                Request(Method.POST, "/users")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Alice","email":"alice@example.com","password":"$password"}"""),
            )
        val userId = """"id":"([^"]+)"""".toRegex().find(createResponse.bodyString())!!.groupValues[1]

        val deleteResponse =
            api.routes(
                Request(Method.DELETE, "/users/$userId")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"id":"$userId"}"""),
            )

        assertEquals(Status.OK, deleteResponse.status)
        assertTrue(deleteResponse.bodyString().contains(""""deleted":true"""))

        val listResponse = api.routes(Request(Method.GET, "/users"))
        assertTrue(!listResponse.bodyString().contains("alice@example.com"))
    }

    @Test
    fun `GET houses preview returns predicted price`() {
        val response = api.routes(Request(Method.GET, "/houses/preview?areaSqMt=110"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("\"predictedPricePerNight\""))
        assertTrue(response.bodyString().contains("\"trainingSource\""))
    }

    @Test
    fun `GET houses cache stats returns counters`() {
        val response = api.routes(Request(Method.GET, "/houses/cache/stats"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("\"hits\""))
        assertTrue(response.bodyString().contains("\"misses\""))
        assertTrue(response.bodyString().contains("\"limit\""))
    }

    @Test
    fun `GET session bootstrap seeds principal data and returns token`() {
        val bootstrap = api.routes(Request(Method.GET, "/session/bootstrap"))
        assertEquals(Status.OK, bootstrap.status)
        assertTrue(bootstrap.bodyString().contains("\"token\""))
        assertTrue(bootstrap.bodyString().contains("\"busyHouseId\""))
        assertTrue(bootstrap.bodyString().contains("\"freeHouseId\""))

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val availableResponse =
            api.routes(
                Request(Method.GET, "/houses/available?startDate=$today&endDate=$tomorrow"),
            )
        assertEquals(Status.OK, availableResponse.status)
        assertTrue(availableResponse.bodyString().contains("Casa Demo Livre"))
        assertTrue(!availableResponse.bodyString().contains("Casa Demo Ocupada"))
    }

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
