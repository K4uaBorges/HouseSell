import { withError } from "../error/renderError.js"
import { createAlert, div, replaceMain } from "../ui/pageComponents.js"

function withLoading(mainContent, loadingLabel = "A carregar...") {
    replaceMain(mainContent, div(createAlert(loadingLabel, "secondary")))
}

function runAsync(mainContent, task, loadingLabel = "A carregar...") {
    withLoading(mainContent, loadingLabel)
    task().catch(error => withError(mainContent, error))
}

export { runAsync, withLoading }
