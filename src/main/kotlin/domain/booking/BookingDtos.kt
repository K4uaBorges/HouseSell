package domain.booking

import kotlinx.serialization.Serializable

@Serializable
data class CreateBookingRequest(
    val hid: String,
    val startDate: String,
    val endDate: String,
)

@Serializable
data class CreateBookingResponse(
    val id: String,
    val hid: String,
    val uid: String,
    val startDate: String,
    val endDate: String,
)

@Serializable
data class GetBookingResponse(
    val id: String,
    val hid: String,
    val uid: String,
    val startDate: String,
    val endDate: String,
)

@Serializable
data class ListBookingsResponse(
    val bookings: List<GetBookingResponse>,
)

@Serializable
data class ListAvailableHousesResponse(
    val houses: List<AvailableHouseResponse>,
)

@Serializable
data class AvailableHouseResponse(
    val id: String,
    val uid: String,
    val title: String,
    val location: String,
    val areaSqMt: Int,
    val pricePerNight: Double,
    val description: String,
)
