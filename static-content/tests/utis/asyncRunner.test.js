import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("withLoading renders loading label", async () => {
    setupDom()
    const { withLoading } = await importFresh("../../utis/asyncRunner.js")

    const mainContent = document.createElement("div")
    withLoading(mainContent, "A processar...")
    assert.match(mainContent.textContent, /A processar/)

    teardownDom()
})

test("runAsync catches failures and renders error block", async () => {
    setupDom()
    const { runAsync } = await importFresh("../../utis/asyncRunner.js")

    const mainContent = document.createElement("div")
    runAsync(mainContent, async () => {
        throw { status: 500, message: "boom" }
    })

    await new Promise(resolve => setTimeout(resolve, 0))
    assert.match(mainContent.textContent, /Erro 500: boom/)

    teardownDom()
})
