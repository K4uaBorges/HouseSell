import {
    buildPage,
    buildUrl,
    button,
    clearFieldsValidation,
    createAlert,
    createJsonPre,
    createLinkList,
    createLinkedOrEmpty,
    createPagingControls,
    div,
    fetchJson,
    form,
    input,
    label,
    normalizePagingQuery,
    replaceMain,
    runAsync,
    validateLocationType,
    validateRequired,
    validateUuid,
} from "../utis/index.js"

const LOCATION_TYPES = ["COUNTRY", "REGION", "DISTRICT", "MUNICIPALITY", "LOCALITY"]

function createLocationForm(mainContent) {
    const statusBox = div()
    const nameInput = input({ class: "form-control", type: "text", required: true, placeholder: "Nome" })
    const typeInput = input({ class: "form-control", type: "text", required: true, placeholder: "Type (COUNTRY, ...)" })
    const parentIdInput = input({ class: "form-control", type: "text", placeholder: "parentId (opcional)" })

    return form(
        {
            class: "border rounded p-3 mb-3",
            onsubmit: async event => {
                event.preventDefault()
                clearFieldsValidation([nameInput, typeInput, parentIdInput])
                const name = nameInput.value.trim()
                const type = typeInput.value.trim().toUpperCase()
                const parentIdRaw = parentIdInput.value.trim()

                const nameOk = validateRequired(nameInput, "name")
                const typeOk = validateLocationType(typeInput, LOCATION_TYPES)
                const parentOk = validateUuid(parentIdInput, "parentId", { optional: true })
                if (!nameOk || !typeOk || !parentOk) {
                    statusBox.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                    return
                }

                statusBox.replaceChildren(createAlert("A criar location...", "secondary"))
                try {
                    const created = await fetchJson(
                        buildUrl("/locations"),
                        {
                            method: "POST",
                            auth: true,
                            body: {
                                name,
                                type,
                                parentId: parentIdRaw || null,
                            },
                        },
                    )
                    statusBox.replaceChildren(createAlert("Location criada.", "success"))
                    window.location.hash = `#locations/${encodeURIComponent(created.id)}`
                } catch (error) {
                    statusBox.replaceChildren(createAlert(error?.message || "Erro ao criar location.", "danger"))
                }
            },
        },
        div({ class: "mb-2 fw-semibold" }, "Criar Location"),
        div({ class: "small text-muted mb-2" }, `Tipos válidos: ${LOCATION_TYPES.join(", ")}`),
        div(
            { class: "row g-2" },
            div({ class: "col-md-4" }, label({ class: "form-label" }, "name"), nameInput),
            div({ class: "col-md-3" }, label({ class: "form-label" }, "type"), typeInput),
            div({ class: "col-md-3" }, label({ class: "form-label" }, "parentId"), parentIdInput),
            div(
                { class: "col-md-2 d-grid" },
                button({ type: "submit", class: "btn btn-primary mt-md-4" }, "Criar"),
            ),
        ),
        statusBox,
    )
}

function getLocations(mainContent, _params = {}, query = {}) {
    const { skip, limit } = normalizePagingQuery(query)

    runAsync(
        mainContent,
        async () => {
            const data = await fetchJson(buildUrl("/locations", { skip, limit }))
            const locations = Array.isArray(data) ? data : Array.isArray(data?.locations) ? data.locations : []
            replaceMain(
                mainContent,
                buildPage(
                    "Locations",
                    createLocationForm(mainContent),
                    createPagingControls("locations", { skip, limit, itemCount: locations.length }),
                    createLinkedOrEmpty(
                        locations,
                        "Sem locations.",
                        location => `#locations/${encodeURIComponent(location.id)}`,
                        location => `${location.name} (${location.type})`,
                    ),
                    createPagingControls("locations", { skip, limit, itemCount: locations.length }),
                ),
            )
        },
        "A carregar locations...",
    )
}

function getLocationById(mainContent, params = {}) {
    runAsync(
        mainContent,
        async () => {
            const lid = params.lid
            const location = await fetchJson(buildUrl(`/locations/${encodeURIComponent(lid)}`))

            const links = [
                { href: `#locations/${encodeURIComponent(lid)}/childrenAll`, text: "Children All" },
                { href: `#locations/${encodeURIComponent(lid)}/childrenDirect`, text: "Children Direct" },
                { href: `#locations/${encodeURIComponent(lid)}/path`, text: "Path" },
            ]

            const updateStatus = div()
            const nameInput =
                input({
                    class: "form-control",
                    type: "text",
                    required: true,
                    value: location.name || "",
                })
            const typeInput =
                input({
                    class: "form-control",
                    type: "text",
                    required: true,
                    value: location.type || "",
                })
            const parentIdInput =
                input({
                    class: "form-control",
                    type: "text",
                    value: location.parentId || "",
                })

            const updateForm =
                form(
                    {
                        class: "border rounded p-3 mb-3",
                        onsubmit: async event => {
                            event.preventDefault()
                            clearFieldsValidation([nameInput, typeInput, parentIdInput])
                            const name = nameInput.value.trim()
                            const type = typeInput.value.trim().toUpperCase()
                            const parentIdRaw = parentIdInput.value.trim()

                            const nameOk = validateRequired(nameInput, "name")
                            const typeOk = validateLocationType(typeInput, LOCATION_TYPES)
                            const parentOk = validateUuid(parentIdInput, "parentId", { optional: true })
                            if (!nameOk || !typeOk || !parentOk) {
                                updateStatus.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                                return
                            }

                            updateStatus.replaceChildren(createAlert("A atualizar location...", "secondary"))
                            try {
                                await fetchJson(
                                    buildUrl(`/locations/${encodeURIComponent(lid)}`),
                                    {
                                        method: "PUT",
                                        auth: true,
                                        body: {
                                            name,
                                            type,
                                            parentId: parentIdRaw || null,
                                        },
                                    },
                                )
                                getLocationById(mainContent, { lid })
                            } catch (error) {
                                updateStatus.replaceChildren(createAlert(error?.message || "Erro ao atualizar location.", "danger"))
                            }
                        },
                    },
                    div({ class: "mb-2 fw-semibold" }, "Atualizar Location"),
                    div({ class: "small text-muted mb-2" }, `Tipos válidos: ${LOCATION_TYPES.join(", ")}`),
                    div(
                        { class: "row g-2" },
                        div({ class: "col-md-4" }, label({ class: "form-label" }, "name"), nameInput),
                        div({ class: "col-md-3" }, label({ class: "form-label" }, "type"), typeInput),
                        div({ class: "col-md-3" }, label({ class: "form-label" }, "parentId"), parentIdInput),
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
                    div({ class: "mb-2 fw-semibold" }, "Remover Location"),
                    button(
                        {
                            type: "button",
                            class: "btn btn-danger",
                            onclick: async () => {
                                if (!window.confirm("Tens a certeza que queres remover esta location?")) return
                                deleteStatus.replaceChildren(createAlert("A remover location...", "secondary"))
                                try {
                                    await fetchJson(
                                        buildUrl(`/locations/${encodeURIComponent(lid)}`),
                                        { method: "DELETE", auth: true, body: { id: lid } },
                                    )
                                    window.location.hash = "#locations"
                                } catch (error) {
                                    deleteStatus.replaceChildren(createAlert(error?.message || "Erro ao remover location.", "danger"))
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
                    `Location ${lid}`,
                    createLinkList(links, item => item.href, item => item.text),
                    createJsonPre(location),
                    updateForm,
                    deleteSection,
                ),
            )
        },
        "A carregar location...",
    )
}

function getLocationChildrenAll(mainContent, params = {}) {
    getLocationSublist(mainContent, params, "childrenAll", "Children All")
}

function getLocationChildrenDirect(mainContent, params = {}) {
    getLocationSublist(mainContent, params, "childrenDirect", "Children Direct")
}

function getLocationPath(mainContent, params = {}) {
    runAsync(
        mainContent,
        async () => {
            const lid = params.lid
            const path = await fetchJson(buildUrl(`/locations/${encodeURIComponent(lid)}/path`))
            const items = Array.isArray(path) ? path : []
            replaceMain(
                mainContent,
                buildPage(
                    `Location ${lid} - Path`,
                    createLinkedOrEmpty(
                        items,
                        "Sem entradas no path.",
                        item => `#locations/${encodeURIComponent(item.id)}`,
                        item => `${item.name} (${item.type})`,
                    ),
                    createJsonPre(path),
                ),
            )
        },
        "A carregar path...",
    )
}

function getLocationSublist(mainContent, params, endpointSuffix, titleSuffix) {
    runAsync(
        mainContent,
        async () => {
            const lid = params.lid
            const data = await fetchJson(buildUrl(`/locations/${encodeURIComponent(lid)}/${endpointSuffix}`))
            const items = Array.isArray(data) ? data : []
            replaceMain(
                mainContent,
                buildPage(
                    `Location ${lid} - ${titleSuffix}`,
                    createLinkedOrEmpty(
                        items,
                        "Sem resultados.",
                        item => `#locations/${encodeURIComponent(item.id)}`,
                        item => `${item.name} (${item.type})`,
                    ),
                    createJsonPre(data),
                ),
            )
        },
        "A carregar locations...",
    )
}

export {
    getLocationById,
    getLocationChildrenAll,
    getLocationChildrenDirect,
    getLocationPath,
    getLocations,
}
