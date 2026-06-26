import { a, button, div, form, h1, h2, input, label, li, option, p, pre, select, ul } from "../dsl/dsl.js"
import { clearFieldValidation, validateDateRange } from "../utis/formValidation.js"
import {buildHash} from "../utis/index.js";

function createTitle(text) {
    return h2({ class: "page-title h4 mb-3" }, text)
}

function createAlert(message, type = "danger") {
    return div({ class: `app-alert alert alert-${type}` }, message)
}

function createJsonPre(data) {
    return pre({ class: "app-code-block bg-light border rounded p-3" }, JSON.stringify(data, null, 2))
}

function createLinkList(items, toHref, toText) {
    return ul(
        { class: "app-list list-group" },
        items.map(item =>
            li(
                { class: "app-list-item list-group-item" },
                a({ href: toHref(item) }, toText(item)),
            ),
        ),
    )
}

function replaceMain(mainContent, node) {
    if (!mainContent) return
    mainContent.replaceChildren(node)
}

function buildPage(title, ...children) {
    return div({ class: "page-shell" }, createTitle(title), ...children)
}

function createLinkedOrEmpty(items, emptyMessage, toHref, toText) {
    if (!items.length) return createAlert(emptyMessage, "secondary")
    return createLinkList(items, toHref, toText)
}

function nextIsoDate(value) {
    const date = new Date(String(value || "").trim())
    if (Number.isNaN(date.getTime())) return ""
    date.setDate(date.getDate() + 1)
    return date.toISOString().slice(0, 10)
}

function createDateSearchForm(
    baseHashPath,
    startValue,
    endValue,
    buttonLabel = "Pesquisar",
    startKey = "dateStart",
    endKey = "dateEnd",
) {
    const startDateInput =
        input({
            class: "form-control",
            type: "date",
            required: true,
            value: startValue,
        })
    const endDateInput =
        input({
            class: "form-control",
            type: "date",
            required: true,
            value: endValue,
        })

    const syncDateConstraints = () => {
        const nextDay = nextIsoDate(startDateInput.value)
        if (nextDay) {
            endDateInput.min = nextDay
        } else {
            endDateInput.removeAttribute("min")
        }
    }

    startDateInput.addEventListener("input", () => {
        syncDateConstraints()
        clearFieldValidation(endDateInput)
    })
    endDateInput.addEventListener("input", () => clearFieldValidation(endDateInput))
    syncDateConstraints()

    return form(
        {
            class: "app-form-section row g-2 align-items-end mb-3",
            onsubmit: event => {
                event.preventDefault()
                const startDate = startDateInput.value.trim()
                const endDate = endDateInput.value.trim()
                if (!validateDateRange(startDateInput, endDateInput)) return
                window.location.hash =
                    buildHash(
                        baseHashPath,
                        {
                            [startKey]: startDate,
                            [endKey]: endDate,
                        },
                    )
            },
        },
        div(
            { class: "col-sm-4" },
            label({ class: "form-label" }, startKey),
            startDateInput,
        ),
        div(
            { class: "col-sm-4" },
            label({ class: "form-label" }, endKey),
            endDateInput,
        ),
        div(
            { class: "col-sm-4" },
            button({ type: "submit", class: "btn btn-primary" }, buttonLabel),
        ),
    )
}

export {
    a,
    buildPage,
    button,
    createAlert,
    createDateSearchForm,
    createJsonPre,
    createLinkList,
    createLinkedOrEmpty,
    div,
    form,
    h1,
    h2,
    input,
    label,
    li,
    option,
    p,
    replaceMain,
    select,
    ul,
}
