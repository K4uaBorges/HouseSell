package domain.house

import domain.booking.AvailableHouseResponse
import domain.house.repository.HouseRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HouseService(private val repo: HouseRepository) {

    fun createHouse(
        ownerId: Uuid,
        titleRaw: String,
        locationRaw: String,
        areaSqMt: Int,
        pricePerNight: Double,
        descriptionRaw: String
    ): House {
        val title = Title.of(titleRaw)
        val location = locationRaw.trim()
        val description = descriptionRaw.trim()
        val house = House(
            id = Uuid.random(),
            uid = ownerId,
            title = title,
            location = location,
            areaSqMt = areaSqMt,
            pricePerNight = pricePerNight,
            description = description,
        )

        house.certified
        return repo.create(house)
    }

    fun getHouseById(id: String): House? = repo.getById(id.trim())

    fun getHouseInfoById(id: String): GetHouseResponse =
        requireNotNull(getHouseById(id)){ "House not found." }.toGetHouseResponse()

    fun listHouses(): List<House> = repo.getAll().sortedBy { it.title.value }
    fun listHousesByOwner(ownerId: Uuid): List<House> =
        repo.getAll().filter { it.uid == ownerId }.sortedBy { it.title.value }

    fun deleteHouse(id: String) = repo.delete(id.trim())

    fun updateHouse(house: House, title: Title, location: String, area: Int, price: Double, dscp: String){
        val upHouse = house.copy(
            title = title,
            location = location,
            areaSqMt = area,
            pricePerNight = price,
            description = dscp
        )
        upHouse.certified
        repo.update(upHouse)
    }

    private val House.certified get() = run {
        require(location.isNotEmpty()) { "Location is required." }
        require(areaSqMt > 0) { "Area must be greater than zero." }
        require(pricePerNight > 0) { "Price must be greater than zero." }
        require(description.isNotEmpty()) { "Description is required." }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun House.toGetHouseResponse() = GetHouseResponse(
    id = id.toString(),
    uid = uid.toString(),
    title = title.value,
    location = location,
    areaSqMt = areaSqMt,
    pricePerNight = pricePerNight,
    description = description,
)

@OptIn(ExperimentalUuidApi::class)
fun House.toAvailableHouseResponse() = AvailableHouseResponse(
    id = id.toString(),
    uid = uid.toString(),
    title = title.value,
    location = location,
    areaSqMt = areaSqMt,
    pricePerNight = pricePerNight,
    description = description,
)
