package main.domain_model.house

import main.api.dto.AvailableHouseResponse
import main.api.dto.GetHouseResponse
import main.data.impl.caches.HouseCacheStats
import main.data.impl.caches.HouseInfoCache
import main.data.interfaces.HouseRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HouseService(
    private val repo: HouseRepository,
    private val cache: HouseInfoCache = HouseInfoCache(0),
) {
    fun createHouse(
        ownerId: Uuid,
        titleRaw: String,
        locationHouse: Uuid,
        areaSqMt: Int,
        pricePerNight: Double,
        descriptionRaw: String,
    ): House {
        val title = Title.of(titleRaw)
        val description = descriptionRaw.trim()
        val house =
            House(
                id = Uuid.random(),
                uid = ownerId,
                title = title,
                lid = locationHouse,
                areaSqMt = areaSqMt,
                pricePerNight = pricePerNight,
                description = description,
            )

        house.certified
        return repo.create(house).also { cache.put(it.id, it) }
    }

    fun getHouseById(id: Uuid): House =
        cache.getById(id) ?: repo.getById(id).also { cache.put(id, it) }

    fun getHouseInfoById(id: Uuid): GetHouseResponse = getHouseById(id).toGetHouseResponse()

    fun listHouses(): List<House> = repo.getAll().sortedBy { it.title.value }

    fun listHousesByOwner(ownerId: Uuid): List<House> = repo.getAll().filter { it.uid == ownerId }.sortedBy { it.title.value }

    fun deleteHouse(id: Uuid) {
        repo.deleteById(id)
        cache.removeById(id)
    }

    fun updateHouse(
        id: Uuid,
        titleRaw: String,
        locationRaw: Uuid,
        areaSM: Int,
        pPN: Double,
        descriptionRaw: String,
    ): House {
        val house = getHouseById(id)
        val upHouse =
            house.copy(
                title = Title.of(titleRaw),
                lid = locationRaw,
                areaSqMt = areaSM,
                pricePerNight = pPN,
                description = descriptionRaw,
            )
        upHouse.certified
        return repo.update(upHouse).also { cache.put(it.id, it) }
    }

    fun cacheStats(): HouseCacheStats = cache.stats()

    private val House.certified get() =
        run {
            require(areaSqMt > 0) { "Area must be greater than zero." }
            require(pricePerNight > 0) { "Price must be greater than zero." }
            require(description.isNotEmpty()) { "Description is required." }
        }
}

@OptIn(ExperimentalUuidApi::class)
fun House.toGetHouseResponse() =
    GetHouseResponse(
        id = id.toString(),
        uid = uid.toString(),
        title = title.value,
        lid = lid.toString(),
        areaSqMt = areaSqMt,
        pricePerNight = pricePerNight,
        description = description,
    )

@OptIn(ExperimentalUuidApi::class)
fun House.toAvailableHouseResponse() =
    AvailableHouseResponse(
        id = id.toString(),
        uid = uid.toString(),
        title = title.value,
        lid = lid.toString(),
        areaSqMt = areaSqMt,
        pricePerNight = pricePerNight,
        description = description,
    )
