import {
    buildPage,
    buildUrl,
    createLinkedOrEmpty,
    fetchJson,
    replaceMain,
    runAsync,
} from "../../utis/index.js"
import { createHouseForm } from "./houseForm.js"
import { normalizeHousesPayload, normalizeLocationsPayload } from "./housePayload.js"

function getMyHouses(mainContent) {
    runAsync(
        mainContent,
        async () => {
            const [housesData, locationsData] =
                await Promise.all([
                    fetchJson(buildUrl("/houses/mine"), { auth: true }),
                    fetchJson(buildUrl("/locations", { limit: 100 })),
                ])
            const houses = normalizeHousesPayload(housesData)
            const locations = normalizeLocationsPayload(locationsData)
            replaceMain(
                mainContent,
                buildPage(
                    "My Houses",
                    createHouseForm(locations),
                    createLinkedOrEmpty(
                        houses,
                        "Sem casas criadas.",
                        house => `#houses/${encodeURIComponent(house.hid)}`,
                        house => `${house.title} (${house.pricePerNight}/noite)`,
                    ),
                ),
            )
        },
        "A carregar my houses...",
    )
}

export { getMyHouses }
