import { a, li, ul } from "../../utis/index.js"

function createHouseListWithBookings(houses) {
    return ul(
        { class: "list-group" },
        houses.map(house =>
            li(
                { class: "list-group-item" },
                a(
                    { href: `#houses/${encodeURIComponent(house.hid)}` },
                    `${house.title} (${house.pricePerNight}/noite)`,
                ),
                " ",
                a(
                    {
                        href: `#houses/${encodeURIComponent(house.hid)}/bookings`,
                        class: "ms-3",
                    },
                    "Ver bookings",
                ),
            ),
        ),
    )
}

export {
    createHouseListWithBookings,
}
