package main.data.impl.mem

import main.data.interfaces.LocationRepository
import main.domain.location.Location
import main.errors.NoLocationExist
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object InMemoryLocationRepository : LocationRepository {
    private val locations = mutableMapOf<String, Location>()

    override fun create(value: Location): Location {
        require(!exists(value.id)) { "Location already exists" }
        value.parentId?.let {
            require(exists(it)) { "Parent location not found" }
            require(!wouldCreateCycle(value.id, it)) { "Would create cycle in location hierarchy" }
        }
        locations[value.id.toString()] = value
        return value
    }

    override fun getById(key: Uuid): Location = locations[key.toString()] ?: throw NoLocationExist("Location not found.")

    override fun getChildrenAll(parentId: Uuid): List<Location> {
        val directChildren = locations.values.filter { it.parentId == parentId }
        return directChildren + directChildren.flatMap { getChildrenAll(it.id) }
    }

    override fun getChildrenDirect(parentId: Uuid): List<Location> = locations.values.filter { it.parentId == parentId }

    override fun getFullPath(id: Uuid): List<Location> {
        val path = mutableListOf<Location>()
        var current: Location? = getById(id)

        while (current != null) {
            path.add(0, current) // prepend to maintain root-to-leaf order
            current = current.parentId?.let { getById(it) }
        }
        return path
    }

    override fun exists(id: Uuid): Boolean = locations.containsKey(id.toString())

    override fun getAll(): List<Location> = locations.values.toList()

    override fun save(value: Location): Location {
        locations[value.id.toString()] ?: throw NoLocationExist("Location not found.")
        locations[value.id.toString()] = value
        return value
    }

    override fun update(updated: Location): Location = save(updated)

    override fun deleteById(key: Uuid) {
        getById(key)
        require(
            locations.values.none {
                it.parentId == key
            },
        )
        locations.remove(key.toString())
    }

    private fun wouldCreateCycle(
        newId: Uuid,
        parentId: Uuid,
    ): Boolean {
        var current: Uuid? = parentId
        while (current != null) {
            if (current == newId) return true
            current = locations[current.toString()]?.parentId
        }
        return false
    }

    override fun clear() = locations.clear()
}
