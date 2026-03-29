package repos.jdbc

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import main.data.impl.jdbc.JdbcHouseRepository
import main.data.impl.jdbc.JdbcLocationRepository
import main.data.impl.jdbc.JdbcUsersRepository
import main.domain_model.house.House
import main.domain_model.house.Title
import main.domain_model.location.Location
import main.domain_model.location.LocationName
import main.domain_model.location.LocationType
import main.domain_model.user.Email
import main.domain_model.user.Name
import main.domain_model.user.User
import main.errors.NoHouseExist
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class JdbcHouseRepositoryTest : repos.jdbc.PostgresTestContainer() {

    private lateinit var houseRepo: JdbcHouseRepository
    private lateinit var userRepo: JdbcUsersRepository
    private lateinit var locationRepo: JdbcLocationRepository

    @BeforeAll
    fun setup() {
        houseRepo = JdbcHouseRepository(dataSource)
        userRepo = JdbcUsersRepository(dataSource)
        locationRepo = JdbcLocationRepository(dataSource)
    }

    @Test
    fun `create should insert house with valid owner and location`() {
        val owner = createAndSaveUser()
        val location = createAndSaveLocation()
        val house = createHouse(owner.id, location.id)

        val result = houseRepo.create(house)

        assertEquals(house.id, result.id)
        assertEquals(house.title.value, result.title.value)
        assertEquals(house.uid, result.uid)
        assertEquals(house.lid, result.lid)
    }

    @Test
    fun `create should fail when owner does not exist`() {
        val nonExistentOwner = Uuid.random()
        val location = createAndSaveLocation()
        val house = createHouse(nonExistentOwner, location.id)

        assertThrows<Exception> {
            houseRepo.create(house)
        }
    }

    @Test
    fun `create should fail when location does not exist`() {
        val owner = createAndSaveUser()
        val nonExistentLocation = Uuid.random()
        val house = createHouse(owner.id, nonExistentLocation)

        assertThrows<Exception> {
            houseRepo.create(house)
        }
    }

    @Test
    fun `getById should return house when exists`() {
        val owner = createAndSaveUser()
        val location = createAndSaveLocation()
        val house = createAndSaveHouse(owner.id, location.id)

        val result = houseRepo.getById(house.id)

        assertEquals(house.id, result.id)
        assertEquals(house.title.value, result.title.value)
        assertEquals(house.areaSqMt, result.areaSqMt)
        assertEquals(house.pricePerNight, result.pricePerNight)
        assertEquals(house.description, result.description)
    }

    @Test
    fun `getById should throw NoHouseExist when not found`() {
        val randomId = Uuid.random()

        assertThrows<NoHouseExist> {
            houseRepo.getById(randomId)
        }
    }

    @Test
    fun `getAll should return all houses`() {
        val owner = createAndSaveUser()
        val location = createAndSaveLocation()

        createAndSaveHouse(owner.id, location.id, title = "Zebra House")
        createAndSaveHouse(owner.id, location.id, title = "Apple House")
        createAndSaveHouse(owner.id, location.id, title = "Mango House")

        val result = houseRepo.getAll()

        assertEquals(3, result.size)
        assertTrue(result.any { it.title.value == "Apple House" })
        assertTrue(result.any { it.title.value == "Mango House" })
        assertTrue(result.any { it.title.value == "Zebra House" })
    }

    @Test
    fun `update should modify house attributes`() {
        val owner = createAndSaveUser()
        val location = createAndSaveLocation()
        val house = createAndSaveHouse(owner.id, location.id, title = "Old Title")

        val updated =
            house.copy(
                title = Title.of("New Title"),
                areaSqMt = 999,
                pricePerNight = 999.99,
                description = "Updated description",
            )

        val result = houseRepo.update(updated)

        assertEquals("New Title", result.title.value)
        assertEquals(999, result.areaSqMt)
        assertEquals(999.99, result.pricePerNight)
        assertEquals("Updated description", result.description)

        val retrieved = houseRepo.getById(house.id)
        assertEquals("New Title", retrieved.title.value)
    }

    @Test
    fun `update should allow changing location`() {
        val owner = createAndSaveUser()
        val location1 = createAndSaveLocation(name = "Location 1")
        val location2 = createAndSaveLocation(name = "Location 2")
        val house = createAndSaveHouse(owner.id, location1.id)

        val updated = house.copy(lid = location2.id)
        houseRepo.update(updated)

        val retrieved = houseRepo.getById(house.id)
        assertEquals(location2.id, retrieved.lid)
    }

    @Test
    fun `update should throw NoHouseExist when house not found`() {
        val owner = createAndSaveUser()
        val location = createAndSaveLocation()
        val nonExistentHouse = createHouse(owner.id, location.id)

        assertThrows<NoHouseExist> {
            houseRepo.update(nonExistentHouse)
        }
    }

    @Test
    fun `deleteById should remove house`() {
        val owner = createAndSaveUser()
        val location = createAndSaveLocation()
        val house = createAndSaveHouse(owner.id, location.id)

        houseRepo.deleteById(house.id)

        assertThrows<NoHouseExist> {
            houseRepo.getById(house.id)
        }
    }

    @Test
    fun `deleteById should throw NoHouseExist when not found`() {
        assertThrows<NoHouseExist> {
            houseRepo.deleteById(Uuid.random())
        }
    }

    @Test
    fun `clear should remove all houses`() {
        val owner = createAndSaveUser()
        val location = createAndSaveLocation()

        createAndSaveHouse(owner.id, location.id)
        createAndSaveHouse(owner.id, location.id)

        houseRepo.clear()

        assertEquals(0, houseRepo.getAll().size)
    }

    @Test
    fun `save should work as alias for update`() {
        val owner = createAndSaveUser()
        val location = createAndSaveLocation()
        val house = createAndSaveHouse(owner.id, location.id, title = "Original")

        val modified = house.copy(title = Title.of("Saved"))
        houseRepo.save(modified)

        val retrieved = houseRepo.getById(house.id)
        assertEquals("Saved", retrieved.title.value)
    }

    @Test
    fun `multiple houses per owner should be allowed`() {
        val owner = createAndSaveUser()
        val location = createAndSaveLocation()

        createAndSaveHouse(owner.id, location.id, title = "House 1")
        createAndSaveHouse(owner.id, location.id, title = "House 2")
        createAndSaveHouse(owner.id, location.id, title = "House 3")

        val result = houseRepo.getAll()
        assertEquals(3, result.size)
    }

    @Test
    fun `multiple houses per location should be allowed`() {
        val owner1 = createAndSaveUser(email = "owner1@test.com")
        val owner2 = createAndSaveUser(email = "owner2@test.com")
        val location = createAndSaveLocation()

        createAndSaveHouse(owner1.id, location.id)
        createAndSaveHouse(owner2.id, location.id)

        val result = houseRepo.getAll()
        assertEquals(2, result.size)
    }

    // Helper methods
    private fun createHouse(
        ownerId: Uuid,
        locationId: Uuid,
        title: String = "Test House",
        areaSqMt: Int = 100,
        pricePerNight: Double = 100.0,
        description: String = "Test description",
    ): House =
        House(
            id = Uuid.random(),
            uid = ownerId,
            title = Title.of(title),
            lid = locationId,
            areaSqMt = areaSqMt,
            pricePerNight = pricePerNight,
            description = description,
        )

    private fun createAndSaveHouse(
        ownerId: Uuid,
        locationId: Uuid,
        title: String = "Test House",
    ): House {
        val house = createHouse(ownerId, locationId, title = title)
        return houseRepo.create(house)
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
}
