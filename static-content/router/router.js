const routes = []
let notFoundRouteHandler = () => {
    const currentPath = extractPath(window.location.hash)
    if (currentPath !== "dashboard") {
        window.location.hash = "dashboard"
    }
}

function normalizePath(rawPath) {
    if (!rawPath) return ""
    return rawPath
        .replace(/^#/, "")
        .replace(/^\//, "")
        .replace(/\/+$/, "")
}

function isInlineQuerySegment(segment) {
    if (!segment || !segment.includes("=")) return false
    const pairs = segment.split("&")
    if (!pairs.length) return false

    return pairs.every(pair => {
        if (!pair) return false
        const separatorIndex = pair.indexOf("=")
        if (separatorIndex <= 0) return false
        const key = pair.slice(0, separatorIndex).trim()
        return key.length > 0
    })
}

function splitHashParts(hash) {
    const raw = normalizePath(hash)
    if (!raw) return { pathPart: "", queryPart: "" }

    if (raw.includes("?")) {
        const separatorIndex = raw.indexOf("?")
        return {
            pathPart: raw.slice(0, separatorIndex),
            queryPart: raw.slice(separatorIndex + 1),
        }
    }

    const segments = raw.split("/")
    const tail = segments[segments.length - 1] || ""
    if (!isInlineQuerySegment(tail)) {
        return { pathPart: raw, queryPart: "" }
    }

    return {
        pathPart: segments.slice(0, -1).join("/"),
        queryPart: tail,
    }
}

function extractPath(hash) {
    const { pathPart } = splitHashParts(hash)
    return normalizePath(pathPart)
}

function parseQuery(hash) {
    const { queryPart } = splitHashParts(hash)
    const searchParams = new URLSearchParams(queryPart)
    const query = {}

    for (const [key, value] of searchParams.entries()) {
        query[key] = value
    }
    return query
}

function splitSegments(path) {
    const normalized = normalizePath(path)
    if (!normalized) return []
    return normalized.split("/").map(segment => decodeURIComponent(segment))
}

function matchRoute(pathTemplate, path) {
    const templateSegments = splitSegments(pathTemplate)
    const pathSegments = splitSegments(path)
    if (templateSegments.length !== pathSegments.length) return null

    const params = {}

    for (let i = 0; i < templateSegments.length; i += 1) {
        const templateSegment = templateSegments[i]
        const pathSegment = pathSegments[i]

        if (templateSegment.startsWith(":")) {
            const paramName = templateSegment.substring(1)
            if (!paramName) return null
            params[paramName] = pathSegment
            continue
        }

        if (templateSegment !== pathSegment) return null
    }

    return params
}

function addRoute(pathTemplate, handler) {
    routes.push({ pathTemplate, handler })
}

function addDefaultNotFoundRouteHandler(handler) {
    notFoundRouteHandler = handler
}

function resolveRoute(path) {
    for (const route of routes) {
        const params = matchRoute(route.pathTemplate, path)
        if (params !== null) {
            return { handler: route.handler, params }
        }
    }
    return null
}

function hashChangeHandler(mainContent = document.getElementById("mainContent")) {
    const hash = window.location.hash || ""
    const path = extractPath(hash)
    const query = parseQuery(hash)

    const route = resolveRoute(path)

    console.log(`Navigating to path: "${path}" with query:`)

    if (!route) {
        notFoundRouteHandler(mainContent, {}, query)
        return
    }

    route.handler(mainContent, route.params, query)
}

const router = {
    addRoute,
    addDefaultNotFoundRouteHandler,
    hashChangeHandler,
}

export default router
