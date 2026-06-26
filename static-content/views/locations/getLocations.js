import {hasAdminAccess, runAsync} from "../../utis/index.js";
import {fetchJson} from "../../api/fetchJson.js";
import {buildUrl} from "../../api/buildUrl.js";
import {buildPage, createLinkedOrEmpty, replaceMain} from "../../ui/pageComponents.js";
import {normalizeLocationsPayload} from "./locations.js";
import {createLocationForm} from "./locationForm.js";
import {getDashboard} from "../dashboard.js";

export function getLocations(mainContent) {
    runAsync(
        mainContent,
        async () => {
            const hasAccess = hasAdminAccess()
            if (!hasAccess) {
                getDashboard(mainContent)
            }


            const data = await fetchJson(buildUrl("/locations"))
            const locations = normalizeLocationsPayload(data)
            replaceMain(
                mainContent,
                buildPage(
                    "Locations",
                    createLocationForm(mainContent),
                    createLinkedOrEmpty(
                        locations,
                        "Sem locations.",
                        location => `#locations/${encodeURIComponent(location.id)}`,
                        location => `${location.name} (${location.type})`,
                    ),
                ),
            )
        },
        "A carregar locations...",
    )
}