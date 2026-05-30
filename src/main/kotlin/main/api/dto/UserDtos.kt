package main.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
data class LoginUserRequest(
    val email: String,
    val password: String,
)

@Serializable
data class UpdateUserRequest(
    val name: String,
    val email: String,
)

@Serializable
data class DeleteUserRequest(
    val id: String,
)

@Serializable
data class UserSessionResponse(
    val id: String,
    val name: String,
    val email: String,
    val token: String,
    val role: String = "USER",
)

typealias CreateUserResponse = UserSessionResponse

typealias LoginUserResponse = UserSessionResponse

@Serializable
data class GetUserResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
)

@Serializable
data class DeleteUserResponse(
    val id: String,
    val deleted: Boolean,
)

@Serializable
data class ListUsersResponse(
    val users: List<GetUserResponse>,
)

@Serializable
data class BootstrapSessionResponse(
    val token: String,
    val userId: String,
    val role: String = "USER",
    val locationId: String,
    val freeHouseId: String,
    val busyHouseId: String,
)
