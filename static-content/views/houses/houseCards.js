import { a, buildHash, createAlert, div, p } from "../../utis/index.js"

function normalizeUsersPayload(data) {
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.users)) return data.users
    return []
}

function createOwnerMap(users = []) {
    return new Map(
        normalizeUsersPayload(users)
            .filter(user => user?.id)
            .map(user => [String(user.id), String(user.name || "Proprietário")]),
    )
}

function houseBookingHash(houseId, startDate = "", endDate = "") {
    return buildHash(
        `houses/${encodeURIComponent(houseId)}/bookings`,
        {
            dateStart: startDate,
            dateEnd: endDate,
        },
    )
}

function createHouseCard(
    house,
    {
        ownerName = "Proprietário",
        startDate = "",
        endDate = "",
        actions = null,
    } = {},
) {
    const fallbackActions = [
        a(
            {
                href: `#houses/${encodeURIComponent(house.hid)}`,
                class: "btn btn-outline-secondary btn-sm",
            },
            "Ver detalhes",
        ),
        a(
            {
                href: houseBookingHash(house.hid, startDate, endDate),
                class: "btn btn-primary btn-sm",
            },
            "Alugar",
        ),
    ]

    return div(
        { class: "house-card card border-0 shadow-sm h-100" },
        div(
            { class: "row g-0 h-100" },
            div(
                { class: "col-md-4" },
                div(
                    {
                        class: "house-card-media bg-light border-end h-100 d-flex align-items-center justify-content-center text-muted fw-semibold",
                        style: { minHeight: "220px" },
                    },
                    "Sem fotos",
                ),
            ),
            div(
                { class: "col-md-8" },
                div(
                    { class: "house-card-body card-body d-flex flex-column h-100" },
                    div(
                        { class: "d-flex flex-wrap justify-content-between align-items-start gap-3 mb-3" },
                        div(
                            div({ class: "h5 mb-1" }, house.title || "House"),
                            p({ class: "text-muted mb-1" }, `Dono: ${ownerName}`),
                            p(
                                { class: "text-muted mb-0" },
                                `${house.locationName || "Localização desconhecida"} • ${house.pricePerNight}/noite`,
                            ),
                        ),
                        div(
                            { class: "d-flex flex-wrap gap-2" },
                            ...(Array.isArray(actions) ? actions : fallbackActions),
                        ),
                    ),
                    p({ class: "mb-3" }, house.description || "Sem descrição."),
                    div(
                        { class: "mt-auto small text-muted" },
                        `Área: ${house.areaSqMt} m²`,
                    ),
                ),
            ),
        ),
    )
}

function createHouseCardGrid(
    houses,
    {
        ownerById = new Map(),
        startDate = "",
        endDate = "",
        emptyMessage = "Sem houses.",
    } = {},
) {
    if (!houses.length) return createAlert(emptyMessage, "secondary")

    return div(
        { class: "house-card-grid row g-3" },
        ...houses.map(house =>
            div(
                { class: "col-12" },
                createHouseCard(house, {
                    ownerName: ownerById.get(String(house.uid || "")) || "Proprietário",
                    startDate,
                    endDate,
                }),
            ),
        ),
    )
}

export {
    createHouseCard,
    createHouseCardGrid,
    houseBookingHash,
    createOwnerMap,
    normalizeUsersPayload,
}
