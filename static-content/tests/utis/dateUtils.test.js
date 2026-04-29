import assert from "node:assert/strict"
import test from "node:test"
import { importFresh } from "../helpers/testEnv.js"

test("todayIsoDate and tomorrowIsoDate return ISO date strings", async () => {
    const { todayIsoDate, tomorrowIsoDate } = await importFresh("../../utis/dateUtils.js")

    const today = todayIsoDate()
    const tomorrow = tomorrowIsoDate()

    assert.match(today, /^\d{4}-\d{2}-\d{2}$/)
    assert.match(tomorrow, /^\d{4}-\d{2}-\d{2}$/)
    assert.ok(tomorrow >= today)
})
