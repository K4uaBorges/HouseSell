import router from "./router/router.js";
import handlers from "./handlers/indexHandlers.js";
import { registerHandlerRoutes } from "./handlers/indexHandlers.js";
import { a, buildUrl, div, fetchJson, h2 } from "./utis/index.js"

window.addEventListener('load', () => {
    void loadHandler()
})
window.addEventListener('hashchange', () => router.hashChangeHandler())

async function loadHandler(){
    renderAppShell()
    await ensureBootstrapPrincipalSession()
    registerRoutes()

    const initialHash = resolveInitialHash()
    if (!window.location.hash && initialHash) {
        window.location.hash = initialHash
        return
    }

    router.hashChangeHandler()
}

async function ensureBootstrapPrincipalSession() {
    try {
        await fetchJson(buildUrl("/session/bootstrap"))
    } catch (error) {
        console.warn("Bootstrap session failed:", error)
    }
}

function resolveInitialHash() {
    if (window.location.hash) return window.location.hash

    const rawPath =
        window.location.pathname
            .replace(/^\/+/, "")
            .replace(/\/+$/, "")

    if (!rawPath || rawPath === "index.html") return "home"
    if (rawPath.startsWith("api/")) return "home"

    const search = window.location.search || ""
    return `${rawPath}${search}`
}

function renderAppShell() {
    const navLinks = [
        { href: "#home", text: "Home" },
        { href: "#users", text: "Users" },
        { href: "#locations", text: "Locations" },
        { href: "#houses", text: "Houses" },
        { href: "#houses/available", text: "Houses Available" },
    ]

    const shell =
        div(
            h2({ class: "h3 mb-2" }, "Houses / Rentals"),
            div(
                { class: "d-flex flex-wrap gap-3 mb-3" },
                navLinks.map(link => a({ href: link.href }, link.text)),
            ),
            div({ id: "mainContent" }),
        )

    document.body.className = "p-3"
    document.body.replaceChildren(shell)
}

function registerRoutes() {
    const homeHandler = resolveHandler("getHome")

    registerHandlerRoutes(router, homeHandler)

    router.addDefaultNotFoundRouteHandler(() => {
        window.location.hash = "home"
    })
}

function resolveHandler(handlerName, fallback = null) {
    const handler = handlers[handlerName]
    if (typeof handler === "function") return handler
    if (typeof fallback === "function") return fallback
    return () => {}
}
