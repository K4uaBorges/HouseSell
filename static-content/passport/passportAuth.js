import { createApiError } from "../error/createApiError.js"
import { readToken } from "../token/tokenStorage.js"

function applyPassportAuthorization(headers = {}) {
    const token = readToken()
    if (!token) {
        throw createApiError(401, "Sem token na sessão. Cria um user para receber token automático.")
    }

    return {
        ...headers,
        Authorization: `Bearer ${token}`,
    }
}

export { applyPassportAuthorization }
