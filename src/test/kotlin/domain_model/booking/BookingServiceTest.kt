package domain_model.booking

import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.domain_model.booking.BookingService
import main.domain_model.booking.Date
import main.domain_model.house.House
import main.domain_model.house.Title
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class BookingServiceTest {
    private val bookingRepo = InMemoryBookingRepository
    private val houseRepo = InMemoryHouseRepository
    private val service = BookingService(bookingRepo, houseRepo)

    @BeforeTest
    fun setup() {
        bookingRepo.clear()
        houseRepo.clear()
    }

    @Test
    fun `create booking for existing house`() {
        val house = createHouse("Casa A")
        val bookerId = Uuid.random()

        val booking = service.createBooking(bookerId, house.id, "2026-05-10", "2026-05-15")

        assertEquals(house.id, booking.hid)
        assertEquals(bookerId, booking.uid)
        assertEquals("2026-05-10", booking.startDate.toString())
        assertEquals("2026-05-15", booking.endDate.toString())
    }

    @Test
    fun `reject overlapping booking for same house`() {
        val house = createHouse("Casa A")
        service.createBooking(Uuid.random(), house.id, "2026-05-10", "2026-05-15")

        assertFailsWith<IllegalArgumentException> {
            service.createBooking(Uuid.random(), house.id, "2026-05-12", "2026-05-18")
        }
    }

    @Test
    fun `reject end date before start date`() {
        val house = createHouse("Casa A")

        assertFailsWith<IllegalArgumentException> {
            service.createBooking(Uuid.random(), house.id, "2026-05-20", "2026-05-10")
        }
    }

    @Test
    fun `list available houses excludes booked houses`() {
        val availableHouse1 = createHouse("AAA")
        val availableHouse2 = createHouse("BBB")
        val bookedHouse = createHouse("ZZZ")
        service.createBooking(Uuid.random(), bookedHouse.id, "2026-05-10", "2026-05-15")

        val houses = service.listAvailableHouses("2026-05-11", "2026-05-12")

        assertEquals(2, houses.size)
        assertEquals(availableHouse1.id, houses.first().id)
        assertEquals("AAA", houses.first().title.value)
    }

    @Test
    fun `list bookings filters by date`() {
        val houseA = createHouse("Casa A")
        service.createBooking(Uuid.random(), houseA.id, "2026-05-20", "2026-05-25")
        service.createBooking(Uuid.random(), houseA.id, "2026-05-10", "2026-05-15")
        service.createBooking(Uuid.random(), houseA.id, "2026-05-26", "2026-05-27")

        val bookings = service.listBookings(houseA.id, Date.of("2026-05-12"), Date.of("2026-05-30"))

        assertEquals(3, bookings.size)
        assertEquals(bookings.first().startDate, "2026-05-10")
        assertEquals(bookings[1].endDate, "2026-05-25")
    }

    @Test
    fun `get booking by id and info works`() {
        val house = createHouse("Casa A")
        val created = service.createBooking(Uuid.random(), house.id, "2026-05-10", "2026-05-15")

        val byId = service.getBookingById(created.id)
        val info = service.getBookingInfoById(created.id)

        assertEquals(created.id, byId.id)
        assertEquals(created.id.toString(), info.id)
    }

    @Test
    fun `update booking changes dates`() {
        val house = createHouse("Casa A")
        val booking = service.createBooking(Uuid.random(), house.id, "2026-05-10", "2026-05-15")

        val updated = service.updateBooking(booking, house.id, "2026-05-11", "2026-05-16")

        assertEquals("2026-05-11", updated.startDate.toString())
        assertEquals("2026-05-16", updated.endDate.toString())
    }

    @Test
    fun `delete booking removes booking`() {
        val house = createHouse("Casa A")
        val booking = service.createBooking(Uuid.random(), house.id, "2026-05-10", "2026-05-15")

        service.deleteBooking(booking.id)

        assertFailsWith<IllegalArgumentException> {
            service.getBookingById(booking.id)
        }
    }

    @Test
    fun `list bookings by house only returns requested house`() {
        val houseA = createHouse("Casa A")
        val houseB = createHouse("Casa B")
        service.createBooking(Uuid.random(), houseA.id, "2026-05-10", "2026-05-15")
        service.createBooking(Uuid.random(), houseB.id, "2026-05-10", "2026-05-15")

        val bookings = service.listBookingsByHouse(houseA.id)

        assertEquals(1, bookings.size)
        assertEquals(houseA.id, bookings.first().hid)
    }

    @Test
    fun `reject invalid date format`() {
        val house = createHouse("Casa A")

        assertFailsWith<IllegalArgumentException> {
            service.createBooking(Uuid.random(), house.id, "invalid-date", "2026-05-15")
        }
    }

    private fun createHouse(title: String): House {
        val house =
            House(
                id = Uuid.random(),
                uid = Uuid.random(),
                title = Title.of(title),
                lid = Uuid.random(),
                areaSqMt = 100,
                pricePerNight = 80.0,
                description = "Casa para testes",
            )
        return houseRepo.create(house)
    }
}
