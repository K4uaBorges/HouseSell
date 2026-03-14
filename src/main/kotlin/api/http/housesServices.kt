package api.http

import domain.booking.*
import domain.house.*
import domain.user.*
import java.time.*
import utils.*
import kotlin.uuid.*

class UnauthorizedException(message: String) : RuntimeException(message)

@OptIn(ExperimentalUuidApi::class)
class HousesServices(
    private val houseService: HouseService,
    private val bookingService: BookingService,
    private val usersService: UsersService,
) {

    fun createUser(request: CreateUserRequest): CreateUserResponse {
        val user = usersService.createUser(request.name, request.email)
        return CreateUserResponse(
            id = user.id.toString(),
            name = user.name,
            email = user.email.value,
            token = user.token.toString(),
        )
    }

    fun listHouses(paging: Paging): ListHousesResponse {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val houses = bookingService
            .listAvailableHouses(
                BookingDateUtils.formatPostgresDate(today),
                BookingDateUtils.formatPostgresDate(tomorrow),
            )
            .map { it.toGetHouseResponse() }
            .page(paging)

        return ListHousesResponse(houses)
    }

    fun listMyHouses(token: Uuid?, paging: Paging): ListHousesResponse {
        val user = requireUserByToken(token)
        val houses = houseService
            .listHousesByOwner(user.id)
            .map { it.toGetHouseResponse() }
            .page(paging)

        return ListHousesResponse(houses)
    }

    fun createHouse(token: Uuid?, request: CreateHouseRequest): CreateHouseResponse {
        val user = requireUserByToken(token)
        val house = houseService.createHouse(
            ownerId = user.id,
            titleRaw = request.title,
            locationRaw = request.location,
            areaSqMt = request.areaSqMt,
            pricePerNight = request.pricePerNight,
            descriptionRaw = request.description,
        )

        return CreateHouseResponse(
            id = house.id.toString(),
            uid = house.uid.toString(),
            title = house.title.value,
            location = house.location,
            areaSqMt = house.areaSqMt,
            pricePerNight = house.pricePerNight,
            description = house.description,
        )
    }

    fun getHouse(hid: String): GetHouseResponse = houseService.getHouseInfoById(hid)

    fun deleteHouse(token: Uuid?, hid: String) {
        val user = requireUserByToken(token)
        val house = requireHouseOwnership(user, hid)

        require(bookingService.listBookingsByHouse(house.id.toString()).isEmpty()) {
            "Cannot remove ad with existing bookings."
        }

        houseService.deleteHouse(house.id.toString())
    }

    fun createBooking(token: Uuid?, request: CreateBookingRequest): CreateBookingResponse {
        val user = requireUserByToken(token)
        val booking = bookingService.createBooking(
            bookerId = user.id,
            hidRaw = request.hid,
            startDateRaw = request.startDate,
            endDateRaw = request.endDate,
        )

        return CreateBookingResponse(
            id = booking.id.toString(),
            hid = booking.hid,
            uid = booking.uid.toString(),
            startDate = booking.startDate.toString(),
            endDate = booking.endDate.toString(),
        )
    }

    fun listBookings(token: Uuid?, hid: String, date: String, paging: Paging): ListBookingsResponse {
        val user = requireUserByToken(token)
        requireHouseOwnership(user, hid)

        val bookings = bookingService
            .listBookings(hid, date)
            .page(paging)

        return ListBookingsResponse(bookings)
    }

    fun getBooking(token: Uuid?, bid: String): domain.booking.GetBookingResponse {
        val user = requireUserByToken(token)

        val booking = requireNotNull(bookingService.getBookingById(bid)) { "Booking not found." }
        val house = requireNotNull(houseService.getHouseById(booking.hid)) { "House not found." }

        if (booking.uid != user.id && house.uid != user.id) {
            throw UnauthorizedException("Only owner or booking user can view this booking.")
        }

        return booking.toGetBookingResponse()
    }

    fun listAvailableHouses(startDate: String, endDate: String, paging: Paging): ListAvailableHousesResponse {
        val houses = bookingService
            .listAvailableHouses(startDate, endDate)
            .map {
                AvailableHouseResponse(
                    id = it.id.toString(),
                    uid = it.uid.toString(),
                    title = it.title.value,
                    location = it.location,
                    areaSqMt = it.areaSqMt,
                    pricePerNight = it.pricePerNight,
                    description = it.description,
                )
            }
            .page(paging)

        return ListAvailableHousesResponse(houses)
    }

    private fun requireUserByToken(token: Uuid?): User {
        if (token == null) {
            throw UnauthorizedException("Missing or invalid bearer token.")
        }

        return usersService.getUserByToken(token)
            ?: throw UnauthorizedException("Invalid bearer token.")
    }

    private fun requireHouseOwnership(user: User, hidRaw: String): House {
        val house = requireNotNull(houseService.getHouseById(hidRaw)) { "House not found." }
        if (house.uid != user.id) {
            throw UnauthorizedException("Only the owner can manage this house ad.")
        }
        return house
    }
}
