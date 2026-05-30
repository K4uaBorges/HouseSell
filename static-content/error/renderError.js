import { createAlert, div, replaceMain } from "../ui/pageComponents.js"

function shouldShowApiOriginHint(error) {
    if (!error || error.status !== 404) return false
    const message = String(error.message || "")
    return message.includes("/api/")
}

function shouldShowApiUnavailableHint(error) {
    if (!error || error.status !== 503) return false
    const message = String(error.message || "")
    return message.includes("/api/")
}

function withError(mainContent, error) {
    const statusPrefix = error?.status ? `Erro ${error.status}: ` : "Erro: "
    const message = error?.message || "Falha ao processar pedido."
    const type = error?.status === 401 ? "warning" : "danger"
    replaceMain(
        mainContent,
        div(
            createAlert(`${statusPrefix}${message}`, type),
            error?.status === 401
                ? createAlert("Sessão inválida. Faz login novamente para continuar.", "warning")
                : null,
            shouldShowApiOriginHint(error)
                ? createAlert(
                    "API não encontrada neste servidor.",
                    "warning",
                )
                : null,
            shouldShowApiUnavailableHint(error)
                ? createAlert(
                    "Nao foi possivel ligar ao backend. Confirma que o servidor da API esta a correr.",
                    "warning",
                )
                : null,
        ),
    )
}

export { withError }
