import {
    button,
    clearFieldsValidation,
    createAlert,
    div,
    form,
    input,
    label,
    option,
    select,
    validateDateRange,
    validateIsoDate,
    validateRequired,
} from "../../utis/index.js"

function createBookingEditor({
    houseOptions = [],
    selectedHouseId = "",
    fixedHouse = null,
    initialStartDate = "",
    initialEndDate = "",
    titleText,
    submitLabel,
    submitClass,
    onSubmit,
}) {
    const statusBox = div()
    const houseInput =
        fixedHouse
            ? input({ class: "form-control", type: "text", value: fixedHouse.title, disabled: true })
            : select(
                { class: "form-select", required: true },
                option({ value: "" }, "Selecionar house"),
                ...houseOptions.map(house =>
                    option(
                        {
                            value: house.hid,
                            selected: house.hid === selectedHouseId,
                        },
                        `${house.title} (${house.pricePerNight}/noite)`,
                    ),
                ),
            )
    const startDateInput = input({ class: "form-control", type: "date", required: true, value: initialStartDate })
    const endDateInput = input({ class: "form-control", type: "date", required: true, value: initialEndDate })

    return form(
        {
            class: "border rounded p-3 mb-3",
            onsubmit: async event => {
                event.preventDefault()
                clearFieldsValidation([houseInput, startDateInput, endDateInput])
                const houseOk = fixedHouse ? true : validateRequired(houseInput, "house")
                const startOk = validateIsoDate(startDateInput, "startDate")
                const endOk = validateIsoDate(endDateInput, "endDate")
                const rangeOk = startOk && endOk ? validateDateRange(startDateInput, endDateInput) : false
                if (!houseOk || !startOk || !endOk || !rangeOk) {
                    statusBox.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                    return
                }

                const payload = {
                    hid: fixedHouse?.hid || String(houseInput.value || "").trim(),
                    startDate: startDateInput.value.trim(),
                    endDate: endDateInput.value.trim(),
                }

                statusBox.replaceChildren(createAlert("A guardar booking...", "secondary"))
                try {
                    await onSubmit(payload)
                    statusBox.replaceChildren(createAlert("Booking guardada.", "success"))
                } catch (error) {
                    statusBox.replaceChildren(createAlert(error?.message || "Erro ao guardar booking.", "danger"))
                }
            },
        },
        div({ class: "mb-2 fw-semibold" }, titleText),
        div(
            { class: "row g-2" },
            div({ class: "col-md-4" }, label({ class: "form-label" }, "house"), houseInput),
            div({ class: "col-md-3" }, label({ class: "form-label" }, "startDate"), startDateInput),
            div({ class: "col-md-3" }, label({ class: "form-label" }, "endDate"), endDateInput),
            div(
                { class: "col-md-2 d-grid" },
                button({ type: "submit", class: `${submitClass} mt-md-4` }, submitLabel),
            ),
        ),
        statusBox,
    )
}

export { createBookingEditor }