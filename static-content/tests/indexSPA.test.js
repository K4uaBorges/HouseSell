import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "./helpers/testEnv.js"

test("indexSPA restores cached session when token remains valid", async () => {
    setupDom()
    localStorage.setItem("houses.auth.token", "cached-token")
    localStorage.setItem("houses.auth.session", JSON.stringify({
        token: "cached-token",
        id: "u1",
        name: "Alice",
        email: "alice@example.com",
        role: "USER",
    }))

    globalThis.fetch = async url => {
        const value = String(url)
        if (value.includes("/api/houses/location-options")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({ locations: [] }),
            }
        }

        if (value.includes("/api/houses/mine")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({ houses: [] }),
            }
        }

        if (value.includes("/api/bookings/mine")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({ bookings: [] }),
            }
        }

        throw new Error(`Unexpected fetch: ${value}`)
    }

    await importFresh("../../indexSPA.js")
    window.dispatchEvent(new window.Event("load"))
    await new Promise(resolve => setTimeout(resolve, 0))

    assert.equal(localStorage.getItem("houses.auth.token"), "cached-token")
    assert.match(document.body.textContent, /Sessão restaurada a partir da cache/i)

    teardownDom()
})

test("indexSPA logout clears cached token and reloads the page", async () => {
    setupDom()
    localStorage.setItem("houses.auth.token", "cached-token")
    localStorage.setItem("houses.auth.session", JSON.stringify({
        token: "cached-token",
        id: "u1",
        name: "Alice",
        email: "alice@example.com",
        role: "USER",
    }))

    globalThis.fetch = async url => {
        const value = String(url)
        if (value.includes("/api/houses/location-options")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({ locations: [] }),
            }
        }

        if (value.includes("/api/houses/mine")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({ houses: [] }),
            }
        }

        if (value.includes("/api/bookings/mine")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({ bookings: [] }),
            }
        }

        throw new Error(`Unexpected fetch: ${value}`)
    }

    let reloadCalled = false
    const indexSpaModule = await importFresh("../../indexSPA.js")
    indexSpaModule.setReloadPageForTests(() => {
        reloadCalled = true
    })

    window.dispatchEvent(new window.Event("load"))
    await new Promise(resolve => setTimeout(resolve, 0))

    document.querySelector("button.btn-outline-danger")?.click()

    assert.equal(localStorage.getItem("houses.auth.token"), null)
    assert.equal(localStorage.getItem("houses.auth.session"), null)
    assert.equal(reloadCalled, true)

    teardownDom()
})
