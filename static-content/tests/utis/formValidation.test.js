import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("validateEmail marks invalid and valid states", async () => {
    setupDom()
    const { validateEmail } = await importFresh("../../utis/formValidation.js")

    const input = document.createElement("input")
    input.value = "invalid"
    document.body.appendChild(input)

    assert.equal(validateEmail(input), false)
    assert.equal(input.classList.contains("is-invalid"), true)

    input.value = "user@example.com"
    assert.equal(validateEmail(input), true)
    assert.equal(input.classList.contains("is-valid"), true)

    teardownDom()
})

test("validateDateRange rejects end before start and sets field error", async () => {
    setupDom()
    const { validateDateRange } = await importFresh("../../utis/formValidation.js")

    const startInput = document.createElement("input")
    const endInput = document.createElement("input")
    startInput.value = "2026-08-10"
    endInput.value = "2026-08-09"
    document.body.append(startInput, endInput)

    const isValid = validateDateRange(startInput, endInput)
    assert.equal(isValid, false)
    assert.equal(endInput.classList.contains("is-invalid"), true)

    teardownDom()
})

test("clearFieldsValidation removes validation classes", async () => {
    setupDom()
    const { clearFieldsValidation, validateRequired } = await importFresh("../../utis/formValidation.js")

    const input = document.createElement("input")
    input.value = ""
    document.body.appendChild(input)
    validateRequired(input, "field")
    assert.equal(input.classList.contains("is-invalid"), true)

    clearFieldsValidation([input])
    assert.equal(input.classList.contains("is-invalid"), false)
    assert.equal(input.classList.contains("is-valid"), false)

    teardownDom()
})
