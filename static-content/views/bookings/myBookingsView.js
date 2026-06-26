import {
    a,
    buildPage,
    buildUrl,
    createAlert,
    div,
    fetchJson,
    h2,
    p,
    replaceMain,
    withError,
    withLoading,
} from "../../utis/index.js"
import { fetchHousesByIds } from "./bookingHouses.js"
import { createBookingsList } from "./bookingLists.js"
import { normalizeBookingsPayload } from "./bookingPayload.js"

function getMyBookings(mainContent) {
    withLoading(mainContent, "A carregar my bookings...")

    fetchJson(buildUrl("/bookings/mine"), { auth: true })
        .then(async data => {
            const bookings = normalizeBookingsPayload(data)
            const houses = await fetchHousesByIds(bookings.map(booking => booking.hid))
            const houseById = new Map(houses.map(house => [house.hid, house]))

            replaceMain(
                mainContent,
                buildPage(
                    "My Bookings",
                    div({ class: "mb-3" }, a({ href: "#bookings/new" }, "Criar nova booking")),
                    createBookingsList(bookings, houseById),
                ),
            )
        })
        .catch(error => {
            if (error?.status === 401) {
                const page =
                    div(
                        h2({ class: "h4 mb-3" }, "My Bookings"),
                        createAlert("Precisas de token", "warning"),
                        p(a({ href: "#home" }, "Ir para Home / configurar token")),
                    )

                replaceMain(mainContent, page)
                return
            }

            withError(mainContent, error)
        })
}

export { getMyBookings }