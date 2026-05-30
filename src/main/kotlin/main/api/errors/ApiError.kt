package main.api.errors

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val status: Int,
    val error: String,
)
