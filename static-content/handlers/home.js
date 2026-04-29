import { a, div, h2, li, p, replaceMain, ul } from "../utis/index.js"

function getHome(mainContent) {
    const quickLinks = [
        { href: "#houses", text: "Ver Houses" },
        { href: "#locations", text: "Ver Locations" },
        { href: "#users", text: "Ver Users" },
        { href: "#houses/available", text: "Ver Houses Available" },
    ]

    const page =
        div(
            h2({ class: "h4 mb-3" }, "Home"),
            p("Navega pelos links para consultar users, locations, houses e bookings da API."),
            ul(
                { class: "list-group" },
                quickLinks.map(item =>
                    li(
                        { class: "list-group-item" },
                        a({ href: item.href }, item.text),
                    ),
                ),
            ),
        )

    replaceMain(mainContent, page)
}

export { getHome }
