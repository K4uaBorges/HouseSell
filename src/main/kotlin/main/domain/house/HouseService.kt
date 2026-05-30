package main.domain.house

import main.api.dto.GetHouseResponse
import main.data.impl.caches.HouseCacheStats
import main.data.impl.caches.HouseInfoCache
import main.data.interfaces.HouseRepository
import main.errors.DuplicateHouseException
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
        ensureUniqueHouseData(ownerId, title.value, description)
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

    fun getHouseById(id: Uuid): House = cache.getById(id) ?: repo.getById(id).also { cache.put(id, it) }

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
        ensureUniqueHouseData(house.uid, titleRaw, descriptionRaw, excludeId = id)
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

    private fun ensureUniqueHouseData(
        ownerId: Uuid,
        titleRaw: String,
        descriptionRaw: String,
        excludeId: Uuid? = null,
    ) {
        val normalizedTitle = normalizeText(titleRaw)
        val normalizedDescription = normalizeText(descriptionRaw)

        repo.getAll()
            .asSequence()
            .filter { it.uid == ownerId }
            .filter { it.id != excludeId }
            .forEach { existing ->
                if (normalizeText(existing.title.value) == normalizedTitle) {
                    throw DuplicateHouseException("Já existe uma house tua com esse título.")
                }
                if (normalizeText(existing.description) == normalizedDescription) {
                    throw DuplicateHouseException("Já existe uma house tua com essa descrição.")
                }
            }
    }

    private fun normalizeText(raw: String): String =
        raw.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

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
