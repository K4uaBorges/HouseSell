import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("getHome renders expected quick links", async () => {
    setupDom()
    localStorage.setItem("houses.auth.token", "user-token")
    localStorage.setItem("houses.auth.session", JSON.stringify({
        token: "user-token",
        id: "u1",
        name: "User",
        email: "user@example.com",
        role: "USER",
    }))

    globalThis.fetch = async url => {
        const value = String(url)

        if (value.includes("/api/houses/location-options")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({
                    locations: [
                        { id: "country-1", name: "Portugal", type: "COUNTRY", parentId: null },
                        { id: "region-1", name: "Continente", type: "REGION", parentId: "country-1" },
                    ],
                }),
            }
        }

        throw new Error(`Unexpected fetch: ${value}`)
    }

    const { getHome } = await importFresh("../../handlers/home.js")

    const mainContent = document.createElement("div")
    getHome(mainContent)
    await new Promise(resolve => setTimeout(resolve, 0))

    assert.match(mainContent.textContent, /Pesquisar houses/)
    assert.match(mainContent.textContent, /Explorar Houses/)
    assert.match(mainContent.textContent, /Criar Booking/)
    const selects = [...mainContent.querySelectorAll("select")]
    assert.equal(selects[0]?.disabled, false)
    assert.equal(selects[1]?.disabled, true)

    teardownDom()
})
