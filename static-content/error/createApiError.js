function createApiError(status, message, payload = null) {
    return { status, message, payload }
}

export { createApiError }
