import { a, createAlert, div, li, ul } from "../../utis/index.js"

function createBookingsList(bookings, houseById = new Map()) {
    if (!bookings.length) return createAlert("Sem bookings.", "secondary")

    return ul(
        { class: "list-group" },
        bookings.map(booking =>
            li(
                { class: "list-group-item" },
                div({ class: "fw-semibold" }, houseById.get(booking.hid)?.title || "House"),
                div({ class: "text-muted mb-2" }, `${booking.startDate} -> ${booking.endDate}`),
                a({ href: `#bookings/${encodeURIComponent(booking.bid)}` }, "Ver detalhe"),
            ),
        ),
    )
}

export { createBookingsList }