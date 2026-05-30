import { div, input, label, option, select } from "../dsl/dsl.js"
import { buildUrl } from "../api/buildUrl.js"
import { fetchJson } from "../api/fetchJson.js"
import {normalizeLocationsPayload} from "./houses/housePayload.js";
import {readToken} from "../token/tokenStorage.js";

const LOCATION_TYPES = ["COUNTRY", "REGION", "DISTRICT", "MUNICIPALITY", "LOCALITY"]

// LOC UTILS ---------------------------------------------------------------//

function resolveParentIdFromKeyword(parentKeyword, locations) {
    const targetHash = locationNameHash(parentKeyword)
    if (!targetHash) return null

    const match =
        locations.find(location => locationNameHash(location?.name) === targetHash)
    return match?.id || null
}

// HOUSE UTILS ---------------------------------------------------------------//
export async function checkIsMine(hid) {
    const token = readToken()
    if (!token) return false
    const myHouses = await fetchJson(buildUrl("/houses/mine"), { auth: true, cache: "no-store" })
    if (Array.isArray(myHouses) ) {
        if (myHouses.any(house => house.id === hid)) return true
    }
    return false
}

function sameLocationType(location, expectedType) {
      return String(location?.type || "").toUpperCase() === expectedType
}

function locationLabel(location) {
      return `${location.name} (${location.type})`
}

function hasLocationsOfType(locations = [], expectedType = "") {
      return locations.some(location => sameLocationType(location, expectedType))
}

function createLocationDropdowns(
    locations = [],
    {
        inputName = "lid",
        initialLocationId = "",
        maxType = "LOCALITY",
        required = true,
    } = {}
) {
    const maxTypeIndex = LOCATION_TYPES.includes(maxType)
        ? LOCATION_TYPES.indexOf(maxType)
        : LOCATION_TYPES.length - 1

    const activeTypes = LOCATION_TYPES.slice(0, maxTypeIndex + 1)
    let rootLocations = normalizeLocationsPayload(locations)
    const childrenCache = new Map()
    const requestTokens = new Array(activeTypes.length).fill(0)

    const leafLocationInput = input({ type: "hidden", name: inputName, required })
    const selects = activeTypes.map(type =>
        select(
            { class: "form-select", disabled: true },
            option({ value: "" }, `Seleciona ${type}`),
        ),
    )

    function setSelectState(level, placeholder, { disabled = true, options = [], selectedId = "" } = {}) {
      selects[level].replaceChildren(
          option({ value: "" }, placeholder),
          ...options.map(location =>
              option(
                  { value: location.id, selected: location.id === selectedId },
                  locationLabel(location),
              ),
          ),
      )
      selects[level].value = options.some(location => location.id === selectedId) ? selectedId : ""
      selects[level].disabled = disabled
    }

    function waitingLabel(level) {
      if (level === 0) return `Seleciona ${activeTypes[level]}`
      return `Seleciona ${activeTypes[level - 1]} primeiro`
    }

    function resetSelect(level) {
      setSelectState(level, waitingLabel(level))
    }

    function showEmpty(level) {
      setSelectState(level, `Sem ${activeTypes[level]} disponíveis`)
    }

    function resetAfter(level) {
      for (let index = level + 1; index < selects.length; index += 1) {
          resetSelect(index)
      }
    }

    function syncLeafLocation() {
      let deepestSelected = ""
      for (const selectInput of selects) {
          const selectedValue = String(selectInput.value || "").trim()
          if (!selectedValue) break
          deepestSelected = selectedValue
      }
      leafLocationInput.value = deepestSelected
    }

    function fillSelect(level, items, selectedId = "") {
      const expectedType = activeTypes[level]
      const typedItems = items.filter(location => sameLocationType(location, expectedType))

      if (!typedItems.length) {
          showEmpty(level)
          return
      }

      setSelectState(
          level,
          `Seleciona ${expectedType}`,
          {
              disabled: false,
              options: typedItems,
              selectedId,
          },
      )
    }

    function showLoading(level) {
      setSelectState(level, `A carregar ${activeTypes[level]}...`)
    }

    function showLoadError(level) {
      setSelectState(level, `Erro ao carregar ${activeTypes[level]}`)
    }

    async function loadRootLocations() {
      if (!rootLocations.length || !hasLocationsOfType(rootLocations, activeTypes[0])) {
          const data = await fetchJson(buildUrl("/locations", { limit: 100 }))
          rootLocations = normalizeLocationsPayload(data)
      }

      return rootLocations
    }

    async function loadChildren(parentId) {
      if (childrenCache.has(parentId)) return childrenCache.get(parentId)

      const data = await fetchJson(
          buildUrl(`/locations/${encodeURIComponent(parentId)}/childrenDirect`),
      )
      const children = normalizeLocationsPayload(data)
      childrenCache.set(parentId, children)
      return children
    }

    async function loadLevel(level, parentId = "", selectedId = "") {
      const token = requestTokens[level] + 1
      requestTokens[level] = token
      showLoading(level)

      try {
          const items = parentId
              ? await loadChildren(parentId)
              : await loadRootLocations()

          if (requestTokens[level] !== token) return

          fillSelect(level, items, selectedId)
          syncLeafLocation()
      } catch {
          if (requestTokens[level] !== token) return
          showLoadError(level)
          syncLeafLocation()
      }
    }

    async function loadInitialPath() {
      if (!initialLocationId) return []

      const data = await fetchJson(
          buildUrl(`/locations/${encodeURIComponent(initialLocationId)}/path`),
      )
      return normalizeLocationsPayload(data)
    }

    async function cascadeFrom(level) {
      resetAfter(level)
      syncLeafLocation()

      const nextLevel = level + 1
      if (nextLevel >= selects.length) return

      const parentId = String(selects[level].value || "").trim()
      if (!parentId) return

      await loadLevel(nextLevel, parentId)
    }

    async function initialize() {
      selects.forEach((selectInput, index) => {
          selectInput.addEventListener("change", () => {
              void cascadeFrom(index)
          })
      })

      try {
          const initialPath = await loadInitialPath()

          if (!initialPath.length) {
              await loadLevel(0)
              return
          }

          await loadLevel(0, "", initialPath[0]?.id || "")

          const maxInitialLevel = Math.min(initialPath.length - 1, selects.length - 1)
          for (let level = 1; level <= maxInitialLevel; level += 1) {
              await loadLevel(level, initialPath[level - 1].id, initialPath[level].id)
          }

          const lastSelectedLevel = maxInitialLevel
          const nextLevel = lastSelectedLevel + 1
          if (nextLevel < selects.length) {
              await loadLevel(nextLevel, initialPath[lastSelectedLevel].id)
          }

          syncLeafLocation()
      } catch {
          await loadLevel(0)
      }
    }

    void initialize()

    return {
      leafLocationInput,
      selectorFields: div(
          { class: "row g-2 mt-3" },
          ...selects.map((selectInput, index) =>
              div(
                  { class: "col-md-4" },
                  label({ class: "form-label" }, activeTypes[index]),
                  selectInput,
              ),
          ),
      ),
    }
}

export { LOCATION_TYPES, createLocationDropdowns, resolveParentIdFromKeyword}
