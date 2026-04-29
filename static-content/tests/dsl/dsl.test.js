import assert from "node:assert/strict"
import test from "node:test"
import { importFresh, setupDom, teardownDom } from "../helpers/testEnv.js"

test("dsl el applies props, dataset, aria and events", async () => {
    setupDom()
    const { el } = await importFresh("../../dsl/dsl.js")

    let clicked = false
    const node =
        el(
            "button",
            {
                class: "btn btn-primary",
                dataset: { testId: "submit-btn" },
                aria: { label: "Submit" },
                style: { color: "red" },
                onclick: () => {
                    clicked = true
                },
            },
            "Guardar",
        )

    node.dispatchEvent(new window.Event("click"))

    assert.equal(node.className, "btn btn-primary")
    assert.equal(node.dataset.testId, "submit-btn")
    assert.equal(node.getAttribute("aria-label"), "Submit")
    assert.equal(node.style.color, "red")
    assert.equal(node.textContent, "Guardar")
    assert.equal(clicked, true)

    teardownDom()
})

test("dsl supports array children and falsy values", async () => {
    setupDom()
    const { div } = await importFresh("../../dsl/dsl.js")

    const node = div(null, ["A", false, "B", [1, null, "C"]])
    assert.equal(node.textContent, "AB1C")

    teardownDom()
})
