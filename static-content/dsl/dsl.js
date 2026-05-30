function isNode(value) {
    return value instanceof Node
}

function isPropsObject(value) {
    return (
        value !== null &&
        typeof value === "object" &&
        !Array.isArray(value) &&
        !isNode(value)
    )
}

function appendChildSafe(element, child) {
    if (child === null || child === undefined || child === false) return

    if (Array.isArray(child)) {
        for (const nested of child) appendChildSafe(element, nested)
        return
    }

    if (isNode(child)) {
        element.appendChild(child)
        return
    }

    if (typeof child === "string" || typeof child === "number") {
        element.appendChild(document.createTextNode(String(child)))
    }
}

function setStyle(element, styleObject) {
    if (!isPropsObject(styleObject)) return
    for (const [key, value] of Object.entries(styleObject)) {
        if (value === null || value === undefined) continue
        element.style[key] = String(value)
    }
}

function setAriaAttributes(element, ariaObject) {
    if (!isPropsObject(ariaObject)) return
    for (const [key, value] of Object.entries(ariaObject)) {
        if (value === null || value === undefined || value === false) continue
        element.setAttribute(`aria-${key}`, String(value))
    }
}

function setDataset(element, datasetObject) {
    if (!isPropsObject(datasetObject)) return
    for (const [key, value] of Object.entries(datasetObject)) {
        if (value === null || value === undefined || value === false) continue
        element.dataset[key] = String(value)
    }
}

function setProp(element, key, value) {
    switch (true) {
        case value === null || value === undefined || value === false:
            return
        case key === "style":
            setStyle(element, value)
            return
        case key === "class" || key === "className":
            element.className = String(value)
            return
        case key === "dataset":
            setDataset(element, value)
            return
        case key === "aria":
            setAriaAttributes(element, value)
            return
        case key.startsWith("on") && typeof value === "function":
            element.addEventListener(key.slice(2).toLowerCase(), value)
            return
        case key === "value":
            element.value = value
            return
        case key === "checked":
            element.checked = Boolean(value)
            return
        case value === true:
            element.setAttribute(key, "")
            return
        case (key.startsWith("data-") || key.startsWith("aria-")) && typeof value !== "object":
            element.setAttribute(key, String(value))
            return
        case key in element && typeof value !== "object":
            try {
                element[key] = value
                return
            } catch {
                element.setAttribute(key, String(value))
                return
            }
        default:
            element.setAttribute(key, String(value))
            return
    }
}

export function el(tagName, props, ...children) {
    const element = document.createElement(tagName)
    let normalizedChildren = children

    if (isPropsObject(props)) {
        for (const [key, value] of Object.entries(props)) {
            setProp(element, key, value)
        }
    } else {
        normalizedChildren = [props, ...children]
    }

    for (const child of normalizedChildren) {
        appendChildSafe(element, child)
    }

    return element
}

const tag =
    tagName =>
    (props, ...children) =>
        el(tagName, props, ...children)

// Bloco container generico para agrupar elementos.
export const div = tag("div")

// Titulo de secao de nivel 2.
export const h2 = tag("h2")

// Titulo principal da pagina.
export const h1 = tag("h1")

// Paragrafo para texto corrido.
export const p = tag("p")

// Link de navegacao para outra rota ou URL.
export const a = tag("a")

// Botao para acionar acoes (click, submit, etc.).
export const button = tag("button")

// Campo de entrada de dados em formularios.
export const input = tag("input")

// Lista de selecao.
export const select = tag("select")

// Opcao dentro de select.
export const option = tag("option")

// Lista nao ordenada para colecoes de itens.
export const ul = tag("ul")

// Item individual dentro de uma lista.
export const li = tag("li")

// Bloco de texto pre-formatado (mantem espacos e quebras).
export const pre = tag("pre")

// Rotulo associado a um campo de formulario.
export const label = tag("label")

// Formulario para agrupar campos e submeter dados.
export const form = tag("form")
