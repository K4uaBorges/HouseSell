import { a, li, ul } from "../../utis/index.js"


function shuffleHouses(items) {
    const list = [...items]
    for (let index = list.length - 1; index > 0; index -= 1) {
        const randomIndex = Math.floor(Math.random() * (index + 1))
        const current = list[index]
        list[index] = list[randomIndex]
        list[randomIndex] = current
    }
    return list
}

function createHouseListWithBookings(houses) {
    return ul(
        { class: "list-group" },
        houses.map(house =>
            li(
                { class: "list-group-item" },
                a(
                    { href: `#houses/${encodeURIComponent(house.id)}` },
                    `${house.title} (${house.pricePerNight}/noite)`,
                ),
                " ",
                a(
                    {
                        href: `#houses/${encodeURIComponent(house.id)}/bookings`,
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
    shuffleHouses
}
