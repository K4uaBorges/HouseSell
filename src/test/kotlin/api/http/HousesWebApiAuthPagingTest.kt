package api.http

import domain.booking.CreateBookingRequest
import domain.booking.CreateBookingResponse
import domain.booking.ListBookingsResponse
import domain.booking.repository.InMemoryBookingRepository
import domain.house.CreateHouseRequest
import domain.house.CreateHouseResponse
import domain.house.GetHouseResponse
import domain.house.ListHousesResponse
import domain.house.repository.InMemoryHouseRepository
import domain.user.CreateUserRequest
import domain.user.CreateUserResponse
import domain.user.repository.InMemoryUsersRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HousesWebApiAuthPagingTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val api = HousesWebApi(HousesDataMem.services)

    @BeforeEach
    fun reset() {
        InMemoryHouseRepository.clear()
        InMemoryBookingRepository.clear()
        InMemoryUsersRepository.clear()
    }

    @Test
    fun `create house requires bearer token`() {
        val body = CreateHouseRequest("My house", "Porto", 80, 100.0, "Center")

        val noAuthResponse = api.routes(
            Request(Method.POST, "/houses").jsonBody(body)
        )

        assertEquals(Status.UNAUTHORIZED, noAuthResponse.status)

        val token = createUser().token
        val authResponse = api.routes(
            Request(Method.POST, "/houses")
                .header("Authorization", "Bearer $token")
                .jsonBody(body)
        )

        assertEquals(Status.CREATED, authResponse.status)
    }

    @Test
    fun `houses list supports skip and limit paging`() {
        val token = createUser().token
        createHouse(token, "A House")
        createHouse(token, "B House")
        createHouse(token, "C House")

        val response = api.routes(Request(Method.GET, "/houses?skip=1&limit=1"))
        val payload = json.decodeFromString<ListHousesResponse>(response.bodyString())

        assertEquals(Status.OK, response.status)
        assertEquals(1, payload.houses.size)
        assertEquals("B House", payload.houses.first().title)
    }

    @Test
    fun `bookings endpoints require bearer token and support paging`() {
        val user = createUser()
        val token = user.token
        val otherUser = createUser("other@example.com")
        val houseId = createHouse(token, "Booking House").id

        val bookingBody = CreateBookingRequest(
            hid = houseId,
            startDate = "20260610",
            endDate = "20260615",
        )

        val noAuthCreate = api.routes(Request(Method.POST, "/bookings").jsonBody(bookingBody))
        assertEquals(Status.UNAUTHORIZED, noAuthCreate.status)

        val authCreate = api.routes(
            Request(Method.POST, "/bookings")
                .header("Authorization", "Bearer $token")
                .jsonBody(bookingBody)
        )
        assertEquals(Status.CREATED, authCreate.status)
        val booking = json.decodeFromString<CreateBookingResponse>(authCreate.bodyString())
        assertEquals(user.id, booking.uid)

        val noAuthList = api.routes(Request(Method.GET, "/bookings?hid=$houseId&date=20260612&skip=0&limit=1"))
        assertEquals(Status.UNAUTHORIZED, noAuthList.status)

        val authList = api.routes(
            Request(Method.GET, "/bookings?hid=$houseId&date=20260612&skip=0&limit=1")
                .header("Authorization", "Bearer $token")
        )
        val listPayload = json.decodeFromString<ListBookingsResponse>(authList.bodyString())

        assertEquals(Status.OK, authList.status)
        assertEquals(1, listPayload.bookings.size)

        val otherUserList = api.routes(
            Request(Method.GET, "/bookings?hid=$houseId&date=20260612&skip=0&limit=1")
                .header("Authorization", "Bearer ${otherUser.token}")
        )
        assertEquals(Status.UNAUTHORIZED, otherUserList.status)
    }

    @Test
    fun `house stores owner and only owner can command ad`() {
        val owner = createUser("owner@example.com")
        val stranger = createUser("stranger@example.com")
        val house = createHouse(owner.token, "Owner House")

        val getHouse = api.routes(Request(Method.GET, "/houses/${house.id}"))
        assertEquals(Status.OK, getHouse.status)
        assertEquals(owner.id, json.decodeFromString<GetHouseResponse>(getHouse.bodyString()).uid)

        val mine = api.routes(
            Request(Method.GET, "/houses/mine")
                .header("Authorization", "Bearer ${owner.token}")
        )
        val minePayload = json.decodeFromString<ListHousesResponse>(mine.bodyString())
        assertEquals(Status.OK, mine.status)
        assertEquals(1, minePayload.houses.size)
        assertEquals(house.id, minePayload.houses.first().id)

        val strangerDelete = api.routes(
            Request(Method.DELETE, "/houses/${house.id}")
                .header("Authorization", "Bearer ${stranger.token}")
        )
        assertEquals(Status.UNAUTHORIZED, strangerDelete.status)

        val ownerDelete = api.routes(
            Request(Method.DELETE, "/houses/${house.id}")
                .header("Authorization", "Bearer ${owner.token}")
        )
        assertEquals(Status.NO_CONTENT, ownerDelete.status)
    }

    private fun createUser(email: String = "alice@example.com"): CreateUserResponse {
        val response = api.routes(
            Request(Method.POST, "/users")
                .jsonBody(CreateUserRequest(name = "Alice", email = email))
        )

        assertEquals(Status.CREATED, response.status)
        return json.decodeFromString(response.bodyString())
    }

    private fun createHouse(token: String, title: String): CreateHouseResponse {
        val response = api.routes(
            Request(Method.POST, "/houses")
                .header("Authorization", "Bearer $token")
                .jsonBody(
                    CreateHouseRequest(
                        title = title,
                        location = "Porto",
                        areaSqMt = 70,
                        pricePerNight = 120.0,
                        description = "Near metro",
                    )
                )
        )

        assertEquals(Status.CREATED, response.status)
        return json.decodeFromString(response.bodyString())
    }

    private inline fun <reified T> Request.jsonBody(payload: T): Request =
        header("Content-Type", "application/json")
            .body(json.encodeToString(payload))
}
