package domain_model.booking

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import main.api.http_server.HousesDataMem
import main.api.http_server.HousesWebApi
import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.impl.mem.InMemoryLocationRepository
import main.data.impl.mem.InMemoryUsersRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class BookingApiTest {
    private val api = HousesWebApi(HousesDataMem.services)

    @BeforeTest
    fun setup() {
        InMemoryUsersRepository.clear()
        InMemoryHouseRepository.clear()
        InMemoryBookingRepository.clear()
        InMemoryLocationRepository.clear()
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

    private fun createUserAndGetToken(email: String): String {
        val response =
            api.routes(
                Request(Method.POST, "/users")
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
