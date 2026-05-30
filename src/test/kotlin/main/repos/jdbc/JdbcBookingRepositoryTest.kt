package main.repos.jdbc

import main.data.impl.caches.HouseInfoCache
import main.data.impl.jdbc.JdbcBookingRepository
import main.data.impl.jdbc.JdbcHouseRepository
import main.data.impl.jdbc.JdbcLocationRepository
import main.data.impl.jdbc.JdbcUsersRepository
import main.domain.booking.Booking
import main.domain.booking.Date
import main.domain.house.House
import main.domain.house.Title
import main.domain.location.Location
import main.domain.location.LocationName
import main.domain.location.LocationType
import main.domain.user.Email
import main.domain.user.Name
import main.domain.user.User
import main.errors.NoBookingExist
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class JdbcBookingRepositoryTest : PostgresTestContainer() {
    private lateinit var bookingRepo: JdbcBookingRepository
    private lateinit var houseRepo: JdbcHouseRepository
    private lateinit var userRepo: JdbcUsersRepository
    private lateinit var locationRepo: JdbcLocationRepository

    @BeforeAll
    fun setup() {
        bookingRepo = JdbcBookingRepository(dataSource)
        houseRepo = JdbcHouseRepository(dataSource, HouseInfoCache(limit = 100))
        userRepo = JdbcUsersRepository(dataSource)
        locationRepo = JdbcLocationRepository(dataSource)
    }

    @Test
    fun `create should insert booking with valid house and user`() {
        val owner = createAndSaveUser()
        val booker = createAndSaveUser(email = "booker@test.com")
        val location = createAndSaveLocation()
        val house = createAndSaveHouse(owner.id, location.id)
        val booking = createBooking(house.id, booker.id, "2024-01-01", "2024-01-05")

        val result = bookingRepo.create(booking)

        assertEquals(booking.id, result.id)
        assertEquals(booking.hid, result.hid)
        assertEquals(booking.uid, result.uid)
        assertEquals(booking.startDate.toString(), result.startDate.toString())
        assertEquals(booking.endDate.toString(), result.endDate.toString())
    }

    @Test
    fun `create should fail when house does not exist`() {
        val booker = createAndSaveUser()
        val nonExistentHouse = Uuid.random()
        val booking = createBooking(nonExistentHouse, booker.id, "2024-01-01", "2024-01-05")

        assertThrows<Exception> {
            bookingRepo.create(booking)
        }
    }

    @Test
    fun `create should fail when user does not exist`() {
        val owner = createAndSaveUser()
        val location = createAndSaveLocation()
        val house = createAndSaveHouse(owner.id, location.id)
        val nonExistentUser = Uuid.random()
        val booking = createBooking(house.id, nonExistentUser, "2024-01-01", "2024-01-05")

        assertThrows<Exception> {
            bookingRepo.create(booking)
        }
    }

    @Test
    fun `getById should return booking when exists`() {
        val owner = createAndSaveUser()
        val booker = createAndSaveUser(email = "booker@test.com")
        val location = createAndSaveLocation()
        val house = createAndSaveHouse(owner.id, location.id)
        val booking = createAndSaveBooking(house.id, booker.id, "2024-01-01", "2024-01-05")

        val result = bookingRepo.getById(booking.id)

        assertEquals(booking.id, result.id)
        assertEquals(booking.hid, result.hid)
        assertEquals(booking.uid, result.uid)
    }

    @Test
    fun `getById should throw NoBookingExist when not found`() {
        val randomId = Uuid.random()

        assertThrows<NoBookingExist> {
            bookingRepo.getById(randomId)
        }
    }

    @Test
    fun `getAll should return all bookings`() {
        val owner = createAndSaveUser()
        val booker1 = createAndSaveUser(email = "booker1@test.com")
        val booker2 = createAndSaveUser(email = "booker2@test.com")
        val location = createAndSaveLocation()
        val house = createAndSaveHouse(owner.id, location.id)

        createAndSaveBooking(house.id, booker1.id, "2024-01-01", "2024-01-05")
        createAndSaveBooking(house.id, booker2.id, "2024-02-01", "2024-02-05")

        val result = bookingRepo.getAll()

        assertEquals(2, result.size)
    }

    @Test
    fun `update should modify booking dates`() {
        val owner = createAndSaveUser()
        val booker = createAndSaveUser(email = "booker@test.com")
        val location = createAndSaveLocation()
        val house = createAndSaveHouse(owner.id, location.id)
        val booking = createAndSaveBooking(house.id, booker.id, "2024-01-01", "2024-01-05")

        val updated =
            booking.copy(
                startDate = Date.of("2024-06-01"),
                endDate = Date.of("2024-06-10"),
            )

        val result = bookingRepo.update(updated)

        assertEquals("2024-06-01", result.startDate.toString())
        assertEquals("2024-06-10", result.endDate.toString())

        val retrieved = bookingRepo.getById(booking.id)
        assertEquals("2024-06-01", retrieved.startDate.toString())
    }

    @Test
    fun `update should allow changing house`() {
        val owner = createAndSaveUser()
        val booker = createAndSaveUser(email = "booker@test.com")
        val location = createAndSaveLocation()
        val house1 = createAndSaveHouse(owner.id, location.id, title = "House 1")
        val house2 = createAndSaveHouse(owner.id, location.id, title = "House 2")
        val booking = createAndSaveBooking(house1.id, booker.id, "2024-01-01", "2024-01-05")

        val updated = booking.copy(hid = house2.id)
        bookingRepo.update(updated)

        val retrieved = bookingRepo.getById(booking.id)
        assertEquals(house2.id, retrieved.hid)
    }

    @Test
    fun `update should throw NoBookingExist when booking not found`() {
        val owner = createAndSaveUser()
        val booker = createAndSaveUser(email = "booker@test.com")
        val location = createAndSaveLocation()
        val house = createAndSaveHouse(owner.id, location.id)
        val nonExistentBooking = createBooking(house.id, booker.id, "2024-01-01", "2024-01-05")

        assertThrows<NoBookingExist> {
            bookingRepo.update(nonExistentBooking)
        }
    }

    @Test
    fun `deleteById should remove booking`() {
        val owner = createAndSaveUser()
        val booker = createAndSaveUser(email = "booker@test.com")
        val location = createAndSaveLocation()
        val house = createAndSaveHouse(owner.id, location.id)
        val booking = createAndSaveBooking(house.id, booker.id, "2024-01-01", "2024-01-05")

        bookingRepo.deleteById(booking.id)

        assertThrows<NoBookingExist> {
            bookingRepo.getById(booking.id)
        }
    }

    @Test
    fun `deleteById should throw NoBookingExist when not found`() {
        assertThrows<NoBookingExist> {
            bookingRepo.deleteById(Uuid.random())
        }
    }

    @Test
    fun `clear should remove all bookings`() {
        val owner = createAndSaveUser()
        val booker1 = createAndSaveUser(email = "booker1@test.com")
        val booker2 = createAndSaveUser(email = "booker2@test.com")
        val location = createAndSaveLocation()
        val house = createAndSaveHouse(owner.id, location.id)

        createAndSaveBooking(house.id, booker1.id, "2024-01-01", "2024-01-05")
        createAndSaveBooking(house.id, booker2.id, "2024-02-01", "2024-02-05")

        bookingRepo.clear()

        assertEquals(0, bookingRepo.getAll().size)
    }

    @Test
    fun `save should work as alias for update`() {
        val owner = createAndSaveUser()
        val booker = createAndSaveUser(email = "booker@test.com")
        val location = createAndSaveLocation()
        val house = createAndSaveHouse(owner.id, location.id)
        val booking = createAndSaveBooking(house.id, booker.id, "2024-01-01", "2024-01-05")

        val modified =
            booking.copy(
                startDate = Date.of("2024-12-01"),
                endDate = Date.of("2024-12-05"),
            )
        bookingRepo.save(modified)

        val retrieved = bookingRepo.getById(booking.id)
        assertEquals("2024-12-01", retrieved.startDate.toString())
        assertEquals("2024-12-05", retrieved.endDate.toString())
    }

    @Test
    fun `multiple bookings for same house should be allowed`() {
        val owner = createAndSaveUser()
        val booker1 = createAndSaveUser(email = "booker1@test.com")
        val booker2 = createAndSaveUser(email = "booker2@test.com")
        val location = createAndSaveLocation()
        val house = createAndSaveHouse(owner.id, location.id)

        // Datas não sobrepostas
        createAndSaveBooking(house.id, booker1.id, "2024-01-01", "2024-01-05")
        createAndSaveBooking(house.id, booker2.id, "2024-01-06", "2024-01-10")

        val result = bookingRepo.getAll()
        assertEquals(2, result.size)
    }

    @Test
    fun `multiple bookings for same user should be allowed`() {
        val owner = createAndSaveUser()
        val booker = createAndSaveUser(email = "booker@test.com")
        val location = createAndSaveLocation()
        val house1 = createAndSaveHouse(owner.id, location.id, title = "House 1")
        val house2 = createAndSaveHouse(owner.id, location.id, title = "House 2")

        createAndSaveBooking(house1.id, booker.id, "2024-01-01", "2024-01-05")
        createAndSaveBooking(house2.id, booker.id, "2024-02-01", "2024-02-05")

        val result = bookingRepo.getAll()
        assertEquals(2, result.size)
    }

    // Helper methods
    private fun createBooking(
        houseId: Uuid,
        userId: Uuid,
        startDate: String,
        endDate: String,
    ): Booking =
        Booking(
            id = Uuid.random(),
            hid = houseId,
            uid = userId,
            startDate = Date.of(startDate),
            endDate = Date.of(endDate),
        )

    private fun createAndSaveBooking(
        houseId: Uuid,
        userId: Uuid,
        startDate: String,
        endDate: String,
    ): Booking {
        val booking = createBooking(houseId, userId, startDate, endDate)
        return bookingRepo.create(booking)
    }

    private fun createAndSaveUser(
        name: String = "Test User",
        email: String = "test${Uuid.random()}@test.com",
    ): User {
        val user =
            User(
                id = Uuid.random(),
                name = Name.of(name),
                email = Email.of(email),
                token = Uuid.random(),
            )
        return userRepo.create(user)
    }

    private fun createAndSaveLocation(name: String = "Test Location"): Location {
        val location =
            Location(
                id = Uuid.random(),
                name = LocationName.of(name),
                type = LocationType.COUNTRY,
                parentId = null,
            )
        return locationRepo.create(location)
    }

    private fun createAndSaveHouse(
        ownerId: Uuid,
        locationId: Uuid,
        title: String = "Test House",
    ): House {
        val house =
            House(
                id = Uuid.random(),
                uid = ownerId,
                title = Title.of(title),
                lid = locationId,
                areaSqMt = 100,
                pricePerNight = 100.0,
                description = "Test description",
            )
        return houseRepo.create(house)
    }
}
