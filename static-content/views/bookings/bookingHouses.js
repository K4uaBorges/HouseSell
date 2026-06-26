import { buildUrl, fetchJson } from "../../utis/index.js"


function dedupeHouses(houses) {
    const seen = new Set()
    return houses.filter(house => {
        if (!house?.hid || seen.has(house.hid)) return false
        seen.add(house.hid)
        return true
    })
}

async function fetchHousesByIds(houseIds) {
    const uniqueIds = [...new Set(houseIds.filter(Boolean))]
    const houses =
        await Promise.all(
            uniqueIds.map(async houseId => {
                try {
                    return await fetchJson(buildUrl(`/houses/${encodeURIComponent(houseId)}`))
                } catch {
                    return null
                }
            }),
        )
    return houses.filter(Boolean)
}

export { dedupeHouses, fetchHousesByIds }