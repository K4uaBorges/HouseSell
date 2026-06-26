import router from "./router/router.js";
import handlers from "./handlers/indexHandlers.js";
import { registerHandlerRoutes } from "./handlers/indexHandlers.js";
import { createHouseCard, createOwnerMap } from "./views/houses/houseCards.js";
import {
    a,
    createSubmitGuard,
    buildUrl,
    button,
    createAlert,
    div,
    fetchJson,
    form,
    hasAdminAccess,
    h1,
    h2,
    input,
    label,
    p,
    readToken,
    removeToken,
    todayIsoDate,
    tomorrowIsoDate,
} from "./utis/index.js"

let isAuthenticated = false
let reloadPage = () => {
    window.location.reload()
}

window.addEventListener('load', () => {
    void loadHandler()
})
window.addEventListener('hashchange', () => {
    if (!isAuthenticated) return
    router.hashChangeHandler()
})

async function loadHandler(){
    registerRoutes()
    const restored = await tryRestoreAuthenticatedSession()
    if (!restored) {
        renderAuthenticationScreen()
    }
}

function normalizeHousesPayload(data) {
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.houses)) return data.houses
    return []
}

function shuffleHouses(items) {
    const list = [...items]
    for (let index = list.length - 1; index > 0; index -= 1) {
        const randomIndex = Math.floor(Math.random() * (index + 1))
        const current = list[index]
        list[index] = list[randomIndex]
        list[randomIndex] = current
    }
    return list
}

function renderAppShell() {
    const canManageLocations = hasAdminAccess()
    const browseLinks = [
        a({ href: "#dashboard" }, "Home"),
        a({ href: "#houses" }, "Houses"),
        a({ href: "#houses/available" }, "Houses Available"),
    ]

    const logoutButton =
        button(
            {
                type: "button",
                class: "btn btn-outline-danger",
                onclick: () => {
                    removeToken()
                    reloadPage()
                },
            },
            "Log out",
        )

    if (canManageLocations) {
        browseLinks.splice(2, 0, a({ href: "#locations" }, "Locations"))
    }

    const shell = div(
        { class: "app-shell" },
        div(
            { class: "topbar d-flex flex-wrap justify-content-between align-items-start gap-3 mb-3" },
            div(
                h1(
                    { class: "app-brand h3 mb-1" },
                    a({ href: "#dashboard", class: "brand-link text-decoration-none text-reset" }, "Booker"),
                ),
                p({ class: "topbar-subtitle text-muted mb-0" }, "Navegação principal."),
            ),
            logoutButton,
        ),
        div(
            { class: "nav-grid row g-3 mb-3" },
            div(
                { class: "col-lg-6" },
                div(
                    { class: "nav-panel border rounded p-3 h-100" },
                    h2({ class: "h6 mb-2" }, "My Dashboard"),
                    div({ class: "d-flex flex-wrap gap-3" },
                        a({ href: "#dashboard" }, "Dashboard"),
                        a({ href: "#account" }, "Minha Conta"),
                        a({ href: "#houses/mine" }, "My Houses"),
                        a({ href: "#bookings/mine" }, "My Bookings"),
                        a({ href: "#houses" }, "New Booking"),
                    ),
                ),
            ),
            div(
                { class: "col-lg-6" },
                div(
                    { class: "nav-panel border rounded p-3 h-100" },
                    h2({ class: "h6 mb-2" }, "Browse"),
                    div({ class: "d-flex flex-wrap gap-3" }, ...browseLinks),
                ),
            ),
        ),
        div({ id: "mainContent", class: "main-stage" }),
    )

    document.body.className = "app-body authenticated p-3"
    document.body.replaceChildren(shell)
}

function renderAuthenticationScreen() {
    isAuthenticated = false

    const authStatus = div()
    const createAccountGuard = createSubmitGuard()
    const authPanel = div()
    const browsePreviewPanel = div()
    const authSectionContainer = div()
    const browseSectionContainer = div()
    const createNameInput = input({ class: "form-control", type: "text", required: true, placeholder: "Nome" })
    const createEmailInput = input({ class: "form-control", type: "email", required: true, placeholder: "Email" })
    const createPasswordInput = input({ class: "form-control", type: "password", required: true, placeholder: "Password", minlength: 8, autocomplete: "new-password" })
    const loginEmailInput = input({ class: "form-control", type: "email", required: true, placeholder: "Email" })
    const loginPasswordInput = input({ class: "form-control", type: "password", required: true, placeholder: "Password", minlength: 8, autocomplete: "current-password" })

    const setActiveSection = sectionName => {
        authSectionContainer.hidden = sectionName !== "auth"
        browseSectionContainer.hidden = sectionName !== "browse"
    }

    const createForm =
        form(
            {
                class: "border rounded p-3 h-100",
                onsubmit: async event => {
                    event.preventDefault()
                    const name = createNameInput.value.trim()
                    const email = createEmailInput.value.trim()
                    const password = createPasswordInput.value
                    if (!name || !email || !password) {
                        authStatus.replaceChildren(createAlert("Preenche nome, email e password para criar conta.", "warning"))
                        return
                    }
                    const guard = createAccountGuard.begin()
                    if (!guard.ok) {
                        authStatus.replaceChildren(createAlert(guard.message, "warning"))
                        return
                    }

                    authStatus.replaceChildren(createAlert("A criar conta...", "secondary"))
                    try {
                        await fetchJson(
                            buildUrl("/users"),
                            { method: "POST", body: { name, email, password } },
                        )
                        await activateAuthenticatedApp("Conta criada com sucesso.")
                    } catch (error) {
                        authStatus.replaceChildren(createAlert(error?.message || "Erro ao criar conta.", "danger"))
                    } finally {
                        createAccountGuard.end()
                    }
                },
            },
            h2({ class: "h6 mb-3" }, "Criar Conta"),
            div({ class: "mb-2" }, label({ class: "form-label" }, "Nome"), createNameInput),
            div({ class: "mb-2" }, label({ class: "form-label" }, "Email"), createEmailInput),
            div({ class: "mb-3" }, label({ class: "form-label" }, "Password"), createPasswordInput),
            button({ type: "submit", class: "btn btn-primary w-100" }, "Criar Conta"),
        )

    const loginForm =
        form(
            {
                class: "border rounded p-3 h-100",
                onsubmit: async event => {
                    event.preventDefault()
                    const email = loginEmailInput.value.trim()
                    const password = loginPasswordInput.value
                    if (!email || !password) {
                        authStatus.replaceChildren(createAlert("Preenche email e password para login.", "warning"))
                        return
                    }

                    authStatus.replaceChildren(createAlert("A autenticar...", "secondary"))
                    try {
                        await fetchJson(
                            buildUrl("/session/login"),
                            { method: "POST", body: { email, password } },
                        )
                        await activateAuthenticatedApp("Login efetuado.")
                    } catch (error) {
                        authStatus.replaceChildren(createAlert(error?.message || "Erro ao autenticar.", "danger"))
                    }
                },
            },
            h2({ class: "h6 mb-3" }, "Login"),
            div({ class: "mb-2" }, label({ class: "form-label" }, "Email"), loginEmailInput),
            div({ class: "mb-3" }, label({ class: "form-label" }, "Password"), loginPasswordInput),
            button({ type: "submit", class: "btn btn-success w-100" }, "Entrar"),
        )

    const showCreateForm = () => {
        authPanel.replaceChildren(createForm)
        setActiveSection("auth")
    }

    const showLoginForm = () => {
        authPanel.replaceChildren(loginForm)
        setActiveSection("auth")
    }

    const showPublicHousesAvailable = async () => {
        setActiveSection("browse")
        browsePreviewPanel.replaceChildren(createAlert("A carregar casas para arrendar...", "secondary"))
        try {
            const [data, usersData] = await Promise.all([
                fetchJson(
                    buildUrl("/houses/available", { startDate: todayIsoDate(), endDate: tomorrowIsoDate() }),
                ),
                fetchJson(buildUrl("/users")),
            ])
            const houses = shuffleHouses(normalizeHousesPayload(data))
            const ownerById = createOwnerMap(usersData)
            const promptLogin = event => {
                event.preventDefault()
                showLoginForm()
                authStatus.replaceChildren(
                    createAlert("Para abrir os detalhes da casa, cria conta ou inicia sessão.", "warning"),
                )
            }

            const content =
                houses.length
                    ? div(
                        { class: "row g-3" },
                        houses.map(house =>
                            div(
                                { class: "col-12" },
                                createHouseCard(
                                    house,
                                    {
                                        ownerName: ownerById.get(String(house.uid || "")) || "Proprietário",
                                        startDate: todayIsoDate(),
                                        endDate: tomorrowIsoDate(),
                                        actions: [
                                            a(
                                                {
                                                    href: `#houses/${encodeURIComponent(house.hid)}`,
                                                    class: "btn btn-outline-secondary btn-sm",
                                                    onclick: promptLogin,
                                                },
                                                "Ver detalhes",
                                            ),
                                            a(
                                                {
                                                    href: `#houses/${encodeURIComponent(house.hid)}/bookings/dateStart=${encodeURIComponent(todayIsoDate())}&dateEnd=${encodeURIComponent(tomorrowIsoDate())}`,
                                                    class: "btn btn-primary btn-sm",
                                                    onclick: promptLogin,
                                                },
                                                "Alugar",
                                            ),
                                        ],
                                    },
                                ),
                            ),
                        ),
                    )
                    : createAlert("Sem casas.", "secondary")

            browsePreviewPanel.replaceChildren(
                div(
                    { class: "border rounded p-3 mt-3" },
                    h2({ class: "h6 mb-2" }, "Houses Available"),
                    content,
                ),
            )
        } catch (error) {
            browsePreviewPanel.replaceChildren(
                createAlert(error?.message || "Erro ao carregar casas disponíveis.", "danger"),
            )
        }
    }

    const publicNavigationLink =
        (text) =>
            a(
                {
                    href: "#houses/available",
                    onclick: event => {
                        event.preventDefault()
                        void showPublicHousesAvailable()
                    },
                },
                text,
            )

    showLoginForm()
    authSectionContainer.replaceChildren(authPanel)
    browseSectionContainer.replaceChildren(browsePreviewPanel)

    const authPage =
        div(
            { class: "auth-shell" },
            h1(
                { class: "app-brand h3 mb-1" },
                a(
                    {
                        href: "#login",
                        class: "brand-link text-decoration-none text-reset",
                        onclick: event => {
                            event.preventDefault()
                            showLoginForm()
                            setActiveSection("auth")
                        },
                    },
                    "Booker",
                ),
            ),
            p({ class: "topbar-subtitle text-muted mb-3" }, "Seleciona a secção e autentica-te em Users."),
            div(
                { class: "auth-nav-grid row g-3 mb-3" },
                div(
                    { class: "col-lg-6" },
                    div(
                        { class: "nav-panel border rounded p-3 h-100" },
                        h2({ class: "h6 mb-2" }, "Users"),
                        div(
                            { class: "d-flex flex-wrap gap-2" },
                            button(
                                {
                                    type: "button",
                                    class: "btn btn-outline-primary",
                                    onclick: showCreateForm,
                                },
                                "Criar Conta",
                            ),
                            button(
                                {
                                    type: "button",
                                    class: "btn btn-outline-success",
                                    onclick: showLoginForm,
                                },
                                "Iniciar Sessão",
                            ),
                        ),
                    ),
                ),
                div(
                    { class: "col-lg-6" },
                    div(
                        { class: "nav-panel border rounded p-3 h-100" },
                        h2({ class: "h6 mb-2" }, "Browse"),
                        div(
                            { class: "d-flex flex-wrap gap-3" },
                            publicNavigationLink("Home"),
                            publicNavigationLink("Houses"),
                            publicNavigationLink("Houses Available"),
                        ),
                    ),
                ),
            ),
            div({ class: "auth-panel-wrap" }, authSectionContainer),
            div({ class: "preview-panel-wrap" }, browseSectionContainer),
            div({ class: "mt-3" }, authStatus),
        )

    document.body.className = "app-body guest p-3"
    document.body.replaceChildren(authPage)
}

async function tryRestoreAuthenticatedSession() {
    const token = String(readToken() || "").trim()
    if (!token) return false

    try {
        await fetchJson(buildUrl("/houses/mine"), { auth: true })
        await activateAuthenticatedApp("Sessão restaurada a partir da cache.")
        return true
    } catch {
        removeToken()
        return false
    }
}

async function activateAuthenticatedApp(successMessage) {
    renderAppShell()
    isAuthenticated = true

    if (successMessage) {
        const mainContent = document.getElementById("mainContent")
        if (mainContent) {
            mainContent.replaceChildren(createAlert(successMessage, "success"))
        }
    }

    if (window.location.hash !== "#dashboard") {
        window.location.hash = "dashboard"
        return
    }

    router.hashChangeHandler()
}

function registerRoutes() {
    const homeHandler = resolveHandler("getHome")

    registerHandlerRoutes(router, homeHandler)

    router.addDefaultNotFoundRouteHandler(() => {
        window.location.hash = "dashboard"
    })
}

function resolveHandler(handlerName, fallback = null) {
    const handler = handlers[handlerName]
    if (typeof handler === "function") return handler
    if (typeof fallback === "function") return fallback
    return () => {}
}

function setReloadPage(fn) {
    reloadPage = typeof fn === "function" ? fn : () => {
        window.location.reload()
    }
}

export { setReloadPage, shuffleHouses }
