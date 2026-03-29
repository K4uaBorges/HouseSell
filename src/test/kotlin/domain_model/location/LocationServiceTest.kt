package domain_model.location

import junit.framework.TestCase.assertFalse
import main.data.impl.mem.InMemoryLocationRepository
import main.domain_model.location.Location
import main.domain_model.location.LocationName
import main.domain_model.location.LocationService
import main.domain_model.location.LocationType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class LocationServiceTest {
    private val repo = InMemoryLocationRepository
    private val service = LocationService(repo)

    @BeforeTest
    fun setup() {
        repo.clear()
    }

    @Test
    fun `create country without parent`() {
        val location = service.createLocation("Portugal", "COUNTRY", null)

        assertEquals("Portugal", location.name.value)
        assertEquals(LocationType.COUNTRY, location.type)
        assertNull(location.parentId)
    }

    @Test
    fun `create region inside country`() {
        val country = service.createLocation("Portugal", "COUNTRY", null)
        val region = service.createLocation("Lisboa", "REGION", country.id.toString())

        assertEquals("Lisboa", region.name.value)
        assertEquals(LocationType.REGION, region.type)
        assertEquals(country.id, region.parentId)
    }

    @Test
    fun `reject duplicate name ignoring case for same parent`() {
        val country = service.createLocation("Portugal", "COUNTRY", null)
        service.createLocation("Norte", "REGION", country.id.toString())

        assertFailsWith<IllegalArgumentException> {
            service.createLocation("nOrte", "REGION", country.id.toString())
        }
    }

    @Test
    fun `reject invalid hierarchy - country can only contain regions`() {
        val country = service.createLocation("Portugal", "COUNTRY", null)
        assertFailsWith<IllegalArgumentException> {
            service.createLocation("Lisboa", "DISTRICT", country.id.toString())
        }
    }

    @Test
    fun `reject non-existent parent`() {
        val parentId = Uuid.random().toString()

        assertFailsWith<IllegalArgumentException> {
            service.createLocation("Norte", "REGION", parentId)
        }
    }

    @Test
    fun `get full path returns root to leaf`() {
        val country = service.createLocation("Portugal", "COUNTRY", null)
        val region = service.createLocation("Norte", "REGION", country.id.toString())
        val district = service.createLocation("Lisboa", "DISTRICT", region.id.toString())
        val municipality = service.createLocation("Oeiras", "MUNICIPALITY", district.id.toString())

        val path = service.getLocationInfoById(municipality.id.toString()).fullPath

        assertEquals(4, path.size)
        assertEquals("Portugal", path[0].name)
        assertEquals("Norte", path[1].name)
        assertEquals("Lisboa", path[2].name)
        assertEquals("Oeiras", path[3].name)
    }

    @Test
    fun `get children all returns all direct children`() {
        val country = service.createLocation("Portugal", "COUNTRY", null)
        service.createLocation("Lisboa", "REGION", country.id.toString())
        val region = service.createLocation("Porto", "REGION", country.id.toString())
        service.createLocation("Porto2", "DISTRICT", region.id.toString())

        val children = service.getChildrenAll(country.id.toString())

        for (child in children) {
            print("Child: ${child.name}, Type: ${child.type}\n")
        }
        assertEquals(3, children.size)
        assertTrue(children.any { it.name == "Lisboa" })
        assertTrue(children.any { it.name == "Porto" })
        assertTrue(children.any { it.name == "Porto2" })
    }

    @Test
    fun `get children direct returns only direct children`() {
        val country = service.createLocation("Portugal", "COUNTRY", null)
        service.createLocation("Lisboa", "REGION", country.id.toString())
        val region = service.createLocation("Porto", "REGION", country.id.toString())
        service.createLocation("Porto2", "DISTRICT", region.id.toString())

        val children = service.getChildrenDirect(country.id.toString())

        for (child in children) {
            print("Child: ${child.name}, Type: ${child.type}\n")
        }
        assertEquals(2, children.size)
        assertTrue(children.any { it.name == "Lisboa" })
        assertTrue(children.any { it.name == "Porto" })
        assertFalse(children.any { it.name == "Porto2" })
    }

    @Test
    fun `repository rejects direct cycle`() {
        val id = Uuid.random()

        assertFailsWith<IllegalArgumentException> {
            repo.create(
                Location(
                    id = id,
                    name = LocationName.Companion.of("Ciclo"),
                    type = LocationType.LOCALITY,
                    parentId = id,
                ),
            )
        }
    }

    @Test
    fun `create all location types in hierarchy`() {
        val country = service.createLocation("Portugal", "COUNTRY", null)
        val region = service.createLocation("Norte", "REGION", country.id.toString())
        val district = service.createLocation("Porto", "DISTRICT", region.id.toString())
        val municipality = service.createLocation("Maia", "MUNICIPALITY", district.id.toString())
        val locality = service.createLocation("Vila Nova", "LOCALITY", municipality.id.toString())

        assertEquals(LocationType.LOCALITY, locality.type)
        assertEquals(5, service.listLocations().size)
    }

    @Test
    fun `reject name too short`() {
        assertFailsWith<IllegalArgumentException> {
            service.createLocation("A", "COUNTRY", null) // 1 char, mínimo é 2
        }
    }

    @Test
    fun `reject invalid location type`() {
        assertFailsWith<IllegalArgumentException> {
            service.createLocation("Test", "INVALID_TYPE", null)
        }
    }

    @Test
    fun `get location info throws when not found`() {
        assertFailsWith<IllegalArgumentException> {
            service.getLocationInfoById("550e8400-e29b-41d4-a716-446655440000")
        }
    }

    @Test
    fun `list locations returns empty list when none exist`() {
        val locations = service.listLocations()
        assertTrue(locations.isEmpty())
    }

    @Test
    fun `get children returns empty list when no children`() {
        val country = service.createLocation("Portugal", "COUNTRY", null)
        val children = service.getChildrenAll(country.id.toString())
        assertTrue(children.isEmpty())
    }

    @Test
    fun `update location changes fields`() {
        val country = service.createLocation("Portugal", "COUNTRY", null)
        val region = service.createLocation("Norte", "REGION", country.id.toString())

        val updated = service.updateLocation(region.id.toString(), "Sul", "REGION", country.id.toString())

        assertEquals("Sul", updated.name.value)
        assertEquals(country.id, updated.parentId)
    }

    @Test
    fun `update location rejects self parent`() {
        val country = service.createLocation("Portugal", "COUNTRY", null)

        assertFailsWith<IllegalArgumentException> {
            service.updateLocation(country.id.toString(), "Portugal", "COUNTRY", country.id.toString())
        }
    }

    @Test
    fun `delete location removes leaf`() {
        val country = service.createLocation("Portugal", "COUNTRY", null)

        service.deleteLocation(country.id.toString())

        assertFailsWith<IllegalArgumentException> {
            service.getLocationById(country.id.toString())
        }
    }

    @Test
    fun `delete location with children fails`() {
        val country = service.createLocation("Portugal", "COUNTRY", null)
        service.createLocation("Lisboa", "REGION", country.id.toString())

        assertFailsWith<IllegalArgumentException> {
            service.deleteLocation(country.id.toString())
        }
    }

    @Test
    fun `get hierarchical path returns id and names`() {
        val country = service.createLocation("Portugal", "COUNTRY", null)
        val region = service.createLocation("Norte", "REGION", country.id.toString())
        val district = service.createLocation("Porto", "DISTRICT", region.id.toString())

        val path = service.getHierarchicalPath(district.id)

        assertEquals(3, path.size)
        assertEquals("Portugal", path[0].second)
        assertEquals("Norte", path[1].second)
        assertEquals("Porto", path[2].second)
    }

    @Test
    fun `get location by parent id returns location`() {
        val country = service.createLocation("Portugal", "COUNTRY", null)

        val result = service.getLocationByParentId(country.id.toString())

        assertEquals(country.id, result.id)
    }
}
