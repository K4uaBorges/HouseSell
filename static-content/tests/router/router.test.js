import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("router resolves dynamic params and inline query syntax", async () => {
    setupDom()
    const { default: router } = await importFresh("../../router/router.js")

    let handled = null
    router.addRoute("houses/:hid", (_mainContent, params, query) => {
        handled = { params, query }
    })

    window.location.hash = "#houses/abc-123/skip=10&limit=5"
    router.hashChangeHandler(document.createElement("div"))

    assert.deepEqual(handled, {
        params: { hid: "abc-123" },
        query: { skip: "10", limit: "5" },
    })

    teardownDom()
})

test("router supports classic querystring format", async () => {
    setupDom()
    const { default: router } = await importFresh("../../router/router.js")

    let querySeen = null
    router.addRoute("users", (_mainContent, _params, query) => {
        querySeen = query
    })

    window.location.hash = "#users?skip=5&limit=2"
    router.hashChangeHandler(document.createElement("div"))

    assert.deepEqual(querySeen, { skip: "5", limit: "2" })

    teardownDom()
})

test("router calls custom notFound handler when route does not exist", async () => {
    setupDom()
    const { default: router } = await importFresh("../../router/router.js")

    let notFoundCalled = false
    router.addDefaultNotFoundRouteHandler((_mainContent, params, query) => {
        notFoundCalled = true
        assert.deepEqual(params, {})
        assert.equal(query.skip, "1")
    })

    window.location.hash = "#unknown/skip=1&limit=2"
    router.hashChangeHandler(document.createElement("div"))

    assert.equal(notFoundCalled, true)

    teardownDom()
})
