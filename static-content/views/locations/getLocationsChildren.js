import {hasAdminAccess, runAsync} from "../../utis/index.js";
import {fetchJson} from "../../api/fetchJson.js";
import {buildUrl} from "../../api/buildUrl.js";
import {buildPage, createJsonPre, createLinkedOrEmpty, replaceMain} from "../../ui/pageComponents.js";
import {getDashboard} from "../dashboard.js";

export function getLocationChildrenAll(mainContent, params = {}) {
    getLocationSublist(mainContent, params, "childrenAll", "Children All")
}

export function getLocationChildrenDirect(mainContent, params = {}) {
    getLocationSublist(mainContent, params, "childrenDirect", "Children Direct")
}

function getLocationSublist(mainContent, params, endpointSuffix, titleSuffix) {
    runAsync(
        mainContent,
        async () => {
            const hasAccess = hasAdminAccess()
            if (!hasAccess) {
                getDashboard(mainContent)
            }
            const lid = params.lid
            const data = await fetchJson(buildUrl(`/locations/${encodeURIComponent(lid)}/${endpointSuffix}`))
            const items = Array.isArray(data) ? data : []
            replaceMain(
                mainContent,
                buildPage(
                    `Location ${lid} - ${titleSuffix}`,
                    createLinkedOrEmpty(
                        items,
                        "Sem resultados.",
                        item => `#locations/${encodeURIComponent(item.id)}`,
                        item => `${item.name} (${item.type})`,
                    ),
                    createJsonPre(data),
                ),
            )
        },
        "A carregar locations...",
    )
}