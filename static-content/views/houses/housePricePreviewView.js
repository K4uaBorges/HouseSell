import {
    buildHash,
    buildPage,
    buildUrl,
    createAlert,
    createJsonPre,
    createLinkList,
    fetchJson,
    replaceMain,
    runAsync,
} from "../../utis/index.js"

function getHousePricePreview(mainContent, _params = {}, query = {}) {
    const parsedArea = Number.parseInt(String(query.areaSqMt ?? "110").trim(), 10)
    const areaSqMt = Number.isInteger(parsedArea) && parsedArea > 0 ? parsedArea : 110
    const suggestedAreas = [40, 60, 80, 100, 120, 160, 220]

    runAsync(
        mainContent,
        async () => {
            const preview = await fetchJson(buildUrl("/houses/preview", { areaSqMt }))
            const links = suggestedAreas.map(area => ({
                href: buildHash("houses/preview", { areaSqMt: area }),
                text: `${area} m²`,
            }))

            replaceMain(
                mainContent,
                buildPage(
                    "Linear Preview",
                    createLinkList(links, item => item.href, item => item.text),
                    createJsonPre(preview),
                ),
            )
        },
        "A calcular previsão...",
    )
}

function getHouseCacheStats(mainContent) {
    runAsync(
        mainContent,
        async () => {
            const stats = await fetchJson(buildUrl("/houses/cache/stats"))
            replaceMain(
                mainContent,
                buildPage(
                    "House Cache Stats",
                    createAlert("Abre o detalhe da mesma house várias vezes para aumentar cache hits.", "secondary"),
                    createJsonPre(stats),
                ),
            )
        },
        "A carregar cache stats...",
    )
}

export { getHouseCacheStats, getHousePricePreview }