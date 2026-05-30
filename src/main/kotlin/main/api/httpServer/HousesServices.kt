package main.api.httpServer

import main.api.dto.BootstrapSessionResponse
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
import main.api.dto.HouseCacheStatsResponse
import main.api.dto.HousePricePreviewResponse
import main.api.dto.ListAvailableHouseDaysResponse
import main.api.dto.ListAvailableHousesResponse
import main.api.dto.ListBookingsResponse
import main.api.dto.ListHousesResponse
import main.api.dto.ListLocationsResponse
import main.api.dto.ListUsersResponse
import main.api.dto.LocationPathEntry
import main.api.dto.LocationSummary
import main.api.dto.LoginUserRequest
import main.api.dto.LoginUserResponse
import main.api.dto.UpdateBookingRequest
import main.api.dto.UpdateHouseRequest
import main.api.dto.UpdateLocationRequest
import main.api.dto.UpdateUserRequest
import main.api.dto.UserSessionResponse
import main.api.utils.Paging
import main.api.utils.page
import main.domain.booking.Booking
import main.domain.booking.BookingService
import main.domain.booking.Date
import main.domain.booking.toGetBookingResponse
import main.domain.house.House
import main.domain.house.HouseService
import main.domain.house.toGetHouseResponse
import main.domain.location.LocationService
import main.domain.location.LocationType
import main.domain.location.toCreateLocationResponse
import main.domain.location.toLocationSummary
import main.domain.prediction.loadTrainingData
import main.domain.prediction.predictPriceForArea
import main.domain.prediction.trainModel
import main.domain.user.User
import main.domain.user.UsersService
import main.domain.user.toGetUserResponse
import main.errors.LidNotLocatityException
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

    fun createUser(request: CreateUserRequest): CreateUserResponse {
        val user = usersService.createUser(request.name, request.email, request.password)
        return user.toSessionResponse()
    }

    fun loginUser(request: LoginUserRequest): LoginUserResponse {
        val user = usersService.authenticateUser(request.email, request.password)
        return user.toSessionResponse()
    }

    fun ensureBootstrapSession(): BootstrapSessionResponse {
        val principalUser =
            usersService.listUsers().firstOrNull { it.email.value.equals(BOOTSTRAP_USER_EMAIL, ignoreCase = true) }
                ?: usersService.createUser(BOOTSTRAP_USER_NAME, BOOTSTRAP_USER_EMAIL, BOOTSTRAP_USER_PASSWORD)

        val location =
            locationService
                .listLocations()
                .firstOrNull {
                    it.name.value.equals(BOOTSTRAP_LOCATION_NAME, ignoreCase = true) &&
                        it.type == LocationType.COUNTRY
                }
                ?: locationService.createLocation(
                    nameRaw = BOOTSTRAP_LOCATION_NAME,
                    typeRaw = LocationType.COUNTRY.name,
                    parentIdRaw = null,
                )

        val housesByTitle =
            houseService
                .listHousesByOwner(principalUser.id)
                .associateBy { it.title.value.lowercase() }

        val freeHouse =
            housesByTitle[BOOTSTRAP_FREE_HOUSE_TITLE.lowercase()]
                ?: houseService.createHouse(
                    ownerId = principalUser.id,
                    titleRaw = BOOTSTRAP_FREE_HOUSE_TITLE,
                    locationHouse = location.id,
                    areaSqMt = 110,
                    pricePerNight = 90.0,
                    descriptionRaw = "Casa de demonstração disponível",
                )

        val busyHouse =
            housesByTitle[BOOTSTRAP_BUSY_HOUSE_TITLE.lowercase()]
                ?: houseService.createHouse(
                    ownerId = principalUser.id,
                    titleRaw = BOOTSTRAP_BUSY_HOUSE_TITLE,
                    locationHouse = location.id,
                    areaSqMt = 140,
                    pricePerNight = 120.0,
                    descriptionRaw = "Casa de demonstração ocupada",
                )

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val busyHouseAvailableToday =
            bookingService
                .listAvailableHouses(today.toString(), tomorrow.toString())
                .any { it.id == busyHouse.id }

        if (busyHouseAvailableToday) {
            bookingService.createBooking(
                bookerId = principalUser.id,
                hid = busyHouse.id,
                startDateRaw = today.toString(),
                endDateRaw = tomorrow.toString(),
            )
        }

        return BootstrapSessionResponse(
            token = principalUser.token.toString(),
            userId = principalUser.id.toString(),
            role = principalUser.role.name,
            locationId = location.id.toString(),
            freeHouseId = freeHouse.id.toString(),
            busyHouseId = busyHouse.id.toString(),
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
        val user = requireUserByToken(token)
        val location =
            locationService.createLocation(
                nameRaw = request.name,
                typeRaw = request.type,
                parentIdRaw = request.parentId,
            )
        return location.toCreateLocationResponse().copy(token = user.token.toString())
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

    fun listLocations(paging: Paging): ListLocationsResponse {
        val locations = locationService.listLocations().map { it.toLocationSummary() }.page(paging)
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
        requireLocality(request.lid)
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
            token = user.token.toString(),
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

    fun previewHousePrice(areaSqMt: Int): HousePricePreviewResponse {
        require(areaSqMt > 0) { "areaSqMt must be greater than zero." }

        val trainingData = loadTrainingData()
        val model = trainModel(trainingData.houses)

        return HousePricePreviewResponse(
            areaSqMt = areaSqMt,
            predictedPricePerNight = predictPriceForArea(areaSqMt, model),
            trainingSource = trainingData.source.name,
            trainingSamples = trainingData.houses.size,
            modelWeight = model.params.w,
            modelBias = model.params.b,
        )
    }

    fun getHouseCacheStats(): HouseCacheStatsResponse {
        val stats = houseService.cacheStats()
        val totalAccesses = stats.hits + stats.misses
        val hitRate =
            if (totalAccesses == 0L) 0.0 else stats.hits.toDouble() / totalAccesses.toDouble()

        return HouseCacheStatsResponse(
            limit = stats.limit,
            size = stats.size,
            hits = stats.hits,
            misses = stats.misses,
            hitRate = hitRate,
        )
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
        val house = houseService.getHouseById(parseUuid(request.hid, "house id"))
        require(house.uid != user.id) { "Não podes criar bookings para a tua própria house." }
        val booking =
            bookingService.createBooking(
                bookerId = user.id,
                hid = house.id,
                startDateRaw = request.startDate,
                endDateRaw = request.endDate,
            )
        return CreateBookingResponse(
            id = booking.id.toString(),
            hid = booking.hid.toString(),
            uid = booking.uid.toString(),
            startDate = booking.startDate.toString(),
            endDate = booking.endDate.toString(),
            token = user.token.toString(),
        )
    }

    fun updateBooking(
        token: Uuid,
        bid: String,
        request: UpdateBookingRequest,
    ): GetBookingResponse {
        val user = requireUserByToken(token)
        val booking = requireBookingUser(user, bid)
        val updated =
            bookingService.updateBooking(
                booking = booking,
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
        val booking = requireBookingUser(user, bid)
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

    fun listMyBookings(
        token: Uuid,
        paging: Paging,
    ): ListBookingsResponse {
        val user = requireUserByToken(token)
        val bookings = bookingService.listBookingsByUser(user.id).map { it.toGetBookingResponse() }.page(paging)
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
        token: Uuid? = null,
        locationIdRaw: String? = null,
        searchRaw: String? = null,
    ): ListAvailableHousesResponse {
        val ownerId = token?.let { requireUserByToken(it).id }
        val locationId =
            locationIdRaw
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { parseUuid(it, "location id") }
        val search =
            searchRaw
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotEmpty() }
        val houses =
            bookingService
                .listAvailableHouses(startDate, endDate)
                .filter { house -> ownerId == null || house.uid != ownerId }
                .filter { house ->
                    locationId == null || locationService.isSameOrDescendant(house.lid, locationId)
                }.filter { house ->
                    search == null ||
                        house.title.value.lowercase().contains(search) ||
                        house.description.lowercase().contains(search)
                }
                .map { it.toGetHouseResponse() }
                .page(paging)

        return ListAvailableHousesResponse(houses)
    }

    fun listAvailableHouseDays(
        hid: String,
        year: Int,
        month: Int,
    ): ListAvailableHouseDaysResponse {
        val houseId = parseUuid(hid, "house id")
        val availableDays =
            bookingService
                .listAvailableDays(houseId, year, month)
                .map { it.toString() }

        return ListAvailableHouseDaysResponse(
            houseId = houseId.toString(),
            year = year,
            month = month,
            availableDays = availableDays,
        )
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

    private fun requireLocality(lid: String) {
        val location = locationService.getLocationById(lid)
        if (location.type != LocationType.LOCALITY) {
            throw LidNotLocatityException("Location must be of type LOCALITY.")
        }
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

    private fun requireBookingUser(
        user: User,
        bidRaw: String,
    ): Booking {
        val bid = parseUuid(bidRaw, "booking id")
        val booking = bookingService.getBookingById(bid)
        if (booking.uid != user.id) {
            throw UnauthorizedException("Only the booking user can manage this booking.")
        }
        return booking
    }

    private fun parseUuid(
        raw: String,
        label: String,
    ): Uuid =
        runCatching { Uuid.parse(raw.trim()) }
            .getOrElse { throw IllegalArgumentException("Invalid $label.") }

    private fun User.toSessionResponse(): UserSessionResponse =
        UserSessionResponse(
            id = id.toString(),
            name = name.value,
            email = email.value,
            token = token.toString(),
            role = role.name,
        )

    companion object {
        private const val BOOTSTRAP_USER_NAME = "Principal Demo"
        private const val BOOTSTRAP_USER_EMAIL = "principal.demo@houses.local"
        private const val BOOTSTRAP_USER_PASSWORD = "Demo123"
        private const val BOOTSTRAP_LOCATION_NAME = "Demo Country"
        private const val BOOTSTRAP_FREE_HOUSE_TITLE = "Casa Demo Livre"
        private const val BOOTSTRAP_BUSY_HOUSE_TITLE = "Casa Demo Ocupada"
    }
}
