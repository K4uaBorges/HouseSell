import {
    buildPage,
    buildUrl,
    button,
    clearFieldsValidation,
    createAlert,
    createJsonPre,
    createLinkList,
    createLinkedOrEmpty,
    createPagingControls,
    div,
    fetchJson,
    form,
    input,
    label,
    normalizePagingQuery,
    replaceMain,
    runAsync,
    validateEmail,
    validateRequired,
} from "../utis/index.js"

function createUserOptionsSection() {
    const options = [
        { href: "#houses/mine", text: "My Houses" },
        { href: "#bookings/mine", text: "My Bookings" },
    ]

    return div(
        { class: "border rounded p-3 mb-3" },
        div({ class: "mb-2 fw-semibold" }, "Opções do Utilizador"),
        createLinkList(options, item => item.href, item => item.text),
    )
}

function createUserForm(mainContent) {
    const statusBox = div()
    const nameInput = input({ class: "form-control", type: "text", required: true, placeholder: "Nome" })
    const emailInput = input({ class: "form-control", type: "email", required: true, placeholder: "Email" })

    return form(
        {
            class: "border rounded p-3 mb-3",
            onsubmit: async event => {
                event.preventDefault()
                clearFieldsValidation([nameInput, emailInput])
                const name = nameInput.value.trim()
                const email = emailInput.value.trim()

                const nameOk = validateRequired(nameInput, "name")
                const emailOk = validateEmail(emailInput, "email")
                if (!nameOk || !emailOk) {
                    statusBox.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                    return
                }

                statusBox.replaceChildren(createAlert("A criar user...", "secondary"))
                try {
                    const created = await fetchJson(
                        buildUrl("/users"),
                        { method: "POST", body: { name, email } },
                    )

                    statusBox.replaceChildren(
                        createAlert(
                            `User criado com sucesso (${created.id}). Token recebido da API e aplicado.`,
                            "success",
                        ),
                    )
                    window.location.hash = `#users/${encodeURIComponent(created.id)}`
                } catch (error) {
                    statusBox.replaceChildren(createAlert(error?.message || "Erro ao criar user.", "danger"))
                }
            },
        },
        div({ class: "mb-2 fw-semibold" }, "Criar User"),
        div(
            { class: "row g-2" },
            div({ class: "col-md-5" }, label({ class: "form-label" }, "name"), nameInput),
            div({ class: "col-md-5" }, label({ class: "form-label" }, "email"), emailInput),
            div(
                { class: "col-md-2 d-grid" },
                button({ type: "submit", class: "btn btn-primary mt-md-4" }, "Criar"),
            ),
        ),
        statusBox,
    )
}

function getUsers(mainContent, _params = {}, query = {}) {
    const { skip, limit } = normalizePagingQuery(query)

    runAsync(
        mainContent,
        async () => {
            const data = await fetchJson(buildUrl("/users", { skip, limit }))
            const users = Array.isArray(data?.users) ? data.users : []
            replaceMain(
                mainContent,
                buildPage(
                    "Users",
                    createUserForm(mainContent),
                    createUserOptionsSection(),
                    createPagingControls("users", { skip, limit, itemCount: users.length }),
                    createLinkedOrEmpty(
                        users,
                        "Sem users.",
                        user => `#users/${encodeURIComponent(user.id)}`,
                        user => `${user.name} (${user.email})`,
                    ),
                    createPagingControls("users", { skip, limit, itemCount: users.length }),
                ),
            )
        },
        "A carregar users...",
    )
}

function getUserById(mainContent, params = {}) {
    runAsync(
        mainContent,
        async () => {
            const uid = params.uid
            const user = await fetchJson(buildUrl(`/users/${encodeURIComponent(uid)}`))

            const updateStatus = div()
            const nameInput =
                input({
                    class: "form-control",
                    type: "text",
                    required: true,
                    value: user.name || "",
                })
            const emailInput =
                input({
                    class: "form-control",
                    type: "email",
                    required: true,
                    value: user.email || "",
                })

            const updateForm =
                form(
                    {
                        class: "border rounded p-3 mb-3",
                        onsubmit: async event => {
                            event.preventDefault()
                            clearFieldsValidation([nameInput, emailInput])
                            const name = nameInput.value.trim()
                            const email = emailInput.value.trim()

                            const nameOk = validateRequired(nameInput, "name")
                            const emailOk = validateEmail(emailInput, "email")
                            if (!nameOk || !emailOk) {
                                updateStatus.replaceChildren(createAlert("Revê os campos assinalados.", "warning"))
                                return
                            }

                            updateStatus.replaceChildren(createAlert("A atualizar user...", "secondary"))
                            try {
                                const updated =
                                    await fetchJson(
                                        buildUrl(`/users/${encodeURIComponent(uid)}`),
                                        { method: "PUT", auth: true, body: { name, email } },
                                    )
                                updateStatus.replaceChildren(createAlert("User atualizado.", "success"))
                                replaceMain(mainContent, buildPage(`User ${uid}`, createJsonPre(updated), updateForm, deleteSection))
                            } catch (error) {
                                updateStatus.replaceChildren(createAlert(error?.message || "Erro ao atualizar user.", "danger"))
                            }
                        },
                    },
                    div({ class: "mb-2 fw-semibold" }, "Atualizar User"),
                    div(
                        { class: "row g-2" },
                        div({ class: "col-md-5" }, label({ class: "form-label" }, "name"), nameInput),
                        div({ class: "col-md-5" }, label({ class: "form-label" }, "email"), emailInput),
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
                    div({ class: "mb-2 fw-semibold" }, "Remover User"),
                    button(
                        {
                            type: "button",
                            class: "btn btn-danger",
                            onclick: async () => {
                                if (!window.confirm("Tens a certeza que queres remover este user?")) return
                                deleteStatus.replaceChildren(createAlert("A remover user...", "secondary"))
                                try {
                                    await fetchJson(
                                        buildUrl(`/users/${encodeURIComponent(uid)}`),
                                        { method: "DELETE", auth: true, body: { id: uid } },
                                    )
                                    window.location.hash = "#users"
                                } catch (error) {
                                    deleteStatus.replaceChildren(createAlert(error?.message || "Erro ao remover user.", "danger"))
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
                    `User ${uid}`,
                    createUserOptionsSection(),
                    createJsonPre(user),
                    updateForm,
                    deleteSection,
                ),
            )
        },
        "A carregar user...",
    )
}

export { getUserById, getUsers }
