const TOKEN_STORAGE_KEY = "houses.auth.token"
const SESSION_STORAGE_KEY = "houses.auth.session"
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

function readSession() {
    const raw = localStorage.getItem(SESSION_STORAGE_KEY)?.trim()
    if (!raw) return null

    try {
        const parsed = JSON.parse(raw)
        return typeof parsed === "object" && parsed ? parsed : null
    } catch {
        return null
    }
}

function pickField(source, key, fallback = "") {
    if (source && typeof source === "object" && Object.hasOwn(source, key)) {
        return source[key]
    }
    return fallback
}

function normalizeSession(session, fallback = {}) {
    if (!session || typeof session !== "object") return null

    const token = String(pickField(session, "token", fallback.token) || "").trim()
    if (!token) return null

    return {
        token,
        id: String(pickField(session, "id", pickField(session, "userId", fallback.id)) || "").trim(),
        name: String(pickField(session, "name", fallback.name) || "").trim(),
        email: String(pickField(session, "email", fallback.email) || "").trim(),
        role: String(pickField(session, "role", fallback.role) || "").trim(),
    }
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

function writeSession(session) {
    const existing = readSession() || {}
    const normalized = normalizeSession(session, existing)
    if (!normalized) return

    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(normalized))
    writeToken(normalized.token)
}

function removeToken() {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    localStorage.removeItem(SESSION_STORAGE_KEY)
    notifyTokenChanged("")
}

function syncTokenFromApiPayload(payload) {
    const token = maybeExtractToken(payload)
    if (!token) return
    writeSession(payload)
}

function hasAdminAccess(session = readSession()) {
    if (!session || typeof session !== "object") return false

    const role = String(session.role || "").trim().toLowerCase()
    return role === "admin"
}

export {
    AUTH_TOKEN_CHANGED_EVENT,
    SESSION_STORAGE_KEY,
    TOKEN_STORAGE_KEY,
    hasAdminAccess,
    readSession,
    readToken,
    removeToken,
    syncTokenFromApiPayload,
    writeSession,
    writeToken,
}
