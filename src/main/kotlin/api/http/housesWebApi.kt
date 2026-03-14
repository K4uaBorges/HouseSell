package api.http

import api.ApiError
import domain.booking.CreateBookingRequest
import domain.house.CreateHouseRequest
import domain.user.CreateUserRequest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.http4k.core.*
import org.http4k.routing.*
import utils.*
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
        jsonResponse(Status.CREATED, services.createUser(body))
    }

    private fun listHouses(request: Request): Response = safe {
        jsonResponse(Status.OK, services.listHouses(pagingOf(request)))
    }

    private fun listMyHouses(request: Request): Response = safe {
        jsonResponse(Status.OK, services.listMyHouses(bearerToken(request), pagingOf(request)))
    }

    private fun createHouse(request: Request): Response = safe {
        val body = decodeBody<CreateHouseRequest>(request)
        jsonResponse(Status.CREATED, services.createHouse(bearerToken(request), body))
    }

    private fun getHouse(request: Request): Response = safe {
        val hid = requirePath(request, "hid", "House id is required.")
        jsonResponse(Status.OK, services.getHouse(hid))
    }

    private fun deleteHouse(request: Request): Response = safe {
        val hid = requirePath(request, "hid", "House id is required.")
        services.deleteHouse(bearerToken(request), hid)
        Response(Status.NO_CONTENT)
    }

    private fun createBooking(request: Request): Response = safe {
        val body = decodeBody<CreateBookingRequest>(request)
        jsonResponse(Status.CREATED, services.createBooking(bearerToken(request), body))
    }

    private fun listBookings(request: Request): Response = safe {
        val hid = requireQuery(request, "hid")
        val date = requireQuery(request, "date")
        jsonResponse(Status.OK, services.listBookings(bearerToken(request), hid, date, pagingOf(request)))
    }

    private fun getBooking(request: Request): Response = safe {
        val bid = requirePath(request, "bid", "Booking id is required.")
        jsonResponse(Status.OK, services.getBooking(bearerToken(request), bid))
    }

    private fun listAvailableHouses(request: Request): Response = safe {
        val startDate = requireQuery(request, "startDate")
        val endDate = requireQuery(request, "endDate")
        jsonResponse(Status.OK, services.listAvailableHouses(startDate, endDate, pagingOf(request)))
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
