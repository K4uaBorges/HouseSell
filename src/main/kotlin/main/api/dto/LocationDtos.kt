package main.api.dto

import kotlinx.serialization.Serializable

/**
 * Os request Delete em Location só existem, não para apagar localização,
 * mas sim mais para apagar localizações erradas ou mal escritas, para que os DEVs e donos do site, consigam ter esse feito,
 * imagina uma pessoa que escreve uma localizaçao nada a haver "aquivendecasa", não faz sentido, entao tem de ser apagada
 */

// Request DTOs
@Serializable
data class CreateLocationRequest(
    val name: String,
    val type: String,
    val parentId: String? = null
)

@Serializable
data class UpdateLocationRequest(
    val name: String,
    val type: String,
    val parentId: String? = null
)

@Serializable
data class DeleteLocationRequest(
    val id: String
)

// Response DTOs
@Serializable
data class CreateLocationResponse(
    val id: String,
    val name: String,
    val type: String,
    val parentId: String?,
    val token: String? = null,
)

@Serializable
data class GetLocationResponse(
    val id: String,
    val name: String,
    val type: String,
    val parentId: String?,
    val fullPath: List<LocationPathEntry>
)

@Serializable
data class DeleteLocationResponse(
    val id: String,
    val deleted: Boolean
)

@Serializable
data class LocationPathEntry(
    val id: String,
    val name: String,
    val type: String
)

@Serializable
data class ListLocationsResponse(
    val locations: List<LocationSummary>
)

@Serializable
data class LocationSummary(
    val id: String,
    val name: String,
    val type: String
)
