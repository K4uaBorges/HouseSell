import {
    a,
    buildPage,
    buildUrl,
    button,
    clearFieldsValidation,
    createAlert,
    createDateSearchForm,
    createJsonPre,
    createLinkedOrEmpty,
    createPagingControls,
    div,
    fetchJson,
    form,
    h2,
    input,
    label,
    p,
    replaceMain,
    runAsync,
    normalizePagingQuery,
    validateDateRange,
    validateIsoDate,
    validateUuid,
    withError,
    withLoading,
} from "../utis/index.js"

function parseBookingPayload(hidInput, startDateInput, endDateInput) {
    const hid = hidInput.value.trim()
    const startDate = startDateInput.value.trim()
    const endDate = endDateInput.value.trim()
    if (!hid || !startDate || !endDate) return null
    return { hid, startDate, endDate }
}

function validateBookingFields(hidInput, startDateInput, endDateInput) {
    const hidOk = validateUuid(hidInput, "hid")
    const startOk = validateIsoDate(startDateInput, "startDate")
    const endOk = validateIsoDate(endDateInput, "endDate")
    const rangeOk = startOk && endOk ? validateDateRange(startDateInput, endDateInput) : false
    return hidOk && startOk && endOk && rangeOk
}

function createBookingForm(mainContent, { hidDefault = "" } = {}) {
    const statusBox = div()
    const hidInput = input({ class: "form-control", type: "text", required: true, value: hidDefault, placeholder: "House ID" })
    const startDateInput = input({ class: "form-control", type: "date", required: true })
    const endDateInput = input({ class: "form-control", type: "date", required: true })

    return form(
        {
            class: "border rounded p-3 mb-3",
            onsubmit: async event => {
                event.preventDefault()
                clearFieldsValidation([hidInput, startDateInput, endDateInput])
                const valid = validateBookingFields(hidInput, startDateInput, endDateInput)
                if (!valid) {
                    statusBox.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                    return
                }
                const payload = parseBookingPayload(hidInput, startDateInput, endDateInput)
                if (!payload) {
                    statusBox.replaceChildren(createAlert("Dados inválidos para criar booking.", "warning"))
                    return
                }

                statusBox.replaceChildren(createAlert("A criar booking...", "secondary"))
                try {
                    const created = await fetchJson(
                        buildUrl("/bookings"),
                        { method: "POST", auth: true, body: payload },
                    )
                    statusBox.replaceChildren(createAlert("Booking criada.", "success"))
                    window.location.hash = `#bookings/${encodeURIComponent(created.id)}`
                } catch (error) {
                    statusBox.replaceChildren(createAlert(error?.message || "Erro ao criar booking.", "danger"))
                }
            },
        },
        div({ class: "mb-2 fw-semibold" }, "Criar Booking"),
        div(
            { class: "row g-2" },
            div({ class: "col-md-4" }, label({ class: "form-label" }, "hid"), hidInput),
            div({ class: "col-md-3" }, label({ class: "form-label" }, "startDate"), startDateInput),
            div({ class: "col-md-3" }, label({ class: "form-label" }, "endDate"), endDateInput),
            div(
                { class: "col-md-2 d-grid" },
                button({ type: "submit", class: "btn btn-primary mt-md-4" }, "Criar"),
            ),
        ),
        statusBox,
    )
}

function getBookingsByHouse(mainContent, params = {}, query = {}) {
    const hid = params.hid
    const { skip, limit } = normalizePagingQuery(query)
    const dateStart = query.dateStart || ""
    const dateEnd = query.dateEnd || ""
    const dateSearchForm =
        createDateSearchForm(
            `#houses/${encodeURIComponent(hid)}/bookings`,
            dateStart,
            dateEnd,
            "Pesquisar",
            "dateStart",
            "dateEnd",
        )

    if (!dateStart || !dateEnd) {
        replaceMain(
            mainContent,
            buildPage(
                `Bookings da House ${hid}`,
                createBookingForm(mainContent, { hidDefault: hid }),
                dateSearchForm,
                createPagingControls(`houses/${encodeURIComponent(hid)}/bookings`, {
                    skip,
                    limit,
                    itemCount: 0,
                    extraQuery: { dateStart, dateEnd },
                }),
                createAlert("Define dateStart e dateEnd e carrega em Pesquisar.", "secondary"),
            ),
        )
        return
    }

    runAsync(
        mainContent,
        async () => {
            const data = await fetchJson(
                buildUrl("/bookings", { hid, dateStart, dateEnd, skip, limit }), { auth: true },
            )
            const bookings = Array.isArray(data?.bookings) ? data.bookings : []
            replaceMain(
                mainContent,
                buildPage(
                    `Bookings da House ${hid}`,
                    createBookingForm(mainContent, { hidDefault: hid }),
                    dateSearchForm,
                    createPagingControls(`houses/${encodeURIComponent(hid)}/bookings`, {
                        skip,
                        limit,
                        itemCount: bookings.length,
                        extraQuery: { dateStart, dateEnd },
                    }),
                    createLinkedOrEmpty(
                        bookings,
                        "Sem bookings para este período.",
                        booking => `#bookings/${encodeURIComponent(booking.id)}`,
                        booking => `${booking.id} | ${booking.startDate} -> ${booking.endDate}`,
                    ),
                    createPagingControls(`houses/${encodeURIComponent(hid)}/bookings`, {
                        skip,
                        limit,
                        itemCount: bookings.length,
                        extraQuery: { dateStart, dateEnd },
                    }),
                    createJsonPre(data),
                ),
            )
        },
        "A carregar bookings da house...",
    )
}

function getMyBookings(mainContent, _params = {}, query = {}) {
    const { skip, limit } = normalizePagingQuery(query)

    withLoading(mainContent, "A carregar my bookings...")

    fetchJson(buildUrl("/bookings/mine", { skip, limit }), { auth: true })
        .then(data => {
            const bookings = Array.isArray(data?.bookings) ? data.bookings : []
            replaceMain(
                mainContent,
                buildPage(
                    "My Bookings",
                    createBookingForm(mainContent),
                    createPagingControls("bookings/mine", { skip, limit, itemCount: bookings.length }),
                    createLinkedOrEmpty(
                        bookings,
                        "Sem bookings para o utilizador autenticado.",
                        booking => `#bookings/${encodeURIComponent(booking.id)}`,
                        booking => `${booking.id} | House ${booking.hid} | ${booking.startDate} -> ${booking.endDate}`,
                    ),
                    createPagingControls("bookings/mine", { skip, limit, itemCount: bookings.length }),
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

            const updateStatus = div()
            const hidInput = input({ class: "form-control", type: "text", required: true, value: booking.hid || "" })
            const startDateInput = input({ class: "form-control", type: "date", required: true, value: booking.startDate || "" })
            const endDateInput = input({ class: "form-control", type: "date", required: true, value: booking.endDate || "" })

            const updateForm =
                form(
                    {
                        class: "border rounded p-3 mb-3",
                        onsubmit: async event => {
                            event.preventDefault()
                            clearFieldsValidation([hidInput, startDateInput, endDateInput])
                            const valid = validateBookingFields(hidInput, startDateInput, endDateInput)
                            if (!valid) {
                                updateStatus.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                                return
                            }
                            const payload = parseBookingPayload(hidInput, startDateInput, endDateInput)
                            if (!payload) {
                                updateStatus.replaceChildren(createAlert("Dados inválidos para atualizar booking.", "warning"))
                                return
                            }

                            updateStatus.replaceChildren(createAlert("A atualizar booking...", "secondary"))
                            try {
                                await fetchJson(
                                    buildUrl(`/bookings/${encodeURIComponent(bid)}`),
                                    { method: "PUT", auth: true, body: payload },
                                )
                                getBookingById(mainContent, { bid })
                            } catch (error) {
                                updateStatus.replaceChildren(createAlert(error?.message || "Erro ao atualizar booking.", "danger"))
                            }
                        },
                    },
                    div({ class: "mb-2 fw-semibold" }, "Atualizar Booking"),
                    div(
                        { class: "row g-2" },
                        div({ class: "col-md-4" }, label({ class: "form-label" }, "hid"), hidInput),
                        div({ class: "col-md-3" }, label({ class: "form-label" }, "startDate"), startDateInput),
                        div({ class: "col-md-3" }, label({ class: "form-label" }, "endDate"), endDateInput),
                        div(
                            { class: "col-md-2 d-grid" },
                            button({ type: "submit", class: "btn btn-warning mt-md-4" }, "Atualizar"),
                        ),
                    ),
                    updateStatus,
                )

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
                    `Booking ${bid}`,
                    createJsonPre(booking),
                    updateForm,
                    deleteSection,
                ),
            )
        },
        "A carregar booking...",
    )
}

export { getBookingById, getBookingsByHouse, getMyBookings }
