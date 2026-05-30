import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("getLocations blocks non-admin users in the SPA", async () => {
    setupDom()
    localStorage.setItem("houses.auth.session", JSON.stringify({
        token: "user-token",
        id: "u1",
        name: "User",
        email: "user@example.com",
        role: "",
    }))

    const { getLocations } = await importFresh("../../handlers/locations.js")
    const mainContent = document.createElement("div")

    getLocations(mainContent)

    assert.match(mainContent.textContent, /Apenas administradores podem aceder a locations/i)

    teardownDom()
})

test("getHouseById hides owner actions for non-owner and shows unavailable intervals", async () => {
    setupDom()
    localStorage.setItem("houses.auth.session", JSON.stringify({
        token: "user-token",
        id: "viewer-1",
        name: "Viewer",
        email: "viewer@example.com",
        role: "",
    }))
    localStorage.setItem("houses.auth.token", "user-token")

    globalThis.fetch = async url => {
        const value = String(url)

        if (value.includes("/api/houses/house-1/available-days")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({
                    houseId: "house-1",
                    year: 2026,
                    month: 6,
                    availableDays: ["2026-06-01", "2026-06-02", "2026-06-05"],
                }),
            }
        }

        if (value.includes("/api/houses/available?")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({
                    houses: [],
                }),
            }
        }

        if (value.includes("/api/houses/preview?")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({
                    areaSqMt: 90,
                    predictedPricePerNight: 120,
                }),
            }
        }

        if (value.includes("/api/locations")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({
                    locations: [{ id: "loc-1", name: "Lisboa", type: "CITY" }],
                }),
            }
        }

        if (value.includes("/api/houses/location-options")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({
                    locations: [{ id: "loc-1", name: "Lisboa", type: "CITY" }],
                }),
            }
        }

        if (value.includes("/api/houses/house-1")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({
                    id: "house-1",
                    uid: "owner-1",
                    title: "Casa Azul",
                    lid: "loc-1",
                    locationName: "Lisboa",
                    locationType: "CITY",
                    areaSqMt: 90,
                    pricePerNight: 80,
                    description: "Perto da praia",
                }),
            }
        }

        throw new Error(`Unexpected fetch: ${value}`)
    }

    const { getHouseById } = await importFresh("../../handlers/houses.js")
    const mainContent = document.createElement("div")

    getHouseById(mainContent, { hid: "house-1" }, { startDate: "2026-06-01", endDate: "2026-06-03" })
    await new Promise(resolve => setTimeout(resolve, 0))

    assert.match(mainContent.textContent, /A casa não está disponível nesse intervalo/i)
    assert.match(mainContent.textContent, /Intervalos indisponíveis neste mês/i)
    assert.doesNotMatch(mainContent.textContent, /Atualizar House/i)
    assert.doesNotMatch(mainContent.textContent, /Remover House/i)

    teardownDom()
})

test("getHouses shows linear machine for location search and hides it when max price is used", async () => {
    setupDom()
    localStorage.setItem("houses.auth.token", "user-token")
    localStorage.setItem("houses.auth.session", JSON.stringify({
        token: "user-token",
        id: "u1",
        name: "User",
        email: "user@example.com",
        role: "USER",
    }))

    globalThis.fetch = async url => {
        const value = String(url)

        if (value.includes("/api/houses/available?")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({
                    houses: [
                        {
                            id: "house-1",
                            title: "Casa Azul",
                            lid: "locality-1",
                            locationName: "Lisboa",
                            locationType: "LOCALITY",
                            pricePerNight: 80,
                            areaSqMt: 90,
                            description: "A",
                        },
                        {
                            id: "house-2",
                            title: "Casa Verde",
                            lid: "locality-1",
                            locationName: "Lisboa",
                            locationType: "LOCALITY",
                            pricePerNight: 120,
                            areaSqMt: 100,
                            description: "B",
                        },
                    ],
                }),
            }
        }

        if (value.includes("/api/houses/location-options")) {
            return {
                ok: true,
                status: 200,
                text: async () => JSON.stringify({
                    locations: [
                        { id: "country-1", name: "Portugal", type: "COUNTRY", parentId: null },
                        { id: "region-1", name: "Continente", type: "REGION", parentId: "country-1" },
                        { id: "district-1", name: "Lisboa", type: "DISTRICT", parentId: "region-1" },
                        { id: "municipality-1", name: "Oeiras", type: "MUNICIPALITY", parentId: "district-1" },
                        { id: "locality-1", name: "Paço de Arcos", type: "LOCALITY", parentId: "municipality-1" },
                    ],
                }),
            }
        }

        throw new Error(`Unexpected fetch: ${value}`)
    }

    const { getHouses } = await importFresh("../../handlers/houses.js")
    const mainContent = document.createElement("div")

    getHouses(mainContent, {}, { districtId: "district-1", startDate: "2026-06-01", endDate: "2026-06-03" })
    await new Promise(resolve => setTimeout(resolve, 0))
    assert.match(mainContent.textContent, /Linear machine/i)
    assert.match(mainContent.textContent, /100\.00\/noite/)

    const selects = [...mainContent.querySelectorAll("select")]
    assert.equal(selects[1]?.disabled, true)
    selects[0].value = "country-1"
    selects[0].dispatchEvent(new window.Event("change"))
    assert.equal(selects[1]?.disabled, false)
    assert.equal(selects[2]?.disabled, true)

    getHouses(mainContent, {}, { districtId: "district-1", startDate: "2026-06-01", endDate: "2026-06-03", maxPrice: "90" })
    await new Promise(resolve => setTimeout(resolve, 0))
    assert.doesNotMatch(mainContent.textContent, /Linear machine/i)

    teardownDom()
})
