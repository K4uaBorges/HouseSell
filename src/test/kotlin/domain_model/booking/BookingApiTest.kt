package domain_model.booking

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import main.api.dto.CreateUserRequest
import main.api.http_server.HousesDataMem
import main.api.http_server.HousesWebApi
import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.impl.mem.InMemoryLocationRepository
import main.data.impl.mem.InMemoryUsersRepository
import main.domain_model.user.Email
import main.domain_model.user.Name
import main.domain_model.user.User
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class BookingApiTest {
    private val api = HousesWebApi(HousesDataMem.services)
    private lateinit var adminToken: String

    @BeforeTest
    fun setup() {
        InMemoryUsersRepository.clear()
        InMemoryHouseRepository.clear()
        InMemoryBookingRepository.clear()
        InMemoryLocationRepository.clear()
        adminToken = seedAuthUserToken().toString()
    }

    @Test
    fun `POST bookings creates booking`() {
        val ownerToken = createUserAndGetToken("owner@example.com")
        val houseId = createHouseAndGetId(ownerToken)
        val bookerToken = createUserAndGetToken("booker@example.com")

        val response =
            api.routes(
                Request(Method.POST, "/bookings")
                    .header("Authorization", "Bearer $bookerToken")
                    .header("Content-Type", "application/json")
                    .body("""{"hid":"$houseId","startDate":"2026-06-10","endDate":"2026-06-12"}"""),
            )

        assertEquals(Status.CREATED, response.status)
        assertTrue(response.bodyString().contains(houseId))
    }

    @Test
    fun `GET booking with invalid id returns 400`() {
        val token = createUserAndGetToken("owner@example.com")

        val response =
            api.routes(
                Request(Method.GET, "/bookings/invalid-uuid")
                    .header("Authorization", "Bearer $token"),
            )

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `POST booking without authorization returns 401`() {
        val response =
            api.routes(
                Request(Method.POST, "/bookings")
                    .header("Content-Type", "application/json")
                    .body("""{"hid":"${Uuid.random()}","startDate":"2026-06-10","endDate":"2026-06-12"}"""),
            )

        assertEquals(Status.UNAUTHORIZED, response.status)
    }

    @Test
    fun `POST overlapping booking returns 400`() {
        val ownerToken = createUserAndGetToken("owner@example.com")
        val houseId = createHouseAndGetId(ownerToken)
        val firstBookerToken = createUserAndGetToken("booker1@example.com")
        val secondBookerToken = createUserAndGetToken("booker2@example.com")

        api.routes(
            Request(Method.POST, "/bookings")
                .header("Authorization", "Bearer $firstBookerToken")
                .header("Content-Type", "application/json")
                .body("""{"hid":"$houseId","startDate":"2026-06-10","endDate":"2026-06-15"}"""),
        )

        val response =
            api.routes(
                Request(Method.POST, "/bookings")
                    .header("Authorization", "Bearer $secondBookerToken")
                    .header("Content-Type", "application/json")
                    .body("""{"hid":"$houseId","startDate":"2026-06-12","endDate":"2026-06-18"}"""),
            )

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `GET bookings without required query params returns 400`() {
        val token = createUserAndGetToken("owner@example.com")

        val response =
            api.routes(
                Request(Method.GET, "/bookings")
                    .header("Authorization", "Bearer $token"),
            )

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `GET booking returns 200 for owner`() {
        val ownerToken = createUserAndGetToken("owner@example.com")
        val houseId = createHouseAndGetId(ownerToken)
        val bookerToken = createUserAndGetToken("booker@example.com")
        val bookingId = createBookingAndGetId(bookerToken, houseId)

        val response =
            api.routes(
                Request(Method.GET, "/bookings/$bookingId")
                    .header("Authorization", "Bearer $ownerToken"),
            )

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains(bookingId))
    }

    @Test
    fun `PUT booking updates booking for booker`() {
        val ownerToken = createUserAndGetToken("owner@example.com")
        val houseId = createHouseAndGetId(ownerToken)
        val bookerToken = createUserAndGetToken("booker@example.com")
        val bookingId = createBookingAndGetId(bookerToken, houseId)

        val response =
            api.routes(
                Request(Method.PUT, "/bookings/$bookingId")
                    .header("Authorization", "Bearer $bookerToken")
                    .header("Content-Type", "application/json")
                    .body("""{"hid":"$houseId","startDate":"2026-06-11","endDate":"2026-06-13"}"""),
            )

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("2026-06-11"))
    }

    @Test
    fun `DELETE booking removes booking`() {
        val ownerToken = createUserAndGetToken("owner@example.com")
        val houseId = createHouseAndGetId(ownerToken)
        val bookerToken = createUserAndGetToken("booker@example.com")
        val bookingId = createBookingAndGetId(bookerToken, houseId)

        val deleteResponse =
            api.routes(
                Request(Method.DELETE, "/bookings/$bookingId")
                    .header("Authorization", "Bearer $bookerToken"),
            )
        val getResponse =
            api.routes(
                Request(Method.GET, "/bookings/$bookingId")
                    .header("Authorization", "Bearer $bookerToken"),
            )

        assertEquals(Status.OK, deleteResponse.status)
        assertEquals(Status.NOT_FOUND, getResponse.status)
    }

    @Test
    fun `GET bookings returns list for house owner`() {
        val ownerToken = createUserAndGetToken("owner@example.com")
        val houseId = createHouseAndGetId(ownerToken)
        val bookerToken = createUserAndGetToken("booker@example.com")
        createBookingAndGetId(bookerToken, houseId)

        val response =
            api.routes(
                Request(Method.GET, "/bookings?hid=$houseId&dateStart=2026-06-01&dateEnd=2026-06-30")
                    .header("Authorization", "Bearer $ownerToken"),
            )

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains(houseId))
    }

    @Test
    fun `GET bookings mine returns only authenticated user bookings`() {
        val userAToken =
            HousesDataMem.services.createUser(CreateUserRequest("User A", "usera@example.com")).token
        val houseId = createHouseAndGetId(userAToken)
        val userBToken =
            HousesDataMem.services.createUser(CreateUserRequest("User B", "userb@example.com")).token

        val bookingAResponse =
            api.routes(
                Request(Method.POST, "/bookings")
                    .header("Authorization", "Bearer $userAToken")
                    .header("Content-Type", "application/json")
                    .body("""{"hid":"$houseId","startDate":"2026-06-10","endDate":"2026-06-12"}"""),
            )
        val bookingAId = extractField(bookingAResponse.bodyString(), "id")

        val bookingBResponse =
            api.routes(
                Request(Method.POST, "/bookings")
                    .header("Authorization", "Bearer $userBToken")
                    .header("Content-Type", "application/json")
                    .body("""{"hid":"$houseId","startDate":"2026-06-13","endDate":"2026-06-15"}"""),
            )
        val bookingBId = extractField(bookingBResponse.bodyString(), "id")

        val response =
            api.routes(
                Request(Method.GET, "/bookings/mine")
                    .header("Authorization", "Bearer $userAToken"),
            )

        assertEquals(Status.CREATED, bookingAResponse.status)
        assertEquals(Status.CREATED, bookingBResponse.status)
        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains(bookingAId))
        assertTrue(!response.bodyString().contains(bookingBId))
    }

    private fun seedAuthUserToken(): Uuid {
        val authUser =
            User(
                id = Uuid.random(),
                name = Name.of("Auth User"),
                email = Email.of("auth@example.com"),
                token = Uuid.random(),
            )
        InMemoryUsersRepository.create(authUser)
        return authUser.token
    }

    private fun createUserAndGetToken(email: String): String {
        val response =
            api.routes(
                Request(Method.POST, "/users")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"User","email":"$email"}"""),
            )

        return extractField(response.bodyString(), "token")
    }

    private fun createHouseAndGetId(ownerToken: String): String {
        val response =
            api.routes(
                Request(Method.POST, "/houses")
                    .header("Authorization", "Bearer $ownerToken")
                    .header("Content-Type", "application/json")
                    .body(
                        """{"title":"Casa Azul","lid":"${Uuid.random()}","areaSqMt":120,"pricePerNight":95.0,"description":"Casa para booking"}""",
                    ),
            )

        return extractField(response.bodyString(), "id")
    }

    private fun createBookingAndGetId(
        bookerToken: String,
        houseId: String,
    ): String {
        val response =
            api.routes(
                Request(Method.POST, "/bookings")
                    .header("Authorization", "Bearer $bookerToken")
                    .header("Content-Type", "application/json")
                    .body("""{"hid":"$houseId","startDate":"2026-06-10","endDate":"2026-06-12"}"""),
            )

        return extractField(response.bodyString(), "id")
    }

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
}
