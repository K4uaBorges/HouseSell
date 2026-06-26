import {
    a,
    buildPage,
    buildUrl,
    createDateSearchForm,
    div,
    fetchJson,
    replaceMain,
    runAsync,
    todayIsoDate,
    tomorrowIsoDate,
} from "../../utis/index.js"
import { createBookingEditor } from "./bookingForm.js"
import { createBookingsList } from "./bookingLists.js"
import { normalizeBookingsPayload } from "./bookingPayload.js"

function getBookingsByHouse(mainContent, params = {}, query = {}) {
    const hid = params.hid
    const dateStart = query.dateStart || ""
    const dateEnd = query.dateEnd || ""

    runAsync(
        mainContent,
        async () => {
            const house = await fetchJson(buildUrl(`/houses/${encodeURIComponent(hid)}`))
            const searchForm =
                createDateSearchForm(
                    `houses/${encodeURIComponent(hid)}/bookings`,
                    dateStart || todayIsoDate(),
                    dateEnd || tomorrowIsoDate(),
                    "Pesquisar",
                    "dateStart",
                    "dateEnd",
                )

            const createForm =
                createBookingEditor({
                    fixedHouse: house,
                    initialStartDate: dateStart || todayIsoDate(),
                    initialEndDate: dateEnd || tomorrowIsoDate(),
                    titleText: "Criar Booking para esta House",
                    submitLabel: "Criar",
                    submitClass: "btn btn-primary",
                    onSubmit: async payload => {
                        const created = await fetchJson(buildUrl("/bookings"), { method: "POST", auth: true, body: payload })
                        window.location.hash = `#bookings/${encodeURIComponent(created.bid)}`
                    },
                })

            if (!dateStart || !dateEnd) {
                replaceMain(
                    mainContent,
                    buildPage(
                        `Bookings de ${house.title}`,
                        createForm,
                        div(
                            { class: "border rounded p-3 mb-3" },
                            div({ class: "fw-semibold mb-2" }, "Pesquisar bookings por intervalo"),
                            searchForm,
                        ),
                        a({ href: `#houses/${encodeURIComponent(hid)}` }, "Voltar à house"),
                    ),
                )
                return
            }

            const data = await fetchJson(buildUrl("/bookings", { hid, dateStart, dateEnd }), { auth: true })
            const bookings = normalizeBookingsPayload(data)
            const houseById = new Map([[house.hid, house]])

            replaceMain(
                mainContent,
                buildPage(
                    `Bookings de ${house.title}`,
                    createForm,
                    div(
                        { class: "border rounded p-3 mb-3" },
                        div({ class: "fw-semibold mb-2" }, "Pesquisar bookings por intervalo"),
                        searchForm,
                    ),
                    createBookingsList(bookings, houseById),
                ),
            )
        },
        "A carregar bookings da house...",
    )
}

export { getBookingsByHouse }