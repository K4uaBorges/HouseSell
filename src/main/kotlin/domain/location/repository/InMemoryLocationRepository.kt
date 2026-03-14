package domain.location.repository

import domain.location.Location
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object InMemoryLocationRepository : LocationRepository {
    private val locationById = mutableMapOf<String, Location>()

    override fun create(location: Location): Location {
        locationById[location.id.toString()] = location
        return location
    }

    override fun delete(id: String) {
        locationById.remove(id)
    }

    override fun update(location: Location): Location {
        locationById[location.id.toString()] = location
        return location
    }

    override fun getById(id: String): Location? = locationById[id]

    override fun getAll(): List<Location> = locationById.values.toList()

    fun clear() {
        locationById.clear()
    }
}
