package domain.booking

import domain.booking.repository.BookingRepository
import domain.house.House
import domain.house.repository.HouseRepository
import utils.BookingDateUtils
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class BookingService(
    private val bookingRepo: BookingRepository,
    private val houseRepo: HouseRepository,
) {

    fun createBooking(bookerId: Uuid, hidRaw: String, startDateRaw: String, endDateRaw: String): Booking {
        val house = requireExistingHouse(hidRaw)
        val startDate = parseDate("startDate", startDateRaw)
        val endDate = parseDate("endDate", endDateRaw)
        validateDateRange(startDate, endDate)
        requireNoOverlap(house.id.toString(), startDate, endDate)
        return bookingRepo.create(
            Booking(
                id = Uuid.random(),
                hid = house.id.toString(),
                uid = bookerId,
                startDate = startDate,
                endDate = endDate,
            )
        )
    }

    fun getBookingById(idRaw: String): Booking? = bookingRepo.getById(idRaw.trim())

    fun getBookingInfoById(idRaw: String): GetBookingResponse =
        requireNotNull(getBookingById(idRaw)){ "Booking not found." }.toGetBookingResponse()

    fun listBookings(hidRaw: String, dateRaw: String): List<GetBookingResponse> {
        val hid = requireExistingHouse(hidRaw).id.toString()
        val date = parseDate("date", dateRaw)
        return bookingRepo.getAll()
            .asSequence()
            .filter { it.hid == hid }
            .filter { includesDate(it, date) }
            .sortedBy { it.startDate.value }
            .map { it.toGetBookingResponse() }
            .toList()
    }

    fun listAvailableHouses(startDateRaw: String, endDateRaw: String): List<House> {
        val startDate = parseDate("startDate", startDateRaw)
        val endDate = parseDate("endDate", endDateRaw)
        validateDateRange(startDate, endDate)
        val bookingsByHouse = bookingRepo.getAll().groupBy { it.hid }
        return houseRepo.getAll()
            .filter { house ->
                val hid = house.id.toString()
                bookingsByHouse[hid].orEmpty().none { booking ->
                    overlaps(booking.startDate, booking.endDate, startDate, endDate)
                }
            }
            .sortedBy { it.title.value }
    }

    private fun requireNoOverlap(hid: String, startDate: Date, endDate: Date) {
        val overlapsExisting = bookingRepo.getAll().any { booking ->
            booking.hid == hid && overlaps(booking.startDate, booking.endDate, startDate, endDate)
        }

        require(!overlapsExisting) { "House is not available for the given period." }
    }

    fun listBookingsByHouse(hidRaw: String): List<Booking> {
        val hid = requireExistingHouse(hidRaw).id.toString()
        return bookingRepo.getAll()
            .asSequence()
            .filter { it.hid == hid }
            .sortedBy { it.startDate.value }
            .toList()
    }

    private fun requireExistingHouse(hidRaw: String): House {
        val hid = hidRaw.trim()
        require(hid.isNotEmpty()) { "hid is required." }
        return requireNotNull(houseRepo.getById(hid)) { "House not found." }
    }

    private fun includesDate(booking: Booking, date: Date): Boolean {
        return BookingDateUtils.includes(booking.startDate.value, booking.endDate.value, date.value)
    }

    private fun overlaps(
        firstStart: Date,
        firstEnd: Date,
        secondStart: Date,
        secondEnd: Date,
    ): Boolean {
        return BookingDateUtils.overlaps(firstStart.value, firstEnd.value, secondStart.value, secondEnd.value)
    }

    private fun parseDate(name: String, raw: String): Date {
        return runCatching { Date.of(raw) }
            .getOrElse { throw IllegalArgumentException("$name: ${it.message}") }
    }

    private fun validateDateRange(startDate: Date, endDate: Date) =
        require(endDate.value.isAfter(startDate.value)) { "endDate must be after startDate." }
}

@OptIn(ExperimentalUuidApi::class)
fun Booking.toGetBookingResponse() = GetBookingResponse(
    id = id.toString(),
    hid = hid,
    uid = uid.toString(),
    startDate = startDate.toString(),
    endDate = endDate.toString(),
)

