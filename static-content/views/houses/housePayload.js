import {
  validatePositiveInt,
  validatePositiveNumber,
  validateRequired,
  validateUuid,
} from "../../utis/index.js"

function normalizeHousesPayload(data) {
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.houses)) return data.houses
    return []
}

function normalizeLocationsPayload(data) {
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.locations)) return data.locations
    return []
}

function parseHousePayload(titleInput, lidInput, areaInput, priceInput, descriptionInput) {
    const title = titleInput.value.trim()
    const lid = String(lidInput.value || "").trim()
    const areaSqMt = Number.parseInt(areaInput.value.trim(), 10)
    const pricePerNight = Number.parseFloat(priceInput.value.trim())
    const description = descriptionInput.value.trim()

    if (!title || !lid || !description) return null
    if (!Number.isInteger(areaSqMt) || areaSqMt <= 0) return null
    if (!Number.isFinite(pricePerNight) || pricePerNight <= 0) return null

    return { title, lid, areaSqMt, pricePerNight, description }
}

function validateHouseFields(titleInput, lidInput, areaInput, priceInput, descriptionInput) {
    const titleOk = validateRequired(titleInput, "title")
    const lidRequiredOk = validateRequired(lidInput, "localização")
    const lidOk = lidRequiredOk ? validateUuid(lidInput, "lid") : false
    const areaOk = validatePositiveInt(areaInput, "areaSqMt")
    const priceOk = validatePositiveNumber(priceInput, "pricePerNight")
    const descriptionOk = validateRequired(descriptionInput, "description")
    return titleOk && lidOk && areaOk && priceOk && descriptionOk
}

export {
    normalizeHousesPayload,
    normalizeLocationsPayload,
    parseHousePayload,
    validateHouseFields,
}