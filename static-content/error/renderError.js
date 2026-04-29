import { createAlert, div, replaceMain } from "../ui/pageComponents.js"

function shouldShowApiOriginHint(error) {
    if (!error || error.status !== 404) return false
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
                ? createAlert("Sessão inválida. Recarrega a página para restaurar a sessão demo.", "warning")
                : null,
            shouldShowApiOriginHint(error)
                ? createAlert(
                    "API não encontrada neste servidor. Usa a app pelo backend ou adiciona ?apiBase=http://localhost:18080/api.",
                    "warning",
                )
                : null,
        ),
    )
}

export { withError }
