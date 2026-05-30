package main.repos.jdbc

import main.data.impl.jdbc.JdbcLocationRepository
import main.domain.location.Location
import main.domain.location.LocationName
import main.domain.location.LocationType
import main.errors.NoLocationExist
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class JdbcLocationRepositoryTest : PostgresTestContainer() {
    private lateinit var repository: JdbcLocationRepository

    @BeforeAll
    fun setup() {
        repository = JdbcLocationRepository(dataSource)
    }

    @Test
    fun `create should insert country without parent`() {
        val location =
            createLocation(
                name = "Portugal",
                type = LocationType.COUNTRY,
                parentId = null,
            )

        val result = repository.create(location)

        assertEquals(location.id, result.id)
        assertEquals("Portugal", result.name.value)
        assertEquals(LocationType.COUNTRY, result.type)
        assertNull(result.parentId)
    }

    @Test
    fun `create should insert location with parent`() {
        val country = createAndSaveLocation("Portugal", LocationType.COUNTRY)
        val region =
            createLocation(
                name = "Norte",
                type = LocationType.REGION,
                parentId = country.id,
            )

        val result = repository.create(region)

        assertEquals(country.id, result.parentId)
    }

    @Test
    fun `create should fail when parent does not exist`() {
        val invalidParent = Uuid.random()
        val location =
            createLocation(
                name = "Invalid",
                type = LocationType.REGION,
                parentId = invalidParent,
            )

        // Deve falhar por foreign key constraint
        assertThrows<Exception> {
            repository.create(location)
        }
    }

    @Test
    fun `create should fail when duplicate name for same parent`() {
        val country = createAndSaveLocation("Portugal", LocationType.COUNTRY)
        repository.create(createLocation("Norte", LocationType.REGION, country.id))

        val duplicate = createLocation("Norte", LocationType.REGION, country.id)

        assertThrows<Exception> {
            repository.create(duplicate)
        }
    }

    @Test
    fun `getById should return location when exists`() {
        val location = createAndSaveLocation("Portugal", LocationType.COUNTRY)

        val result = repository.getById(location.id)

        assertEquals(location.id, result.id)
        assertEquals("Portugal", result.name.value)
    }

    @Test
    fun `getById should throw NoLocationExist when not found`() {
        val randomId = Uuid.random()

        assertThrows<NoLocationExist> {
            repository.getById(randomId)
        }
    }

    @Test
    fun `getAll should return all locations sorted by name`() {
        createAndSaveLocation("Zambia", LocationType.COUNTRY)
        createAndSaveLocation("Argentina", LocationType.COUNTRY)
        createAndSaveLocation("Brazil", LocationType.COUNTRY)

        val result = repository.getAll()

        assertEquals(3, result.size)
        assertTrue(result.any { it.name.value == "Argentina" })
        assertTrue(result.any { it.name.value == "Brazil" })
        assertTrue(result.any { it.name.value == "Zambia" })
    }

    @Test
    fun `getChildrenAll should return all children (recursive)`() {
        val country = createAndSaveLocation("Portugal", LocationType.COUNTRY)
        val region1 = createAndSaveLocation("Norte", LocationType.REGION, country.id)
        val region2 = createAndSaveLocation("Centro", LocationType.REGION, country.id)
        createAndSaveLocation("Porto", LocationType.DISTRICT, region1.id)

        val children = repository.getChildrenAll(country.id)

        assertEquals(3, children.size)
        assertTrue(children.any { it.name.value == "Norte" })
        assertTrue(children.any { it.name.value == "Centro" })
        assertTrue(children.any { it.name.value == "Porto" })
    }

    @Test
    fun `getChildrenAll should return empty list when no children`() {
        val country = createAndSaveLocation("Portugal", LocationType.COUNTRY)

        val children = repository.getChildrenAll(country.id)

        assertTrue(children.isEmpty())
    }

    @Test
    fun `getChildrenDirect should return direct children only`() {
        val country = createAndSaveLocation("Portugal", LocationType.COUNTRY)
        val district1 = createAndSaveLocation("Lisboa", LocationType.DISTRICT, country.id)
        val district2 = createAndSaveLocation("Porto", LocationType.DISTRICT, country.id)
        createAndSaveLocation("Oeiras", LocationType.MUNICIPALITY, district1.id)

        val children = repository.getChildrenDirect(country.id)

        assertEquals(2, children.size)
        assertTrue(children.any { it.name.value == "Lisboa" })
        assertTrue(children.any { it.name.value == "Porto" })
        assertFalse(children.any { it.name.value == "Oeiras" })
    }

    @Test
    fun `getChildrenDirect should return empty list when no children`() {
        val country = createAndSaveLocation("Portugal", LocationType.COUNTRY)

        val children = repository.getChildrenDirect(country.id)

        assertTrue(children.isEmpty())
    }

    @Test
    fun `getFullPath should return path from root to leaf`() {
        val country = createAndSaveLocation("Portugal", LocationType.COUNTRY)
        val region = createAndSaveLocation("Norte", LocationType.REGION, country.id)
        val district = createAndSaveLocation("Lisboa", LocationType.DISTRICT, region.id)
        val municipality = createAndSaveLocation("Oeiras", LocationType.MUNICIPALITY, district.id)

        val path = repository.getFullPath(municipality.id)

        assertEquals(4, path.size)
        assertEquals("Portugal", path[0].name.value)
        assertEquals("Norte", path[1].name.value)
        assertEquals("Lisboa", path[2].name.value)
        assertEquals("Oeiras", path[3].name.value)
    }

    @Test
    fun `getFullPath should return single element for root location`() {
        val country = createAndSaveLocation("Portugal", LocationType.COUNTRY)

        val path = repository.getFullPath(country.id)

        assertEquals(1, path.size)
        assertEquals("Portugal", path[0].name.value)
    }

    @Test
    fun `exists should return true for existing location`() {
        val location = createAndSaveLocation("Portugal", LocationType.COUNTRY)

        assertTrue(repository.exists(location.id))
    }

    @Test
    fun `exists should return false for non-existing location`() {
        assertFalse(repository.exists(Uuid.random()))
    }

    @Test
    fun `update should modify location`() {
        val country = createAndSaveLocation("Portugal", LocationType.COUNTRY)
        val location = createAndSaveLocation("Norte", LocationType.REGION, country.id)

        val updated =
            location.copy(
                name = LocationName.of("NewName"),
                type = LocationType.REGION,
            )

        val result = repository.update(updated)

        assertEquals("NewName", result.name.value)
        assertEquals(LocationType.REGION, result.type)

        val retrieved = repository.getById(location.id)
        assertEquals("NewName", retrieved.name.value)
    }

    @Test
    fun `update should allow changing parent`() {
        val country1 = createAndSaveLocation("Portugal", LocationType.COUNTRY)
        val country2 = createAndSaveLocation("Spain", LocationType.COUNTRY)
        val region = createAndSaveLocation("Norte", LocationType.REGION, country1.id)

        val updated = region.copy(parentId = country2.id)
        repository.update(updated)

        val retrieved = repository.getById(region.id)
        assertEquals(country2.id, retrieved.parentId)
    }

    @Test
    fun `update should throw NoLocationExist when location not found`() {
        val nonExistent = createLocation("Test", LocationType.COUNTRY)

        assertThrows<NoLocationExist> {
            repository.update(nonExistent)
        }
    }

    @Test
    fun `deleteById should remove location without children`() {
        val location = createAndSaveLocation("Portugal", LocationType.COUNTRY)

        repository.deleteById(location.id)

        assertFalse(repository.exists(location.id))
    }

    @Test
    fun `deleteById should fail when location has children`() {
        val country = createAndSaveLocation("Portugal", LocationType.COUNTRY)
        createAndSaveLocation("Lisboa", LocationType.REGION, country.id)

        // Deve falhar por constraint de foreign key
        assertThrows<Exception> {
            repository.deleteById(country.id)
        }
    }

    @Test
    fun `deleteById should throw NoLocationExist when not found`() {
        assertThrows<NoLocationExist> {
            repository.deleteById(Uuid.random())
        }
    }

    @Test
    fun `clear should remove all locations`() {
        createAndSaveLocation("Portugal", LocationType.COUNTRY)
        createAndSaveLocation("Spain", LocationType.COUNTRY)

        repository.clear()

        assertEquals(0, repository.getAll().size)
    }

    @Test
    fun `complex hierarchy should work correctly`() {
        // Criar hierarquia completa: Country -> Region -> District -> Municipality -> Locality
        val country = createAndSaveLocation("Portugal", LocationType.COUNTRY)
        val region = createAndSaveLocation("Norte", LocationType.REGION, country.id)
        val district = createAndSaveLocation("Porto", LocationType.DISTRICT, region.id)
        val municipality = createAndSaveLocation("Maia", LocationType.MUNICIPALITY, district.id)
        val locality = createAndSaveLocation("Vila Nova", LocationType.LOCALITY, municipality.id)

        // Verificar path completo
        val path = repository.getFullPath(locality.id)
        assertEquals(5, path.size)

        // Verificar children em cada nível
        assertEquals(4, repository.getChildrenAll(country.id).size)
        assertEquals(3, repository.getChildrenAll(region.id).size)
        assertEquals(2, repository.getChildrenAll(district.id).size)
        assertEquals(1, repository.getChildrenAll(municipality.id).size)
        assertEquals(0, repository.getChildrenAll(locality.id).size)
    }

    // Helper methods
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

    private fun createAndSaveLocation(
        name: String,
        type: LocationType,
        parentId: Uuid? = null,
    ): Location {
        val location = createLocation(name, type, parentId)
        return repository.create(location)
    }
}
