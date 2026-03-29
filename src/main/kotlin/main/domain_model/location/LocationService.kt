package main.domain_model.location

import main.api.dto.CreateLocationResponse
import main.api.dto.GetLocationResponse
import main.api.dto.LocationPathEntry
import main.api.dto.LocationSummary
import main.data.interfaces.LocationRepository
import main.domain_model.location.LocationType.Companion.isAllowedChild
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class LocationService(
    private val repo: LocationRepository,
) {
    fun createLocation(
        nameRaw: String,
        typeRaw: String,
        parentIdRaw: String?,
    ): Location {
        val name = LocationName.of(nameRaw)
        val type = LocationType.of(typeRaw)
        val parentId = parentIdRaw?.let(::parseUuid)
        val parent = parentId?.let { repo.getById(it) }

        if (type == LocationType.COUNTRY) {
            require(parentId == null) { "Country cannot have parent location" }
        } else {
            require(parent != null) { "Parent location not found" }
        }

        parent?.let {
            require(isAllowedChild(it.type, type)) { "Invalid hierarchy: ${it.type} cannot contain $type" }
            require(it.type.isHigherThan(type)) { "Invalid hierarchy: ${it.type} cannot contain $type" }
        }

        require(!existsLocationWithSameName(name, parentId)) {
            "Location with name '${name.value}' already exists for this parent"
        }

        return repo.create(
            Location(
                id = Uuid.random(),
                name = name,
                type = type,
                parentId = parentId,
            ),
        )
    }

    fun getHierarchicalPath(locId: Uuid): List<Pair<Uuid, String>> = repo.getFullPath(locId).map { it.id to it.name.value }

    fun getLocationByParentId(parentIdRaw: String): Location {
        val parentId = parseUuid(parentIdRaw)
        return repo.getById(parentId)
    }

    fun getLocationById(idRaw: String): Location = repo.getById(parseUuid(idRaw))

    fun getLocationInfoById(idRaw: String): GetLocationResponse {
        val location = getLocationById(idRaw)
        return GetLocationResponse(
            id = location.id.toString(),
            name = location.name.value,
            type = location.type.name,
            parentId = location.parentId?.toString(),
            fullPath =
                repo.getFullPath(location.id).map {
                    LocationPathEntry(
                        id = it.id.toString(),
                        name = it.name.value,
                        type = it.type.name,
                    )
                },
        )
    }

    fun getChildrenAll(parentIdRaw: String): List<LocationSummary> {
        val parentId = parseUuid(parentIdRaw)
        require(repo.exists(parentId)) { "Parent location not found" }

        return repo
            .getChildrenAll(parentId)
            .sortedBy { it.name.value }
            .map {
                LocationSummary(it.id.toString(), it.name.value, it.type.name)
            }
    }

    fun getChildrenDirect(parentIdRaw: String): List<LocationSummary> {
        val parentId = parseUuid(parentIdRaw)
        require(repo.exists(parentId)) { "Parent location not found" }

        return repo
            .getChildrenDirect(parentId)
            .sortedBy { it.name.value }
            .map {
                LocationSummary(it.id.toString(), it.name.value, it.type.name)
            }
    }

    fun listLocations(): List<Location> = repo.getAll().sortedBy { it.name.value }

    fun updateLocation(
        idRaw: String,
        nameRaw: String,
        typeRaw: String,
        parentIdRaw: String?,
    ): Location {
        val id = parseUuid(idRaw)
        val current = requireNotNull(repo.getById(id)) { "Location not found" }
        val name = LocationName.of(nameRaw)
        val type = LocationType.of(typeRaw)
        val parentId = parentIdRaw?.let(::parseUuid)

        if (type == LocationType.COUNTRY) {
            require(parentId == null) { "Country cannot have parent location" }
        } else {
            require(parentId != null) { "Parent location not found" }
        }

        parentId?.let { pid ->
            require(pid != id) { "Location cannot be its own parent" }
            val parent = repo.getById(pid)
            require(parent.type.isHigherThan(type)) { "Invalid hierarchy: ${parent.type} cannot contain $type" }
            require(!wouldCreateCycle(id, pid)) { "Would create cycle in location hierarchy" }
        }

        require(!existsLocationWithSameName(name, parentId, excludeId = id)) {
            "Location with name '${name.value}' already exists for this parent"
        }

        return repo.update(
            current.copy(
                name = name,
                type = type,
                parentId = parentId,
            ),
        )
    }

    fun deleteLocation(idRaw: String) {
        val id = parseUuid(idRaw)
        require(repo.exists(id)) { "Location not found" }
        require(repo.getChildrenAll(id).isEmpty()) { "Cannot delete location with children" }
        repo.deleteById(id)
    }

    private fun wouldCreateCycle(
        locationId: Uuid,
        parentId: Uuid,
    ): Boolean {
        var current: Uuid? = parentId
        while (current != null) {
            if (current == locationId) return true
            current = repo.getById(current).parentId
        }
        return false
    }

    private fun existsLocationWithSameName(
        name: LocationName,
        parentId: Uuid?,
        excludeId: Uuid? = null,
    ): Boolean =
        repo.getAll().any { location ->
            location.id != excludeId &&
                location.parentId == parentId &&
                location.name.value.equals(name.value, ignoreCase = true)
        }

    private fun parseUuid(raw: String): Uuid =
        runCatching { Uuid.parse(raw.trim()) }
            .getOrElse { throw IllegalArgumentException("Invalid location id.") }
}

@OptIn(ExperimentalUuidApi::class)
fun Location.toCreateLocationResponse() =
    CreateLocationResponse(
        id = id.toString(),
        name = name.value,
        type = type.name,
        parentId = parentId?.toString(),
    )

@OptIn(ExperimentalUuidApi::class)
fun Location.toLocationSummary() =
    LocationSummary(
        id = id.toString(),
        name = name.value,
        type = type.name,
    )
