import {
    button,
    div,
    form,
    input,
    label,
    option,
    select,
} from "../utis/index.js"

const LOCATION_FILTER_LEVELS = [
    { type: "COUNTRY", queryKey: "countryId", label: "País", emptyLabel: "Todos os países" },
    { type: "REGION", queryKey: "regionId", label: "Região", emptyLabel: "Todas as regiões" },
    { type: "DISTRICT", queryKey: "districtId", label: "Distrito", emptyLabel: "Todos os distritos" },
    { type: "MUNICIPALITY", queryKey: "municipalityId", label: "Município", emptyLabel: "Todos os municípios" },
    { type: "LOCALITY", queryKey: "localityId", label: "Localidade", emptyLabel: "Todas as localidades" },
]

function normalizeLocationsPayload(data) {
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.locations)) return data.locations
    return []
}

function normalizeType(rawType) {
    return String(rawType || "").trim().toUpperCase()
}

function createLocationsById(locations = []) {
    return new Map(
        locations
            .filter(location => location && typeof location === "object" && String(location.id || "").trim())
            .map(location => [String(location.id).trim(), location]),
    )
}

function sortLocationsByName(locations = []) {
    return [...locations].sort((left, right) =>
        String(left?.name || "").localeCompare(String(right?.name || ""), undefined, { sensitivity: "base" }))
}

function getDirectChildren(locations = [], type, parentId = "") {
    const normalizedType = normalizeType(type)
    const normalizedParentId = String(parentId || "").trim()

    return sortLocationsByName(
        locations.filter(location => {
            const locationType = normalizeType(location?.type)
            const locationParentId = String(location?.parentId || "").trim()

            if (locationType !== normalizedType) return false
            if (normalizedType === "COUNTRY") return !locationParentId
            return locationParentId === normalizedParentId
        }),
    )
}

function createPathByType(locationsById, locationId) {
    const pathByType = {}
    const visited = new Set()
    let currentId = String(locationId || "").trim()

    while (currentId && !visited.has(currentId)) {
        visited.add(currentId)
        const current = locationsById.get(currentId)
        if (!current) break

        const type = normalizeType(current.type)
        if (type && !pathByType[type]) {
            pathByType[type] = current
        }

        currentId = String(current.parentId || "").trim()
    }

    return pathByType
}

function getLocationSearchLabel(house, locationsById) {
    const pathByType = createPathByType(locationsById, house?.lid)
    const names =
        LOCATION_FILTER_LEVELS
            .map(level => String(pathByType[level.type]?.name || "").trim())
            .filter(Boolean)

    if (names.length) return names.join(" > ")

    const locationName = String(house?.locationName || "").trim()
    const locationType = String(house?.locationType || "").trim()
    if (!locationName) return "Localização desconhecida"
    return locationType ? `${locationName} (${locationType})` : locationName
}

function createLocationFilterForm({
    locations = [],
    initialQuery = {},
    startDateInput,
    endDateInput,
    maxPriceInput,
    onSubmit,
    submitLabel = "Pesquisar",
    className = "border rounded p-3 mb-3",
}) {
    const selectedIds =
        Object.fromEntries(
            LOCATION_FILTER_LEVELS.map(level => [level.type, ""]),
        )

    const controls = new Map()
    const normalizedLocations = normalizeLocationsPayload(locations)

    const applyInitialSelection = () => {
        let parentId = ""

        for (const level of LOCATION_FILTER_LEVELS) {
            const rawSelectedId = String(initialQuery[level.queryKey] || "").trim()
            const candidates = getDirectChildren(normalizedLocations, level.type, parentId)
            const selectedId =
                rawSelectedId && candidates.some(candidate => candidate.id === rawSelectedId)
                    ? rawSelectedId
                    : ""

            selectedIds[level.type] = selectedId
            parentId = selectedId
        }
    }

    const renderSelect = index => {
        const level = LOCATION_FILTER_LEVELS[index]
        const selectNode = controls.get(level.type)
        const parentLevel = LOCATION_FILTER_LEVELS[index - 1]
        const parentId = parentLevel ? selectedIds[parentLevel.type] : ""
        const parentLabel = parentLevel ? parentLevel.label.toLowerCase() : ""
        const candidates = getDirectChildren(normalizedLocations, level.type, parentId)
        const disabled = index > 0 && !parentId
        const placeholderLabel =
            disabled
                ? `Seleciona ${parentLabel} primeiro`
                : level.emptyLabel

        selectNode.replaceChildren(
            option({ value: "" }, placeholderLabel),
            ...candidates.map(candidate =>
                option(
                    {
                        value: candidate.id,
                        selected: candidate.id === selectedIds[level.type],
                    },
                    candidate.name,
                ),
            ),
        )
        selectNode.disabled = disabled
    }

    applyInitialSelection()

    const fields =
        LOCATION_FILTER_LEVELS.map((level, index) => {
            const selectNode = select({ class: "form-select" })
            controls.set(level.type, selectNode)

            selectNode.addEventListener("change", () => {
                selectedIds[level.type] = String(selectNode.value || "").trim()

                for (let descendantIndex = index + 1; descendantIndex < LOCATION_FILTER_LEVELS.length; descendantIndex += 1) {
                    selectedIds[LOCATION_FILTER_LEVELS[descendantIndex].type] = ""
                }

                for (let renderIndex = index + 1; renderIndex < LOCATION_FILTER_LEVELS.length; renderIndex += 1) {
                    renderSelect(renderIndex)
                }
            })

            return div(
                { class: "col-lg-2" },
                label({ class: "form-label" }, level.label),
                selectNode,
            )
        })

    for (let index = 0; index < LOCATION_FILTER_LEVELS.length; index += 1) {
        renderSelect(index)
    }

    const locationNameInput =
        input({
            class: "form-control",
            type: "search",
            value: String(initialQuery.locationName || "").trim(),
            placeholder: "Pesquisar localização",
        })

    return form(
        {
            class: className,
            onsubmit: event => {
                event.preventDefault()
                const query =
                    {
                        locationName: locationNameInput.value.trim(),
                        startDate: startDateInput.value.trim(),
                        endDate: endDateInput.value.trim(),
                        maxPrice: maxPriceInput.value.trim(),
                    }

                for (const level of LOCATION_FILTER_LEVELS) {
                    query[level.queryKey] = selectedIds[level.type]
                }

                onSubmit(query)
            },
        },
        div({ class: "fw-semibold mb-3" }, "Pesquisar houses"),
        div(
            { class: "row g-2 align-items-end" },
            ...fields,
            div({ class: "col-lg-2" }, label({ class: "form-label" }, "Pesquisa livre"), locationNameInput),
            div({ class: "col-lg-2" }, label({ class: "form-label" }, "Data Início"), startDateInput),
            div({ class: "col-lg-2" }, label({ class: "form-label" }, "Data Fim"), endDateInput),
            div({ class: "col-lg-1" }, label({ class: "form-label" }, "Preço Máximo"), maxPriceInput),
            div({ class: "col-lg-1 d-grid" }, button({ type: "submit", class: "btn btn-primary" }, submitLabel)),
        ),
    )
}

export {
    LOCATION_FILTER_LEVELS,
    createLocationFilterForm,
    createLocationsById,
    createPathByType,
    getLocationSearchLabel,
    normalizeLocationsPayload,
}
