import {
    a,
    buildHash,
    buildPage,
    buildUrl,
    button,
    clearFieldsValidation,
    createAlert,
    createDateSearchForm,
    createJsonPre,
    createLinkList,
    createLinkedOrEmpty,
    createPagingControls,
    div,
    fetchJson,
    form,
    input,
    label,
    li,
    replaceMain,
    runAsync,
    todayIsoDate,
    tomorrowIsoDate,
    ul,
    normalizePagingQuery,
    validatePositiveInt,
    validatePositiveNumber,
    validateRequired,
    validateUuid,
} from "../utis/index.js"

function parseHousePayload(titleInput, lidInput, areaInput, priceInput, descriptionInput) {
    const title = titleInput.value.trim()
    const lid = lidInput.value.trim()
    const areaSqMt = Number.parseInt(areaInput.value.trim(), 10)
    const pricePerNight = Number.parseFloat(priceInput.value.trim())
    const description = descriptionInput.value.trim()

    if (!title || !lid || !description) return null
    if (!Number.isInteger(areaSqMt) || areaSqMt <= 0) return null
    if (!Number.isFinite(pricePerNight) || pricePerNight <= 0) return null

    return { title, lid, areaSqMt, pricePerNight, description }
}

function validateHouseFields(titleInput, lidInput, areaInput, priceInput, descriptionInput) {
    const titleOk = validateRequired(titleInput, "title")
    const lidOk = validateUuid(lidInput, "lid")
    const areaOk = validatePositiveInt(areaInput, "areaSqMt")
    const priceOk = validatePositiveNumber(priceInput, "pricePerNight")
    const descriptionOk = validateRequired(descriptionInput, "description")
    return titleOk && lidOk && areaOk && priceOk && descriptionOk
}

function createHouseForm(mainContent) {
    const statusBox = div()
    const titleInput = input({ class: "form-control", type: "text", required: true, placeholder: "Título" })
    const lidInput = input({ class: "form-control", type: "text", required: true, placeholder: "Location ID" })
    const areaInput = input({ class: "form-control", type: "number", required: true, min: "1", placeholder: "Area (m²)" })
    const priceInput = input({ class: "form-control", type: "number", required: true, min: "0.01", step: "0.01", placeholder: "Preço/noite" })
    const descriptionInput = input({ class: "form-control", type: "text", required: true, placeholder: "Descrição" })

    return form(
        {
            class: "border rounded p-3 mb-3",
            onsubmit: async event => {
                event.preventDefault()
                clearFieldsValidation([titleInput, lidInput, areaInput, priceInput, descriptionInput])
                const valid = validateHouseFields(titleInput, lidInput, areaInput, priceInput, descriptionInput)
                if (!valid) {
                    statusBox.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                    return
                }
                const payload = parseHousePayload(titleInput, lidInput, areaInput, priceInput, descriptionInput)
                if (!payload) {
                    statusBox.replaceChildren(createAlert("Dados inválidos para criar house.", "warning"))
                    return
                }

                statusBox.replaceChildren(createAlert("A criar house...", "secondary"))
                try {
                    const created = await fetchJson(
                        buildUrl("/houses"),
                        { method: "POST", auth: true, body: payload },
                    )
                    statusBox.replaceChildren(createAlert("House criada.", "success"))
                    window.location.hash = `#houses/${encodeURIComponent(created.id)}`
                } catch (error) {
                    statusBox.replaceChildren(createAlert(error?.message || "Erro ao criar house.", "danger"))
                }
            },
        },
        div({ class: "mb-2 fw-semibold" }, "Criar House"),
        div(
            { class: "row g-2" },
            div({ class: "col-md-4" }, label({ class: "form-label" }, "title"), titleInput),
            div({ class: "col-md-4" }, label({ class: "form-label" }, "lid"), lidInput),
            div({ class: "col-md-2" }, label({ class: "form-label" }, "areaSqMt"), areaInput),
            div({ class: "col-md-2" }, label({ class: "form-label" }, "pricePerNight"), priceInput),
            div({ class: "col-md-10" }, label({ class: "form-label" }, "description"), descriptionInput),
            div(
                { class: "col-md-2 d-grid" },
                button({ type: "submit", class: "btn btn-primary mt-md-4" }, "Criar"),
            ),
        ),
        statusBox,
    )
}

function createHouseListWithBookings(houses) {
    return ul(
        { class: "list-group" },
        houses.map(house =>
            li(
                { class: "list-group-item" },
                a(
                    { href: `#houses/${encodeURIComponent(house.id)}` },
                    `${house.title} (${house.pricePerNight}/noite)`,
                ),
                " ",
                a(
                    {
                        href: `#houses/${encodeURIComponent(house.id)}/bookings`,
                        class: "ms-3",
                    },
                    "Ver bookings",
                ),
            ),
        ),
    )
}

function getHouses(mainContent, _params = {}, query = {}) {
    const { skip, limit } = normalizePagingQuery(query)

    runAsync(
        mainContent,
        async () => {
            const data = await fetchJson(buildUrl("/houses", { skip, limit }))
            const houses = Array.isArray(data?.houses) ? data.houses : []
            replaceMain(
                mainContent,
                buildPage(
                    "Houses",
                    createHouseForm(mainContent),
                    createPagingControls("houses", { skip, limit, itemCount: houses.length }),
                    !houses.length ? createAlert("Sem houses.", "secondary") : createHouseListWithBookings(houses),
                    createPagingControls("houses", { skip, limit, itemCount: houses.length }),
                ),
            )
        },
        "A carregar houses...",
    )
}

function getHouseById(mainContent, params = {}) {
    runAsync(
        mainContent,
        async () => {
            const hid = params.hid
            const house = await fetchJson(buildUrl(`/houses/${encodeURIComponent(hid)}`))
            const preview = await fetchJson(
                buildUrl("/houses/preview", { areaSqMt: house.areaSqMt }),
            )

            // Trigger one extra read of the same house to showcase cache usage.
            await fetchJson(buildUrl(`/houses/${encodeURIComponent(hid)}`))
            const cacheStats = await fetchJson(buildUrl("/houses/cache/stats"))

            const updateStatus = div()
            const titleInput = input({ class: "form-control", type: "text", required: true, value: house.title || "" })
            const lidInput = input({ class: "form-control", type: "text", required: true, value: house.lid || "" })
            const areaInput = input({ class: "form-control", type: "number", required: true, min: "1", value: String(house.areaSqMt || "") })
            const priceInput =
                input({
                    class: "form-control",
                    type: "number",
                    required: true,
                    min: "0.01",
                    step: "0.01",
                    value: String(house.pricePerNight || ""),
                })
            const descriptionInput = input({ class: "form-control", type: "text", required: true, value: house.description || "" })

            const updateForm =
                form(
                    {
                        class: "border rounded p-3 mb-3",
                        onsubmit: async event => {
                            event.preventDefault()
                            clearFieldsValidation([titleInput, lidInput, areaInput, priceInput, descriptionInput])
                            const valid = validateHouseFields(titleInput, lidInput, areaInput, priceInput, descriptionInput)
                            if (!valid) {
                                updateStatus.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                                return
                            }
                            const payload = parseHousePayload(titleInput, lidInput, areaInput, priceInput, descriptionInput)
                            if (!payload) {
                                updateStatus.replaceChildren(createAlert("Dados inválidos para atualizar house.", "warning"))
                                return
                            }

                            updateStatus.replaceChildren(createAlert("A atualizar house...", "secondary"))
                            try {
                                await fetchJson(
                                    buildUrl(`/houses/${encodeURIComponent(hid)}`),
                                    { method: "PUT", auth: true, body: payload },
                                )
                                getHouseById(mainContent, { hid })
                            } catch (error) {
                                updateStatus.replaceChildren(createAlert(error?.message || "Erro ao atualizar house.", "danger"))
                            }
                        },
                    },
                    div({ class: "mb-2 fw-semibold" }, "Atualizar House"),
                    div(
                        { class: "row g-2" },
                        div({ class: "col-md-4" }, label({ class: "form-label" }, "title"), titleInput),
                        div({ class: "col-md-4" }, label({ class: "form-label" }, "lid"), lidInput),
                        div({ class: "col-md-2" }, label({ class: "form-label" }, "areaSqMt"), areaInput),
                        div({ class: "col-md-2" }, label({ class: "form-label" }, "pricePerNight"), priceInput),
                        div({ class: "col-md-10" }, label({ class: "form-label" }, "description"), descriptionInput),
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
                    { class: "border rounded p-3 mb-3" },
                    div({ class: "mb-2 fw-semibold" }, "Remover House"),
                    button(
                        {
                            type: "button",
                            class: "btn btn-danger",
                            onclick: async () => {
                                if (!window.confirm("Tens a certeza que queres remover esta house?")) return
                                deleteStatus.replaceChildren(createAlert("A remover house...", "secondary"))
                                try {
                                    await fetchJson(
                                        buildUrl(`/houses/${encodeURIComponent(hid)}`),
                                        { method: "DELETE", auth: true, body: { id: hid } },
                                    )
                                    window.location.hash = "#houses"
                                } catch (error) {
                                    deleteStatus.replaceChildren(createAlert(error?.message || "Erro ao remover house.", "danger"))
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
                    `House ${hid}`,
                    createAlert(
                        `Linear preview (${house.areaSqMt} m²): ${preview.predictedPricePerNight}/noite`,
                        "info",
                    ),
                    createAlert(
                        `Cache hits=${cacheStats.hits} misses=${cacheStats.misses} size=${cacheStats.size}/${cacheStats.limit}`,
                        "secondary",
                    ),
                    createJsonPre(house),
                    createJsonPre(preview),
                    updateForm,
                    deleteSection,
                ),
            )
        },
        "A carregar house...",
    )
}

function getHousesAvailable(mainContent, _params = {}, query = {}) {
    const { skip, limit } = normalizePagingQuery(query)
    const startDate = query.startDate || todayIsoDate()
    const endDate = query.endDate || tomorrowIsoDate()

    runAsync(
        mainContent,
        async () => {
            const data = await fetchJson(
                buildUrl("/houses/available", { startDate, endDate, skip, limit }),
            )
            const houses = Array.isArray(data?.houses) ? data.houses : []
            replaceMain(
                mainContent,
                buildPage(
                    "Houses Available",
                    createDateSearchForm(
                        "#houses/available",
                        startDate,
                        endDate,
                        "Pesquisar",
                        "startDate",
                        "endDate",
                    ),
                    createPagingControls("houses/available", {
                        skip,
                        limit,
                        itemCount: houses.length,
                        extraQuery: { startDate, endDate },
                    }),
                    createLinkedOrEmpty(
                        houses,
                        "Sem houses disponíveis para o período.",
                        house => `#houses/${encodeURIComponent(house.id)}`,
                        house => `${house.title} (${house.pricePerNight}/noite)`,
                    ),
                    createPagingControls("houses/available", {
                        skip,
                        limit,
                        itemCount: houses.length,
                        extraQuery: { startDate, endDate },
                    }),
                    createJsonPre(data),
                ),
            )
        },
        "A carregar houses disponíveis...",
    )
}

function getMyHouses(mainContent, _params = {}, query = {}) {
    const { skip, limit } = normalizePagingQuery(query)

    runAsync(
        mainContent,
        async () => {
            const data = await fetchJson(buildUrl("/houses/mine", { skip, limit }), { auth: true })
            const houses = Array.isArray(data?.houses) ? data.houses : []
            replaceMain(
                mainContent,
                buildPage(
                    "My Houses",
                    createPagingControls("houses/mine", { skip, limit, itemCount: houses.length }),
                    createLinkedOrEmpty(
                        houses,
                        "Sem houses do utilizador autenticado.",
                        house => `#houses/${encodeURIComponent(house.id)}`,
                        house => `${house.title} (${house.pricePerNight}/noite)`,
                    ),
                    createPagingControls("houses/mine", { skip, limit, itemCount: houses.length }),
                ),
            )
        },
        "A carregar my houses...",
    )
}

function getHousePricePreview(mainContent, _params = {}, query = {}) {
    const parsedArea = Number.parseInt(String(query.areaSqMt ?? "110").trim(), 10)
    const areaSqMt = Number.isInteger(parsedArea) && parsedArea > 0 ? parsedArea : 110
    const suggestedAreas = [40, 60, 80, 100, 120, 160, 220]

    runAsync(
        mainContent,
        async () => {
            const preview = await fetchJson(buildUrl("/houses/preview", { areaSqMt }))
            const links = suggestedAreas.map(area => ({
                href: buildHash("houses/preview", { areaSqMt: area }),
                text: `${area} m²`,
            }))

            replaceMain(
                mainContent,
                buildPage(
                    "Linear Preview",
                    createLinkList(links, item => item.href, item => item.text),
                    createJsonPre(preview),
                ),
            )
        },
        "A calcular previsão...",
    )
}

function getHouseCacheStats(mainContent) {
    runAsync(
        mainContent,
        async () => {
            const stats = await fetchJson(buildUrl("/houses/cache/stats"))
            replaceMain(
                mainContent,
                buildPage(
                    "House Cache Stats",
                    createAlert("Abre o detalhe da mesma house várias vezes para aumentar cache hits.", "secondary"),
                    createJsonPre(stats),
                ),
            )
        },
        "A carregar cache stats...",
    )
}

export {
    getHouseById,
    getHouseCacheStats,
    getHouses,
    getHousesAvailable,
    getHousePricePreview,
    getMyHouses,
}
