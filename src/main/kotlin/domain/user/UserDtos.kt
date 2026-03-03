package domain.user

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String
)

@Serializable
data class CreateUserResponse(
    val id: String,
    val name: String,
    val email: String,
    val token: String
)

@Serializable
data class GetUserResponse(
    val id: String,
    val name: String,
    val email: String
)