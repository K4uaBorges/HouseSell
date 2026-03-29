package repos.mem

import main.data.impl.mem.InMemoryBookingRepository
import main.domain_model.booking.Booking
import main.domain_model.booking.Date
import main.errors.NoBookingExist
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InMemoryBookingRepositoryTest {

    private lateinit var repo: InMemoryBookingRepository

    @BeforeTest
    fun setup() {
        InMemoryBookingRepository.clear()
        repo = InMemoryBookingRepository
    }

    @Test
    fun `create should store booking`() {
        val booking = createSampleBooking()

        val result = repo.create(booking)

        assertEquals(booking.id, result.id)
        assertEquals(1, repo.getAll().size)
    }

    @Test
    fun `getById should return booking when exists`() {
        val booking = createSampleBooking()
        repo.create(booking)

        val result = repo.getById(booking.id)

        assertEquals(booking.id, result.id)
        assertEquals(booking.hid, result.hid)
    }

    @Test
    fun `getById should throw NoBookingExist when not found`() {
        val randomId = Uuid.random()

        assertFailsWith<NoBookingExist> {
            repo.getById(randomId)
        }
    }

    @Test
    fun `update should modify existing booking`() {
        val booking = createSampleBooking()
        repo.create(booking)

        val updated = booking.copy(
            startDate = Date.of("2025-06-01"),
            endDate = Date.of("2025-06-10")
        )

        val result = repo.update(updated)

        assertEquals("2025-06-01", result.startDate.toString())
        val retrieved = repo.getById(booking.id)
        assertEquals("2025-06-01", retrieved.startDate.toString())
    }

    @Test
    fun `update should throw when booking does not exist`() {
        val nonExistent = createSampleBooking()

        assertFailsWith<NoBookingExist> {
            repo.update(nonExistent)
        }
    }

    @Test
    fun `deleteById should remove booking`() {
        val booking = createSampleBooking()
        repo.create(booking)

        repo.deleteById(booking.id)

        assertEquals(0, repo.getAll().size)
        assertFailsWith<NoBookingExist> {
            repo.getById(booking.id)
        }
    }

    @Test
    fun `clear should remove all bookings`() {
        repo.create(createSampleBooking())
        repo.create(createSampleBooking())

        repo.clear()

        assertEquals(0, repo.getAll().size)
    }

    // Helper
    private fun createSampleBooking(
        hid: Uuid = Uuid.random(),
        uid: Uuid = Uuid.random()
    ): Booking = Booking(
        id = Uuid.random(),
        hid = hid,
        uid = uid,
        startDate = Date.of("2025-01-01"),
        endDate = Date.of("2025-01-05")
    )
}
