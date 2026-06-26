function locationNameHash(rawValue) {
    const normalized =
        String(rawValue || "")
            .trim()
            .toLowerCase()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")

    if (!normalized) return ""

    let hash = 0
    for (let index = 0; index < normalized.length; index += 1) {
        hash = ((hash * 31) + normalized.charCodeAt(index)) | 0
    }

    return `loc-${Math.abs(hash).toString(16).padStart(8, "0")}`
}

function normalizeLocationsPayload(data) {
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.locations)) return data.locations
    return []
}

function resolveParentIdFromKeyword(parentKeyword, locations) {
    const targetHash = locationNameHash(parentKeyword)
    if (!targetHash) return null

    const match =
        locations.find(location => locationNameHash(location?.name) === targetHash)
    return match?.id || null
}

export {
    locationNameHash,
    resolveParentIdFromKeyword,
    normalizeLocationsPayload,
}
export {
    getLocationChildrenAll,
    getLocationChildrenDirect,
} from "./getLocationsChildren.js"
export { getLocations } from "./getLocations.js"
export { getLocationById } from "./getLocationsById.js"
export { createLocationForm } from "./locationForm.js"
export { getLocationPath } from "./getLocationsPath.js"
