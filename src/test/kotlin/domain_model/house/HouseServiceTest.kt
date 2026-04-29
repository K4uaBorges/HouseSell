package domain_model.house

import main.data.impl.caches.HouseInfoCache
import main.data.impl.mem.InMemoryHouseRepository
import main.domain_model.house.HouseService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HouseServiceTest {
    private val repo = InMemoryHouseRepository
    private val service = HouseService(repo, HouseInfoCache(limit = 100))

    @BeforeTest
    fun setup() {
        repo.clear()
    }

    @Test
    fun `create house with valid payload`() {
        val ownerId = Uuid.random()
        val locationId = Uuid.random()

        val house = service.createHouse(ownerId, "Casa Azul", locationId, 120, 95.0, "Casa perto da praia")

        assertEquals(ownerId, house.uid)
        assertEquals(locationId, house.lid)
        assertEquals("Casa Azul", house.title.value)
        assertEquals(120, house.areaSqMt)
        assertEquals(95.0, house.pricePerNight)
    }

    @Test
    fun `reject area less than or equal to zero`() {
        assertFailsWith<IllegalArgumentException> {
            service.createHouse(Uuid.random(), "Casa Azul", Uuid.random(), 0, 95.0, "Casa perto da praia")
        }
    }

    @Test
    fun `reject empty description`() {
        assertFailsWith<IllegalArgumentException> {
            service.createHouse(Uuid.random(), "Casa Azul", Uuid.random(), 120, 95.0, "   ")
        }
    }

    @Test
    fun `reject non positive price`() {
        assertFailsWith<IllegalArgumentException> {
            service.createHouse(Uuid.random(), "Casa Azul", Uuid.random(), 120, 0.0, "Descricao")
        }
    }

    @Test
    fun `list houses sorted by title`() {
        val owner = Uuid.random()
        service.createHouse(owner, "Zulu", Uuid.random(), 100, 80.0, "Primeira")
        service.createHouse(owner, "Alpha", Uuid.random(), 100, 80.0, "Segunda")

        val houses = service.listHouses()

        assertEquals(2, houses.size)
        assertEquals("Alpha", houses[0].title.value)
        assertEquals("Zulu", houses[1].title.value)
    }

    @Test
    fun `update house changes selected fields`() {
        val owner = Uuid.random()
        val created = service.createHouse(owner, "Casa Azul", Uuid.random(), 100, 90.0, "Descricao inicial")
        val newLocation = Uuid.random()

        val updated = service.updateHouse(created.id, "Casa Verde", newLocation, 130, 120.0, "Nova descricao")

        assertEquals(created.id, updated.id)
        assertEquals("Casa Verde", updated.title.value)
        assertEquals(newLocation, updated.lid)
        assertEquals(130, updated.areaSqMt)
        assertEquals(120.0, updated.pricePerNight)
    }

    @Test
    fun `get house by id and info returns same house`() {
        val created = service.createHouse(Uuid.random(), "Casa Azul", Uuid.random(), 120, 95.0, "Descricao")

        val byId = service.getHouseById(created.id)
        val info = service.getHouseInfoById(created.id)

        assertEquals(created.id, byId.id)
        assertEquals(created.id.toString(), info.id)
    }

    @Test
    fun `list houses by owner returns only owner houses`() {
        val ownerA = Uuid.random()
        val ownerB = Uuid.random()
        service.createHouse(ownerA, "Casa A", Uuid.random(), 100, 80.0, "A")
        service.createHouse(ownerB, "Casa B", Uuid.random(), 100, 80.0, "B")

        val houses = service.listHousesByOwner(ownerA)

        assertEquals(1, houses.size)
        assertEquals(ownerA, houses.first().uid)
    }

    @Test
    fun `delete house removes house`() {
        val created = service.createHouse(Uuid.random(), "Casa Azul", Uuid.random(), 120, 95.0, "Descricao")

        service.deleteHouse(created.id)

        assertFailsWith<IllegalArgumentException> {
            service.getHouseById(created.id)
        }
    }
}
