function normalizeHousesPayload(data) {
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.houses)) return data.houses
    return []
}

function normalizeBookingsPayload(data) {
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.bookings)) return data.bookings
    return []
}

export { normalizeBookingsPayload, normalizeHousesPayload }