import {
    buildHash,
    buildPage,
    buildUrl,
    button,
    createAlert,
    div,
    fetchJson,
    form,
    input,
    label,
    normalizePagingQuery,
    readToken,
    replaceMain,
    runAsync,
    todayIsoDate,
    tomorrowIsoDate,
} from "../../utis/index.js"
import { createLocationDropdowns } from "../viewUtils.js"
import { createHouseCardGrid, createOwnerMap } from "./houseCards.js"
import { normalizeHousesPayload, normalizeLocationsPayload } from "./housePayload.js"

function hasSearchQuery(query) {
    return ["search", "locationId", "startDate", "endDate", "limit", "skip"].some(key => {
        const value = String(query.get(key) || "").trim()
        return value.length > 0
    })
}

function createSearchPagingControls({ skip, limit, itemCount, query }) {
    const hasPrev = skip > 0
    const hasNext = itemCount >= limit
    const prevSkip = Math.max(0, skip - limit)
    const nextSkip = skip + limit

    return div(
        { class: "d-flex flex-wrap justify-content-between align-items-center gap-3 border rounded p-3 mt-3" },
        div({ class: "small text-muted" }, `Resultados nesta página: ${itemCount}`),
        div(
            { class: "d-flex gap-2" },
            hasPrev
                ? button(
                    {
                        type: "button",
                        class: "btn btn-outline-secondary",
                        onclick: () => {
                            window.location.hash = buildHash("houses", { ...query, skip: prevSkip, limit })
                        },
                    },
                    "Anterior",
                )
                : button({ type: "button", class: "btn btn-outline-secondary", disabled: true }, "Anterior"),
            hasNext
                ? button(
                    {
                        type: "button",
                        class: "btn btn-outline-secondary",
                        onclick: () => {
                            window.location.hash = buildHash("houses", { ...query, skip: nextSkip, limit })
                        },
                    },
                    "Seguinte",
                )
                : button({ type: "button", class: "btn btn-outline-secondary", disabled: true }, "Seguinte"),
        ),
    )
}

function getHouses(mainContent) {
    const hash = String(window.location.hash || "")
    const hashQuery = hash.includes("/") ? hash.split("/").slice(1).join("/") : ""
    const query = new URLSearchParams(hashQuery)

    const searched = hasSearchQuery(query)
    const searchTerm = String(query.get("search") || "").trim()
    const locationId = String(query.get("locationId") || "").trim()
    const startDate = String(query.get("startDate") || todayIsoDate()).trim()
    const endDate = String(query.get("endDate") || tomorrowIsoDate()).trim()
    const { skip, limit } = normalizePagingQuery({
        skip: query.get("skip"),
        limit: query.get("limit"),
    })

    runAsync(
        mainContent,
        async () => {
            const locationsData = await fetchJson(buildUrl("/locations", { limit: 100 }))
            const locations = normalizeLocationsPayload(locationsData)

            const searchInput = input({ class: "form-control", type: "search", value: searchTerm, placeholder: "Lugar / nome da casa" })
            const startDateInput = input({ class: "form-control", type: "date", value: startDate })
            const endDateInput = input({ class: "form-control", type: "date", value: endDate })
            const limitInput = input({ class: "form-control", type: "number", min: "1", max: "100", step: "1", value: String(limit) })

            const {
                leafLocationInput: lidInput,
                selectorFields: selectorRow,
            } = createLocationDropdowns(
                locations,
                {
                    initialLocationId: locationId,
                    maxType: "LOCALITY",
                    required: false,
                },
            )

            const searchForm =
                form(
                    {
                        class: "border rounded p-3",
                        onsubmit: event => {
                            event.preventDefault()
                            const nextSearch = searchInput.value.trim()
                            const nextLocation = String(lidInput.value || "").trim()
                            const nextStart = startDateInput.value.trim()
                            const nextEnd = endDateInput.value.trim()
                            const nextLimit = Math.min(100, Math.max(1, Number.parseInt(limitInput.value.trim(), 10) || 20))

                            window.location.hash =
                                buildHash(
                                    "houses",
                                    {
                                        search: nextSearch,
                                        locationId: nextLocation,
                                        startDate: nextStart,
                                        endDate: nextEnd,
                                        skip: 0,
                                        limit: nextLimit,
                                    },
                                )
                        },
                    },
                    div(
                        { class: "row g-2 align-items-end" },
                        div({ class: "col-lg-3 col-md-6" }, label({ class: "form-label" }, "Pesquisa"), searchInput),
                        div({ class: "col-lg-2 col-md-6" }, label({ class: "form-label" }, "Data Início"), startDateInput),
                        div({ class: "col-lg-2 col-md-6" }, label({ class: "form-label" }, "Data Fim"), endDateInput),
                        div({ class: "col-lg-2 col-md-6" }, label({ class: "form-label" }, "Casas por página"), limitInput),
                        div(
                            { class: "col-lg-2 col-md-12 d-grid" },
                            button({ type: "submit", class: "btn btn-primary" }, "Pesquisar"),
                        ),
                    ),
                    selectorRow,
                )

            if (!searched) {
                replaceMain(
                    mainContent,
                    buildPage(
                        "Houses",
                        div(
                            { class: "row justify-content-center mb-3" },
                            div({ class: "col-lg-10 col-xl-9" }, searchForm),
                        ),
                        createAlert("Define os filtros e pesquisa para ver resultados.", "secondary"),
                    ),
                )
                return
            }

            const token = String(readToken() || "").trim()
            const availableQuery = { startDate, endDate, search: searchTerm, locationId, skip, limit }
            const [availableData, usersData] = await Promise.all([
                fetchJson(buildUrl("/houses/available", availableQuery), token ? { auth: true } : {}),
                fetchJson(buildUrl("/users")),
            ])

            const houses = normalizeHousesPayload(availableData)
            const ownerById = createOwnerMap(usersData)

            let predictionPanel = createAlert("Sem dados suficientes para estimar preço neste filtro.", "secondary")
            if (houses.length > 0) {
                const averageArea = Math.round(houses.reduce((sum, house) => sum + Number(house.areaSqMt || 0), 0) / houses.length)
                const prediction = await fetchJson(buildUrl("/houses/preview", { areaSqMt: averageArea }))
                predictionPanel =
                    div(
                        { class: "border rounded p-3 mb-3" },
                        div({ class: "fw-semibold mb-2" }, "Machine Learning"),
                        div({ class: "text-muted" }, `Estimativa para ${averageArea} m² médios neste filtro.`),
                        div({ class: "h5 mb-0 mt-2" }, `${prediction.predictedPricePerNight}/noite`),
                    )
            }

            replaceMain(
                mainContent,
                buildPage(
                    "Houses",
                    div(
                        { class: "row justify-content-center mb-3" },
                        div({ class: "col-lg-10 col-xl-9" }, searchForm),
                    ),
                    predictionPanel,
                    createHouseCardGrid(
                        houses,
                        {
                            ownerById,
                            startDate,
                            endDate,
                            emptyMessage: "Sem casas disponíveis para este filtro.",
                        },
                    ),
                    createSearchPagingControls({
                        skip,
                        limit,
                        itemCount: houses.length,
                        query: {
                            search: searchTerm,
                            locationId,
                            startDate,
                            endDate,
                        },
                    }),
                ),
            )
        },
        "A carregar houses...",
    )
}

export { getHouses }
