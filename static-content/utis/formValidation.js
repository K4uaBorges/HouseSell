const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const ISO_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

function ensureFeedbackElement(input) {
    if (!input) return null

    const next = input.nextElementSibling
    if (next && next.classList?.contains("invalid-feedback")) return next

    const feedback = document.createElement("div")
    feedback.className = "invalid-feedback"
    input.insertAdjacentElement("afterend", feedback)
    return feedback
}

function setFieldError(input, message) {
    if (!input) return false
    const feedback = ensureFeedbackElement(input)
    input.classList.add("is-invalid")
    input.classList.remove("is-valid")
    if (feedback) feedback.textContent = message
    return false
}

function setFieldValid(input) {
    if (!input) return true
    const feedback = ensureFeedbackElement(input)
    input.classList.remove("is-invalid")
    input.classList.add("is-valid")
    if (feedback) feedback.textContent = ""
    return true
}

function clearFieldValidation(input) {
    if (!input) return
    const feedback = ensureFeedbackElement(input)
    input.classList.remove("is-invalid")
    input.classList.remove("is-valid")
    if (feedback) feedback.textContent = ""
}

function clearFieldsValidation(inputs = []) {
    inputs.forEach(input => clearFieldValidation(input))
}

function isUuidString(value) {
    return UUID_PATTERN.test(String(value || "").trim())
}

function validateRequired(input, label) {
    const value = input?.value?.trim() || ""
    if (!value) return setFieldError(input, `${label} é obrigatório.`)
    return setFieldValid(input)
}

function validateEmail(input, label = "email") {
    const value = input?.value?.trim() || ""
    if (!value) return setFieldError(input, `${label} é obrigatório.`)
    if (!EMAIL_PATTERN.test(value)) return setFieldError(input, `${label} inválido.`)
    return setFieldValid(input)
}

function validateUuid(input, label, { optional = false } = {}) {
    const value = input?.value?.trim() || ""
    if (!value && optional) return setFieldValid(input)
    if (!value) return setFieldError(input, `${label} é obrigatório.`)
    if (!UUID_PATTERN.test(value)) return setFieldError(input, `${label} deve ser um UUID válido.`)
    return setFieldValid(input)
}

function validatePositiveInt(input, label) {
    const value = input?.value?.trim() || ""
    if (!value) return setFieldError(input, `${label} é obrigatório.`)
    const parsed = Number.parseInt(value, 10)
    if (!Number.isInteger(parsed) || parsed <= 0) return setFieldError(input, `${label} deve ser inteiro > 0.`)
    return setFieldValid(input)
}

function validatePositiveNumber(input, label) {
    const value = input?.value?.trim() || ""
    if (!value) return setFieldError(input, `${label} é obrigatório.`)
    const parsed = Number.parseFloat(value)
    if (!Number.isFinite(parsed) || parsed <= 0) return setFieldError(input, `${label} deve ser número > 0.`)
    return setFieldValid(input)
}

function validateLocationType(input, validTypes = []) {
    const value = input?.value?.trim().toUpperCase() || ""
    if (!value) return setFieldError(input, "type é obrigatório.")
    if (!validTypes.includes(value)) {
        return setFieldError(input, `type inválido. Usa: ${validTypes.join(", ")}`)
    }
    return setFieldValid(input)
}

function validateIsoDate(input, label) {
    const value = input?.value?.trim() || ""
    if (!value) return setFieldError(input, `${label} é obrigatório.`)
    if (!ISO_DATE_PATTERN.test(value)) return setFieldError(input, `${label} deve estar em YYYY-MM-DD.`)
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return setFieldError(input, `${label} inválida.`)
    return setFieldValid(input)
}

function validateDateRange(startInput, endInput) {
    const start = startInput?.value?.trim() || ""
    const end = endInput?.value?.trim() || ""
    if (!start || !end) return false
    if (start >= end) {
        setFieldError(endInput, "endDate deve ser maior que startDate.")
        return false
    }
    return true
}

export {
    clearFieldValidation,
    clearFieldsValidation,
    isUuidString,
    validateDateRange,
    validateEmail,
    validateIsoDate,
    validateLocationType,
    validatePositiveInt,
    validatePositiveNumber,
    validateRequired,
    validateUuid,
}
