package domain.house.repository

import domain.house.House

interface HouseRepository {
    fun create(house: House): House
    fun delete(id: String)
    fun update(house: House): House
    fun getById(id: String): House?
    fun getAll(): List<House>
}
