package main.api.utils

import main.errors.UnauthorizedException
import org.http4k.core.Request
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun bearerToken(authorizationHeader: String?): Uuid {
    val header =
        authorizationHeader?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw UnauthorizedException("Missing Authorization header.")
    val prefix = "Bearer "
    if (!header.startsWith(prefix, ignoreCase = true)) {
        throw UnauthorizedException("Authorization header must start with Bearer.")
    }

    val raw = header.substring(prefix.length).trim()
    if (raw.isEmpty()) throw UnauthorizedException("Bearer token is missing.")
    return runCatching { Uuid.parse(raw) }
        .getOrElse { throw UnauthorizedException("Invalid bearer token.") }
}

@OptIn(ExperimentalUuidApi::class)
fun bearerToken(request: Request): Uuid = bearerToken(request.header("Authorization"))

@OptIn(ExperimentalUuidApi::class)
fun bearerTokenOrNull(authorizationHeader: String?): Uuid? {
    val header = authorizationHeader?.trim().orEmpty()
    if (header.isEmpty()) return null
    return bearerToken(header)
}

@OptIn(ExperimentalUuidApi::class)
fun bearerTokenOrNull(request: Request): Uuid? = bearerTokenOrNull(request.header("Authorization"))
