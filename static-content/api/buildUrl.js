// API requests are routed through backend under /api.
const API_BASE = "/api"
const API_BASE_STORAGE_KEY = "houses.api.base"

function normalizeApiBase(rawValue) {
    const value = String(rawValue || "").trim()
    if (!value) return ""
    return value.endsWith("/") ? value.slice(0, -1) : value
}

function isAbsoluteHttpUrl(value) {
    return /^https?:\/\//i.test(value)
}

function resolveApiBase() {
    if (typeof window === "undefined") return API_BASE

    const globalBase = normalizeApiBase(window.__HOUSES_API_BASE__)
    if (globalBase) return globalBase

    const queryBase = normalizeApiBase(new URLSearchParams(window.location.search).get("apiBase"))
    if (queryBase) {
        try {
            window.localStorage.setItem(API_BASE_STORAGE_KEY, queryBase)
        } catch {}
        return queryBase
    }

    try {
        const storedBase = normalizeApiBase(window.localStorage.getItem(API_BASE_STORAGE_KEY))
        if (storedBase) return storedBase
    } catch {}

    return API_BASE
}

function buildUrl(path, query = {}) {
    const normalizedPath = path.startsWith("/") ? path : `/${path}`
    const basePath = resolveApiBase()
    const fullPath = `${basePath}${normalizedPath}`
    const url = new URL(fullPath, window.location.origin)

    for (const [key, value] of Object.entries(query)) {
        if (value === null || value === undefined) continue
        const trimmed = String(value).trim()
        if (!trimmed) continue
        url.searchParams.set(key, trimmed)
    }

    return isAbsoluteHttpUrl(basePath)
        ? `${url.origin}${url.pathname}${url.search}`
        : `${url.pathname}${url.search}`
}

export { buildUrl }
