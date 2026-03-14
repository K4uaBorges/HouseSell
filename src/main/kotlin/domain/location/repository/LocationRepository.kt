package domain.location.repository

import domain.house.House
import domain.location.Location

interface LocationRepository {
    fun create(location: Location): Location
    fun delete(id: String)
    fun update(location: Location): Location
    fun getById(id: String): Location?
    fun getAll(): List<Location>
}
