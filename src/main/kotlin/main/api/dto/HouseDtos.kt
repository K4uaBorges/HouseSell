package main.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class HouseWriteRequest(
    val title: String,
    val lid: String,
    val areaSqMt: Int,
    val pricePerNight: Double,
    val description: String,
)

typealias CreateHouseRequest = HouseWriteRequest

typealias UpdateHouseRequest = HouseWriteRequest

@Serializable
data class DeleteHouseRequest(
    val id: String,
)

@Serializable
data class CreateHouseResponse(
    val id: String,
    val uid: String,
    val title: String,
    val lid: String,
    val locationName: String = "",
    val locationType: String = "",
    val areaSqMt: Int,
    val pricePerNight: Double,
    val description: String,
    val token: String? = null,
)

@Serializable
data class GetHouseResponse(
    val id: String,
    val uid: String,
    val title: String,
    val lid: String,
    val locationName: String = "",
    val locationType: String = "",
    val areaSqMt: Int,
    val pricePerNight: Double,
    val description: String,
)

@Serializable
data class DeleteHouseResponse(
    val id: String,
    val deleted: Boolean,
)

@Serializable
data class ListHousesResponse(
    val houses: List<GetHouseResponse>,
)

@Serializable
data class HousePricePreviewResponse(
    val areaSqMt: Int,
    val predictedPricePerNight: Long,
    val trainingSource: String,
    val trainingSamples: Int,
    val modelWeight: Double,
    val modelBias: Double,
)

@Serializable
data class HouseCacheStatsResponse(
    val limit: Int,
    val size: Int,
    val hits: Long,
    val misses: Long,
    val hitRate: Double,
)

@Serializable
data class HouseAvailableDaysResponse(
    val houseId: String,
    val year: Int,
    val month: Int,
    val availableDays: List<String>,
)
