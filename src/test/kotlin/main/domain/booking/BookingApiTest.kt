package main.domain.booking

import main.api.dto.CreateUserRequest
import main.api.httpServer.HousesDataMem
import main.api.httpServer.HousesWebApi
import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.impl.mem.InMemoryLocationRepository
import main.data.impl.mem.InMemoryUsersRepository
import main.domain.user.Email
import main.domain.user.Name
import main.domain.user.User
import main.domain.user.UserRole
import main.utils.hashPassword
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
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
    fun `POST bookings rejects booking own house`() {
        val ownerToken = createUserAndGetToken("owner-own-booking@example.com")
        val houseId = createHouseAndGetId(ownerToken)

        val response =
            api.routes(
                Request(Method.POST, "/bookings")
                    .header("Authorization", "Bearer $ownerToken")
                    .header("Content-Type", "application/json")
                    .body("""{"hid":"$houseId","startDate":"2026-06-10","endDate":"2026-06-12"}"""),
            )

        assertEquals(Status.BAD_REQUEST, response.status)
        assertTrue(response.bodyString().contains("Não podes criar bookings para a tua própria house."))
    }

    @Test
    fun `GET houses available with authorization excludes owners houses`() {
        val ownerToken = createUserAndGetToken("owner-available-filter@example.com")
        val otherToken = createUserAndGetToken("other-available-filter@example.com")
        val ownerHouseId = createHouseAndGetId(ownerToken)
        val otherHouseId = createHouseAndGetId(otherToken)

        val response =
            api.routes(
                Request(Method.GET, "/houses/available?startDate=2026-06-10&endDate=2026-06-12")
                    .header("Authorization", "Bearer $ownerToken"),
            )

        assertEquals(Status.OK, response.status)
        assertTrue(!response.bodyString().contains(ownerHouseId))
        assertTrue(response.bodyString().contains(otherHouseId))
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
                    .body(
                        """
                        {
                            "startDate":"2026-06-11",
                            "endDate":"2026-06-13"
                        }
                        """.trimIndent(),
                    ),
            )

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("2026-06-11"))
        assertTrue(response.bodyString().contains("\"hid\":\"$houseId\""))
    }

    @Test
    fun `PUT booking does not change house even if hid is sent`() {
        val ownerToken = createUserAndGetToken("owner-change-house@example.com")
        val originalHouseId = createHouseAndGetId(ownerToken, "Casa Original")
        val otherHouseId = createHouseAndGetId(ownerToken, "Casa Alternativa")
        val bookerToken = createUserAndGetToken("booker-change-house@example.com")
        val bookingId = createBookingAndGetId(bookerToken, originalHouseId)

        val response =
            api.routes(
                Request(Method.PUT, "/bookings/$bookingId")
                    .header("Authorization", "Bearer $bookerToken")
                    .header("Content-Type", "application/json")
                    .body(
                        """
                        {
                            "hid":"$otherHouseId",
                            "startDate":"2026-06-11",
                            "endDate":"2026-06-13"
                        }
                        """.trimIndent(),
                    ),
            )

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("\"hid\":\"$originalHouseId\""))
        assertTrue(!response.bodyString().contains("\"hid\":\"$otherHouseId\""))
    }

    @Test
    fun `PUT booking by house owner is unauthorized when owner is not booker`() {
        val ownerToken = createUserAndGetToken("owner-cannot-update@example.com")
        val houseId = createHouseAndGetId(ownerToken)
        val bookerToken = createUserAndGetToken("booker-update-owner@example.com")
        val bookingId = createBookingAndGetId(bookerToken, houseId)

        val response =
            api.routes(
                Request(Method.PUT, "/bookings/$bookingId")
                    .header("Authorization", "Bearer $ownerToken")
                    .header("Content-Type", "application/json")
                    .body(
                        """
                        {
                            "startDate":"2026-06-11",
                            "endDate":"2026-06-13"
                        }
                        """.trimIndent(),
                    ),
            )

        assertEquals(Status.UNAUTHORIZED, response.status)
    }

    @Test
    fun `DELETE booking by house owner is unauthorized when owner is not booker`() {
        val ownerToken = createUserAndGetToken("owner-cannot-delete@example.com")
        val houseId = createHouseAndGetId(ownerToken)
        val bookerToken = createUserAndGetToken("booker-delete-owner@example.com")
        val bookingId = createBookingAndGetId(bookerToken, houseId)

        val deleteResponse =
            api.routes(
                Request(Method.DELETE, "/bookings/$bookingId")
                    .header("Authorization", "Bearer $ownerToken"),
            )

        val getResponse =
            api.routes(
                Request(Method.GET, "/bookings/$bookingId")
                    .header("Authorization", "Bearer $bookerToken"),
            )

        assertEquals(Status.UNAUTHORIZED, deleteResponse.status)
        assertEquals(Status.OK, getResponse.status)
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
        val ownerToken =
            HousesDataMem.services.createUser(
                CreateUserRequest(
                    "Owner",
                    "owner-bookings@example.com",
                    password,
                ),
            ).token
        val houseId = createHouseAndGetId(ownerToken)
        val userAToken =
            HousesDataMem.services.createUser(
                CreateUserRequest(
                    "User A",
                    "usera@example.com",
                    password,
                ),
            ).token
        val userBToken =
            HousesDataMem.services.createUser(
                CreateUserRequest(
                    "User B",
                    "userb@example.com",
                    password,
                ),
            ).token

        val bookingAResponse =
            api.routes(
                Request(Method.POST, "/bookings")
                    .header("Authorization", "Bearer $userAToken")
                    .header("Content-Type", "application/json")
                    .body("""{"hid":"$houseId","startDate":"2026-06-10","endDate":"2026-06-12"}"""),
            )
        val bookingAId = extractField(bookingAResponse.bodyString(), "bid")

        val bookingBResponse =
            api.routes(
                Request(Method.POST, "/bookings")
                    .header("Authorization", "Bearer $userBToken")
                    .header("Content-Type", "application/json")
                    .body("""{"hid":"$houseId","startDate":"2026-06-13","endDate":"2026-06-15"}"""),
            )
        val bookingBId = extractField(bookingBResponse.bodyString(), "bid")

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

    @Test
    fun `GET house available days returns open days for requested month`() {
        val ownerToken = createUserAndGetToken("owner@example.com")
        val houseId = createHouseAndGetId(ownerToken)
        val bookerToken = createUserAndGetToken("booker@example.com")
        createBookingAndGetId(bookerToken, houseId)

        val response =
            api.routes(
                Request(Method.GET, "/houses/$houseId/available-days?year=2026&month=6"),
            )

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("\"houseId\":\"$houseId\""))
        assertTrue(!response.bodyString().contains("2026-06-10"))
        assertTrue(response.bodyString().contains("2026-06-12"))
    }

    @Test
    fun `GET house available days rejects invalid month`() {
        val ownerToken = createUserAndGetToken("owner@example.com")
        val houseId = createHouseAndGetId(ownerToken)

        val response =
            api.routes(
                Request(Method.GET, "/houses/$houseId/available-days?year=2026&month=13"),
            )

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    private fun seedAuthUserToken(): Uuid {
        val authUser =
            User(
                id = Uuid.random(),
                name = Name.of("Auth User"),
                email = Email.of("auth@example.com"),
                passwordHash = hashPassword(password),
                token = Uuid.random(),
                role = UserRole.ADMIN,
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
                    .body("""{"name":"User","email":"$email","password":"$password"}"""),
            )

        return extractField(response.bodyString(), "token")
    }

    private fun createHouseAndGetId(
        ownerToken: String,
        title: String = "Casa Azul",
    ): String {
        val locationId = createLeafLocationAndGetId()
        val response =
            api.routes(
                Request(Method.POST, "/houses")
                    .header("Authorization", "Bearer $ownerToken")
                    .header("Content-Type", "application/json")
                    .body(
                        """
                        {
                            "title":"$title",
                            "lid":"$locationId",
                            "areaSqMt":120,
                            "pricePerNight":95.0,
                            "description":"Casa para booking $title"
                        }
                        """.trimIndent(),
                    ),
            )

        return extractField(response.bodyString(), "hid")
    }

    private fun createLeafLocationAndGetId(): String {
        val seed = Uuid.random().toString().take(8)
        val countryId = createLocationAndGetId("PT-$seed", "COUNTRY")
        val regionId = createLocationAndGetId("Reg-$seed", "REGION", countryId)
        val districtId = createLocationAndGetId("Dis-$seed", "DISTRICT", regionId)
        val municipalityId = createLocationAndGetId("Mun-$seed", "MUNICIPALITY", districtId)
        return createLocationAndGetId("Loc-$seed", "LOCALITY", municipalityId)
    }

    private fun createLocationAndGetId(
        name: String,
        type: String,
        parentId: String? = null,
    ): String {
        val parentField = parentId?.let { "\"$it\"" } ?: "null"
        val response =
            api.routes(
                Request(Method.POST, "/locations")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"$name","type":"$type","parentId":$parentField}"""),
            )

        assertEquals(Status.CREATED, response.status, response.bodyString())
        return extractField(response.bodyString(), "lid")
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

        return extractField(response.bodyString(), "bid")
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
