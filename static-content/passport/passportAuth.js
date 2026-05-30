import { createApiError } from "../error/createApiError.js"
import { readToken } from "../token/tokenStorage.js"

function applyPassportAuthorization(headers = {}) {
    const token = readToken()
    if (!token) {
        throw createApiError(401, "Sem token na sessão. Faz login ou cria conta primeiro.")
    }

    return {
        ...headers,
        Authorization: `Bearer ${token}`,
    }
}

export { applyPassportAuthorization }
