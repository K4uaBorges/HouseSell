import {
    areComparableValuesEqual,
    clearFieldsValidation,
    createSubmitGuard, hasAdminAccess,
    runAsync,
    validateLocationType,
    validateRequired, validateUuid
} from "../../utis/index.js";
import {fetchJson} from "../../api/fetchJson.js";
import {buildUrl} from "../../api/buildUrl.js";
import {button, div, form, input, label, option, select} from "../../dsl/dsl.js";
import {LOCATION_TYPES} from "../viewUtils.js";
import {buildPage, createAlert, createJsonPre, createLinkList, replaceMain} from "../../ui/pageComponents.js";
import {getDashboard} from "../dashboard.js";

export function getLocationById(mainContent, params = {}) {
    runAsync(
        mainContent,
        async () => {
            const hasAccess = hasAdminAccess()
            if (!hasAccess) {
                getDashboard(mainContent)
            }
            const lid = params.lid
            const location = await fetchJson(buildUrl(`/locations/${encodeURIComponent(lid)}`))

            const links = [
                { href: `#locations/${encodeURIComponent(lid)}/childrenAll`, text: "Children All" },
                { href: `#locations/${encodeURIComponent(lid)}/childrenDirect`, text: "Children Direct" },
                { href: `#locations/${encodeURIComponent(lid)}/path`, text: "Path" },
            ]

            const updateStatus = div()
            const updateGuard = createSubmitGuard()
            const nameInput =
                input({
                    class: "form-control",
                    type: "text",
                    required: true,
                    value: location.name || "",
                })
            const typeInput =
                select(
                    { class: "form-select", required: true },
                    option({ value: "" }, "Seleciona type"),
                    ...LOCATION_TYPES.map(type => option({ value: type, selected: type === (location.type || "") }, type)),
                )
            const parentIdInput =
                input({
                    class: "form-control",
                    type: "text",
                    value: location.parentId || "",
                })

            const updateForm =
                form(
                    {
                        class: "border rounded p-3 mb-3",
                        onsubmit: async event => {
                            event.preventDefault()
                            clearFieldsValidation([nameInput, typeInput, parentIdInput])
                            const name = nameInput.value.trim()
                            const type = typeInput.value.trim().toUpperCase()
                            const parentIdRaw = parentIdInput.value.trim()

                            const nameOk = validateRequired(nameInput, "name")
                            const typeOk = validateLocationType(typeInput, LOCATION_TYPES)
                            const parentOk = validateUuid(parentIdInput, "parentId", { optional: true })
                            if (!nameOk || !typeOk || !parentOk) {
                                updateStatus.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                                return
                            }
                            const payload = {
                                name,
                                type,
                                parentId: parentIdRaw || null,
                            }
                            const initialPayload = {
                                name: location.name || "",
                                type: location.type || "",
                                parentId: location.parentId || null,
                            }
                            if (areComparableValuesEqual(payload, initialPayload)) {
                                updateStatus.replaceChildren(createAlert("Não existem alterações para guardar.", "secondary"))
                                return
                            }
                            const guard = updateGuard.begin()
                            if (!guard.ok) {
                                updateStatus.replaceChildren(createAlert(guard.message, "warning"))
                                return
                            }

                            updateStatus.replaceChildren(createAlert("A atualizar location...", "secondary"))
                            try {
                                await fetchJson(
                                    buildUrl(`/locations/${encodeURIComponent(lid)}`),
                                    {
                                        method: "PUT",
                                        auth: true,
                                        body: payload,
                                    },
                                )
                                getLocationById(mainContent, { lid })
                            } catch (error) {
                                updateStatus.replaceChildren(createAlert(error?.message || "Erro ao atualizar location.", "danger"))
                            } finally {
                                updateGuard.end()
                            }
                        },
                    },
                    div({ class: "mb-2 fw-semibold" }, "Atualizar Location"),
                    div({ class: "small text-muted mb-2" }, `Tipos válidos: ${LOCATION_TYPES.join(", ")}`),
                    div(
                        { class: "row g-2" },
                        div({ class: "col-md-4" }, label({ class: "form-label" }, "name"), nameInput),
                        div({ class: "col-md-3" }, label({ class: "form-label" }, "type"), typeInput),
                        div({ class: "col-md-3" }, label({ class: "form-label" }, "parentId"), parentIdInput),
                        div(
                            { class: "col-md-2 d-grid" },
                            button({ type: "submit", class: "btn btn-warning mt-md-4" }, "Atualizar"),
                        ),
                    ),
                    updateStatus,
                )

            const deleteStatus = div()
            const deleteSection =
                div(
                    { class: "border rounded p-3" },
                    div({ class: "mb-2 fw-semibold" }, "Remover Location"),
                    button(
                        {
                            type: "button",
                            class: "btn btn-danger",
                            onclick: async () => {
                                if (!window.confirm("Tens a certeza que queres remover esta location?")) return
                                deleteStatus.replaceChildren(createAlert("A remover location...", "secondary"))
                                try {
                                    await fetchJson(
                                        buildUrl(`/locations/${encodeURIComponent(lid)}`),
                                        { method: "DELETE", auth: true, body: { id: lid } },
                                    )
                                    window.location.hash = "#locations"
                                } catch (error) {
                                    deleteStatus.replaceChildren(createAlert(error?.message || "Erro ao remover location.", "danger"))
                                }
                            },
                        },
                        "Remover",
                    ),
                    deleteStatus,
                )

            replaceMain(
                mainContent,
                buildPage(
                    `Location ${lid}`,
                    createLinkList(links, item => item.href, item => item.text),
                    createJsonPre(location),
                    updateForm,
                    deleteSection,
                ),
            )
        },
        "A carregar location...",
    )
}