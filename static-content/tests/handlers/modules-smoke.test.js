import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("handlers modules export callable entry points", async () => {
    setupDom()

    const bookings = await importFresh("../../handlers/bookings.js")
    const houses = await importFresh("../../handlers/houses.js")
    const locations = await importFresh("../../handlers/locations.js")
    const users = await importFresh("../../handlers/users.js")

    assert.equal(typeof bookings.getMyBookings, "function")
    assert.equal(typeof bookings.getBookingsByHouse, "function")
    assert.equal(typeof bookings.getBookingById, "function")
    assert.equal(typeof bookings.getCreateBookingView, "function")

    assert.equal(typeof houses.getHouses, "function")
    assert.equal(typeof houses.getHouseById, "function")
    assert.equal(typeof houses.getHousesAvailable, "function")
    assert.equal(typeof houses.getHouseAvailableDays, "function")

    assert.equal(typeof locations.getLocations, "function")
    assert.equal(typeof locations.getLocationById, "function")

    assert.equal(typeof users.getUsers, "function")
    assert.equal(typeof users.getUserById, "function")

    teardownDom()
})
