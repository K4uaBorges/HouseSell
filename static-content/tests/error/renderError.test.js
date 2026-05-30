import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("withError renders warning details for unauthorized errors", async () => {
    setupDom()
    const { withError } = await importFresh("../../error/renderError.js")

    const mainContent = document.createElement("div")
    withError(mainContent, { status: 401, message: "No token" })

    assert.match(mainContent.textContent, /Erro 401: No token/)
    assert.match(mainContent.textContent, /Sessão inválida/)

    teardownDom()
})

test("withError renders default message for unknown errors", async () => {
    setupDom()
    const { withError } = await importFresh("../../error/renderError.js")

    const mainContent = document.createElement("div")
    withError(mainContent, {})

    assert.match(mainContent.textContent, /Erro: Falha ao processar pedido\./)

    teardownDom()
})

test("withError renders api origin hint for 404 on /api paths", async () => {
    setupDom()
    const { withError } = await importFresh("../../error/renderError.js")

    const mainContent = document.createElement("div")
    withError(mainContent, { status: 404, message: "HTTP 404 (/api/users?skip=0&limit=20)" })

    assert.match(mainContent.textContent, /API não encontrada neste servidor/)

    teardownDom()
})

test("withError renders backend unavailable hint for 503 on /api paths", async () => {
    setupDom()
    const { withError } = await importFresh("../../error/renderError.js")

    const mainContent = document.createElement("div")
    withError(mainContent, { status: 503, message: "API indisponivel ou servidor desligado. URL: http://localhost:8081/api/session/login" })

    assert.match(mainContent.textContent, /backend/i)
    assert.match(mainContent.textContent, /servidor da API esta a correr/i)

    teardownDom()
})
