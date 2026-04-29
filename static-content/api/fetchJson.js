import { createApiError } from "../error/createApiError.js"
import { applyPassportAuthorization } from "../passport/passportAuth.js"
import { syncTokenFromApiPayload } from "../token/tokenStorage.js"

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

    const response = await fetch(url, { method, headers, body: payload })
    const rawBody = await response.text()

    let parsedBody = null
    if (rawBody) {
        try {
            parsedBody = JSON.parse(rawBody)
        } catch {
            parsedBody = rawBody
        }
    }

    if (!response.ok) {
        const apiMessage =
            typeof parsedBody === "object" && parsedBody && "error" in parsedBody
                ? parsedBody.error
                : `HTTP ${response.status} (${url})`
        throw createApiError(response.status, apiMessage, parsedBody)
    }

    syncTokenFromApiPayload(parsedBody)
    return parsedBody
}

export { fetchJson }
