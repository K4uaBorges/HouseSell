import {
    a,
    buildHash,
    buildPage,
    buildUrl,
    button,
    createAlert,
    div,
    fetchJson,
    replaceMain,
    runAsync,
} from "../../utis/index.js"
import { dedupeHouses } from "./bookingHouses.js"
import { createBookingEditor } from "./bookingForm.js"
import { normalizeHousesPayload } from "./bookingPayload.js"

function getBookingById(mainContent, params = {}) {
    runAsync(
        mainContent,
        async () => {
            const bid = params.bid
            const booking = await fetchJson(buildUrl(`/bookings/${encodeURIComponent(bid)}`), { auth: true })
            const currentHouse = await fetchJson(buildUrl(`/houses/${encodeURIComponent(booking.hid)}`))
            const availableHouses =
                normalizeHousesPayload(
                    await fetchJson(
                        buildUrl("/houses/available", { startDate: booking.startDate, endDate: booking.endDate }),
                    ),
                )
            const houseOptions = dedupeHouses([currentHouse, ...availableHouses])

            const updateForm =
                createBookingEditor({
                    houseOptions,
                    selectedHouseId: booking.hid,
                    initialStartDate: booking.startDate,
                    initialEndDate: booking.endDate,
                    titleText: "Atualizar Booking",
                    submitLabel: "Atualizar",
                    submitClass: "btn btn-warning",
                    onSubmit: async payload => {
                        await fetchJson(
                            buildUrl(`/bookings/${encodeURIComponent(bid)}`),
                            { method: "PUT", auth: true, body: payload },
                        )
                        getBookingById(mainContent, { bid })
                    },
                })

            const deleteStatus = div()
            const deleteSection =
                div(
                    { class: "border rounded p-3" },
                    div({ class: "mb-2 fw-semibold" }, "Remover Booking"),
                    button(
                        {
                            type: "button",
                            class: "btn btn-danger",
                            onclick: async () => {
                                if (!window.confirm("Tens a certeza que queres remover este booking?")) return
                                deleteStatus.replaceChildren(createAlert("A remover booking...", "secondary"))
                                try {
                                    await fetchJson(
                                        buildUrl(`/bookings/${encodeURIComponent(bid)}`),
                                        { method: "DELETE", auth: true, body: { id: bid } },
                                    )
                                    window.location.hash = "#bookings/mine"
                                } catch (error) {
                                    deleteStatus.replaceChildren(createAlert(error?.message || "Erro ao remover booking.", "danger"))
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
                    `Booking em ${currentHouse.title}`,
                    div(
                        { class: "border rounded p-3 mb-3" },
                        div({ class: "fw-semibold" }, currentHouse.title),
                        div({ class: "text-muted" }, `${booking.startDate} -> ${booking.endDate}`),
                        div({ class: "d-flex flex-wrap gap-3 mt-3" },
                            a({ href: `#houses/${encodeURIComponent(currentHouse.hid)}` }, "Ver house"),
                            a(
                                {
                                    href:
                                        buildHash(
                                            `houses/${encodeURIComponent(currentHouse.hid)}/bookings`,
                                            { dateStart: booking.startDate, dateEnd: booking.endDate },
                                        ),
                                },
                                "Ver bookings da house",
                            ),
                        ),
                    ),
                    updateForm,
                    deleteSection,
                ),
            )
        },
        "A carregar booking...",
    )
}

export { getBookingById }