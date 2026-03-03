package utils

import org.http4k.core.Request
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun bearerToken(authorizationHeader: String?): Uuid? {
    val header = authorizationHeader?.trim() ?: return null
    val prefix = "Bearer "
    if (!header.startsWith(prefix, ignoreCase = true)) return null

    val raw = header.substring(prefix.length).trim()
    if (raw.isEmpty()) return null
    return runCatching { Uuid.parse(raw) }.getOrNull()
}

@OptIn(ExperimentalUuidApi::class)
fun bearerToken(request: Request): Uuid? =
    bearerToken(request.header("Authorization"))
