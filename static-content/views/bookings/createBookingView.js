import {
    buildPage,
    buildUrl,
    createAlert,
    createDateSearchForm,
    fetchJson,
    replaceMain,
    runAsync,
    todayIsoDate,
    tomorrowIsoDate,
} from "../../utis/index.js"
import { createBookingEditor } from "./bookingForm.js"
import { normalizeHousesPayload } from "./bookingPayload.js"

function getCreateBookingView(mainContent, _params = {}, query = {}) {
    const startDate = String(query.dateStart || todayIsoDate()).trim()
    const endDate = String(query.dateEnd || tomorrowIsoDate()).trim()

    runAsync(
        mainContent,
        async () => {
            const houses = normalizeHousesPayload(await fetchJson(buildUrl("/houses/available", { startDate, endDate })))

            replaceMain(
                mainContent,
                buildPage(
                    "Criar Booking",
                    createDateSearchForm("bookings/new", startDate, endDate, "Pesquisar Houses", "dateStart", "dateEnd"),
                    createBookingEditor({
                        houseOptions: houses,
                        selectedHouseId: houses[0]?.hid || "",
                        initialStartDate: startDate,
                        initialEndDate: endDate,
                        titleText: "Nova Booking",
                        submitLabel: "Criar",
                        submitClass: "btn btn-primary",
                        onSubmit: async payload => {
                            const created = await fetchJson(buildUrl("/bookings"), { method: "POST", auth: true, body: payload })
                            window.location.hash = `#bookings/${encodeURIComponent(created.bid)}`
                        },
                    }),
                    houses.length
                        ? createAlert(`Encontradas ${houses.length} houses disponíveis neste período.`, "secondary")
                        : createAlert("Não existem houses disponíveis neste período.", "warning"),
                ),
            )
        },
        "A carregar houses disponíveis para booking...",
    )
}

export { getCreateBookingView }