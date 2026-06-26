import {hasAdminAccess, runAsync} from "../../utis/index.js";
import {fetchJson} from "../../api/fetchJson.js";
import {buildUrl} from "../../api/buildUrl.js";
import {buildPage, createJsonPre, createLinkedOrEmpty, replaceMain} from "../../ui/pageComponents.js";
import {getDashboard} from "../dashboard.js";

export function getLocationPath(mainContent, params = {}) {
    runAsync(
        mainContent,
        async () => {
            const hasAccess = hasAdminAccess()
            if (!hasAccess) {
                getDashboard(mainContent)
            }
            const lid = params.lid
            const path = await fetchJson(buildUrl(`/locations/${encodeURIComponent(lid)}/path`))
            const items = Array.isArray(path) ? path : []
            replaceMain(
                mainContent,
                buildPage(
                    `Location ${lid} - Path`,
                    createLinkedOrEmpty(
                        items,
                        "Sem entradas no path.",
                        item => `#locations/${encodeURIComponent(item.id)}`,
                        item => `${item.name} (${item.type})`,
                    ),
                    createJsonPre(path),
                ),
            )
        },
        "A carregar path...",
    )
}