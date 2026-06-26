import {
    buildUrl,
    button,
    clearFieldsValidation,
    createSubmitGuard,
    createAlert,
    div,
    fetchJson,
    form,
    input,
    label,
} from "../../utis/index.js"
import { createLocationDropdowns } from "../viewUtils.js"
import { parseHousePayload, validateHouseFields } from "./housePayload.js"

function createHouseForm(locations = []) {
    const statusBox = div()
    const submitGuard = createSubmitGuard()
    const titleInput = input({ class: "form-control", type: "search", required: true, placeholder: "Procurar nome da casa" })
    const areaInput = input({ class: "form-control", type: "number", required: true, min: "1", placeholder: "Area (m²)" })
    const priceInput = input({ class: "form-control", type: "number", required: true, min: "0.01", step: "0.01", placeholder: "Preço/noite" })
    const descriptionInput = input({ class: "form-control", type: "text", required: true, placeholder: "Descrição" })
    const {
        leafLocationInput: lidInput,
        selectorFields: selectorRow,
    } = createLocationDropdowns(locations)

    return form(
        {
            class: "border rounded p-3 mb-3",
            onsubmit: async event => {
                event.preventDefault()
                clearFieldsValidation([titleInput, lidInput, areaInput, priceInput, descriptionInput])
                const valid = validateHouseFields(titleInput, lidInput, areaInput, priceInput, descriptionInput)
                if (!valid) {
                    statusBox.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                    return
                }
                const payload = parseHousePayload(titleInput, lidInput, areaInput, priceInput, descriptionInput)
                if (!payload) {
                    statusBox.replaceChildren(createAlert("Dados inválidos para criar house.", "warning"))
                    return
                }
                const guard = submitGuard.begin()
                if (!guard.ok) {
                    statusBox.replaceChildren(createAlert(guard.message, "warning"))
                    return
                }

                statusBox.replaceChildren(createAlert("A criar house...", "secondary"))
                try {
                    const created = await fetchJson(
                        buildUrl("/houses"),
                        { method: "POST", auth: true, body: payload },
                    )
                    console.warn("House created:", created)
                    statusBox.replaceChildren(createAlert("House criada.", "success"))
                    if (created?.hid) {
                        window.location.hash = "#houses/mine"
                    }
                } catch (error) {
                    statusBox.replaceChildren(createAlert(error?.message || "Erro ao criar house.", "danger"))
                } finally {
                    submitGuard.end()
                }
            },
        },
        div({ class: "mb-2 fw-semibold" }, "Criar House"),
        div(
            { class: "row g-2" },
            div({ class: "col-md-4" }, label({ class: "form-label" }, "title"), titleInput),
            div({ class: "col-md-2" }, label({ class: "form-label" }, "areaSqMt"), areaInput),
            div({ class: "col-md-2" }, label({ class: "form-label" }, "pricePerNight"), priceInput),
            div({ class: "col-md-10" }, label({ class: "form-label" }, "description"), descriptionInput),
            div(
                { class: "col-md-2 d-grid" },
                button({ type: "submit", class: "btn btn-primary mt-md-4" }, "Criar"),
            ),
        ),
        selectorRow,
        statusBox,
    )
}

export { createHouseForm }
