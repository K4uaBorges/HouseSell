import assert from "node:assert/strict"
import test from "node:test"
import { importFresh } from "../helpers/testEnv.js"

test("createApiError builds a standard error payload", async () => {
    const { createApiError } = await importFresh("../../error/createApiError.js")
    const err = createApiError(401, "Unauthorized", { reason: "missing token" })

    assert.deepEqual(err, {
        status: 401,
        message: "Unauthorized",
        payload: { reason: "missing token" },
    })
})
