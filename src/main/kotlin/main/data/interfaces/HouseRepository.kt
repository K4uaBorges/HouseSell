package main.data.interfaces

import main.data.interfaces.Repository
import main.domain_model.house.House
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface HouseRepository : Repository<Uuid, House> {
    override fun create(value: House): House

    override fun save(value: House): House

    override fun getById(key: Uuid): House

    override fun getAll(): List<House>

    override fun deleteById(key: Uuid)

    override fun update(updated: House): House

    override fun clear()
}
