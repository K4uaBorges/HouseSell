import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("buildUrl prefixes /api and serializes query params", async () => {
    setupDom({ url: "https://www.housesell.com/" })
    const { buildUrl } = await importFresh("../../api/buildUrl.js")

    const url = buildUrl("/houses/available", { skip: 10, limit: 5, empty: "   " })
    assert.equal(url, "/api/houses/available?skip=10&limit=5")

    teardownDom()
})

test("buildUrl accepts path without leading slash", async () => {
    setupDom({ url: "https://www.housesell.com/" })
    const { buildUrl } = await importFresh("../../api/buildUrl.js")

    const url = buildUrl("users")
    assert.equal(url, "/api/users")

    teardownDom()
})

test("buildUrl ignores apiBase querystring and keeps current server base", async () => {
    setupDom({ url: "https://www.housesell.com/?apiBase=http://localhost:18080/api" })
    const { buildUrl } = await importFresh("../../api/buildUrl.js")

    const url = buildUrl("/users", { skip: 0, limit: 20 })
    assert.equal(url, "/api/users?skip=0&limit=20")
    assert.equal(localStorage.getItem("houses.api.base"), null)

    teardownDom()
})

test("buildUrl ignores apiBase persisted in localStorage", async () => {
    setupDom({ url: "https://www.housesell.com/" })
    localStorage.setItem("houses.api.base", "http://localhost:18080/api")
    const { buildUrl } = await importFresh("../../api/buildUrl.js")

    const url = buildUrl("/users")
    assert.equal(url, "/api/users")

    teardownDom()
})

test("buildUrl uses global api base when provided", async () => {
    setupDom({ url: "http://localhost:8080/" })
    window.__HOUSES_API_BASE__ = "http://localhost:8091/api"
    const { buildUrl } = await importFresh("../../api/buildUrl.js")

    const url = buildUrl("/users")
    assert.equal(url, "http://localhost:8091/api/users")

    teardownDom()
})
