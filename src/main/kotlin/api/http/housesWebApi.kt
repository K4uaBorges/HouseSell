package api.http

import api.ApiError
import domain.booking.CreateBookingRequest
import domain.booking.CreateBookingResponse
import domain.booking.ListAvailableHousesResponse
import domain.booking.ListBookingsResponse
import domain.booking.toAvailableHouseResponse
import domain.booking.toGetBookingResponse
import domain.house.CreateHouseRequest
import domain.house.CreateHouseResponse
import domain.house.ListHousesResponse
import domain.house.toGetHouseResponse
import domain.user.CreateUserRequest
import domain.user.CreateUserResponse
import domain.user.User
import java.time.LocalDate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import utils.BookingDateUtils
import utils.Paging
import utils.bearerToken
import utils.page
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class HousesWebApi(private val services: HousesServices) {
    private val json = Json { ignoreUnknownKeys = true }

    val routes: HttpHandler = routes(
        "/users" bind Method.POST to ::createUser,

        "/houses" bind Method.GET to ::listHouses,
        "/houses/mine" bind Method.GET to ::listMyHouses,
        "/houses" bind Method.POST to ::createHouse,
        "/houses/available" bind Method.GET to ::listAvailableHouses,
        "/houses/{hid}" bind Method.GET to ::getHouse,
        "/houses/{hid}" bind Method.DELETE to ::deleteHouse,

        "/bookings" bind Method.POST to ::createBooking,
        "/bookings" bind Method.GET to ::listBookings,
        "/bookings/{bid}" bind Method.GET to ::getBooking,
    )

    private fun createUser(request: Request): Response = safe {
        val body = decodeBody<CreateUserRequest>(request)
        val user = services.usersService.createUser(
            nameRaw = body.name,
            emailRaw = body.email,
        )

        jsonResponse(
            Status.CREATED,
            CreateUserResponse(
                id = user.id.toString(),
                name = user.name,
                email = user.email.value,
                token = user.token.toString(),
            ),
        )
    }

    private fun listHouses(request: Request): Response = safe {
        val paging = pagingOf(request)
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val houses = services.bookingService
            .listAvailableHouses(
                BookingDateUtils.formatPostgresDate(today),
                BookingDateUtils.formatPostgresDate(tomorrow),
            )
            .map { it.toGetHouseResponse() }
            .page(paging)

        jsonResponse(Status.OK, ListHousesResponse(houses))
    }

    private fun listMyHouses(request: Request): Response = safe {
        val user = requireAuthenticatedUser(request)
        val paging = pagingOf(request)
        val houses = services.houseService.listHousesByOwner(user.id)
            .map { it.toGetHouseResponse() }
            .page(paging)
        jsonResponse(Status.OK, ListHousesResponse(houses))
    }

    private fun createHouse(request: Request): Response = safe {
        val user = requireAuthenticatedUser(request)

        val body = decodeBody<CreateHouseRequest>(request)
        val house = services.houseService.createHouse(
            ownerId = user.id,
            titleRaw = body.title,
            locationRaw = body.location,
            areaSqMt = body.areaSqMt,
            pricePerNight = body.pricePerNight,
            descriptionRaw = body.description,
        )

        jsonResponse(
            Status.CREATED,
            CreateHouseResponse(
                id = house.id.toString(),
                uid = house.uid.toString(),
                title = house.title.value,
                location = house.location,
                areaSqMt = house.areaSqMt,
                pricePerNight = house.pricePerNight,
                description = house.description,
            ),
        )
    }

    private fun getHouse(request: Request): Response = safe {
        val hid = requirePath(request, "hid", "House id is required.")
        jsonResponse(Status.OK, services.houseService.getHouseInfoById(hid))
    }

    private fun deleteHouse(request: Request): Response = safe {
        val user = requireAuthenticatedUser(request)
        val hid = requirePath(request, "hid", "House id is required.")
        val house = requireHouseOwnership(user, hid)
        require(services.bookingService.listBookingsByHouse(house.id.toString()).isEmpty()) {
            "Cannot remove ad with existing bookings."
        }
        services.houseService.deleteHouse(house.id.toString())
        Response(Status.NO_CONTENT)
    }

    private fun createBooking(request: Request): Response = safe {
        val user = requireAuthenticatedUser(request)

        val body = decodeBody<CreateBookingRequest>(request)
        val booking = services.bookingService.createBooking(
            bookerId = user.id,
            hidRaw = body.hid,
            startDateRaw = body.startDate,
            endDateRaw = body.endDate,
        )

        jsonResponse(
            Status.CREATED,
            CreateBookingResponse(
                id = booking.id.toString(),
                hid = booking.hid,
                uid = booking.uid.toString(),
                startDate = booking.startDate.toString(),
                endDate = booking.endDate.toString(),
            ),
        )
    }

    private fun listBookings(request: Request): Response = safe {
        val user = requireAuthenticatedUser(request)

        val hid = requireQuery(request, "hid")
        val date = requireQuery(request, "date")
        requireHouseOwnership(user, hid)
        val paging = pagingOf(request)

        val bookings = services.bookingService.listBookings(hid, date).page(paging)
        jsonResponse(Status.OK, ListBookingsResponse(bookings))
    }

    private fun getBooking(request: Request): Response = safe {
        val user = requireAuthenticatedUser(request)

        val bid = requirePath(request, "bid", "Booking id is required.")
        val booking = requireNotNull(services.bookingService.getBookingById(bid)) { "Booking not found." }
        val house = requireNotNull(services.houseService.getHouseById(booking.hid)) { "House not found." }
        if (booking.uid != user.id && house.uid != user.id) {
            throw UnauthorizedException("Only owner or booking user can view this booking.")
        }
        jsonResponse(Status.OK, booking.toGetBookingResponse())
    }

    private fun listAvailableHouses(request: Request): Response = safe {
        val startDate = requireQuery(request, "startDate")
        val endDate = requireQuery(request, "endDate")
        val paging = pagingOf(request)

        val houses = services.bookingService.listAvailableHouses(startDate, endDate)
            .map { it.toAvailableHouseResponse() }
            .page(paging)

        jsonResponse(Status.OK, ListAvailableHousesResponse(houses))
    }

    private fun requireAuthenticatedUser(request: Request): User =
        services.usersService.getUserByToken(
            bearerToken(request) ?: throw UnauthorizedException("Missing or invalid bearer token.")
        ) ?: throw UnauthorizedException("Invalid bearer token.")

    private fun requireHouseOwnership(user: User, hidRaw: String): domain.house.House {
        val house = requireNotNull(services.houseService.getHouseById(hidRaw)) { "House not found." }
        if (house.uid != user.id) {
            throw UnauthorizedException("Only the owner can manage this house ad.")
        }
        return house
    }

    private fun requirePath(request: Request, key: String, message: String): String {
        val value = request.path(key)?.trim().orEmpty()
        require(value.isNotEmpty()) { message }
        return value
    }

    private fun requireQuery(request: Request, key: String): String {
        return request.query(key)?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("$key query parameter is required.")
    }

    private fun pagingOf(request: Request): Paging =
        Paging.of(request.query("skip"), request.query("limit"))

    private inline fun <reified T> decodeBody(request: Request): T {
        val payload = request.bodyString().trim()
        require(payload.isNotEmpty()) { "Request body is required." }
        return json.decodeFromString(payload)
    }

    private inline fun <reified T> jsonResponse(status: Status, payload: T): Response {
        return Response(status)
            .header("Content-Type", "application/json; charset=utf-8")
            .body(json.encodeToString(payload))
    }

    private fun safe(block: () -> Response): Response {
        return try {
            block()
        } catch (ex: UnauthorizedException) {
            jsonResponse(Status.UNAUTHORIZED, ApiError(ex.message ?: "Unauthorized."))
        } catch (ex: IllegalArgumentException) {
            jsonResponse(Status.BAD_REQUEST, ApiError(ex.message ?: "Invalid request."))
        } catch (_: SerializationException) {
            jsonResponse(Status.BAD_REQUEST, ApiError("Invalid JSON body."))
        } catch (_: Throwable) {
            jsonResponse(Status.INTERNAL_SERVER_ERROR, ApiError("Internal server error."))
        }
    }
}

private class UnauthorizedException(message: String) : RuntimeException(message)
