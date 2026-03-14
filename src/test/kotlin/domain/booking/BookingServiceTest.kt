package domain.booking

import domain.booking.repository.InMemoryBookingRepository
import domain.house.House
import domain.house.HouseService
import domain.house.repository.InMemoryHouseRepository
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalUuidApi::class)
class BookingServiceTest {
    private val houseService = HouseService(InMemoryHouseRepository)
    private val bookingService = BookingService(InMemoryBookingRepository, InMemoryHouseRepository)

    @BeforeEach
    fun setup() {
        InMemoryHouseRepository.clear()
        InMemoryBookingRepository.clear()
    }

    @Test
    fun `create booking with hid and dates`() {
        val house = createHouse("House One")

        val booking = bookingService.createBooking(
            bookerId = Uuid.random(),
            hidRaw = house.id.toString(),
            startDateRaw = "20260410",
            endDateRaw = "20260415",
        )

        assertEquals(house.id.toString(), booking.hid)
        assertTrue(booking.uid.toString().isNotBlank())
        assertEquals("20260410", booking.startDate.toString())
        assertEquals("20260415", booking.endDate.toString())
    }

    @Test
    fun `get detailed booking information`() {
        val house = createHouse("House Two")
        val booking = bookingService.createBooking(
            bookerId = Uuid.random(),
            hidRaw = house.id.toString(),
            startDateRaw = "20260501",
            endDateRaw = "20260503",
        )

        val detailed = bookingService.getBookingInfoById(booking.id.toString())

        assertEquals(booking.id.toString(), detailed.id)
        assertEquals(house.id.toString(), detailed.hid)
        assertEquals(booking.uid.toString(), detailed.uid)
        assertEquals("20260501", detailed.startDate)
        assertEquals("20260503", detailed.endDate)
    }

    @Test
    fun `list bookings by hid and date`() {
        val house = createHouse("House Three")
        bookingService.createBooking(Uuid.random(), house.id.toString(), "20260501", "20260505")
        val second = bookingService.createBooking(Uuid.random(), house.id.toString(), "20260505", "20260508")

        val onSecondDay = bookingService.listBookings(house.id.toString(), "20260502")
        val onBoundaryDay = bookingService.listBookings(house.id.toString(), "20260505")

        assertEquals(1, onSecondDay.size)
        assertEquals("20260501", onSecondDay.first().startDate)

        assertEquals(1, onBoundaryDay.size)
        assertEquals(second.id.toString(), onBoundaryDay.first().id)
    }

    @Test
    fun `list available houses by period`() {
        val reservedHouse = createHouse("House Reserved")
        val freeHouse = createHouse("House Free")

        bookingService.createBooking(Uuid.random(), reservedHouse.id.toString(), "20260610", "20260615")

        val overlappingPeriod = bookingService.listAvailableHouses("20260612", "20260614")
        val afterCheckoutPeriod = bookingService.listAvailableHouses("20260615", "20260617")

        assertEquals(listOf(freeHouse.id.toString()), overlappingPeriod.map { it.id.toString() })
        assertTrue(afterCheckoutPeriod.map { it.id.toString() }.containsAll(listOf(reservedHouse.id.toString(), freeHouse.id.toString())))
    }

    @Test
    fun `reject overlapping booking for same house`() {
        val house = createHouse("House Four")
        bookingService.createBooking(Uuid.random(), house.id.toString(), "20260710", "20260712")

        val error = assertFailsWith<IllegalArgumentException> {
            bookingService.createBooking(Uuid.random(), house.id.toString(), "20260711", "20260713")
        }

        assertEquals("House is not available for the given period.", error.message)
    }

    private fun createHouse(title: String): House {
        return houseService.createHouse(
            ownerId = Uuid.random(),
            titleRaw = title,
            locationRaw = "Lisbon",
            areaSqMt = 90,
            pricePerNight = 120.0,
            descriptionRaw = "Near downtown",
        )
    }
}
