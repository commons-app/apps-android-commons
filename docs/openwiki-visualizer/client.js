import { colorsForTypes, escapeHtml, hexA, matchesFilter, nodeRadius, normalize, shouldShowNodeLabel, signature, stripFrontmatter, } from "./client-lib.js";
// --- Module state -----------------------------------------------------------
/**
 * The full wiki graph as last fetched from /api/graph.
 */
let graph = {
    root: "",
    generatedAt: "",
    types: [],
    nodes: [],
    edges: [],
};
/**
 * Node fill color per page type, rebuilt on each load.
 */
let colorForType = {};
/**
 * The live force-graph instance, or null before the first render.
 */
let G = null;
/**
 * Id of the selected node (drives the highlight), or null when nothing is selected.
 */
let current = null;
/**
 * Id of the page shown in the reader and marked active in the index, or null.
 */
let readerId = null;
/**
 * Id of the node under the pointer, used only for contextual label display.
 */
let hoverId = null;
/**
 * Id of the entry page, drawn larger and always labelled, or null before load.
 */
let anchorId = null;
/**
 * Topology signature of the last render, used to skip redundant re-layouts.
 */
let lastSig = "";
/**
 * Active search text. Reserved for a future search box; "" means "match all".
 */
const filterQ = "";
/**
 * Active type filter. Reserved for a future filter UI; "" means "match all".
 */
const filterType = "";
/** True when this client was emitted as a static visualizer export. */
const isStaticExport = document.documentElement.dataset.staticExport === "true";
/** Static exports carry their graph beside the client; live mode uses the server route. */
const graphUrl = isStaticExport ? "./graph.json" : "/api/graph";
/**
 * Persisted render-node objects keyed by id, reused across reloads so layout holds.
 */
const nodeById = new Map();
/**
 * Nodes currently emphasised (the selection/hover neighbourhood).
 */
const highlightNodes = new Set();
/**
 * Links currently emphasised (edges within the highlighted neighbourhood).
 */
const highlightLinks = new Set();
// --- DOM and theme helpers --------------------------------------------------
/**
 * Query one element by selector, typed as HTMLElement (every target here exists).
 */
const $ = (sel) => document.querySelector(sel);
/**
 * Look up a wiki node by id, or undefined when it is not in the current graph.
 */
const byId = (id) => graph.nodes.find((n) => n.id === id);
/**
 * Read a CSS custom property off the body, so colors follow the active theme.
 */
const cssVar = (name) => getComputedStyle(document.body).getPropertyValue(name).trim();
/**
 * The current node-label text color.
 */
const labelColor = () => cssVar("--node-label");
/**
 * The current edge (link) color.
 */
const edgeColor = () => cssVar("--edge");
/**
 * The current graph-canvas background color.
 */
const graphBg = () => cssVar("--graph-bg");
/**
 * Whether a node is the entry (anchor) page.
 */
const isAnchor = (n) => n.id === anchorId;
/**
 * Whether a node survives the active search text and type filter.
 */
const passesFilter = (n) => matchesFilter(n, filterQ, filterType);
// --- Legend and sidebar (the page index) ------------------------------------
/**
 * Recompute the per-type node colors for the current graph.
 */
function assignColors() {
    colorForType = colorsForTypes(graph.types);
}
/**
 * Render the type/color legend.
 */
function buildLegend() {
    $("#legend").innerHTML = graph.types
        .map((t) => `<div class="item"><span class="swatch" style="background:${colorForType[t]}"></span>${escapeHtml(t)}</div>`)
        .join("");
}
/**
 * The whole wiki as a browsable index: pages grouped by type, always visible.
 * All wiki-sourced text goes through escapeHtml; colors come from our palette.
 */
function buildSidebar() {
    const groups = graph.types
        .map((t) => ({
        t,
        items: graph.nodes
            .filter((n) => n.type === t)
            .sort((a, b) => a.title.localeCompare(b.title)),
    }))
        .filter((g) => g.items.length);
    const head = `<div class="sb-head"><span class="sb-title">Pages</span>` +
        `<span class="sb-count">${graph.nodes.length}</span></div>`;
    const body = groups
        .map((g) => {
        const items = g.items
            .map((n) => `<button class="nav-item" data-id="${escapeHtml(n.id)}">` +
            `<span class="dot" style="background:${colorForType[g.t]}"></span>` +
            `<span class="nm" title="${escapeHtml(n.title)}">${escapeHtml(n.title)}</span></button>`)
            .join("");
        return (`<div class="sb-group"><div class="sb-group-head">` +
            `<span class="swatch" style="background:${colorForType[g.t]}"></span>${escapeHtml(g.t)}</div>` +
            items +
            `</div>`);
    })
        .join("");
    $("#sidebar").innerHTML = head + body;
    $("#sidebar")
        .querySelectorAll(".nav-item")
        .forEach((b) => b.addEventListener("click", () => {
        const id = b.dataset.id;
        if (id)
            selectNode(id);
    }));
    refreshSidebarActive();
    refreshSidebarFilter();
}
/**
 * Highlight the page open in the reader and scroll it into view in the index.
 */
function refreshSidebarActive() {
    $("#sidebar")
        .querySelectorAll(".nav-item")
        .forEach((b) => {
        const on = b.dataset.id === readerId;
        b.classList.toggle("active", on);
        if (on)
            b.scrollIntoView({ block: "nearest" });
    });
}
/**
 * Hide index rows (and emptied groups) that the search/type filter excludes.
 */
function refreshSidebarFilter() {
    $("#sidebar")
        .querySelectorAll(".nav-item")
        .forEach((b) => {
        const n = byId(b.dataset.id ?? "");
        b.classList.toggle("hidden", !n || !passesFilter(n));
    });
    $("#sidebar")
        .querySelectorAll(".sb-group")
        .forEach((g) => {
        const anyVisible = [...g.querySelectorAll(".nav-item")].some((b) => !b.classList.contains("hidden"));
        g.classList.toggle("hidden", !anyVisible);
    });
}
// --- Graph canvas -----------------------------------------------------------
/**
 * Create the force-graph instance and wire its paint, link, and interaction
 * callbacks, then pin the canvas to its column and loosen the charge force.
 */
function initGraph() {
    const container = $("#graph");
    G = ForceGraph()(container)
        .backgroundColor(graphBg())
        .nodeRelSize(4)
        .nodeCanvasObjectMode(() => "replace")
        .nodeCanvasObject(paintNode)
        .nodePointerAreaPaint((n, color, ctx) => {
        // Hit area matches the drawn circle so clicks/hover line up with the glow.
        ctx.fillStyle = color;
        ctx.beginPath();
        ctx.arc(n.x ?? 0, n.y ?? 0, n.r + 3, 0, 2 * Math.PI);
        ctx.fill();
    })
        .linkColor((l) => {
        if (isLinkDimmed(l))
            return hexA(edgeColor(), 0.05);
        return highlightLinks.has(l) ? "#7FC8FF" : hexA(edgeColor(), 0.7);
    })
        .linkWidth((l) => (highlightLinks.has(l) ? 2 : 0.7))
        .linkCurvature(0.12)
        .linkDirectionalParticles((l) => isLinkDimmed(l) ? 0 : highlightLinks.has(l) ? 4 : 2)
        .linkDirectionalParticleWidth((l) => (highlightLinks.has(l) ? 2.6 : 1.7))
        .linkDirectionalParticleSpeed(0.006)
        .linkDirectionalParticleColor(() => "#7FC8FF")
        .onNodeClick((n) => selectNode(n.id))
        .onNodeHover(hoverHighlight);
    // Deliberately NO onBackgroundClick handler: clicking empty graph space must
    // not change any page state (issue #670). Clearing the selection/reader on a
    // stray click made the open page vanish, so background clicks are a no-op.
    // Pin the canvas to its column. Without this, force-graph falls back to the
    // window width and centres the graph behind the reader/index panels.
    const fitSize = () => {
        if (!G)
            return;
        // Setting a canvas width clears its pixels. ResizeObserver runs late in the
        // browser's paint cycle, so leaving the normal animation loop in charge can
        // expose that cleared frame while the splitter is moving. Restarting the
        // loop makes force-graph repaint synchronously at the new size.
        G.pauseAnimation()
            .width(container.clientWidth)
            .height(container.clientHeight)
            .resumeAnimation();
    };
    fitSize();
    new ResizeObserver(fitSize).observe(container);
    // A little breathing room so nodes settle apart instead of clumping.
    G.d3Force("charge").strength(-140);
}
/**
 * Obsidian-style node: a soft colored glow, a solid core, and a legible label.
 */
function paintNode(n, ctx, scale) {
    if (n.x === undefined || n.y === undefined)
        return;
    const dim = !passesFilter(n);
    const sel = n.id === current;
    const hot = highlightNodes.has(n);
    const r = n.r;
    const base = dim ? 0.12 : 1;
    // Glow halo (skipped when dimmed so filtered-out nodes recede).
    if (!dim) {
        const gr = r * (sel ? 3.4 : hot ? 2.8 : 2.2);
        const glow = ctx.createRadialGradient(n.x, n.y, r * 0.5, n.x, n.y, gr);
        glow.addColorStop(0, hexA(n.color, sel ? 0.5 : hot ? 0.38 : 0.22));
        glow.addColorStop(1, hexA(n.color, 0));
        ctx.fillStyle = glow;
        ctx.beginPath();
        ctx.arc(n.x, n.y, gr, 0, 2 * Math.PI);
        ctx.fill();
    }
    // Solid core.
    ctx.globalAlpha = base;
    ctx.beginPath();
    ctx.arc(n.x, n.y, r, 0, 2 * Math.PI);
    ctx.fillStyle = sel ? "#FFFFFF" : n.color;
    ctx.fill();
    if (sel || hot) {
        ctx.lineWidth = 1.6 / scale;
        ctx.strokeStyle = hexA("#FFFFFF", 0.92);
        ctx.stroke();
    }
    // Label: contextual only. The sidebar is the always-visible index; the graph
    // labels the current interaction target or selected neighbourhood.
    if (shouldShowNodeLabel(n, {
        selectedId: current,
        hoveredId: hoverId,
        isInSelectedNeighborhood: hot,
    })) {
        const fs = Math.max(10 / scale, 3.2);
        ctx.font = `${sel || n.anchor ? 700 : 600} ${fs}px Inter, sans-serif`;
        ctx.textAlign = "center";
        ctx.textBaseline = "top";
        const y = n.y + r + 2.5 / scale;
        ctx.globalAlpha = dim ? 0.25 : 1;
        ctx.lineWidth = 3.5 / scale;
        ctx.strokeStyle = hexA(graphBg(), 0.9);
        ctx.strokeText(n.title, n.x, y);
        ctx.fillStyle = sel ? "#FFFFFF" : labelColor();
        ctx.fillText(n.title, n.x, y);
    }
    ctx.globalAlpha = 1;
}
/**
 * Whether a link should recede because the active filter excludes an endpoint.
 */
function isLinkDimmed(l) {
    if (!filterQ && !filterType)
        return false;
    return !passesFilter(l.source) || !passesFilter(l.target);
}
/**
 * Fill the highlight sets with a node and its immediate neighbourhood.
 */
function neighborsOf(node) {
    highlightNodes.clear();
    highlightLinks.clear();
    if (!node || !G)
        return;
    highlightNodes.add(node);
    G.graphData().links.forEach((l) => {
        if (l.source === node || l.target === node) {
            highlightLinks.add(l);
            highlightNodes.add(l.source);
            highlightNodes.add(l.target);
        }
    });
}
/**
 * Highlight a node's neighbourhood on hover, unless a page is already selected.
 */
function hoverHighlight(node) {
    $("#graph").style.cursor = node ? "pointer" : "";
    if (current)
        return; // a selected page keeps its own highlight
    hoverId = node?.id ?? null;
    neighborsOf(node ?? undefined);
}
// --- Selection and reader ---------------------------------------------------
/**
 * Select a node: highlight its neighbourhood and open it in the reader.
 *
 * Intentionally does NOT move the camera: clicking a node to read it should
 * never yank the graph out from under you. Pan/zoom stay where you left them.
 */
function selectNode(id) {
    current = id;
    hoverId = null;
    neighborsOf(nodeById.get(id));
    renderReader(id);
}
/**
 * Render the reader panel for a page. DOM-only: never moves the camera.
 */
function renderReader(id) {
    const n = byId(id);
    if (!n)
        return;
    readerId = id;
    const tags = n.tags.length
        ? `<div class="tags">${n.tags.map((t) => `<span class="tag">${escapeHtml(t)}</span>`).join("")}</div>`
        : "";
    const desc = n.description
        ? `<p class="desc">${escapeHtml(n.description)}</p>`
        : "";
    const backEls = n.backlinks
        .map((b) => {
        const t = byId(b);
        return t
            ? `<span class="chip" data-id="${b}">${escapeHtml(t.title)}</span>`
            : "";
    })
        .join("");
    const back = n.backlinks.length
        ? `<div class="backlinks"><span class="eyebrow">Referenced by</span>${backEls}</div>`
        : "";
    // The page body is rendered as markdown, and marked passes raw HTML through, so
    // sanitize before innerHTML: DOMPurify strips scripts, event handlers, and unsafe
    // URL schemes. This is defense in depth on top of the server's CSP.
    const html = DOMPurify.sanitize(marked.parse(stripFrontmatter(n.body)));
    $("#detail").innerHTML =
        `<div class="eyebrow">${escapeHtml(n.type)}</div>` +
            `<h1 class="doc-title">${escapeHtml(n.title)}</h1>` +
            desc +
            tags +
            `<hr class="rule" />` +
            `<div class="md">${html}</div>` +
            back;
    rewriteLinks(n);
    renderMermaid();
    $("#detail").scrollTop = 0;
    $("#detail")
        .querySelectorAll(".chip")
        .forEach((c) => c.addEventListener("click", () => {
        const id = c.dataset.id;
        if (id)
            selectNode(id);
    }));
    refreshSidebarActive();
}
/**
 * Turn in-page markdown links that resolve to a node into in-app navigation.
 */
function rewriteLinks(node) {
    const dir = node.id.includes("/")
        ? node.id.slice(0, node.id.lastIndexOf("/"))
        : "";
    $("#detail")
        .querySelectorAll(".md a")
        .forEach((a) => {
        const href = a.getAttribute("href") ?? "";
        if (!href.endsWith(".md") && !href.includes(".md#"))
            return;
        const clean = href.split("#")[0];
        const target = normalize(dir, clean).replace(/\.md$/, "");
        if (byId(target)) {
            a.classList.add("wikilink");
            a.addEventListener("click", (e) => {
                e.preventDefault();
                selectNode(target);
            });
        }
    });
}
/**
 * Upgrade fenced mermaid code blocks in the reader into rendered diagrams.
 */
function renderMermaid() {
    const blocks = $("#detail").querySelectorAll("code.language-mermaid");
    let i = 0;
    blocks.forEach((code) => {
        const pre = document.createElement("pre");
        pre.className = "mermaid";
        pre.textContent = code.textContent;
        code.closest("pre")?.replaceWith(pre);
        i++;
    });
    if (i > 0) {
        try {
            mermaid.run({ nodes: $("#detail").querySelectorAll(".mermaid") });
        }
        catch {
            // Diagram render failures are non-fatal; leave the source block in place.
        }
    }
}
// --- Theme ------------------------------------------------------------------
/**
 * Toggle light/dark theme, re-theming mermaid, the canvas, and the open page.
 */
function toggleTheme() {
    const root = document.documentElement;
    const next = root.getAttribute("data-theme") === "dark" ? "light" : "dark";
    root.setAttribute("data-theme", next);
    mermaid.initialize({
        startOnLoad: false,
        theme: next === "dark" ? "dark" : "neutral",
    });
    if (G)
        G.backgroundColor(graphBg()); // node/label colors are read live each frame
    if (current)
        renderReader(current);
}
// --- Data load and live reload ----------------------------------------------
/**
 * Fetch the graph and (re)render everything. Preserves the layout and viewport
 * when the topology is unchanged, so a live reload does not snap the graph.
 */
async function load(firstTime) {
    const res = await fetch(graphUrl);
    graph = (await res.json());
    $("#wiki-name").textContent = `${graph.root} · ${graph.nodes.length} pages`;
    const entry = graph.nodes.find((n) => /quickstart/i.test(n.id)) ??
        graph.nodes.find((n) => /(^|\/)(index|overview|home)$/i.test(n.id)) ??
        graph.nodes[0];
    anchorId = entry ? entry.id : null;
    assignColors();
    buildLegend();
    buildSidebar();
    const sig = signature(graph);
    if (firstTime) {
        initGraph();
        if (G) {
            G.graphData(buildGraphData());
            // Start a little more zoomed-in than force-graph's node-count default
            // (4/∛n). Setting our own zoom also suppresses that default, which only
            // auto-applies while the zoom is still untouched, so this value sticks.
            G.zoom((4 / Math.cbrt(graph.nodes.length || 1)) * 1.35);
        }
        lastSig = sig;
    }
    else if (sig !== lastSig) {
        // Topology changed: re-feed data, reusing persisted node objects so the
        // layout and viewport stay put instead of snapping.
        if (G)
            G.graphData(buildGraphData());
        lastSig = sig;
    }
    // else: identical topology -> leave the graph and viewport untouched entirely.
    if (current && byId(current))
        renderReader(current);
    else if (firstTime && anchorId)
        renderReader(anchorId);
}
/**
 * Build the force-graph payload, reusing node objects across reloads so
 * positions and camera survive a refresh.
 */
function buildGraphData() {
    const ids = new Set(graph.nodes.map((n) => n.id));
    for (const id of [...nodeById.keys()])
        if (!ids.has(id))
            nodeById.delete(id);
    const nodes = graph.nodes.map((n) => {
        let o = nodeById.get(n.id);
        if (!o) {
            o = {
                id: n.id,
                title: n.title,
                type: n.type,
                size: n.size,
                tags: n.tags,
                description: n.description,
                color: "#7FC8FF",
                anchor: false,
                r: 4,
            };
            nodeById.set(n.id, o);
        }
        o.title = n.title;
        o.type = n.type;
        o.size = n.size;
        o.tags = n.tags;
        o.description = n.description;
        o.color = colorForType[n.type] || "#7FC8FF";
        o.anchor = isAnchor(n);
        o.r = nodeRadius(n.size, isAnchor(n));
        return o;
    });
    // force-graph resolves these string endpoints to node objects in place.
    const links = graph.edges.map((e) => ({
        source: e.source,
        target: e.target,
    }));
    return { nodes, links };
}
/**
 * Show a transient status toast.
 */
function toast(msg) {
    const t = $("#toast");
    t.textContent = msg;
    t.classList.add("show");
    setTimeout(() => t.classList.remove("show"), 1800);
}
/**
 * Subscribe to server-sent reload events and track the live/stale indicator.
 */
function connectSSE() {
    const es = new EventSource("/events");
    es.addEventListener("reload", () => {
        void load(false).then(() => toast("Wiki updated"));
    });
    es.onerror = () => {
        $("#live").classList.add("stale");
        $("#live-text").textContent = "Reconnecting";
    };
    es.onopen = () => {
        $("#live").classList.remove("stale");
        $("#live-text").textContent = "Live";
    };
}
// --- Graph panel: resizable splitter + collapse-to-fullscreen-doc toggle ----
/**
 * Lets the graph column be resized by dragging, or collapsed entirely so the
 * reader takes the full width. Width is stored as a percentage of the main
 * row (not raw pixels) so it keeps behaving sanely across window resizes, and
 * persists in localStorage across reloads.
 */
function initGraphPanelControls() {
    const mainEl = $("#main");
    const graphEl = $("#graph");
    const splitterEl = $("#splitter");
    const toggleBtn = $("#toggle-graph");
    const overlayEl = $("#graph-overlay");
    const WIDTH_KEY = "openwiki-graph-width";
    const COLLAPSED_KEY = "openwiki-graph-collapsed";
    let lastWidth = localStorage.getItem(WIDTH_KEY) || "50%";
    let collapsed = localStorage.getItem(COLLAPSED_KEY) === "1";
    function setCollapsed(next) {
        collapsed = next;
        localStorage.setItem(COLLAPSED_KEY, collapsed ? "1" : "0");
        graphEl.style.width = collapsed ? "0px" : lastWidth;
        splitterEl.classList.toggle("hidden", collapsed);
        overlayEl.style.display = collapsed ? "none" : "";
        toggleBtn.classList.toggle("active", collapsed);
        toggleBtn.title = collapsed ? "Show graph" : "Hide graph";
    }
    setCollapsed(collapsed);
    toggleBtn.addEventListener("click", () => setCollapsed(!collapsed));
    splitterEl.addEventListener("dblclick", () => setCollapsed(!collapsed));
    let dragging = false;
    splitterEl.addEventListener("mousedown", (e) => {
        if (collapsed)
            return;
        dragging = true;
        splitterEl.classList.add("dragging");
        mainEl.classList.add("dragging");
        e.preventDefault();
    });
    window.addEventListener("mousemove", (e) => {
        if (!dragging)
            return;
        const mainRect = mainEl.getBoundingClientRect();
        const sidebarWidth = $("#sidebar").getBoundingClientRect().width;
        const avail = mainRect.width - sidebarWidth;
        const minPx = 80;
        const maxPx = Math.max(minPx, avail - 260); // keep room for the reader
        let px = e.clientX - mainRect.left - sidebarWidth;
        px = Math.max(minPx, Math.min(px, maxPx));
        // % is resolved by the browser against the flex container's full width
        // (mainRect.width, sidebar included), not just the space left after the
        // sidebar — divide by the same basis or the graph silently overshoots
        // into the reader's space as the sidebar eats a bigger share of the row.
        lastWidth = `${((px / mainRect.width) * 100).toFixed(2)}%`;
        graphEl.style.width = lastWidth;
    });
    window.addEventListener("mouseup", () => {
        if (!dragging)
            return;
        dragging = false;
        splitterEl.classList.remove("dragging");
        mainEl.classList.remove("dragging");
        localStorage.setItem(WIDTH_KEY, lastWidth);
    });
}
// --- Bootstrap --------------------------------------------------------------
// Wire the theme toggle and load the graph. Only the live server supplies SSE reloads.
$("#theme").addEventListener("click", toggleTheme);
initGraphPanelControls();
mermaid.initialize({ startOnLoad: false, theme: "dark" });
marked.setOptions({ breaks: false, gfm: true });
void load(true).then(() => {
    if (!isStaticExport)
        connectSSE();
});
