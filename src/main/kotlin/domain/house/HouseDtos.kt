package domain.house

import kotlinx.serialization.Serializable

@Serializable
data class CreateHouseRequest(
    val title: String,
    val location: String,
    val areaSqMt: Int,
    val pricePerNight: Double,
    val description: String
)

@Serializable
data class CreateHouseResponse(
    val id: String,
    val uid: String,
    val title: String,
    val location: String,
    val areaSqMt: Int,
    val pricePerNight: Double,
    val description: String
)

@Serializable
data class GetHouseResponse(
    val id: String,
    val uid: String,
    val title: String,
    val location: String,
    val areaSqMt: Int,
    val pricePerNight: Double,
    val description: String
)

@Serializable
data class ListHousesResponse(
    val houses: List<GetHouseResponse>,
)
