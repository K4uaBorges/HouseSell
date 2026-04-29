import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("getHome renders expected quick links", async () => {
    setupDom()
    const { getHome } = await importFresh("../../handlers/home.js")

    const mainContent = document.createElement("div")
    getHome(mainContent)

    assert.match(mainContent.textContent, /Ver Houses/)
    assert.match(mainContent.textContent, /Ver Locations/)
    assert.match(mainContent.textContent, /Ver Users/)

    teardownDom()
})
