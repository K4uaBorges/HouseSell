import {
    a,
    buildPage,
    buildUrl,
    button,
    createAlert,
    createJsonPre,
    div,
    fetchJson,
    form,
    input,
    label,
    replaceMain,
    runAsync,
} from "../../utis/index.js"

function normalizeAvailableDaysPayload(data) {
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.days)) return data.days
    if (Array.isArray(data?.availableDays)) return data.availableDays
    return []
}

function getAvailableDays(mainContent, params = {}, query = {}) {
    const hid = params.hid
    const now = new Date()
    const defaultYear = String(query.year || now.getFullYear())
    const defaultMonth = String(query.month || now.getMonth() + 1)

    const yearInput = input({
        class: "form-control",
        type: "number",
        required: true,
        value: defaultYear,
        placeholder: "Year",
    })

    const monthInput = input({
        class: "form-control",
        type: "number",
        min: "1",
        max: "12",
        required: true,
        value: defaultMonth,
        placeholder: "Month",
    })

    const searchForm =
        form(
            {
                class: "border rounded p-3 mb-3",
                onsubmit: event => {
                    event.preventDefault()
                    const year = String(yearInput.value || "").trim()
                    const month = String(monthInput.value || "").trim()
                    if (!year || !month) return
                    window.location.hash = `#houses/${encodeURIComponent(hid)}/available-days?year=${encodeURIComponent(year)}&month=${encodeURIComponent(month)}`
                },
            },
            div(
                { class: "row g-2 align-items-end" },
                div({ class: "col-lg-3 col-md-6" }, label({ class: "form-label" }, "Ano"), yearInput),
                div({ class: "col-lg-3 col-md-6" }, label({ class: "form-label" }, "Mês"), monthInput),
                div(
                    { class: "col-lg-2 col-md-12 d-grid" },
                    button({ type: "submit", class: "btn btn-primary" }, "Pesquisar"),
                ),
                div(
                    { class: "col-lg-4 col-md-12" },
                    a(
                        {
                            href: `#houses/${encodeURIComponent(hid)}`,
                            class: "btn btn-outline-secondary w-100",
                        },
                        "Voltar ao detalhe",
                    ),
                ),
            ),
        )

    runAsync(
        mainContent,
        async () => {
            const year = Number.parseInt(yearInput.value, 10)
            const month = Number.parseInt(monthInput.value, 10)

            const response = await fetchJson(
                buildUrl(`/houses/${encodeURIComponent(hid)}/available-days`, {
                    year,
                    month,
                }),
            )

            const availableDays = normalizeAvailableDaysPayload(response)
            const displayMonthName = `mês ${month}`

            replaceMain(
                mainContent,
                buildPage(
                    `House ${hid} - Available Days`,
                    createAlert(`Dias disponíveis para ${displayMonthName} de ${year}.`, "info"),
                    searchForm,
                    availableDays.length
                        ? div(
                            { class: "border rounded p-3 mb-3" },
                            div({ class: "fw-semibold mb-2" }, "Available Days"),
                            div(
                                { class: "d-flex flex-wrap gap-2" },
                                ...availableDays.map(day =>
                                    div(
                                        { class: "badge p-2", style: { backgroundColor: "#d1e7dd", color: "#0f5132", border: "1px solid #c7e0d6" } },
                                        String(day),
                                    ),
                                ),
                            ),
                        )
                        : createAlert("Sem dias disponíveis para este mês.", "secondary"),
                    createJsonPre(response),
                ),
            )
        },
        "A carregar available days...",
    )
}

export { getAvailableDays }