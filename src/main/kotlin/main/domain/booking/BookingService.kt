package main.domain.booking

import main.api.dto.GetBookingResponse
import main.data.interfaces.BookingRepository
import main.data.interfaces.HouseRepository
import main.domain.house.House
import main.utils.BookingDateUtils
import java.time.YearMonth
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
        startDateRaw: String,
        endDateRaw: String,
    ): Booking {
        val house = requireExistingHouse(booking.hid)
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
        validateDateRange(dateStart, dateEnd)
        return bookingRepo
            .getAll()
            .asSequence()
            .filter { it.hid == hid }
            .filter { overlaps(it.startDate, it.endDate, dateStart, dateEnd) }
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

    fun listAvailableDaysByMonth(
        hid: Uuid,
        year: Int,
        month: Int,
    ): List<String> {
        require(year in 1..9999) { "year must be between 1 and 9999." }
        require(month in 1..12) { "month must be between 1 and 12." }

        val house = requireExistingHouse(hid)
        val yearMonth = YearMonth.of(year, month)
        val bookingsByHouse =
            bookingRepo.getAll()
                .filter { it.hid == house.id }

        return (1..yearMonth.lengthOfMonth())
            .map { day -> yearMonth.atDay(day) }
            .filter { date ->
                val startDate = Date.of(date.toString())
                val endDate = Date.of(date.plusDays(1).toString())
                bookingsByHouse.none { booking ->
                    overlaps(booking.startDate, booking.endDate, startDate, endDate)
                }
            }.map { it.toString() }
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

    fun listBookingsByUser(userId: Uuid): List<Booking> = bookingRepo.getByUserId(userId)

    fun listAvailableDays(
        hid: Uuid,
        year: Int,
        month: Int,
    ): List<Date> {
        require(year in 1..9999) { "year must be between 1 and 9999." }
        require(month in 1..12) { "month must be between 1 and 12." }

        val house = requireExistingHouse(hid)
        val yearMonth = java.time.YearMonth.of(year, month)

        val houseBookings =
            bookingRepo
                .getAll()
                .filter { it.hid == house.id }

        return (1..yearMonth.lengthOfMonth())
            .map { day ->
                java.time.LocalDate.of(year, month, day)
            }
            .filter { localDate ->
                val dayStart = Date.of(localDate.toString())
                val dayEnd = Date.of(localDate.plusDays(1).toString())

                houseBookings.none { booking ->
                    overlaps(
                        booking.startDate,
                        booking.endDate,
                        dayStart,
                        dayEnd,
                    )
                }
            }
            .map { localDate ->
                Date.of(localDate.toString())
            }
    }

    private fun requireExistingHouse(hid: Uuid): House = houseRepo.getById(hid)

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
