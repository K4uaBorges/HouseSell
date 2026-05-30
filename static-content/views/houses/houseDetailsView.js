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
    p,
    readSession,
    replaceMain,
    runAsync,
    todayIsoDate,
    tomorrowIsoDate,
} from "../../utis/index.js"
import { createHouseCard, createHouseCardGrid, createOwnerMap, houseBookingHash } from "./houseCards.js"
import { createLocationDropdowns } from "../viewUtils.js"
import { normalizeLocationsPayload, parseHousePayload, validateHouseFields } from "./housePayload.js"

function getHouseById(mainContent, params = {}) {
    runAsync(
        mainContent,
        async () => {
            const hid = params.hid
            const session = readSession()
            const startDate = todayIsoDate()
            const endDate = tomorrowIsoDate()
            const [house, locationsData, ownerData, suggestedData] = await Promise.all([
                fetchJson(buildUrl(`/houses/${encodeURIComponent(hid)}`)),
                fetchJson(buildUrl("/locations", { limit: 100 })),
                fetchJson(buildUrl("/users")),
                session?.token
                    ? fetchJson(buildUrl("/houses/available", { startDate, endDate }), { auth: true })
                    : fetchJson(buildUrl("/houses/available", { startDate, endDate })),
            ])
            const locations = normalizeLocationsPayload(locationsData)
            const ownerById = createOwnerMap(ownerData)
            const isMine = String(session?.id || "").trim() === String(house.uid || "").trim()
            const recommendedHouses =
                (Array.isArray(suggestedData?.houses) ? suggestedData.houses : Array.isArray(suggestedData) ? suggestedData : [])
                    .filter(candidate => candidate.id !== house.id)
                    .slice(0, 3)

            if (isMine) {
                let houseBookings = []
                try {
                    const bookingsData = await fetchJson(
                        buildUrl(`/houses/${encodeURIComponent(hid)}/bookings`, {
                            dateStart: startDate,
                            dateEnd: "2027-05-30",
                        }),
                        { auth: true },
                    )
                    houseBookings = Array.isArray(bookingsData?.bookings) ? bookingsData.bookings : []
                } catch {
                    houseBookings = []
                }

                const updateStatus = div()
                const updateGuard = createSubmitGuard()
                const updateSection = div({ class: "border rounded p-3 mb-3", hidden: true })
                const deleteStatus = div()
                const titleInput = input({
                    class: "form-control",
                    type: "text",
                    required: true,
                    value: house.title || ""
                })
                const {leafLocationInput: lidInput, selectorFields: locationSelector} =
                    createLocationDropdowns(locations, {
                        inputName: "lid",
                        initialLocationId: house.lid || "",
                    })
                const areaInput = input({
                    class: "form-control",
                    type: "number",
                    required: true,
                    min: "1",
                    value: String(house.areaSqMt || "")
                })
                const priceInput =
                    input({
                        class: "form-control",
                        type: "number",
                        required: true,
                        min: "0.01",
                        step: "0.01",
                        value: String(house.pricePerNight || ""),
                    })
                const descriptionInput = input({
                    class: "form-control",
                    type: "text",
                    required: true,
                    value: house.description || ""
                })

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
                                const initialPayload = {
                                    title: house.title || "",
                                    lid: String(house.lid || "").trim(),
                                    areaSqMt: Number(house.areaSqMt || 0),
                                    pricePerNight: Number(house.pricePerNight || 0),
                                    description: house.description || "",
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

                                updateStatus.replaceChildren(createAlert("A atualizar house...", "secondary"))
                                try {
                                    await fetchJson(
                                        buildUrl(`/houses/${encodeURIComponent(hid)}`),
                                        {method: "PUT", auth: true, body: payload},
                                    )
                                    getHouseById(mainContent, {hid})
                                } catch (error) {
                                    updateStatus.replaceChildren(createAlert(error?.message || "Erro ao atualizar house.", "danger"))
                                } finally {
                                    updateGuard.end()
                                }
                            },
                        },
                        div({class: "mb-2 fw-semibold"}, "Atualizar House"),
                        div(
                            {class: "row g-2"},
                            div({class: "col-md-4"}, label({class: "form-label"}, "title"), titleInput),
                            div({class: "col-md-2"}, label({class: "form-label"}, "areaSqMt"), areaInput),
                            div({class: "col-md-2"}, label({class: "form-label"}, "pricePerNight"), priceInput),
                            div({class: "col-md-10"}, label({class: "form-label"}, "description"), descriptionInput),
                            div(
                                {class: "col-md-2 d-grid"},
                                button({type: "submit", class: "btn btn-warning mt-md-4"}, "Atualizar"),
                            ),
                        ),
                        locationSelector,
                        updateStatus,
                    )
                updateSection.replaceChildren(updateForm)

                const houseCard = createHouseCard(house, {
                    ownerName: ownerById.get(String(house.uid || "")) || "Proprietário",
                    startDate,
                    endDate,
                    actions: [
                        button(
                            {
                                type: "button",
                                class: "btn btn-warning btn-sm",
                                onclick: () => {
                                    updateSection.hidden = !updateSection.hidden
                                },
                            },
                            "Editar",
                        ),
                        button(
                            {
                                type: "button",
                                class: "btn btn-danger btn-sm",
                                disabled: houseBookings.length > 0,
                                onclick: async () => {
                                    if (!window.confirm("Tens a certeza que queres remover esta house?")) return
                                    deleteStatus.replaceChildren(createAlert("A remover house...", "secondary"))
                                    try {
                                        await fetchJson(
                                            buildUrl(`/houses/${encodeURIComponent(hid)}`),
                                            {method: "DELETE", auth: true, body: {id: hid}},
                                        )
                                        window.location.hash = "#houses/mine"
                                    } catch (error) {
                                        deleteStatus.replaceChildren(createAlert(error?.message || "Erro ao remover house.", "danger"))
                                    }
                                },
                            },
                            "Deletar",
                        ),
                    ],
                })

                const bookingsSection =
                    div(
                        { class: "border rounded p-3 mb-3" },
                        div({ class: "fw-semibold mb-2" }, "Bookings reservados para esta house"),
                        houseBookings.length
                            ? div(
                                { class: "d-flex flex-column gap-2" },
                                ...houseBookings.map(booking =>
                                    div(
                                        { class: "border rounded p-2" },
                                        div({ class: "fw-semibold" }, `Booking ${booking.id}`),
                                        p({ class: "mb-0 text-muted" }, `${booking.startDate} -> ${booking.endDate}`),
                                    ),
                                ),
                            )
                            : createAlert("Esta house ainda não tem bookings.", "secondary"),
                    )
                replaceMain(
                    mainContent,
                    buildPage(
                        house.title || `House ${hid}`,
                        houseCard,
                        houseBookings.length
                            ? createAlert("Esta house não pode ser apagada enquanto tiver bookings.", "warning")
                            : null,
                        deleteStatus,
                        div(
                            { class: "border rounded p-3 mb-3" },
                            div({ class: "fw-semibold mb-2" }, "Descrição"),
                            p({ class: "mb-0" }, house.description || "Sem descrição."),
                        ),
                        updateSection,
                        bookingsSection,
                        div({ class: "fw-semibold mb-3" }, "Poderá gostar"),
                        createHouseCardGrid(
                            recommendedHouses,
                            {
                                ownerById,
                                startDate,
                                endDate,
                                emptyMessage: "Sem outras houses para sugerir de momento.",
                            },
                        ),
                    ),
                )
            }
            else {
                const houseCard = createHouseCard(house, {
                    ownerName: ownerById.get(String(house.uid || "")) || "Proprietário",
                    startDate,
                    endDate,
                    actions: [
                        a(
                            {
                                href: `#users/${encodeURIComponent(house.uid)}`,
                                class: "btn btn-outline-secondary btn-sm",
                            },
                            "Ver contacto",
                        ),
                        a(
                            {
                                href: houseBookingHash(house.id, startDate, endDate),
                                class: "btn btn-primary btn-sm",
                            },
                            "Alugar",
                        ),
                    ],
                })

                replaceMain(
                    mainContent,
                    buildPage(
                        house.title || `House ${hid}`,
                        houseCard,
                        div(
                            { class: "border rounded p-3 mb-3" },
                            div({ class: "fw-semibold mb-2" }, "Descrição"),
                            p({ class: "mb-0" }, house.description || "Sem descrição."),
                        ),
                        div({ class: "fw-semibold mb-3" }, "Poderá gostar"),
                        createHouseCardGrid(
                            recommendedHouses,
                            {
                                ownerById,
                                startDate,
                                endDate,
                                emptyMessage: "Sem outras houses para sugerir de momento.",
                            },
                        ),
                    ),
                )
            }
        },
        "A carregar house...",
    )
}

export { getHouseById }
