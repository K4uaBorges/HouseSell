import {
    a,
    buildHash,
    buildPage,
    buildUrl,
    createAlert,
    fetchJson,
    input,
    li,
    replaceMain,
    runAsync,
    todayIsoDate,
    tomorrowIsoDate,
    ul,
} from "../utis/index.js"
import {
    createLocationFilterForm,
    normalizeLocationsPayload,
} from "../handlers/locationFilter.js"

function getHome(mainContent) {
    runAsync(
        mainContent,
        async () => {
            const locationsData = await fetchJson(buildUrl("/locations", { limit: 20 }))
            const locations = normalizeLocationsPayload(locationsData)
            const startDateInput = input({ class: "form-control", type: "date", value: todayIsoDate() })
            const endDateInput = input({ class: "form-control", type: "date", value: tomorrowIsoDate() })
            const maxPriceInput = input({ class: "form-control", type: "number", min: "0.01", step: "0.01", placeholder: "Preço máximo" })

            const searchForm =
                createLocationFilterForm({
                    locations,
                    startDateInput,
                    endDateInput,
                    maxPriceInput,
                    className: "border rounded p-3 mb-4",
                    onSubmit: query => {
                        window.location.hash = buildHash("houses", query)
                    },
                })

            const quickLinks = [
                { href: "#houses", text: "Explorar Houses" },
                { href: "#houses/mine", text: "Criar House" },
                { href: "#bookings/new", text: "Criar Booking" },
            ]

            replaceMain(
                mainContent,
                buildPage(
                    "Home",
                    searchForm,
                    createAlert("Escolhe um país primeiro para desbloquear região, distrito, município e localidade.", "secondary"),
                    ul(
                        { class: "list-group" },
                        quickLinks.map(item =>
                            li(
                                { class: "list-group-item" },
                                a({ href: item.href }, item.text),
                            ),
                        ),
                    ),
                ),
            )
        },
        "A carregar home...",
    )
}

export { getHome }
