import { JSDOM } from "jsdom"

let activeDom = null

function setupDom({
    html = "<!doctype html><html><body></body></html>",
    url = "http://localhost/",
} = {}) {
    teardownDom()
    activeDom = new JSDOM(html, { url })
    const { window } = activeDom

    globalThis.window = window
    globalThis.document = window.document
    globalThis.Node = window.Node
    globalThis.Event = window.Event
    globalThis.CustomEvent = window.CustomEvent
    globalThis.localStorage = window.localStorage

    return window
}

function teardownDom() {
    if (activeDom) {
        activeDom.window.close()
        activeDom = null
    }

    try { delete globalThis.window } catch {}
    try { delete globalThis.document } catch {}
    try { delete globalThis.Node } catch {}
    try { delete globalThis.Event } catch {}
    try { delete globalThis.CustomEvent } catch {}
    try { delete globalThis.localStorage } catch {}
}

async function importFresh(relativePathFromHelper) {
    const fileUrl = new URL(relativePathFromHelper, import.meta.url)
    const nonce = `${Date.now()}-${Math.random().toString(16).slice(2)}`
    return import(`${fileUrl.href}?fresh=${nonce}`)
}

export { importFresh, setupDom, teardownDom }
