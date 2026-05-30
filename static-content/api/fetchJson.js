import { createApiError } from "../error/createApiError.js"
import { applyPassportAuthorization } from "../passport/passportAuth.js"
import { syncTokenFromApiPayload } from "../token/tokenStorage.js"
import { rememberApiBaseFromResolvedUrl } from "./buildUrl.js"

function parseBody(rawBody) {
    if (!rawBody) return null

    try {
        return JSON.parse(rawBody)
    } catch {
        return rawBody
    }
}

function createHttpError(status, requestUrl, parsedBody) {
    const apiMessage =
        typeof parsedBody === "object" && parsedBody && "error" in parsedBody
            ? parsedBody.error
            : `HTTP ${status} (${requestUrl})`
    return createApiError(status, apiMessage, parsedBody)
}

async function fetchJson(
    url,
    { auth = false, method = "GET", body = null, headers: extraHeaders = {} } = {},
) {
    let headers = { Accept: "application/json", ...extraHeaders }

    if (auth) {
        headers = applyPassportAuthorization(headers)
    }

    let payload = undefined
    if (body !== null && body !== undefined) {
        headers["Content-Type"] = headers["Content-Type"] || "application/json"
        payload = typeof body === "string" ? body : JSON.stringify(body)
    }

    let response
    try {
        response = await fetch(url, { method, headers, body: payload })
    } catch {
        throw createApiError(
            503,
            `API indisponivel ou servidor desligado. URL: ${url}`,
        )
    }

    const rawBody = await response.text()
    const parsedBody = parseBody(rawBody)

    if (!response.ok) {
        throw createHttpError(response.status, url, parsedBody)
    }

    rememberApiBaseFromResolvedUrl(url)
    syncTokenFromApiPayload(parsedBody)
    return parsedBody
}

export { fetchJson }
