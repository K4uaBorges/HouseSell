package main.repos.mem

import main.data.impl.mem.InMemoryHouseRepository
import main.domain.house.House
import main.domain.house.Title
import main.errors.NoHouseExist
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InMemoryHouseRepositoryTest {
    private lateinit var repo: InMemoryHouseRepository

    @BeforeTest
    fun setup() {
        InMemoryHouseRepository.clear()
        repo = InMemoryHouseRepository
    }

    @Test
    fun `create and getById should work`() {
        val house = createHouse("Casa Teste")

        repo.create(house)
        val result = repo.getById(house.id)

        assertEquals(house.title.value, result.title.value)
    }

    @Test
    fun `getAll should return all houses`() {
        repo.create(createHouse("Casa A"))
        repo.create(createHouse("Casa B"))

        assertEquals(2, repo.getAll().size)
    }

    @Test
    fun `update should modify house`() {
        val house = repo.create(createHouse("Casa Antiga"))
        val updated = house.copy(title = Title.of("Casa Nova"))

        repo.update(updated)

        assertEquals("Casa Nova", repo.getById(house.id).title.value)
    }

    @Test
    fun `deleteById should remove house`() {
        val house = repo.create(createHouse("Casa"))

        repo.deleteById(house.id)

        assertFailsWith<NoHouseExist> {
            repo.getById(house.id)
        }
    }

    // Helper
    private fun createHouse(title: String): House =
        House(
            id = Uuid.random(),
            uid = Uuid.random(),
            title = Title.of(title),
            lid = Uuid.random(),
            areaSqMt = 100,
            pricePerNight = 50.0,
            description = "Test description",
        )
}
