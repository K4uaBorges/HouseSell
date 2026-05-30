import {
    a,
    areComparableValuesEqual,
    buildPage,
    buildUrl,
    button,
    clearFieldsValidation,
    createSubmitGuard,
    createAlert,
    div,
    fetchJson,
    form,
    input,
    label,
    li,
    p,
    replaceMain,
    runAsync,
    todayIsoDate,
    tomorrowIsoDate,
    ul,
    validateDateRange,
    validateIsoDate,
    validateRequired,
    withError,
    withLoading,
} from "../../utis/index.js"
import { createHouseCard, createOwnerMap } from "../houses/houseCards.js"

function normalizeHousesPayload(data) {
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.houses)) return data.houses
    return []
}

function normalizeBookingsPayload(data) {
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.bookings)) return data.bookings
    return []
}

async function fetchHousesByIds(houseIds) {
    const uniqueIds = [...new Set(houseIds.filter(Boolean))]
    const houses =
        await Promise.all(
            uniqueIds.map(async houseId => {
                try {
                    return await fetchJson(buildUrl(`/houses/${encodeURIComponent(houseId)}`))
                } catch {
                    return null
                }
            }),
        )
    return houses.filter(Boolean)
}

function createBookingEditor({
    fixedHouse,
    initialStartDate = "",
    initialEndDate = "",
    titleText,
    submitLabel,
    submitClass,
    onSubmit,
    skipUnchangedCheck = true,
}) {
    const statusBox = div()
    const submitGuard = createSubmitGuard()
    const startDateInput = input({ class: "form-control", type: "date", required: true, value: initialStartDate })
    const endDateInput = input({ class: "form-control", type: "date", required: true, value: initialEndDate })

    return form(
        {
            class: "border rounded p-3 mb-3",
            onsubmit: async event => {
                event.preventDefault()
                clearFieldsValidation([startDateInput, endDateInput])
                const startOk = validateIsoDate(startDateInput, "startDate")
                const endOk = validateIsoDate(endDateInput, "endDate")
                const rangeOk = startOk && endOk ? validateDateRange(startDateInput, endDateInput) : false
                if (!startOk || !endOk || !rangeOk) {
                    statusBox.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                    return
                }

                const payload = {
                    hid: fixedHouse.id,
                    startDate: startDateInput.value.trim(),
                    endDate: endDateInput.value.trim(),
                }
                if (!skipUnchangedCheck && fixedHouse && areComparableValuesEqual(payload, {
                    hid: fixedHouse.id,
                    startDate: initialStartDate,
                    endDate: initialEndDate,
                })) {
                    statusBox.replaceChildren(createAlert("Não existem alterações para guardar.", "secondary"))
                    return
                }
                const guard = submitGuard.begin()
                if (!guard.ok) {
                    statusBox.replaceChildren(createAlert(guard.message, "warning"))
                    return
                }

                statusBox.replaceChildren(createAlert("A guardar booking...", "secondary"))
                try {
                    await onSubmit(payload)
                    statusBox.replaceChildren(createAlert("Booking guardada.", "success"))
                } catch (error) {
                    statusBox.replaceChildren(createAlert(error?.message || "Erro ao guardar booking.", "danger"))
                } finally {
                    submitGuard.end()
                }
            },
        },
        div({ class: "mb-2 fw-semibold" }, titleText),
        div(
            { class: "row g-2" },
            div({ class: "col-md-5" }, label({ class: "form-label" }, "startDate"), startDateInput),
            div({ class: "col-md-5" }, label({ class: "form-label" }, "endDate"), endDateInput),
            div(
                { class: "col-md-2 d-grid" },
                button({ type: "submit", class: `${submitClass} mt-md-4` }, submitLabel),
            ),
        ),
        statusBox,
    )
}

function createBookingsList(bookings, houseById = new Map()) {
    if (!bookings.length) return createAlert("Sem bookings.", "secondary")

    return ul(
        { class: "list-group" },
        bookings.map(booking =>
            li(
                { class: "list-group-item" },
                div({ class: "fw-semibold" }, houseById.get(booking.hid)?.title || "House"),
                div({ class: "text-muted mb-2" }, `${booking.startDate} -> ${booking.endDate}`),
                a({ href: `#bookings/${encodeURIComponent(booking.id)}` }, "Ver detalhe"),
            ),
        ),
    )
}

function getCreateBookingView(mainContent, _params = {}, query = {}) {
    window.location.hash = "#houses"
}

function getBookingsByHouse(mainContent, params = {}, query = {}) {
    const hid = params.hid
    const dateStart = String(query.dateStart || todayIsoDate()).trim()
    const dateEnd = String(query.dateEnd || tomorrowIsoDate()).trim()

    runAsync(
        mainContent,
        async () => {
            const [house, usersData] = await Promise.all([
                fetchJson(buildUrl(`/houses/${encodeURIComponent(hid)}`)),
                fetchJson(buildUrl("/users")),
            ])
            const ownerById = createOwnerMap(usersData)

            const createForm =
                createBookingEditor({
                    fixedHouse: house,
                    initialStartDate: dateStart,
                    initialEndDate: dateEnd,
                    titleText: "Reserva esta house",
                    submitLabel: "Criar",
                    submitClass: "btn btn-primary",
                    onSubmit: async payload => {
                        const created = await fetchJson(buildUrl("/bookings"), { method: "POST", auth: true, body: payload })
                        window.location.hash = `#bookings/${encodeURIComponent(created.id)}`
                    },
                })

            replaceMain(
                mainContent,
                buildPage(
                    `Criar Booking`,
                    createHouseCard(house, {
                        ownerName: ownerById.get(String(house.uid || "")) || "Proprietário",
                        actions: [],
                    }),
                    createForm,
                ),
            )
        },
        "A carregar house para booking...",
    )
}

function getMyBookings(mainContent) {
    withLoading(mainContent, "A carregar my bookings...")

    fetchJson(buildUrl("/bookings/mine"), { auth: true })
        .then(async data => {
            const bookings = normalizeBookingsPayload(data)
            const houses = await fetchHousesByIds(bookings.map(booking => booking.hid))
            const houseById = new Map(houses.map(house => [house.id, house]))

            replaceMain(
                mainContent,
                buildPage(
                    "My Bookings",
                    div({ class: "mb-3" }, a({ href: "#houses" }, "Criar nova booking")),
                    createBookingsList(bookings, houseById),
                ),
            )
        })
        .catch(error => {
            if (error?.status === 401) {
                const page =
                    div(
                        h2({ class: "h4 mb-3" }, "My Bookings"),
                        createAlert("Precisas de token", "warning"),
                        p(a({ href: "#home" }, "Ir para Home / configurar token")),
                    )

                replaceMain(mainContent, page)
                return
            }

            withError(mainContent, error)
        })
}

function getBookingById(mainContent, params = {}) {
    runAsync(
        mainContent,
        async () => {
            const bid = params.bid
            const booking = await fetchJson(buildUrl(`/bookings/${encodeURIComponent(bid)}`), { auth: true })
            const currentHouse = await fetchJson(buildUrl(`/houses/${encodeURIComponent(booking.hid)}`))

            const updateForm =
                createBookingEditor({
                    fixedHouse: currentHouse,
                    initialStartDate: booking.startDate,
                    initialEndDate: booking.endDate,
                    titleText: "Atualizar Booking",
                    submitLabel: "Atualizar",
                    submitClass: "btn btn-warning",
                    skipUnchangedCheck: false,
                    onSubmit: async payload => {
                        await fetchJson(
                            buildUrl(`/bookings/${encodeURIComponent(bid)}`),
                            { method: "PUT", auth: true, body: payload },
                        )
                        getBookingById(mainContent, { bid })
                    },
                })

            const deleteStatus = div()
            const deleteSection =
                div(
                    { class: "border rounded p-3" },
                    div({ class: "mb-2 fw-semibold" }, "Remover Booking"),
                    button(
                        {
                            type: "button",
                            class: "btn btn-danger",
                            onclick: async () => {
                                if (!window.confirm("Tens a certeza que queres remover este booking?")) return
                                deleteStatus.replaceChildren(createAlert("A remover booking...", "secondary"))
                                try {
                                    await fetchJson(
                                        buildUrl(`/bookings/${encodeURIComponent(bid)}`),
                                        { method: "DELETE", auth: true, body: { id: bid } },
                                    )
                                    window.location.hash = "#bookings/mine"
                                } catch (error) {
                                    deleteStatus.replaceChildren(createAlert(error?.message || "Erro ao remover booking.", "danger"))
                                }
                            },
                        },
                        "Remover",
                    ),
                    deleteStatus,
                )

            replaceMain(
                mainContent,
                buildPage(
                    `Booking em ${currentHouse.title}`,
                    div(
                        { class: "border rounded p-3 mb-3" },
                        div({ class: "fw-semibold" }, currentHouse.title),
                        div({ class: "text-muted" }, `${booking.startDate} -> ${booking.endDate}`),
                        div({ class: "d-flex flex-wrap gap-3 mt-3" },
                            a({ href: `#houses/${encodeURIComponent(currentHouse.id)}` }, "Ver house"),
                            a(
                                {
                                    href: `#houses/${encodeURIComponent(currentHouse.id)}/bookings/dateStart=${encodeURIComponent(booking.startDate)}&dateEnd=${encodeURIComponent(booking.endDate)}`,
                                },
                                "Refazer reserva nesta house",
                            ),
                        ),
                    ),
                    updateForm,
                    deleteSection,
                ),
            )
        },
        "A carregar booking...",
    )
}

export { getBookingById, getBookingsByHouse, getCreateBookingView, getMyBookings }
