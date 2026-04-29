const TOKEN_STORAGE_KEY = "houses.auth.token"
const AUTH_TOKEN_CHANGED_EVENT = "houses:auth-token-changed"

function notifyTokenChanged(token) {
    window.dispatchEvent(
        new CustomEvent(AUTH_TOKEN_CHANGED_EVENT, {
            detail: { token: token || "" },
        }),
    )
}

function maybeExtractToken(payload) {
    if (!payload || typeof payload !== "object") return ""
    const raw = typeof payload.token === "string" ? payload.token.trim() : ""
    return raw
}

function readToken() {
    return localStorage.getItem(TOKEN_STORAGE_KEY)?.trim()
}

function writeToken(token) {
    const normalized = String(token || "").trim()
    if (!normalized) return
    localStorage.setItem(TOKEN_STORAGE_KEY, normalized)
    notifyTokenChanged(normalized)
}

function removeToken() {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    notifyTokenChanged("")
}

function syncTokenFromApiPayload(payload) {
    const token = maybeExtractToken(payload)
    if (!token) return
    writeToken(token)
}

export {
    AUTH_TOKEN_CHANGED_EVENT,
    TOKEN_STORAGE_KEY,
    readToken,
    removeToken,
    syncTokenFromApiPayload,
    writeToken,
}
