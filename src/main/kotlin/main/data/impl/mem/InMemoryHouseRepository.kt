package main.data.impl.mem

import main.data.interfaces.HouseRepository
import main.domain.house.House
import main.errors.NoHouseExist
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object InMemoryHouseRepository : HouseRepository {
    private val housesById = mutableMapOf<String, House>()

    override fun create(value: House): House {
        housesById[value.id.toString()] = value
        return value
    }

    override fun save(value: House): House {
        housesById[value.id.toString()] ?: throw NoHouseExist("House not found.")
        housesById[value.id.toString()] = value
        return value
    }

    override fun update(updated: House): House = save(updated)

    override fun getById(key: Uuid): House = housesById[key.toString()] ?: throw NoHouseExist("House not found.")

    override fun getAll(): List<House> = housesById.values.toList()

    override fun deleteById(key: Uuid) {
        housesById.remove(key.toString()) ?: throw NoHouseExist("House not found.")
    }

    override fun clear() {
        housesById.clear()
    }
}
