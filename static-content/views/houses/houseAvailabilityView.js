import {
    buildPage,
    buildUrl,
    fetchJson,
    readToken,
    replaceMain,
    runAsync,
    todayIsoDate,
    tomorrowIsoDate,
} from "../../utis/index.js"
import { createHouseCardGrid, createOwnerMap } from "./houseCards.js"
import { normalizeHousesPayload } from "./housePayload.js"
import { shuffleHouses } from "../../indexSPA.js"

function getHousesAvailable(mainContent, _params = {}, query = {}) {
    const startDate = query.startDate || todayIsoDate()
    const endDate = query.endDate || tomorrowIsoDate()

    runAsync(
        mainContent,
        async () => {
            const token = String(readToken() || "").trim()
            const [data, usersData] = await Promise.all([
                fetchJson(
                    buildUrl("/houses/available", { startDate, endDate }),
                    token ? { auth: true } : {},
                ),
                fetchJson(buildUrl("/users")),
            ])
            const houses = normalizeHousesPayload(data)
            const randomHouses = shuffleHouses(houses)
            const ownerById = createOwnerMap(usersData)

            replaceMain(
                mainContent,
                buildPage(
                    "Houses Available",
                    createHouseCardGrid(
                        randomHouses,
                        {
                            ownerById,
                            startDate,
                            endDate,
                            emptyMessage: "Sem casas disponíveis para arrendar neste período.",
                        },
                    ),
                ),
            )
        },
        "A carregar houses disponíveis...",
    )
}

export { getHousesAvailable }
