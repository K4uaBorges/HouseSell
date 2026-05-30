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
    const { AUTH_TOKEN_CHANGED_EVENT, removeToken, writeSession } = await importFresh("../../token/tokenStorage.js")

    let eventPayload = null
    window.addEventListener(AUTH_TOKEN_CHANGED_EVENT, event => {
        eventPayload = event.detail?.token
    })

    writeSession({ token: "token-2", id: "u2", email: "user@example.com" })
    removeToken()

    assert.equal(localStorage.getItem("houses.auth.token"), null)
    assert.equal(localStorage.getItem("houses.auth.session"), null)
    assert.equal(eventPayload, "")

    teardownDom()
})

test("syncTokenFromApiPayload writes token only when present", async () => {
    setupDom()
    const { readSession, readToken, syncTokenFromApiPayload } = await importFresh("../../token/tokenStorage.js")

    syncTokenFromApiPayload({ id: "x1" })
    assert.equal(readToken(), undefined)

    syncTokenFromApiPayload({ token: "from-api", id: "u1", email: "alice@example.com" })
    assert.equal(readToken(), "from-api")
    assert.deepEqual(readSession(), {
        token: "from-api",
        id: "u1",
        name: "",
        email: "alice@example.com",
        role: "",
    })

    teardownDom()
})

test("syncTokenFromApiPayload preserves known session fields when later payloads only refresh token", async () => {
    setupDom()
    const { readSession, syncTokenFromApiPayload } = await importFresh("../../token/tokenStorage.js")

    syncTokenFromApiPayload({ token: "token-1", id: "u9", name: "Alice", email: "alice@example.com" })
    syncTokenFromApiPayload({ token: "token-2", uid: "ignored-owner-id" })

    assert.deepEqual(readSession(), {
        token: "token-2",
        id: "u9",
        name: "Alice",
        email: "alice@example.com",
        role: "",
    })

    teardownDom()
})

test("hasAdminAccess only accepts admin role from the backend session", async () => {
    setupDom()
    const { hasAdminAccess, writeSession } = await importFresh("../../token/tokenStorage.js")

    writeSession({ token: "role-token", id: "u1", email: "member@example.com", role: "admin" })
    assert.equal(hasAdminAccess(), true)

    writeSession({ token: "mail-token", id: "u2", email: "principal.demo@houses.local", role: "" })
    assert.equal(hasAdminAccess(), false)

    writeSession({ token: "user-token", id: "u3", email: "user@example.com", role: "" })
    assert.equal(hasAdminAccess(), false)

    teardownDom()
})
