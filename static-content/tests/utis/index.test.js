import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("utis index re-exports core helpers", async () => {
    setupDom()
    const module = await importFresh("../../utis/index.js")

    assert.equal(typeof module.buildUrl, "function")
    assert.equal(typeof module.fetchJson, "function")
    assert.equal(typeof module.runAsync, "function")
    assert.equal(typeof module.normalizePagingQuery, "function")
    assert.equal(typeof module.validateEmail, "function")

    teardownDom()
})
