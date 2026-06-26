import {button, div, form, input, label, option, select} from "../../dsl/dsl.js";
import {clearFieldsValidation, createSubmitGuard, validateLocationType, validateRequired} from "../../utis/index.js";
import {createAlert} from "../../ui/pageComponents.js";
import {fetchJson} from "../../api/fetchJson.js";
import {buildUrl} from "../../api/buildUrl.js";
import {LOCATION_TYPES} from "../viewUtils.js";
import {locationNameHash} from "./locations.js";

export function createLocationForm(mainContent) {
    const statusBox = div()
    const submitGuard = createSubmitGuard()
    const nameInput = input({ class: "form-control", type: "text", required: true, placeholder: "Nome" })
    const typeInput =
        select(
            { class: "form-select", required: true },
            option({ value: "" }, "Seleciona type"),
            ...LOCATION_TYPES.map(type => option({ value: type }, type)),
        )
    const parentWordInput = input({ class: "form-control", type: "text", placeholder: "Palavra do parent (ex: Portugal)" })

    const syncParentWordState = () => {
        const selectedType = String(typeInput.value || "").trim().toUpperCase()
        const isCountry = selectedType === "COUNTRY"

        parentWordInput.disabled = isCountry
        parentWordInput.required = !isCountry
        parentWordInput.value = isCountry ? "" : parentWordInput.value
        parentWordInput.placeholder = isCountry
            ? "COUNTRY não tem parent"
            : "Palavra do parent (ex: Portugal)"

        if (isCountry) {
            parentWordInput.classList.remove("is-invalid")
            parentHashHint.textContent = "Hash parent: -"
        } else {
            refreshParentHashHint()
        }
    }
    const parentHashHint = div({ class: "small text-muted mt-1" }, "Hash parent: -")

    const refreshParentHashHint = () => {
        const hash = locationNameHash(parentWordInput.value)
        parentHashHint.textContent = hash ? `Hash parent: ${hash}` : "Hash parent: -"
    }
    parentWordInput.addEventListener("input", refreshParentHashHint)
    typeInput.addEventListener("change", syncParentWordState)
    syncParentWordState()

    return form(
        {
            class: "border rounded p-3 mb-3",
            onsubmit: async event => {
                event.preventDefault()
                clearFieldsValidation([nameInput, typeInput, parentWordInput])
                const name = nameInput.value.trim()
                const type = typeInput.value.trim().toUpperCase()
                const parentWord = parentWordInput.value.trim()

                const nameOk = validateRequired(nameInput, "name")
                const typeOk = validateLocationType(typeInput, LOCATION_TYPES)
                if (!nameOk || !typeOk) {
                    statusBox.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                    return
                }
                const guard = submitGuard.begin()
                if (!guard.ok) {
                    statusBox.replaceChildren(createAlert(guard.message, "warning"))
                    return
                }

                statusBox.replaceChildren(createAlert("A criar location...", "secondary"))
                try {
                    let parentId = null
                    if (type !== "COUNTRY") {
                        if (!parentWord) {
                            parentWordInput.classList.add("is-invalid")
                            statusBox.replaceChildren(
                                createAlert("Para tipos diferentes de COUNTRY, indica a palavra da location parent.", "warning"),
                            )
                            return
                        }

                        const locationsData = await fetchJson(buildUrl("/locations"))
                        const locations = normalizeLocationsPayload(locationsData)
                        parentId = resolveParentIdFromKeyword(parentWord, locations)

                        if (!parentId) {
                            parentWordInput.classList.add("is-invalid")
                            statusBox.replaceChildren(
                                createAlert(
                                    `Nenhuma location encontrada para o hash ${locationNameHash(parentWord)}.`,
                                    "warning",
                                ),
                            )
                            return
                        }
                    }

                    const created = await fetchJson(
                        buildUrl("/locations"),
                        {
                            method: "POST",
                            auth: true,
                            body: {
                                name,
                                type,
                                parentId,
                            },
                        },
                    )
                    statusBox.replaceChildren(createAlert("Location criada.", "success"))
                    window.location.hash = `#locations/${encodeURIComponent(created.lid)}`
                } catch (error) {
                    statusBox.replaceChildren(createAlert(error?.message || "Erro ao criar location.", "danger"))
                } finally {
                    submitGuard.end()
                }
            },
        },
        div({ class: "mb-2 fw-semibold" }, "Criar Location"),
        div({ class: "small text-muted mb-2" }, `Tipos válidos: ${LOCATION_TYPES.join(", ")}`),
        div(
            { class: "row g-2" },
            div({ class: "col-md-4" }, label({ class: "form-label" }, "name"), nameInput),
            div({ class: "col-md-3" }, label({ class: "form-label" }, "type"), typeInput),
            div(
                { class: "col-md-3" },
                label({ class: "form-label" }, "parentWord"),
                parentWordInput,
                parentHashHint,
            ),
            div(
                { class: "col-md-2 d-grid" },
                button({ type: "submit", class: "btn btn-primary mt-md-4" }, "Criar"),
            ),
        ),
        statusBox,
    )
}