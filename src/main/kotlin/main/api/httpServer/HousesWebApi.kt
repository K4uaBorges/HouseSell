package main.api.httpServer

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import main.api.dto.CreateBookingRequest
import main.api.dto.CreateHouseRequest
import main.api.dto.CreateLocationRequest
import main.api.dto.CreateUserRequest
import main.api.dto.DeleteBookingRequest
import main.api.dto.DeleteBookingResponse
import main.api.dto.DeleteHouseRequest
import main.api.dto.DeleteHouseResponse
import main.api.dto.DeleteLocationRequest
import main.api.dto.DeleteLocationResponse
import main.api.dto.DeleteUserRequest
import main.api.dto.DeleteUserResponse
import main.api.dto.LoginUserRequest
import main.api.dto.UpdateBookingRequest
import main.api.dto.UpdateHouseRequest
import main.api.dto.UpdateLocationRequest
import main.api.dto.UpdateUserRequest
import main.api.errors.ApiError
import main.api.utils.Paging
import main.api.utils.bearerToken
import main.api.utils.bearerTokenOrNull
import main.errors.DomainErrorException
import main.errors.LidNotLocatityException
import main.errors.NoBookingExist
import main.errors.NoHouseExist
import main.errors.NoLocationExist
import main.errors.NoUserExist
import main.errors.ServerErrorException
import main.errors.UnauthorizedException
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class HousesWebApi(
    private val services: HousesServices,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val routes: RoutingHttpHandler =
        routes(
            // Users
            "/session/bootstrap" bind Method.GET to ::bootstrapSession,
            "/session/login" bind Method.POST to ::loginSession,
            "/users" bind Method.POST to ::createUser,
            "/users" bind Method.GET to ::listUsers,
            "/users/{uid}" bind Method.GET to ::getUser,
            "/users/{uid}" bind Method.PUT to ::updateUser,
            "/users/{uid}" bind Method.DELETE to ::deleteUser,
            // Locations
            "/locations" bind Method.POST to ::createLocation,
            "/locations" bind Method.GET to ::listLocations,
            "/locations/getCountries" bind Method.GET to ::listCountries,
            "/locations/{lid}" bind Method.GET to ::getLocation,
            "/locations/{lid}" bind Method.PUT to ::updateLocation,
            "/locations/{lid}" bind Method.DELETE to ::deleteLocation,
            "/locations/{lid}/childrenAll" bind Method.GET to ::getLocationChildrenAll,
            "/locations/{lid}/childrenDirect" bind Method.GET to ::getLocationChildrenDirect,
            "/locations/{lid}/path" bind Method.GET to ::getLocationPath,
            // Houses
            "/houses" bind Method.GET to ::listHouses,
            "/houses/mine" bind Method.GET to ::listMyHouses,
            "/houses" bind Method.POST to ::createHouse,
            "/houses/available" bind Method.GET to ::listAvailableHouses,
            "/houses/preview" bind Method.GET to ::previewHousePrice,
            "/houses/cache/stats" bind Method.GET to ::getHouseCacheStats,
            "/houses/{hid}" bind Method.GET to ::getHouse,
            "/houses/{hid}" bind Method.PUT to ::updateHouse,
            "/houses/{hid}" bind Method.DELETE to ::deleteHouse,
            "/houses/{hid}/available-days" bind Method.GET to ::listAvailableHouseDays,
            // Bookings
            "/bookings" bind Method.POST to ::createBooking,
            "/bookings" bind Method.GET to ::listBookings,
            "/bookings/mine" bind Method.GET to ::listMyBookings,
            "/bookings/{bid}" bind Method.GET to ::getBooking,
            "/bookings/{bid}" bind Method.PUT to ::updateBooking,
            "/bookings/{bid}" bind Method.DELETE to ::deleteBooking,
        )

    // ==================== USER ====================
    private fun bootstrapSession(request: Request): Response =
        safe {
            jsonResponse(Status.OK, services.ensureBootstrapSession())
        }

    private fun loginSession(request: Request): Response =
        safe {
            val body = decodeBody<LoginUserRequest>(request)
            jsonResponse(Status.OK, services.loginUser(body))
        }

    private fun createUser(request: Request): Response =
        safe {
            val body = decodeBody<CreateUserRequest>(request)
            jsonResponse(Status.CREATED, services.createUser(body))
        }

    private fun listUsers(request: Request): Response =
        safe {
            jsonResponse(Status.OK, services.listUsers(pagingOf(request)))
        }

    private fun getUser(request: Request): Response =
        safe {
            val uid = requirePath(request, "uid", "User id is required.")
            jsonResponse(Status.OK, services.getUser(uid))
        }

    private fun updateUser(request: Request): Response =
        safe {
            val token = bearerToken(request)
            val uid = requirePath(request, "uid", "User id is required.")
            val body = decodeBody<UpdateUserRequest>(request)
            jsonResponse(Status.OK, services.updateUser(token, uid, body))
        }

    private fun deleteUser(request: Request): Response =
        safe {
            val token = bearerToken(request)
            val uid = requirePath(request, "uid", "User id is required.")
            val body = decodeOptionalBody<DeleteUserRequest>(request)
            validateDeleteRequestId(uid, body?.id, "user id")
            services.deleteUser(token, uid)
            jsonResponse(Status.OK, DeleteUserResponse(id = uid, deleted = true))
        }

    // ==================== LOCATION ====================
    private fun createLocation(request: Request): Response =
        safe {
            val token = bearerToken(request)
            val body = decodeBody<CreateLocationRequest>(request)
            jsonResponse(Status.CREATED, services.createLocation(token, body))
        }

    private fun listLocations(request: Request): Response =
        safe {
            jsonResponse(Status.OK, services.listLocations(pagingOf(request)))
        }

    private fun listCountries(request: Request): Response =
        safe {
            jsonResponse(Status.OK, services.listCountries())
        }

    private fun getLocation(request: Request): Response =
        safe {
            val lid = requirePath(request, "lid", "Location id is required.")
            jsonResponse(Status.OK, services.getLocation(lid))
        }

    private fun updateLocation(request: Request): Response =
        safe {
            val token = bearerToken(request)
            val lid = requirePath(request, "lid", "Location id is required.")
            val body = decodeBody<UpdateLocationRequest>(request)
            jsonResponse(Status.OK, services.updateLocation(token, lid, body))
        }

    private fun deleteLocation(request: Request): Response =
        safe {
            val token = bearerToken(request)
            val lid = requirePath(request, "lid", "Location id is required.")
            val body = decodeOptionalBody<DeleteLocationRequest>(request)
            validateDeleteRequestId(lid, body?.id, "location id")
            services.deleteLocation(token, lid)
            jsonResponse(Status.OK, DeleteLocationResponse(lid = lid, deleted = true))
        }

    private fun getLocationChildrenAll(request: Request): Response =
        safe {
            val lid = requirePath(request, "lid", "Location id is required.")
            jsonResponse(Status.OK, services.getLocationChildrenAll(lid))
        }

    private fun getLocationChildrenDirect(request: Request): Response =
        safe {
            val lid = requirePath(request, "lid", "Location id is required.")
            jsonResponse(Status.OK, services.getLocationChildrenDirect(lid))
        }

    private fun getLocationPath(request: Request): Response =
        safe {
            val lid = requirePath(request, "lid", "Location id is required.")
            jsonResponse(Status.OK, services.getLocationPath(lid))
        }

    // ==================== HOUSE ====================
    private fun listHouses(request: Request): Response =
        safe {
            jsonResponse(Status.OK, services.listHouses(pagingOf(request)))
        }

    private fun listMyHouses(request: Request): Response =
        safe {
            jsonResponse(Status.OK, services.listMyHouses(bearerToken(request), pagingOf(request)))
        }

    private fun createHouse(request: Request): Response =
        safe {
            val body = decodeBody<CreateHouseRequest>(request)
            jsonResponse(Status.CREATED, services.createHouse(bearerToken(request), body))
        }

    private fun getHouse(request: Request): Response =
        safe {
            val hid = requirePath(request, "hid", "House id is required.")
            jsonResponse(Status.OK, services.getHouse(hid))
        }

    private fun previewHousePrice(request: Request): Response =
        safe {
            val areaSqMt = requireQueryInt(request, "areaSqMt")
            jsonResponse(Status.OK, services.previewHousePrice(areaSqMt))
        }

    private fun getHouseCacheStats(request: Request): Response =
        safe {
            jsonResponse(Status.OK, services.getHouseCacheStats())
        }

    private fun updateHouse(request: Request): Response =
        safe {
            val hid = requirePath(request, "hid", "House id is required.")
            val body = decodeBody<UpdateHouseRequest>(request)
            jsonResponse(Status.OK, services.updateHouse(bearerToken(request), hid, body))
        }

    private fun deleteHouse(request: Request): Response =
        safe {
            val hid = requirePath(request, "hid", "House id is required.")
            val body = decodeOptionalBody<DeleteHouseRequest>(request)
            validateDeleteRequestId(hid, body?.id, "house id")
            services.deleteHouse(bearerToken(request), hid)
            jsonResponse(Status.OK, DeleteHouseResponse(hid = hid, deleted = true))
        }

    // ==================== BOOKING ====================
    private fun createBooking(request: Request): Response =
        safe {
            val body = decodeBody<CreateBookingRequest>(request)
            jsonResponse(Status.CREATED, services.createBooking(bearerToken(request), body))
        }

    private fun listBookings(request: Request): Response =
        safe {
            val hid = requireQuery(request, "hid")
            val dateS = requireQuery(request, "dateStart")
            val dateE = requireQuery(request, "dateEnd")
            jsonResponse(Status.OK, services.listBookings(bearerToken(request), hid, dateS, dateE, pagingOf(request)))
        }

    private fun listMyBookings(request: Request): Response =
        safe {
            jsonResponse(Status.OK, services.listMyBookings(bearerToken(request), pagingOf(request)))
        }

    private fun getBooking(request: Request): Response =
        safe {
            val bid = requirePath(request, "bid", "Booking id is required.")
            jsonResponse(Status.OK, services.getBooking(bearerToken(request), bid))
        }

    private fun updateBooking(request: Request): Response =
        safe {
            val bid = requirePath(request, "bid", "Booking id is required.")
            val body = decodeBody<UpdateBookingRequest>(request)
            jsonResponse(Status.OK, services.updateBooking(bearerToken(request), bid, body))
        }

    private fun deleteBooking(request: Request): Response =
        safe {
            val bid = requirePath(request, "bid", "Booking id is required.")
            val body = decodeOptionalBody<DeleteBookingRequest>(request)
            validateDeleteRequestId(bid, body?.id, "booking id")
            services.deleteBooking(bearerToken(request), bid)
            jsonResponse(Status.OK, DeleteBookingResponse(bid = bid, deleted = true))
        }

    private fun listAvailableHouses(request: Request): Response =
        safe {
            val startDate = requireQuery(request, "startDate")
            val endDate = requireQuery(request, "endDate")
            val locationId = request.query("locationId")
            val search = request.query("search")
            jsonResponse(
                Status.OK,
                services.listAvailableHouses(startDate, endDate, pagingOf(request), bearerTokenOrNull(request), locationId, search),
            )
        }

    private fun listAvailableHouseDays(request: Request): Response =
        safe {
            val hid = requirePath(request, "hid", "House id is required.")
            val year = requireQueryInt(request, "year")
            val month = requireQueryInt(request, "month")
            jsonResponse(Status.OK, services.listAvailableHouseDays(hid, year, month))
        }

    private fun requirePath(
        request: Request,
        key: String,
        message: String,
    ): String {
        val value = request.path(key)?.trim().orEmpty()
        require(value.isNotEmpty()) { message }
        return value
    }

    private fun requireQuery(
        request: Request,
        key: String,
    ): String =
        request.query(key)?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("$key query parameter is required.")

    private fun requireQueryInt(
        request: Request,
        key: String,
    ): Int =
        requireQuery(request, key).toIntOrNull()
            ?: throw IllegalArgumentException("$key query parameter must be an integer.")

    private fun pagingOf(request: Request): Paging = Paging.of(request.query("skip"), request.query("limit"))

    private inline fun <reified T> decodeBody(request: Request): T {
        val payload = request.bodyString().trim()
        require(payload.isNotEmpty()) { "Request body is required." }
        return json.decodeFromString(payload)
    }

    private inline fun <reified T> decodeOptionalBody(request: Request): T? {
        val payload = request.bodyString().trim()
        if (payload.isEmpty()) return null
        return json.decodeFromString(payload)
    }

    private fun validateDeleteRequestId(
        pathId: String,
        bodyId: String?,
        label: String,
    ) {
        if (bodyId == null) return
        require(bodyId.trim() == pathId) { "Request body $label must match route id." }
    }

    private inline fun <reified T> jsonResponse(
        status: Status,
        payload: T,
    ): Response =
        Response(status)
            .header("Content-Type", "application/json; charset=utf-8")
            .body(json.encodeToString(payload))

    private fun safe(block: () -> Response): Response =
        try {
            block()
        } catch (ex: UnauthorizedException) {
            jsonResponse(Status.UNAUTHORIZED, ApiError(Status.UNAUTHORIZED.code, ex.message ?: "Unauthorized."))
        } catch (ex: NoUserExist) {
            jsonResponse(Status.NOT_FOUND, ApiError(Status.NOT_FOUND.code, ex.message ?: "User not found."))
        } catch (ex: NoHouseExist) {
            jsonResponse(Status.NOT_FOUND, ApiError(Status.NOT_FOUND.code, ex.message ?: "House not found."))
        } catch (ex: LidNotLocatityException) {
            jsonResponse(Status.BAD_REQUEST, ApiError(Status.BAD_REQUEST.code, ex.message ?: "Invalid location id."))
        } catch (ex: NoLocationExist) {
            jsonResponse(Status.NOT_FOUND, ApiError(Status.NOT_FOUND.code, ex.message ?: "Location not found."))
        } catch (ex: NoBookingExist) {
            jsonResponse(Status.NOT_FOUND, ApiError(Status.NOT_FOUND.code, ex.message ?: "Booking not found."))
        } catch (_: SerializationException) {
            jsonResponse(Status.BAD_REQUEST, ApiError(Status.BAD_REQUEST.code, "Invalid JSON body."))
        } catch (ex: DomainErrorException) {
            jsonResponse(Status.BAD_REQUEST, ApiError(Status.BAD_REQUEST.code, ex.message ?: "Invalid request."))
        } catch (ex: IllegalArgumentException) {
            jsonResponse(Status.BAD_REQUEST, ApiError(Status.BAD_REQUEST.code, ex.message ?: "Invalid request."))
        } catch (ex: ServerErrorException) {
            ex.printStackTrace()
            jsonResponse(Status.INTERNAL_SERVER_ERROR, ApiError(Status.INTERNAL_SERVER_ERROR.code, ex.message ?: "Internal server error."))
        } catch (ex: Throwable) {
            ex.printStackTrace()
            jsonResponse(Status.INTERNAL_SERVER_ERROR, ApiError(Status.INTERNAL_SERVER_ERROR.code, "Internal server error."))
        }
}
