import {
    areComparableValuesEqual,
    buildPage,
    buildUrl,
    button,
    clearFieldsValidation,
    createSubmitGuard,
    createAlert,
    createJsonPre,
    createLinkList,
    createLinkedOrEmpty,
    div,
    fetchJson,
    form,
    input,
    label,
    option,
    replaceMain,
    runAsync,
    select,
    validateLocationType,
    validateRequired,
    validateUuid,
} from "../../utis/index.js"

const LOCATION_TYPES = ["COUNTRY", "REGION", "DISTRICT", "MUNICIPALITY", "LOCALITY"]

function locationNameHash(rawValue) {
    const normalized =
        String(rawValue || "")
            .trim()
            .toLowerCase()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")

    if (!normalized) return ""

    let hash = 0
    for (let index = 0; index < normalized.length; index += 1) {
        hash = ((hash * 31) + normalized.charCodeAt(index)) | 0
    }

    return `loc-${Math.abs(hash).toString(16).padStart(8, "0")}`
}

function normalizeLocationsPayload(data) {
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.locations)) return data.locations
    return []
}

function resolveParentIdFromKeyword(parentKeyword, locations) {
    const targetHash = locationNameHash(parentKeyword)
    if (!targetHash) return null

    const match =
        locations.find(location => locationNameHash(location?.name) === targetHash)
    return match?.id || null
}

function createLocationForm(mainContent) {
    const statusBox = div()
    const submitGuard = createSubmitGuard()
    const nameInput = input({ class: "form-control", type: "text", required: true, placeholder: "Nome" })
    const typeInput =
        select(
            { class: "form-select", required: true },
            option({ value: "" }, "Seleciona type"),
            ...LOCATION_TYPES.map(type => option({ value: type }, type)),
        )
    const parentWordInput = input({ class: "form-control", type: "text", placeholder: "Palavra do parent (ex: Portugal)" })

    const syncParentWordState = () => {
          const selectedType = String(typeInput.value || "").trim().toUpperCase()
          const isCountry = selectedType === "COUNTRY"

          parentWordInput.disabled = isCountry
          parentWordInput.required = !isCountry
          parentWordInput.value = isCountry ? "" : parentWordInput.value
          parentWordInput.placeholder = isCountry
              ? "COUNTRY não tem parent"
              : "Palavra do parent (ex: Portugal)"

          if (isCountry) {
              parentWordInput.classList.remove("is-invalid")
              parentHashHint.textContent = "Hash parent: -"
          } else {
              refreshParentHashHint()
          }
    }
    const parentHashHint = div({ class: "small text-muted mt-1" }, "Hash parent: -")

    const refreshParentHashHint = () => {
        const hash = locationNameHash(parentWordInput.value)
        parentHashHint.textContent = hash ? `Hash parent: ${hash}` : "Hash parent: -"
    }
    parentWordInput.addEventListener("input", refreshParentHashHint)
    typeInput.addEventListener("change", syncParentWordState)
    syncParentWordState()

    return form(
        {
            class: "border rounded p-3 mb-3",
            onsubmit: async event => {
                event.preventDefault()
                clearFieldsValidation([nameInput, typeInput, parentWordInput])
                const name = nameInput.value.trim()
                const type = typeInput.value.trim().toUpperCase()
                const parentWord = parentWordInput.value.trim()

                const nameOk = validateRequired(nameInput, "name")
                const typeOk = validateLocationType(typeInput, LOCATION_TYPES)
                if (!nameOk || !typeOk) {
                    statusBox.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                    return
                }
                const guard = submitGuard.begin()
                if (!guard.ok) {
                    statusBox.replaceChildren(createAlert(guard.message, "warning"))
                    return
                }

                statusBox.replaceChildren(createAlert("A criar location...", "secondary"))
                try {
                    let parentId = null
                    if (type !== "COUNTRY") {
                        if (!parentWord) {
                            parentWordInput.classList.add("is-invalid")
                            statusBox.replaceChildren(
                                createAlert("Para tipos diferentes de COUNTRY, indica a palavra da location parent.", "warning"),
                            )
                            return
                        }

                        const locationsData = await fetchJson(buildUrl("/locations"))
                        const locations = normalizeLocationsPayload(locationsData)
                        parentId = resolveParentIdFromKeyword(parentWord, locations)

                        if (!parentId) {
                            parentWordInput.classList.add("is-invalid")
                            statusBox.replaceChildren(
                                createAlert(
                                    `Nenhuma location encontrada para o hash ${locationNameHash(parentWord)}.`,
                                    "warning",
                                ),
                            )
                            return
                        }
                    }

                    const created = await fetchJson(
                        buildUrl("/locations"),
                        {
                            method: "POST",
                            auth: true,
                            body: {
                                name,
                                type,
                                parentId,
                            },
                        },
                    )
                    statusBox.replaceChildren(createAlert("Location criada.", "success"))
                    window.location.hash = `#locations/${encodeURIComponent(created.id)}`
                } catch (error) {
                    statusBox.replaceChildren(createAlert(error?.message || "Erro ao criar location.", "danger"))
                } finally {
                    submitGuard.end()
                }
            },
        },
        div({ class: "mb-2 fw-semibold" }, "Criar Location"),
        div({ class: "small text-muted mb-2" }, `Tipos válidos: ${LOCATION_TYPES.join(", ")}`),
        div(
            { class: "row g-2" },
            div({ class: "col-md-4" }, label({ class: "form-label" }, "name"), nameInput),
            div({ class: "col-md-3" }, label({ class: "form-label" }, "type"), typeInput),
            div(
                { class: "col-md-3" },
                label({ class: "form-label" }, "parentWord"),
                parentWordInput,
                parentHashHint,
            ),
            div(
                { class: "col-md-2 d-grid" },
                button({ type: "submit", class: "btn btn-primary mt-md-4" }, "Criar"),
            ),
        ),
        statusBox,
    )
}

function getLocations(mainContent) {
    runAsync(
        mainContent,
        async () => {
            const data = await fetchJson(buildUrl("/locations"))
            const locations = normalizeLocationsPayload(data)
            replaceMain(
                mainContent,
                buildPage(
                    "Locations",
                    createLocationForm(mainContent),
                    createLinkedOrEmpty(
                        locations,
                        "Sem locations.",
                        location => `#locations/${encodeURIComponent(location.id)}`,
                        location => `${location.name} (${location.type})`,
                    ),
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
            const updateGuard = createSubmitGuard()
            const nameInput =
                input({
                    class: "form-control",
                    type: "text",
                    required: true,
                    value: location.name || "",
                })
            const typeInput =
                select(
                    { class: "form-select", required: true },
                    option({ value: "" }, "Seleciona type"),
                    ...LOCATION_TYPES.map(type => option({ value: type, selected: type === (location.type || "") }, type)),
                )
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
                            const payload = {
                                name,
                                type,
                                parentId: parentIdRaw || null,
                            }
                            const initialPayload = {
                                name: location.name || "",
                                type: location.type || "",
                                parentId: location.parentId || null,
                            }
                            if (areComparableValuesEqual(payload, initialPayload)) {
                                updateStatus.replaceChildren(createAlert("Não existem alterações para guardar.", "secondary"))
                                return
                            }
                            const guard = updateGuard.begin()
                            if (!guard.ok) {
                                updateStatus.replaceChildren(createAlert(guard.message, "warning"))
                                return
                            }

                            updateStatus.replaceChildren(createAlert("A atualizar location...", "secondary"))
                            try {
                                await fetchJson(
                                    buildUrl(`/locations/${encodeURIComponent(lid)}`),
                                    {
                                        method: "PUT",
                                        auth: true,
                                        body: payload,
                                    },
                                )
                                getLocationById(mainContent, { lid })
                            } catch (error) {
                                updateStatus.replaceChildren(createAlert(error?.message || "Erro ao atualizar location.", "danger"))
                            } finally {
                                updateGuard.end()
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
