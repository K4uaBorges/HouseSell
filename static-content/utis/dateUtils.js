function todayIsoDate() {
    return new Date().toISOString().slice(0, 10)
}

function tomorrowIsoDate() {
    const date = new Date()
    date.setDate(date.getDate() + 1)
    return date.toISOString().slice(0, 10)
}

export { todayIsoDate, tomorrowIsoDate }
