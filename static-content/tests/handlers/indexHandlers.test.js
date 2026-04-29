import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("registerHandlerRoutes registers every route binding", async () => {
    setupDom()
    const { registerHandlerRoutes, routeBindings } = await importFresh("../../handlers/indexHandlers.js")

    const registered = []
    const fakeRouter = {
        addRoute(path, handler) {
            registered.push({ path, handler })
        },
    }

    const fallback = () => {}
    registerHandlerRoutes(fakeRouter, fallback)

    assert.equal(registered.length, routeBindings.length)
    assert.equal(registered.some(item => item.path === "houses/avaliable"), true)
    assert.equal(registered.every(item => typeof item.handler === "function"), true)

    teardownDom()
})

test("handlers module exports key handler functions", async () => {
    setupDom()
    const { handlers } = await importFresh("../../handlers/indexHandlers.js")

    assert.equal(typeof handlers.getUsers, "function")
    assert.equal(typeof handlers.getHouses, "function")
    assert.equal(typeof handlers.getLocations, "function")
    assert.equal(typeof handlers.getMyBookings, "function")

    teardownDom()
})
