import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("normalizePagingQuery uses defaults and limits boundaries", async () => {
    const { normalizePagingQuery } = await importFresh("../../utis/paging.js")

    assert.deepEqual(normalizePagingQuery({}), { skip: 0, limit: 20 })
    assert.deepEqual(normalizePagingQuery({ skip: "-10", limit: "999" }), { skip: 0, limit: 100 })
    assert.deepEqual(normalizePagingQuery({ skip: "4", limit: "0" }), { skip: 4, limit: 1 })
})

test("buildHash renders inline query path format", async () => {
    const { buildHash } = await importFresh("../../utis/paging.js")
    const hash = buildHash("houses/available", { skip: 10, limit: 5 })
    assert.equal(hash, "#houses/available/skip=10&limit=5")
})

test("createPagingControls applies paging and updates hash", async () => {
    setupDom()
    const { createPagingControls } = await importFresh("../../utis/paging.js")

    const node = createPagingControls("houses", {
        skip: 0,
        limit: 20,
        itemCount: 20,
        extraQuery: { startDate: "2026-06-01" },
    })
    document.body.appendChild(node)

    const numberInputs = node.querySelectorAll("input[type='number']")
    numberInputs[0].value = "10"
    numberInputs[1].value = "5"
    const form = node.querySelector("form")
    form.dispatchEvent(new window.Event("submit", { bubbles: true, cancelable: true }))

    assert.equal(window.location.hash, "#houses/startDate=2026-06-01&skip=10&limit=5")

    teardownDom()
})
