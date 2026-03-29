package main.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateHouseRequest(
    val title: String,
    val lid: String,
    val areaSqMt: Int,
    val pricePerNight: Double,
    val description: String
)

@Serializable
data class UpdateHouseRequest(
    val title: String,
    val lid: String,
    val areaSqMt: Int,
    val pricePerNight: Double,
    val description: String
)

@Serializable
data class DeleteHouseRequest(
    val id: String
)

@Serializable
data class CreateHouseResponse(
    val id: String,
    val uid: String,
    val title: String,
    val lid: String,
    val areaSqMt: Int,
    val pricePerNight: Double,
    val description: String
)

@Serializable
data class GetHouseResponse(
    val id: String,
    val uid: String,
    val title: String,
    val lid: String,
    val areaSqMt: Int,
    val pricePerNight: Double,
    val description: String
)

@Serializable
data class DeleteHouseResponse(
    val id: String,
    val deleted: Boolean
)

@Serializable
data class ListHousesResponse(
    val houses: List<GetHouseResponse>,
)
