package domain.location

import kotlinx.serialization.Serializable

@Serializable
enum class Region{ COUNTRY, REGION, DISTRICT, MUNICIPALITY, LOCALITY }

@Serializable
data class CreateLocationRequest(
    val name: String,
    val type: Region,
    val parentId: String? = null // UUID em string (ou usa UUID com serializer)
)

@Serializable
data class CreateLocationResponse(val id: String)

@Serializable
data class LocationResponse(
    val id: String,
    val name: String,
    val type: Region,
    val parentId: String? = null
)

@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val skip: Int,
    val limit: Int
)

@Serializable
data class PathResponse(
    val items: List<LocationResponse>
)