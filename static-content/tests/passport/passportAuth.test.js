import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("applyPassportAuthorization injects bearer token header", async () => {
    setupDom()
    localStorage.setItem("houses.auth.token", "abc-123")
    const { applyPassportAuthorization } = await importFresh("../../passport/passportAuth.js")

    const headers = applyPassportAuthorization({ Accept: "application/json" })
    assert.deepEqual(headers, {
        Accept: "application/json",
        Authorization: "Bearer abc-123",
    })

    teardownDom()
})

test("applyPassportAuthorization throws when no token exists", async () => {
    setupDom()
    const { applyPassportAuthorization } = await importFresh("../../passport/passportAuth.js")

    assert.throws(
        () => applyPassportAuthorization(),
        error => error.status === 401 && /Sem token na sessão/.test(error.message),
    )

    teardownDom()
})
