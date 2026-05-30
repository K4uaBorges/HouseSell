package main.repos.mem

import main.data.impl.mem.InMemoryLocationRepository
import main.domain.location.Location
import main.domain.location.LocationName
import main.domain.location.LocationType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InMemoryLocationRepositoryTest {
    private lateinit var repo: InMemoryLocationRepository

    @BeforeTest
    fun setup() {
        InMemoryLocationRepository.clear()
        repo = InMemoryLocationRepository
    }

    @Test
    fun `create should store location`() {
        val location = createLocation("Portugal", LocationType.COUNTRY)

        val result = repo.create(location)

        assertEquals("Portugal", result.name.value)
        assertNull(result.parentId)
    }

    @Test
    fun `create should reject when parent does not exist`() {
        val invalid = createLocation("Lisboa", LocationType.DISTRICT, parentId = Uuid.random())

        assertFailsWith<IllegalArgumentException> {
            repo.create(invalid)
        }
    }

    @Test
    fun `getChildrenAll should return even children of children`() {
        val country = repo.create(createLocation("Portugal", LocationType.COUNTRY))
        val region = repo.create(createLocation("Norte", LocationType.REGION, country.id))
        repo.create(createLocation("Lisboa", LocationType.DISTRICT, region.id))

        val children = repo.getChildrenAll(country.id)

        assertEquals(2, children.size)
        assertTrue(children.any { it.name.value == "Norte" })
        assertTrue(children.any { it.name.value == "Lisboa" })
    }

    @Test
    fun `getChildrenDirect should return only direct children`() {
        val country = repo.create(createLocation("Portugal", LocationType.COUNTRY))
        val district = repo.create(createLocation("Lisboa", LocationType.DISTRICT, country.id))
        val municipality = repo.create(createLocation("Oeiras", LocationType.MUNICIPALITY, district.id))

        val children = repo.getChildrenDirect(country.id)

        assertEquals(1, children.size)
        assertTrue(children.any { it.name.value == "Lisboa" })
        assertFalse(children.any { it.name.value == "Oeiras" })
    }

    @Test
    fun `getFullPath should return path from root to leaf`() {
        val country = repo.create(createLocation("Portugal", LocationType.COUNTRY))
        val region = repo.create(createLocation("Norte", LocationType.REGION, country.id))
        val district = repo.create(createLocation("Lisboa", LocationType.DISTRICT, region.id))
        val municipality = repo.create(createLocation("Oeiras", LocationType.MUNICIPALITY, district.id))

        val path = repo.getFullPath(municipality.id)

        assertEquals(4, path.size)
        assertEquals("Portugal", path[0].name.value)
        assertEquals("Norte", path[1].name.value)
        assertEquals("Lisboa", path[2].name.value)
        assertEquals("Oeiras", path[3].name.value)
    }

    @Test
    fun `exists should return true for existing location`() {
        val location = repo.create(createLocation("Portugal", LocationType.COUNTRY))

        assertTrue(repo.exists(location.id))
    }

    @Test
    fun `exists should return false for non-existing location`() {
        assertFalse(repo.exists(Uuid.random()))
    }

    @Test
    fun `delete should reject if location has children`() {
        val country = repo.create(createLocation("Portugal", LocationType.COUNTRY))
        repo.create(createLocation("Lisboa", LocationType.REGION, country.id))

        assertFailsWith<IllegalArgumentException> {
            repo.deleteById(country.id)
        }
    }

    // Helper
    private fun createLocation(
        name: String,
        type: LocationType,
        parentId: Uuid? = null,
    ): Location =
        Location(
            id = Uuid.random(),
            name = LocationName.of(name),
            type = type,
            parentId = parentId,
        )
}
