import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("writeToken and readToken persist token in localStorage", async () => {
    setupDom()
    const { readToken, writeToken } = await importFresh("../../token/tokenStorage.js")

    writeToken("token-1")
    assert.equal(readToken(), "token-1")

    teardownDom()
})

test("removeToken clears token and emits auth token changed event", async () => {
    setupDom()
    const { AUTH_TOKEN_CHANGED_EVENT, removeToken, writeToken } = await importFresh("../../token/tokenStorage.js")

    let eventPayload = null
    window.addEventListener(AUTH_TOKEN_CHANGED_EVENT, event => {
        eventPayload = event.detail?.token
    })

    writeToken("token-2")
    removeToken()

    assert.equal(localStorage.getItem("houses.auth.token"), null)
    assert.equal(eventPayload, "")

    teardownDom()
})

test("syncTokenFromApiPayload writes token only when present", async () => {
    setupDom()
    const { readToken, syncTokenFromApiPayload } = await importFresh("../../token/tokenStorage.js")

    syncTokenFromApiPayload({ id: "x1" })
    assert.equal(readToken(), undefined)

    syncTokenFromApiPayload({ token: "from-api" })
    assert.equal(readToken(), "from-api")

    teardownDom()
})
