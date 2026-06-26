package main.data.interfaces

import main.domain.location.Location
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface LocationRepository : Repository<Uuid, Location> {
    override fun create(value: Location): Location

    override fun getById(key: Uuid): Location

    override fun getAll(): List<Location>

    override fun save(value: Location): Location

    override fun deleteById(key: Uuid)

    override fun update(updated: Location): Location

    override fun clear()

    fun getCountries(): List<Location>

    fun getChildrenAll(parentId: Uuid): List<Location>

    fun getChildrenDirect(parentId: Uuid): List<Location>

    fun getFullPath(id: Uuid): List<Location>

    fun exists(id: Uuid): Boolean
}
