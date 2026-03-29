package main.domain_model.booking

import main.api.dto.GetBookingResponse
import main.data.interfaces.BookingRepository
import main.data.interfaces.HouseRepository
import main.domain_model.house.House
import main.utils.BookingDateUtils
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class BookingService(
    private val bookingRepo: BookingRepository,
    private val houseRepo: HouseRepository,
) {
    fun createBooking(
        bookerId: Uuid,
        hid: Uuid,
        startDateRaw: String,
        endDateRaw: String,
    ): Booking {
        val house = requireExistingHouse(hid)
        val startDate = parseDate("startDate", startDateRaw)
        val endDate = parseDate("endDate", endDateRaw)
        validateDateRange(startDate, endDate)
        requireNoOverlap(house.id, startDate, endDate)
        return bookingRepo.create(
            Booking(
                id = Uuid.random(),
                hid = house.id,
                uid = bookerId,
                startDate = startDate,
                endDate = endDate,
            ),
        )
    }

    fun getBookingById(id: Uuid): Booking = bookingRepo.getById(id)

    fun updateBooking(
        booking: Booking,
        hid: Uuid,
        startDateRaw: String,
        endDateRaw: String,
    ): Booking {
        val house = requireExistingHouse(hid)
        val startDate = parseDate("startDate", startDateRaw)
        val endDate = parseDate("endDate", endDateRaw)
        validateDateRange(startDate, endDate)
        requireNoOverlap(
            hid = house.id,
            startDate = startDate,
            endDate = endDate,
            ignoreBookingId = booking.id.toString(),
        )

        val updated =
            booking.copy(
                hid = house.id,
                startDate = startDate,
                endDate = endDate,
            )

        return bookingRepo.update(updated)
    }

    fun deleteBooking(idbooking: Uuid) = bookingRepo.deleteById(idbooking)

    fun getBookingInfoById(id: Uuid): GetBookingResponse = getBookingById(id).toGetBookingResponse()

    fun listBookings(
        hid: Uuid,
        dateStart: Date,
        dateEnd: Date,
    ): List<GetBookingResponse> {
        val hid = requireExistingHouse(hid).id
        return bookingRepo
            .getAll()
            .asSequence()
            .filter { it.hid == hid }
            .filter { it.startDate >= dateStart || it.endDate <= dateEnd }
            .sortedBy { it.startDate.value }
            .map { it.toGetBookingResponse() }
            .toList()
    }

    fun listAvailableHouses(
        startDateRaw: String,
        endDateRaw: String,
    ): List<House> {
        val startDate = parseDate("startDate", startDateRaw)
        val endDate = parseDate("endDate", endDateRaw)
        validateDateRange(startDate, endDate)
        val bookingsByHouse = bookingRepo.getAll().groupBy { it.hid }
        return houseRepo
            .getAll()
            .filter { house ->
                val hid = house.id
                bookingsByHouse[hid].orEmpty().none { booking ->
                    overlaps(booking.startDate, booking.endDate, startDate, endDate)
                }
            }.sortedBy { it.title.value }
    }

    private fun requireNoOverlap(
        hid: Uuid,
        startDate: Date,
        endDate: Date,
        ignoreBookingId: String? = null,
    ) {
        val overlapsExisting =
            bookingRepo.getAll().any { booking ->
                booking.hid == hid &&
                    booking.id.toString() != ignoreBookingId &&
                    overlaps(booking.startDate, booking.endDate, startDate, endDate)
            }

        require(!overlapsExisting) { "House is not available for the given period." }
    }

    fun listBookingsByHouse(hid: Uuid): List<Booking> {
        val hid = requireExistingHouse(hid).id
        return bookingRepo
            .getAll()
            .asSequence()
            .filter { it.hid == hid }
            .sortedBy { it.startDate.value }
            .toList()
    }

    private fun requireExistingHouse(hid: Uuid): House = houseRepo.getById(hid)

    private fun includesDate(
        booking: Booking,
        date: Date,
    ): Boolean = BookingDateUtils.includes(booking.startDate.value, booking.endDate.value, date.value)

    private fun overlaps(
        firstStart: Date,
        firstEnd: Date,
        secondStart: Date,
        secondEnd: Date,
    ): Boolean = BookingDateUtils.overlaps(firstStart.value, firstEnd.value, secondStart.value, secondEnd.value)

    private fun parseDate(
        name: String,
        raw: String,
    ): Date =
        runCatching { Date.of(raw) }
            .getOrElse { throw IllegalArgumentException("$name: ${it.message}") }

    private fun validateDateRange(
        startDate: Date,
        endDate: Date,
    ) = require(startDate.value < endDate.value) { "endDate must be after startDate." }
}

@OptIn(ExperimentalUuidApi::class)
fun Booking.toGetBookingResponse() =
    GetBookingResponse(
        id = id.toString(),
        hid = hid.toString(),
        uid = uid.toString(),
        startDate = startDate.toString(),
        endDate = endDate.toString(),
    )
