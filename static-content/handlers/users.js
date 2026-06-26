import {
    a,
    areComparableValuesEqual,
    buildPage,
    buildUrl,
    button,
    createSubmitGuard,
    createAlert,
    createLinkedOrEmpty,
    div,
    fetchJson,
    form,
    input,
    label,
    p,
    readSession,
    replaceMain,
    runAsync,
    validateEmail,
    validateRequired,
    writeSession,
} from "../utis/index.js"

const USER_PREVIEW_LIMIT = 2

function normalizeListPayload(data, key) {
    if (Array.isArray(data?.[key])) return data[key]
    return []
}

export async function fetchHouseTitles(bookings) {
    const uniqueHouseIds = [...new Set(bookings.map(booking => booking.hid).filter(Boolean))]
    const houses =
        await Promise.all(
            uniqueHouseIds.map(async houseId => {
                try {
                    return await fetchJson(buildUrl(`/houses/${encodeURIComponent(houseId)}`))
                } catch {
                    return null
                }
            }),
        )
    return new Map(houses.filter(Boolean).map(house => [house.hid, house.title]))
}

function getUsers(mainContent) {
    runAsync(
        mainContent,
        async () => {
            const [housesData, bookingsData] =
                await Promise.all([
                    fetchJson(buildUrl("/houses/mine"), { auth: true }),
                    fetchJson(buildUrl("/bookings/mine"), { auth: true }),
                ])

            const houses = normalizeListPayload(housesData, "houses").slice(0, USER_PREVIEW_LIMIT)
            const bookings = normalizeListPayload(bookingsData, "bookings").slice(0, USER_PREVIEW_LIMIT)
            const houseTitles = await fetchHouseTitles(bookings)

            replaceMain(
                mainContent,
                buildPage(
                    "Users",
                    div(
                        { class: "border rounded p-3 mb-3" },
                        div({ class: "fw-semibold mb-2" }, `My Houses (máx ${USER_PREVIEW_LIMIT})`),
                        createLinkedOrEmpty(
                            houses,
                            "Sem casas criadas.",
                            house => `#houses/${encodeURIComponent(house.hid)}`,
                            house => `${house.title} (${house.pricePerNight}/noite)`,
                        ),
                    ),
                    div(
                        { class: "border rounded p-3 mb-3" },
                        div({ class: "fw-semibold mb-2" }, `My Bookings (máx ${USER_PREVIEW_LIMIT})`),
                        createLinkedOrEmpty(
                            bookings,
                            "Sem bookings reservados.",
                            booking => `#bookings/${encodeURIComponent(booking.bid)}`,
                            booking => `${houseTitles.get(booking.hid) || "House"} | ${booking.startDate} -> ${booking.endDate}`,
                        ),
                    ),
                    div(
                        { class: "d-flex flex-wrap gap-3" },
                        a({ href: "#houses/mine" }, "Ver todas as houses"),
                        a({ href: "#bookings/mine" }, "Ver todos os bookings"),
                        a({ href: "#bookings/new" }, "Criar booking"),
                    ),
                ),
            )
        },
        "A carregar users...",
    )
}

function getUserById(mainContent, params = {}) {
    runAsync(
        mainContent,
        async () => {
            const user = await fetchJson(buildUrl(`/users/${encodeURIComponent(params.uid)}`))

            replaceMain(
                mainContent,
                buildPage(
                    "Contacto do utilizador",
                    div(
                        { class: "card border-0 shadow-sm" },
                        div(
                            { class: "row g-0" },
                            div(
                                { class: "col-md-4" },
                                div(
                                    {
                                        class: "h-100 d-flex align-items-center justify-content-center bg-light border-end",
                                        style: { minHeight: "240px" },
                                    },
                                    div(
                                        {
                                            class: "rounded-circle border bg-white d-flex align-items-center justify-content-center text-muted fw-semibold",
                                            style: { width: "150px", height: "150px", textAlign: "center", padding: "12px" },
                                        },
                                        "Utilizador desconhecido",
                                    ),
                                ),
                            ),
                            div(
                                { class: "col-md-8" },
                                div(
                                    { class: "card-body d-flex flex-column justify-content-center h-100" },
                                    div({ class: "h4 mb-3" }, user.name || "Sem nome"),
                                    p({ class: "mb-0 text-muted" }, `Contacto: ${user.email || "Sem email"}`),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        },
        "A carregar contacto do utilizador...",
    )
}

function getMyAccount(mainContent) {
    runAsync(
        mainContent,
        async () => {
            const session = readSession()
            if (!session?.id || !session?.token) {
                replaceMain(mainContent, buildPage("Minha conta", createAlert("Sessão indisponível.", "warning")))
                return
            }

            console.log("Session data:", session)

            const user = await fetchJson(buildUrl(`/users/${encodeURIComponent(session.id)}`))
            const statusBox = div()
            const updateGuard = createSubmitGuard()
            const nameInput = input({ class: "form-control", type: "text", required: true, value: user.name || "" })
            const emailInput = input({ class: "form-control", type: "email", required: true, value: user.email || "" })

            const editForm =
                form(
                    {
                        class: "border rounded p-3 mt-3",
                        onsubmit: async event => {
                            event.preventDefault()
                            const nameOk = validateRequired(nameInput, "nome")
                            const emailOk = validateEmail(emailInput, "email")
                            if (!nameOk || !emailOk) {
                                statusBox.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                                return
                            }
                            const payload = {
                                name: nameInput.value.trim(),
                                email: emailInput.value.trim(),
                            }
                            const initialPayload = {
                                name: user.name || "",
                                email: user.email || "",
                            }
                            if (areComparableValuesEqual(payload, initialPayload)) {
                                statusBox.replaceChildren(createAlert("Não existem alterações para guardar.", "secondary"))
                                return
                            }
                            const guard = updateGuard.begin()
                            if (!guard.ok) {
                                statusBox.replaceChildren(createAlert(guard.message, "warning"))
                                return
                            }

                            statusBox.replaceChildren(createAlert("A guardar alterações...", "secondary"))
                            try {
                                const updated = await fetchJson(
                                    buildUrl(`/users/${encodeURIComponent(session.id)}`),
                                    {
                                        method: "PUT",
                                        auth: true,
                                        body: payload,
                                    },
                                )
                                writeSession({ ...session, name: updated.name, email: updated.email })
                                statusBox.replaceChildren(createAlert("Conta atualizada.", "success"))
                            } catch (error) {
                                statusBox.replaceChildren(createAlert(error?.message || "Erro ao atualizar conta.", "danger"))
                            } finally {
                                updateGuard.end()
                            }
                        },
                    },
                    div({ class: "fw-semibold mb-3" }, "Editar dados"),
                    div({ class: "row g-2" },
                        div({ class: "col-md-6" }, label({ class: "form-label" }, "Nome"), nameInput),
                        div({ class: "col-md-6" }, label({ class: "form-label" }, "Email"), emailInput),
                    ),
                    div({ class: "mt-3 d-grid d-md-flex" }, button({ type: "submit", class: "btn btn-primary" }, "Guardar")),
                    statusBox,
                )

            replaceMain(
                mainContent,
                buildPage(
                    "Minha conta",
                    div(
                        { class: "card border-0 shadow-sm" },
                        div(
                            { class: "row g-0" },
                            div(
                                { class: "col-md-4" },
                                div(
                                    {
                                        class: "h-100 d-flex align-items-center justify-content-center bg-light border-end",
                                        style: { minHeight: "240px" },
                                    },
                                    div(
                                        {
                                            class: "rounded-circle border bg-white d-flex align-items-center justify-content-center text-muted fw-semibold",
                                            style: { width: "150px", height: "150px", textAlign: "center", padding: "12px" },
                                        },
                                        "Utilizador desconhecido",
                                    ),
                                ),
                            ),
                            div(
                                { class: "col-md-8" },
                                div(
                                    { class: "card-body d-flex flex-column justify-content-center h-100" },
                                    div({ class: "h4 mb-3" }, user.name || "Sem nome"),
                                    p({ class: "mb-0 text-muted" }, `Contacto: ${user.email || "Sem email"}`),
                                ),
                            ),
                        ),
                    ),
                    editForm,
                ),
            )
        },
        "A carregar conta...",
    )
}

export { getMyAccount, getUserById, getUsers }
