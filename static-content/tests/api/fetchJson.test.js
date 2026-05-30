import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("fetchJson sends authorization and stores token from successful response", async () => {
    setupDom()
    localStorage.setItem("houses.auth.token", "session-token")

    let fetchInput = null
    globalThis.fetch = async (url, options) => {
        fetchInput = { url, options }
        return {
            ok: true,
            status: 200,
            text: async () => JSON.stringify({ id: "u1", token: "new-token" }),
        }
    }

    const { fetchJson } = await importFresh("../../api/fetchJson.js")
    const payload = await fetchJson("/api/users", { auth: true, method: "POST", body: { name: "Alice" } })

    assert.equal(payload.id, "u1")
    assert.equal(fetchInput.options.method, "POST")
    assert.equal(fetchInput.options.headers.Authorization, "Bearer session-token")
    assert.equal(fetchInput.options.headers["Content-Type"], "application/json")
    assert.equal(fetchInput.options.body, JSON.stringify({ name: "Alice" }))
    assert.equal(localStorage.getItem("houses.auth.token"), "new-token")

    teardownDom()
})

test("fetchJson throws structured api error for non-2xx responses", async () => {
    setupDom()

    globalThis.fetch = async () => ({
        ok: false,
        status: 400,
        text: async () => JSON.stringify({ error: "validation failed" }),
    })

    const { fetchJson } = await importFresh("../../api/fetchJson.js")

    await assert.rejects(
        () => fetchJson("/api/users"),
        error => {
            assert.equal(error.status, 400)
            assert.equal(error.message, "validation failed")
            assert.deepEqual(error.payload, { error: "validation failed" })
            return true
        },
    )

    teardownDom()
})

test("fetchJson returns raw text when body is not JSON", async () => {
    setupDom()

    globalThis.fetch = async () => ({
        ok: true,
        status: 200,
        text: async () => "plain-text",
    })

    const { fetchJson } = await importFresh("../../api/fetchJson.js")
    const payload = await fetchJson("/api/plain")
    assert.equal(payload, "plain-text")

    teardownDom()
})

test("fetchJson does a single request and does not retry on 404", async () => {
    setupDom({ url: "http://localhost:8080/" })

    const attempts = []
    globalThis.fetch = async (url) => {
        attempts.push(String(url))
        return {
            ok: false,
            status: 404,
            text: async () => "<!doctype html><title>404 Not Found</title>",
        }
    }

    const { fetchJson } = await importFresh("../../api/fetchJson.js")

    await assert.rejects(
        () => fetchJson("/api/locations"),
        error => {
            assert.equal(error.status, 404)
            assert.equal(error.message, "HTTP 404 (/api/locations)")
            return true
        },
    )

    assert.deepEqual(attempts, ["/api/locations"])

    teardownDom()
})

test("fetchJson wraps network failure for a single current-server url", async () => {
    setupDom({ url: "http://localhost:8080/" })

    const attempts = []
    globalThis.fetch = async (url) => {
        attempts.push(String(url))
        throw new TypeError("NetworkError when attempting to fetch resource.")
    }

    const { fetchJson } = await importFresh("../../api/fetchJson.js")

    await assert.rejects(
        () => fetchJson("http://localhost:8091/api/locations"),
        error => {
            assert.equal(error.status, 503)
            assert.equal(error.message, "API indisponivel ou servidor desligado. URL: http://localhost:8091/api/locations")
            return true
        },
    )

    assert.deepEqual(attempts, ["http://localhost:8091/api/locations"])

    teardownDom()
})

test("fetchJson does not persist api base when an absolute local api url succeeds", async () => {
    setupDom({ url: "http://localhost:8080/" })

    globalThis.fetch = async () => ({
        ok: true,
        status: 200,
        text: async () => JSON.stringify({ locations: [] }),
    })

    const { fetchJson } = await importFresh("../../api/fetchJson.js")
    const payload = await fetchJson("http://localhost:8091/api/locations")

    assert.deepEqual(payload, { locations: [] })
    assert.equal(localStorage.getItem("houses.api.base"), null)

    teardownDom()
})
