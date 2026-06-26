import {
    a,
    buildPage,
    buildUrl,
    createLinkedOrEmpty,
    div,
    fetchJson,
    li,
    replaceMain,
    runAsync,
    ul,
} from "../utis/index.js"
import {fetchHouseTitles} from "../handlers/users.js";

function getDashboard(mainContent) {
    runAsync(
        mainContent,
        async () => {
            const [housesData, bookingsData] =
                await Promise.all(
                    [
                        fetchJson(buildUrl("/houses/mine"), { auth: true }),
                        fetchJson(buildUrl("/bookings/mine"), { auth: true }),
                    ],
                )

            const houses = Array.isArray(housesData?.houses) ? housesData.houses : []
            const bookings = Array.isArray(bookingsData?.bookings) ? bookingsData.bookings : []
            const houseTitles = await fetchHouseTitles(bookings)

            replaceMain(
                mainContent,
                buildPage(
                    "My Dashboard",
                    div(
                        { class: "border rounded p-3 mb-3" },
                        div({ class: "fw-semibold mb-2" }, "Ações"),
                        ul(
                            { class: "list-group" },
                            li({ class: "list-group-item" }, a({ href: "#account" }, "Minha conta")),
                            li({ class: "list-group-item" }, a({ href: "#houses/mine" }, "Explorar as minhas houses")),
                            li({ class: "list-group-item" }, a({ href: "#bookings/mine" }, "Explorar os meus bookings")),
                            li({ class: "list-group-item" }, a({ href: "#houses" }, "Criar um novo booking")),
                        ),
                    ),
                    div(
                        { class: "border rounded p-3 mb-3" },
                        div({ class: "fw-semibold mb-2" }, "My Houses"),
                        createLinkedOrEmpty(
                            houses,
                            "Sem casas criadas.",
                            house => `#houses/${encodeURIComponent(house.hid)}`,
                            house => `${house.title} (${house.pricePerNight}/noite)`,
                        ),
                    ),
                    div(
                        { class: "border rounded p-3 mb-3" },
                        div({ class: "fw-semibold mb-2" }, "My Bookings"),
                        createLinkedOrEmpty(
                            bookings,
                            "Sem bookings reservados.",
                            booking => `#bookings/${encodeURIComponent(booking.bid)}`,
                            booking => `${houseTitles.get(booking.hid) || "House"} | ${booking.startDate} -> ${booking.endDate}`,
                        ),
                    ),
                ),
            )
        },
        "A carregar dashboard...",
    )
}

export { getDashboard }
