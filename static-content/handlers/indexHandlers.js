import { getBookingById, getBookingsByHouse, getCreateBookingView, getMyBookings } from "../views/bookings/bookings.js"
import { getDashboard } from "../views/dashboard.js"
import { getHome } from "../views/home.js"
import {
    getHouseById,
    getHouseCacheStats,
    getHouses,
    getHousesAvailable,
    getHousePricePreview,
    getMyHouses
} from "../views/houses/houses.js"
import {
    getLocationById,
    getLocationChildrenAll,
    getLocationChildrenDirect,
    getLocationPath,
    getLocations,
} from "../views/locations/locations.js"
import { getMyAccount, getUserById, getUsers } from "./users.js"
import {getAvailableDays} from "../views/houses/availableDays.js";

const routeBindings = [
    { path: "home", handler: getDashboard },
    { path: "dashboard", handler: getDashboard },
    { path: "houses", handler: getHouses },
    { path: "houses/available", handler: getHousesAvailable },
    // Alias kept for backward compatibility with misspelled links.
    { path: "houses/avaliable", handler: getHousesAvailable },
    { path: "houses/preview", handler: getHousePricePreview },
    { path: "houses/cache", handler: getHouseCacheStats },
    { path: "houses/mine", handler: getMyHouses },
    { path: "houses/:hid", handler: getHouseById },
    { path: "houses/:hid/bookings", handler: getBookingsByHouse },
    { path: "houses/:hid/available-days", handler: getAvailableDays },
    { path: "locations", handler: getLocations },
    { path: "locations/:lid", handler: getLocationById },
    { path: "locations/:lid/childrenAll", handler: getLocationChildrenAll },
    { path: "locations/:lid/childrenDirect", handler: getLocationChildrenDirect },
    { path: "locations/:lid/path", handler: getLocationPath },
    { path: "users", handler: getUsers },
    { path: "account", handler: getMyAccount },
    { path: "users/:uid", handler: getUserById },
    { path: "bookings/new", handler: getCreateBookingView },
    { path: "bookings/mine", handler: getMyBookings },
    { path: "bookings/:bid", handler: getBookingById },
]

const handlers = {
    getDashboard,
    getHome,
    getUsers,
    getMyAccount,
    getUserById,
    getLocations,
    getLocationById,
    getLocationChildrenAll,
    getLocationChildrenDirect,
    getLocationPath,
    getHouses,
    getHouseById,
    getHousesAvailable,
    getAvailableDays,
    getHousePricePreview,
    getHouseCacheStats,
    getMyHouses,
    getBookingsByHouse,
    getCreateBookingView,
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
