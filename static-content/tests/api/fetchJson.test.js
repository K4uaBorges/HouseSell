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
