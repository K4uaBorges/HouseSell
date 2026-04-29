import { getBookingById, getBookingsByHouse, getMyBookings } from "./bookings.js"
import { getHome } from "./home.js"
import { getHouseById, getHouseCacheStats, getHouses, getHousesAvailable, getHousePricePreview, getMyHouses } from "./houses.js"
import {
    getLocationById,
    getLocationChildrenAll,
    getLocationChildrenDirect,
    getLocationPath,
    getLocations,
} from "./locations.js"
import { getUserById, getUsers } from "./users.js"

const routeBindings = [
    { path: "home", handler: getHome },
    { path: "houses", handler: getHouses },
    { path: "houses/available", handler: getHousesAvailable },
    // Alias kept for backward compatibility with misspelled links.
    { path: "houses/avaliable", handler: getHousesAvailable },
    { path: "houses/preview", handler: getHousePricePreview },
    { path: "houses/cache", handler: getHouseCacheStats },
    { path: "houses/mine", handler: getMyHouses },
    { path: "houses/:hid", handler: getHouseById },
    { path: "houses/:hid/bookings", handler: getBookingsByHouse },
    { path: "locations", handler: getLocations },
    { path: "locations/:lid", handler: getLocationById },
    { path: "locations/:lid/childrenAll", handler: getLocationChildrenAll },
    { path: "locations/:lid/childrenDirect", handler: getLocationChildrenDirect },
    { path: "locations/:lid/path", handler: getLocationPath },
    { path: "users", handler: getUsers },
    { path: "users/:uid", handler: getUserById },
    { path: "bookings/mine", handler: getMyBookings },
    { path: "bookings/:bid", handler: getBookingById },
]

const handlers = {
    getHome,
    getUsers,
    getUserById,
    getLocations,
    getLocationById,
    getLocationChildrenAll,
    getLocationChildrenDirect,
    getLocationPath,
    getHouses,
    getHouseById,
    getHousesAvailable,
    getHousePricePreview,
    getHouseCacheStats,
    getMyHouses,
    getBookingsByHouse,
    getMyBookings,
    getBookingById,
}

function registerHandlerRoutes(router, fallbackHandler = getHome) {
    routeBindings.forEach(({ path, handler }) => {
        const resolved = typeof handler === "function" ? handler : fallbackHandler
        router.addRoute(path, resolved)
    })
}

export { registerHandlerRoutes, routeBindings }
export { handlers }
export default handlers
