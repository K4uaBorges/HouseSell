package domain.house.repository

import domain.house.House
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object InMemoryHouseRepository : HouseRepository {
    private val housesById = mutableMapOf<String, House>()

    override fun create(house: House): House {
        housesById[house.id.toString()] = house
        return house
    }

    override fun delete(id: String) {
        housesById.remove(id)
    }

    override fun update(house: House): House {
        housesById[house.id.toString()] = house
        return house
    }

    override fun getById(id: String): House? = housesById[id]

    override fun getAll(): List<House> = housesById.values.toList()

    fun clear() {
        housesById.clear()
    }
}
