// API requests are routed to /api or to a single configured API base.
const API_BASE = "/api"

function rememberApiBaseFromResolvedUrl(_url) {}

function isAbsoluteHttpUrl(value) {
    return /^https?:\/\//i.test(value)
}

function resolveApiBase() {
    if (typeof window === "undefined") return API_BASE
    const configured = String(window.__HOUSES_API_BASE__ || "").trim()
    if (!configured) return API_BASE
    return configured.endsWith("/") ? configured.slice(0, -1) : configured
}

function buildUrl(path, query = {}) {
    const normalizedPath = path.startsWith("/") ? path : `/${path}`
    const apiBase = resolveApiBase()
    const url = new URL(`${apiBase}${normalizedPath}`, window.location.origin)

    for (const [key, value] of Object.entries(query)) {
        if (value === null || value === undefined) continue
        const trimmed = String(value).trim()
        if (!trimmed) continue
        url.searchParams.set(key, trimmed)
    }

    return isAbsoluteHttpUrl(apiBase)
        ? `${url.origin}${url.pathname}${url.search}`
        : `${url.pathname}${url.search}`
}

export { buildUrl, rememberApiBaseFromResolvedUrl }
