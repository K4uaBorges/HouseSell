package main.api.http_server

import main.api.dto.AvailableHouseResponse
import main.api.dto.CreateBookingRequest
import main.api.dto.CreateBookingResponse
import main.api.dto.CreateHouseRequest
import main.api.dto.CreateHouseResponse
import main.api.dto.CreateLocationRequest
import main.api.dto.CreateLocationResponse
import main.api.dto.CreateUserRequest
import main.api.dto.CreateUserResponse
import main.api.dto.GetBookingResponse
import main.api.dto.GetHouseResponse
import main.api.dto.GetLocationResponse
import main.api.dto.GetUserResponse
import main.api.dto.ListAvailableHousesResponse
import main.api.dto.ListBookingsResponse
import main.api.dto.ListHousesResponse
import main.api.dto.ListLocationsResponse
import main.api.dto.ListUsersResponse
import main.api.dto.LocationPathEntry
import main.api.dto.LocationSummary
import main.api.dto.UpdateBookingRequest
import main.api.dto.UpdateHouseRequest
import main.api.dto.UpdateLocationRequest
import main.api.dto.UpdateUserRequest
import main.api.utils.Paging
import main.api.utils.page
import main.domain_model.booking.Booking
import main.domain_model.booking.BookingService
import main.domain_model.booking.Date
import main.domain_model.booking.toGetBookingResponse
import main.domain_model.house.House
import main.domain_model.house.HouseService
import main.domain_model.house.toGetHouseResponse
import main.domain_model.location.LocationService
import main.domain_model.location.toCreateLocationResponse
import main.domain_model.location.toLocationSummary
import main.domain_model.user.User
import main.domain_model.user.UsersService
import main.domain_model.user.toGetUserResponse
import main.errors.UnauthorizedException
import main.utils.BookingDateUtils
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HousesServices(
    private val houseService: HouseService,
    private val bookingService: BookingService,
    private val usersService: UsersService,
    private val locationService: LocationService,
) {
    // ==================== USER ====================

    fun createUser(
        token: Uuid,
        request: CreateUserRequest,
    ): CreateUserResponse {
        requireUserByToken(token)
        val user = usersService.createUser(request.name, request.email)
        return CreateUserResponse(
            id = user.id.toString(),
            name = user.name.value,
            email = user.email.value,
            token = user.token.toString(),
        )
    }

    fun getUser(uid: String): GetUserResponse = requireUserById(uid).toGetUserResponse()

    fun listUsers(paging: Paging): ListUsersResponse {
        val users = usersService.listUsers().map { it.toGetUserResponse() }.page(paging)
        return ListUsersResponse(users)
    }

    fun updateUser(
        token: Uuid,
        uid: String,
        request: UpdateUserRequest,
    ): GetUserResponse {
        requireUserByToken(token)
        val user = usersService.updateUserById(uid, request.name, request.email)
        return user.toGetUserResponse()
    }

    fun deleteUser(
        token: Uuid,
        uid: String,
    ) {
        requireUserByToken(token)
        usersService.deleteUserById(uid)
    }

    // ==================== LOCATION ====================

    fun createLocation(
        token: Uuid,
        request: CreateLocationRequest,
    ): CreateLocationResponse {
        requireUserByToken(token)
        val location =
            locationService.createLocation(
                nameRaw = request.name,
                typeRaw = request.type,
                parentIdRaw = request.parentId,
            )
        return location.toCreateLocationResponse()
    }

    fun updateLocation(
        token: Uuid,
        lid: String,
        request: UpdateLocationRequest,
    ): GetLocationResponse {
        requireUserByToken(token)
        locationService.updateLocation(
            idRaw = lid,
            nameRaw = request.name,
            typeRaw = request.type,
            parentIdRaw = request.parentId,
        )
        return locationService.getLocationInfoById(lid)
    }

    fun deleteLocation(
        token: Uuid,
        lid: String,
    ) {
        requireUserByToken(token)
        locationService.deleteLocation(lid)
    }

    fun getLocation(lid: String): GetLocationResponse = locationService.getLocationInfoById(lid)

    fun getLocationChildrenAll(lid: String): List<LocationSummary> = locationService.getChildrenAll(lid)

    fun getLocationChildrenDirect(lid: String): List<LocationSummary> = locationService.getChildrenDirect(lid)

    fun getLocationPath(lid: String): List<LocationPathEntry> = locationService.getLocationInfoById(lid).fullPath

    fun listLocations(): ListLocationsResponse {
        val locations = locationService.listLocations().map { it.toLocationSummary() }
        return ListLocationsResponse(locations)
    }

    // ==================== HOUSE ====================

    fun listHouses(paging: Paging): ListHousesResponse {
        val today = java.sql.Date.valueOf(LocalDate.now())
        val tomorrow = java.sql.Date.valueOf(LocalDate.now().plusDays(1))

        val houses =
            bookingService
                .listAvailableHouses(
                    BookingDateUtils.formatPostgresDate(today),
                    BookingDateUtils.formatPostgresDate(tomorrow),
                ).map { it.toGetHouseResponse() }
                .page(paging)

        return ListHousesResponse(houses)
    }

    fun listMyHouses(
        token: Uuid,
        paging: Paging,
    ): ListHousesResponse {
        val user = requireUserByToken(token)
        val houses = houseService.listHousesByOwner(user.id).map { it.toGetHouseResponse() }.page(paging)
        return ListHousesResponse(houses)
    }

    fun createHouse(
        token: Uuid,
        request: CreateHouseRequest,
    ): CreateHouseResponse {
        val user = requireUserByToken(token)
        val house =
            houseService.createHouse(
                ownerId = user.id,
                titleRaw = request.title,
                locationHouse = parseUuid(request.lid, "location id"),
                areaSqMt = request.areaSqMt,
                pricePerNight = request.pricePerNight,
                descriptionRaw = request.description,
            )

        return CreateHouseResponse(
            id = house.id.toString(),
            uid = house.uid.toString(),
            title = house.title.value,
            lid = house.lid.toString(),
            areaSqMt = house.areaSqMt,
            pricePerNight = house.pricePerNight,
            description = house.description,
        )
    }

    fun updateHouse(
        token: Uuid,
        hid: String,
        request: UpdateHouseRequest,
    ): GetHouseResponse {
        val user = requireUserByToken(token)
        val house = requireHouseOwnership(user, hid)
        val updated =
            houseService.updateHouse(
                id = house.id,
                titleRaw = request.title,
                locationRaw = parseUuid(request.lid, "location id"),
                areaSM = request.areaSqMt,
                pPN = request.pricePerNight,
                descriptionRaw = request.description.trim(),
            )
        return updated.toGetHouseResponse()
    }

    fun getHouse(hid: String): GetHouseResponse {
        val houseId = parseUuid(hid, "house id")
        return houseService.getHouseInfoById(houseId)
    }

    fun deleteHouse(
        token: Uuid,
        hid: String,
    ) {
        val user = requireUserByToken(token)
        val house = requireHouseOwnership(user, hid)

        require(bookingService.listBookingsByHouse(house.id).isEmpty()) {
            "Cannot remove ad with existing bookings."
        }

        houseService.deleteHouse(house.id)
    }

    // ==================== BOOKING ====================

    fun createBooking(
        token: Uuid,
        request: CreateBookingRequest,
    ): CreateBookingResponse {
        val user = requireUserByToken(token)
        val booking =
            bookingService.createBooking(
                bookerId = user.id,
                hid = parseUuid(request.hid, "house id"),
                startDateRaw = request.startDate,
                endDateRaw = request.endDate,
            )

        return CreateBookingResponse(
            id = booking.id.toString(),
            hid = booking.hid.toString(),
            uid = booking.uid.toString(),
            startDate = booking.startDate.toString(),
            endDate = booking.endDate.toString(),
        )
    }

    fun updateBooking(
        token: Uuid,
        bid: String,
        request: UpdateBookingRequest,
    ): GetBookingResponse {
        val user = requireUserByToken(token)
        val booking = requireBookingAccess(user, bid)
        val updated =
            bookingService.updateBooking(
                booking = booking,
                hid = parseUuid(request.hid, "house id"),
                startDateRaw = request.startDate,
                endDateRaw = request.endDate,
            )

        return updated.toGetBookingResponse()
    }

    fun deleteBooking(
        token: Uuid,
        bid: String,
    ) {
        val user = requireUserByToken(token)
        val booking = requireBookingAccess(user, bid)
        bookingService.deleteBooking(booking.id)
    }

    fun listBookings(
        token: Uuid,
        hid: String,
        dateStart: String,
        dateEnd: String,
        paging: Paging,
    ): ListBookingsResponse {
        val user = requireUserByToken(token)
        val hidUuid = parseUuid(hid, "house id")
        requireHouseOwnership(user, hid)

        val bookings = bookingService.listBookings(hidUuid, Date.of(dateStart), Date.of(dateEnd)).page(paging)
        return ListBookingsResponse(bookings)
    }

    fun getBooking(
        token: Uuid,
        bid: String,
    ): GetBookingResponse {
        val user = requireUserByToken(token)
        val booking = requireBookingAccess(user, bid)
        return booking.toGetBookingResponse()
    }

    fun listAvailableHouses(
        startDate: String,
        endDate: String,
        paging: Paging,
    ): ListAvailableHousesResponse {
        val houses =
            bookingService
                .listAvailableHouses(startDate, endDate)
                .map {
                    AvailableHouseResponse(
                        id = it.id.toString(),
                        uid = it.uid.toString(),
                        title = it.title.value,
                        lid = it.lid.toString(),
                        areaSqMt = it.areaSqMt,
                        pricePerNight = it.pricePerNight,
                        description = it.description,
                    )
                }.page(paging)

        return ListAvailableHousesResponse(houses)
    }

    private fun requireUserByToken(token: Uuid): User =
        runCatching { usersService.getUserByToken(token) }
            .getOrElse { throw UnauthorizedException("Invalid bearer token.") }

    private fun requireUserById(uidRaw: String): User {
        val uid = parseUuid(uidRaw, "user id")
        return usersService.getUserById(uid)
    }

    private fun requireHouseOwnership(
        user: User,
        hidRaw: String,
    ): House {
        val hid = parseUuid(hidRaw, "house id")
        val house = houseService.getHouseById(hid)
        if (house.uid != user.id) {
            throw UnauthorizedException("Only the owner can manage this house ad.")
        }
        return house
    }

    private fun requireBookingAccess(
        user: User,
        bidRaw: String,
    ): Booking {
        val bid = parseUuid(bidRaw, "booking id")
        val booking = bookingService.getBookingById(bid)
        val house = houseService.getHouseById(booking.hid)
        if (booking.uid != user.id && house.uid != user.id) {
            throw UnauthorizedException("Only owner or booking user can access this booking.")
        }
        return booking
    }

    private fun parseUuid(
        raw: String,
        label: String,
    ): Uuid =
        runCatching { Uuid.parse(raw.trim()) }
            .getOrElse { throw IllegalArgumentException("Invalid $label.") }
}
