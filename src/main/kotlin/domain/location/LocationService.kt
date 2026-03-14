package domain.location

import domain.location.repository.LocationRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
class LocationService(
    private val locations: LocationRepository
) {
    @OptIn(ExperimentalUuidApi::class)
    fun createLocation(name: String, type: LocationType, parentId: Uuid?): Uuid {
        require(name.isNotBlank()) { "name is required" }

        if (parentId == null) {
            require(type == LocationType.COUNTRY) { "only COUNTRY can be a root location" }
            val id = Uuid.random()
            locations.create(Location(id, name.trim(), type))
            return id
        }

        val parent = locations.getById(parentId.toString())
            ?: throw IllegalArgumentException("parentId does not exist")

        require(LocationType.isAllowedChild(parent.type, type)) {
            "invalid type hierarchy: ${parent.type} -> $type"
        }

        val id = Uuid.random()
        locations.create(Location(id, name.trim(), type, parentId))
        return id
    }
}