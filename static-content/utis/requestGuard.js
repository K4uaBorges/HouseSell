const DEFAULT_SUBMIT_COOLDOWN_MS = 1500

function normalizeComparable(value) {
    if (typeof value === "string") return value.trim()
    if (Array.isArray(value)) return value.map(normalizeComparable)

    if (value && typeof value === "object") {
        return Object.fromEntries(
            Object.keys(value)
                .sort()
                .map(key => [key, normalizeComparable(value[key])]),
        )
    }

    return value
}

function areComparableValuesEqual(left, right) {
    return JSON.stringify(normalizeComparable(left)) === JSON.stringify(normalizeComparable(right))
}

function createSubmitGuard({ cooldownMs = DEFAULT_SUBMIT_COOLDOWN_MS } = {}) {
    let inFlight = false
    let lastSubmittedAt = 0

    return {
        begin() {
            const now = Date.now()
            if (inFlight) {
                return { ok: false, message: "Já existe um pedido em curso." }
            }
            if ((now - lastSubmittedAt) < cooldownMs) {
                return { ok: false, message: "Espera um instante antes de voltar a submeter." }
            }

            inFlight = true
            lastSubmittedAt = now
            return { ok: true }
        },
        end() {
            inFlight = false
        },
    }
}

export {
    DEFAULT_SUBMIT_COOLDOWN_MS,
    areComparableValuesEqual,
    createSubmitGuard,
}
