import { a, button, createAlert, div, form, input, label } from "../ui/pageComponents.js"

const PAGING_DEFAULT_SKIP = 0
const PAGING_DEFAULT_LIMIT = 20
const PAGING_MAX_LIMIT = 100

function toIntOrNull(value) {
    const parsed = Number.parseInt(String(value ?? "").trim(), 10)
    return Number.isInteger(parsed) ? parsed : null
}

function normalizePagingQuery(query = {}) {
    const rawSkip = toIntOrNull(query.skip)
    const rawLimit = toIntOrNull(query.limit)

    const skip = rawSkip === null ? PAGING_DEFAULT_SKIP : Math.max(0, rawSkip)
    const limit =
        rawLimit === null
            ? PAGING_DEFAULT_LIMIT
            : Math.min(PAGING_MAX_LIMIT, Math.max(1, rawLimit))

    return { skip, limit }
}

function buildHash(baseHashPath, query = {}) {
    const normalizedBase =
        String(baseHashPath || "")
            .trim()
            .replace(/^#/, "")
            .replace(/^\/+/, "")
            .replace(/\/+$/, "")

    const search = new URLSearchParams()

    for (const [key, value] of Object.entries(query)) {
        if (value === null || value === undefined) continue
        const trimmed = String(value).trim()
        if (!trimmed) continue
        search.set(key, trimmed)
    }

    const queryString = search.toString()
    return queryString ? `#${normalizedBase}/${queryString}` : `#${normalizedBase}`
}

function createPagingControls(
    baseHashPath,
    {
        skip,
        limit,
        itemCount,
        extraQuery = {},
    },
) {
    const prevSkip = Math.max(0, skip - limit)
    const nextSkip = skip + limit
    const hasPrev = skip > 0
    const hasNext = itemCount >= limit

    const skipInput =
        input({
            class: "form-control",
            type: "number",
            min: "0",
            step: "1",
            value: String(skip),
        })
    const limitInput =
        input({
            class: "form-control",
            type: "number",
            min: "1",
            max: String(PAGING_MAX_LIMIT),
            step: "1",
            value: String(limit),
        })
    const status = div()

    const applyForm =
        form(
            {
                class: "row g-2 align-items-end",
                onsubmit: event => {
                    event.preventDefault()
                    const parsedSkip = toIntOrNull(skipInput.value)
                    const parsedLimit = toIntOrNull(limitInput.value)
                    if (parsedSkip === null || parsedSkip < 0) {
                        status.replaceChildren(createAlert("skip deve ser >= 0.", "warning"))
                        return
                    }
                    if (parsedLimit === null || parsedLimit < 1 || parsedLimit > PAGING_MAX_LIMIT) {
                        status.replaceChildren(createAlert(`limit deve estar entre 1 e ${PAGING_MAX_LIMIT}.`, "warning"))
                        return
                    }

                    window.location.hash =
                        buildHash(
                            baseHashPath,
                            {
                                ...extraQuery,
                                skip: parsedSkip,
                                limit: parsedLimit,
                            },
                        )
                },
            },
            div(
                { class: "col-sm-3" },
                label({ class: "form-label mb-1" }, "skip"),
                skipInput,
            ),
            div(
                { class: "col-sm-3" },
                label({ class: "form-label mb-1" }, `limit (1-${PAGING_MAX_LIMIT})`),
                limitInput,
            ),
            div(
                { class: "col-sm-2 d-grid" },
                button({ type: "submit", class: "btn btn-primary" }, "Aplicar"),
            ),
            div(
                { class: "col-sm-4 d-flex gap-2 justify-content-sm-end mt-2 mt-sm-0" },
                hasPrev
                    ? a(
                        {
                            class: "btn btn-outline-secondary",
                            href:
                                buildHash(
                                    baseHashPath,
                                    {
                                        ...extraQuery,
                                        skip: prevSkip,
                                        limit,
                                    },
                                ),
                        },
                        "Anterior",
                    )
                    : button({ type: "button", class: "btn btn-outline-secondary", disabled: true }, "Anterior"),
                hasNext
                    ? a(
                        {
                            class: "btn btn-outline-secondary",
                            href:
                                buildHash(
                                    baseHashPath,
                                    {
                                        ...extraQuery,
                                        skip: nextSkip,
                                        limit,
                                    },
                                ),
                        },
                        "Seguinte",
                    )
                    : button({ type: "button", class: "btn btn-outline-secondary", disabled: true }, "Seguinte"),
            ),
            div({ class: "col-12" }, status),
        )

    return div(
        { class: "border rounded p-3 mb-3" },
        div({ class: "fw-semibold mb-2" }, "Paginação"),
        div({ class: "small text-muted mb-2" }, `Resultados nesta página: ${itemCount}`),
        applyForm,
    )
}

export {
    PAGING_DEFAULT_LIMIT,
    PAGING_DEFAULT_SKIP,
    PAGING_MAX_LIMIT,
    buildHash,
    createPagingControls,
    normalizePagingQuery,
}
