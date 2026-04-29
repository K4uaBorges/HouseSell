import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("createDateSearchForm updates hash using inline query format", async () => {
    setupDom()
    const { createDateSearchForm } = await importFresh("../../ui/pageComponents.js")

    const formNode = createDateSearchForm("houses/available", "2026-06-10", "2026-06-12")
    document.body.appendChild(formNode)

    const [startInput, endInput] = formNode.querySelectorAll("input[type='date']")
    startInput.value = "2026-07-01"
    endInput.value = "2026-07-05"

    formNode.dispatchEvent(new window.Event("submit", { bubbles: true, cancelable: true }))
    assert.equal(window.location.hash, "#houses/available/dateStart=2026-07-01&dateEnd=2026-07-05")

    teardownDom()
})

test("createLinkedOrEmpty returns alert when list is empty", async () => {
    setupDom()
    const { createLinkedOrEmpty } = await importFresh("../../ui/pageComponents.js")

    const node = createLinkedOrEmpty([], "Sem resultados", () => "#x", () => "X")
    assert.match(node.textContent, /Sem resultados/)

    teardownDom()
})

test("replaceMain swaps current content", async () => {
    setupDom()
    const { div, replaceMain } = await importFresh("../../ui/pageComponents.js")

    const mainContent = document.createElement("div")
    mainContent.textContent = "old"
    replaceMain(mainContent, div("new"))

    assert.equal(mainContent.textContent, "new")

    teardownDom()
})
