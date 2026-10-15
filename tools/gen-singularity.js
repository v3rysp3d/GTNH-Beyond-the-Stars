#!/usr/bin/env node
// gen-singularity.js - the Singularity Ladder questbook generator (docs/questbook-integration.md §4).
//
// ONE source of truth, questbook/singularity.json, becomes:
//   1. questbook/DefaultQuests/  our three ladder chapters (Tier 13 - UXV, Tier 14 - MAX, The Endgame),
//      our quests grafted into the pack's UIV and UMV chapters (ADDED files only), OrderLines.txt.
//      This is the mirror for new worlds and /bq_admin default load: an UNEDITED pack plus our content.
//   2. src/main/resources/assets/beyondthestars/questbook/overlay.json  the bundle the jar carries:
//      every chapter and quest of ours (the plant chapter too, read back from DefaultQuests) in
//      BetterQuesting's typed-key JSON, plus the compiled pack edits BQBridge applies to a live world.
//   3. src/main/java/com/nateyburr/beyondthestars/BTSQuestIds.java  every id we own, for /btsquests.
//   4. questbook/id-ledger.txt  append-only: an id once shipped can never change meaning or vanish.
//
// It runs as the LAST step of questbook/gen-questbook.ps1 (which wipes and rewrites DefaultQuests/
// for the plant chapter first), so the one build command stays questbook/gen-bom.ps1. It can also
// be run on its own:  node tools/gen-singularity.js [options]
//
//   --src <root>       addon source tree whose Java + structures/manifest.json are checked and
//                      counted (default: this repo). Registry names of OUR items, fluids and
//                      machines are read out of that tree's Java, never typed twice.
//   --pack <dir>       the GTNH pack's DefaultQuests (default: the RC-2 instance). Every pack edit
//                      is checked against it (id, name, chapter, every replaceText) - the offline
//                      drift check; drift is written to questbook/pack-drift.txt and fails the run.
//   --feature k=v      override one feature flag for this run (scenario tests); repeatable.
//   --check            write nothing; exit 1 if any generated file differs from what is on disk.
//   --selftest         break the data on purpose (overlap, line crossing, drifted name, unknown
//                      item, duplicate id, off-grid, ledger change, long bold run, low-contrast
//                      colour, odd glyph, non-empty hidden chapter, a pack quest still naming a
//                      hidden chapter) and prove each assertion fires.
//
// Every failure is collected and printed together, naming the offender; nothing is written
// unless the whole run is clean.
'use strict';

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const os = require('os');

const REPO = path.resolve(__dirname, '..');
const QB = path.join(REPO, 'questbook');
const DEFAULT_PACK = path.join(os.homedir(),
    'AppData/Roaming/PrismLauncher/instances/rc - 2 TO THE STARS/.minecraft/config/betterquesting/DefaultQuests');
const OVERLAY_REL = 'src/main/resources/assets/beyondthestars/questbook/overlay.json';
const IDS_JAVA_REL = 'src/main/java/com/nateyburr/beyondthestars/BTSQuestIds.java';
const PLANT_LINE_LOW = 47990;
const ENTRY_LOW = 48020;
const GRID = 12;
const LINE_CLEARANCE = 3;

// ------------------------------------------------------------------------------------ utilities

class F32 { constructor(v) { this.v = v; } } // a float NBT value: serialised as -1.0, not -1

function b64Of(high, low) {
    const b = Buffer.alloc(16);
    b.writeBigInt64BE(BigInt(high), 0);
    b.writeBigInt64BE(BigInt(low), 8);
    return b.toString('base64').replace(/\+/g, '-').replace(/\//g, '_');
}
function idOfB64(s) {
    const b = Buffer.from(s.replace(/-/g, '+').replace(/_/g, '/'), 'base64');
    if (b.length !== 16) throw new Error(`not a 16-byte quest id: ${s}`);
    return { high: b.readBigInt64BE(0), low: b.readBigInt64BE(8) };
}
// Pack files carry 64-bit ids JSON numbers cannot hold; read them as BigInt.
function readTypedJson(file) {
    const t = fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, '');
    return JSON.parse(t.replace(/("(?:questIDHigh|questIDLow|questLineIDHigh|questLineIDLow):4"\s*:\s*)(-?\d+)/g, '$1"#big$2"'),
        (k, v) => (typeof v === 'string' && v.startsWith('#big') ? BigInt(v.slice(4)) : v));
}
// BetterQuesting writes 2-space JSON; BigInt ids go out as raw digits, floats keep their ".0".
function stringify(v, ind = '') {
    const nx = ind + '  ';
    if (v === null || v === undefined) return 'null';
    if (typeof v === 'bigint') return v.toString();
    if (v instanceof F32) return Number.isInteger(v.v) ? v.v.toFixed(1) : String(v.v);
    if (typeof v === 'number') return String(v);
    if (typeof v === 'boolean') return v ? 'true' : 'false';
    if (typeof v === 'string') return JSON.stringify(v);
    if (Array.isArray(v)) {
        if (!v.length) return '[]';
        return '[\n' + v.map(x => nx + stringify(x, nx)).join(',\n') + '\n' + ind + ']';
    }
    const keys = Object.keys(v);
    if (!keys.length) return '{}';
    return '{\n' + keys.map(k => nx + JSON.stringify(k) + ': ' + stringify(v[k], nx)).join(',\n') + '\n' + ind + '}';
}
const strip = s => String(s || '').replace(/\u00a7./g, '').trim();
function walkFiles(dir, ext) {
    let out = [];
    if (!fs.existsSync(dir)) return out;
    for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
        const p = path.join(dir, e.name);
        if (e.isDirectory()) out = out.concat(walkFiles(p, ext));
        else if (!ext || p.endsWith(ext)) out.push(p);
    }
    return out;
}
const sha1 = s => crypto.createHash('sha1').update(s).digest('hex');
const fmtNum = n => Number(n).toLocaleString('en-US');

// ----------------------------------------------------------------------------- text formatting
// The source is ASCII-only. {6} {l} {r} ... become the section sign + code (as Fmt() in
// gen-questbook.ps1); named glyph tokens and {u+XXXX} carry the few non-ASCII characters the
// Eye's own GUI prints (its gauges are literally named with Greek letters).
// There is deliberately no {sqrt}, {Phi}, {lfloor}/{rfloor} or {sun}: Minecraft's font draws those
// as odd glyphs in the book (seen in the 2026-10-13 screenshots), so the text says "sqrt", "flux",
// "floor(...)" and "SM" instead; PLAYER_TEXT_RULES below rejects the characters however they arrive.
const GLYPHS = {
    times: '\u00d7', pm: '\u00b1', ge: '\u2265', le: '\u2264', to: '\u2192', minus: '\u2212',
    sq: '\u00b2', deg: '\u00b0', theta: '\u0398', omega: '\u03a9', Pi: '\u03a0',
    dot: '\u00b7', ast: '*', sup11: '\u00b9\u00b9', sup13: '\u00b9\u00b3', div: '\u00f7',
};

// ------------------------------------------------------------------------- player-text rules
// Every string a player reads in the book (our quests, our chapters, the plant chapter read back
// from gen-questbook.ps1, and every text a pack edit writes) is checked against how BetterQuesting
// actually draws it on its parchment page (background #FBF3DF, measured from a client screenshot):
//  - colour codes with too little contrast there: yellow 0.96:1, aqua 1.11:1, green 1.20:1
//    anywhere; in descriptions also gold 1.73:1, grey 2.0:1 and light purple 2.37:1 (the round-2
//    screenshots: the plant chapter's gold key lines were the hardest text in the book to read).
//    Use {5} dark purple (5.8:1) for headings, {9} blue (4.6:1) for highlights, {1} dark blue
//    (12:1) for key lines and chapter names in the text, {8} dark grey (6.7:1) for asides, {2} dark
//    green (2.8:1) for "done", and {3} dark aqua / {6} gold only in quest names (a title is drawn
//    bold, and on the map's dark tooltips too);
//  - BOLD runs longer than BOLD_MAX visible characters (style: a long heading is colour + capitals,
//    not bold). The cut-off letters the first screenshots showed are wrapSafe()'s job below;
//  - text that wrapSafe() would still change (only gen-questbook.ps1's text can arrive that way:
//    its Fmt applies the same two steps);
//  - glyphs Minecraft's font draws oddly (sqrt, Phi, floor brackets, the sun sign).
const BOLD_MAX = 16;
const LOW_CONTRAST = { e: 'yellow', b: 'aqua', a: 'green' };
const LOW_CONTRAST_BODY = { 6: 'gold', 7: 'grey', d: 'light purple' };
const ODD_GLYPHS = /[\u03a6\u221a\u230a\u230b\u2609]/;

// ----------------------------------------------------------------------------- wrap-safe text
// BetterQuesting 3.8.89-GTNH wraps a description with java.text.BreakIterator and measures each
// break segment ON ITS OWN (RenderUtils.splitString: width of the line so far + width of the next
// segment). Two consequences, both seen in the round-2 screenshots (2026-10-13):
//  - a bold run over several words is measured bold only in its first segment, so "DONE WHEN",
//    "MASS INFUSION" or "The tier gate:" pushed their line one pixel per later bold glyph past the
//    box and its last letter was cut ("escrou", "anc", "ever"). Step 2 re-opens bold after every
//    space inside a bold run, so each segment carries its own code; it draws exactly the same;
//  - a colour code glued to a number ("{9}1.51", "{9}50,000,000") makes BreakIterator break the
//    number after its first '.' or ',' ("1." at a line end, "51 x 10..." on the next). Step 1 moves
//    a code group that follows a space and precedes a digit in front of that space ("and{9} 1.51"),
//    or in front of an opening bracket ("{2}(1,920"): the number is one segment again. Likewise a
//    code between a closing bracket and punctuation ("(operator){r}.") lets the '.' start the next
//    line on its own; the code moves after the punctuation ("(operator).{r}").
// Both steps are idempotent (step 1 never moves a bare bold code, step 2 never doubles one), so
// gen-questbook.ps1's WrapSafe (the same two steps) and this one agree on the plant chapter.
const SEC = '\u00a7';
function wrapSafe(text) {
    const s = String(text)
        .replace(/ ((?:\u00a7[0-9a-fk-or])*\u00a7[0-9a-fr](?:\u00a7[0-9a-fk-or])*)(?=[0-9])/g, '$1 ')
        .replace(/\(((?:\u00a7[0-9a-fk-or])*\u00a7[0-9a-fr](?:\u00a7[0-9a-fk-or])*)(?=[0-9])/g, '$1(')
        .replace(/([)\]])((?:\u00a7[0-9a-fk-or])+)([.,;:!?]+)/g, '$1$3$2');
    let out = '', bold = false;
    for (let i = 0; i < s.length; i++) {
        const ch = s[i];
        if (ch === SEC && i + 1 < s.length) {
            const c = s[i + 1].toLowerCase();
            if (c === 'l') bold = true; else if (/[0-9a-fr]/.test(c)) bold = false;
            out += ch + s[i + 1];
            i++;
            continue;
        }
        out += ch;
        if (ch !== ' ' || !bold || i + 1 >= s.length) continue;
        const n = s[i + 1];
        if (n === ' ' || n === '\n') continue;
        if (n === SEC && i + 2 < s.length && /[0-9a-flr]/.test(s[i + 2].toLowerCase())) continue;
        out += SEC + 'l';
    }
    return out;
}

function playerTextProblems(text, isName) {
    const out = [];
    const s = String(text || '');
    const bad = new Set();
    for (const m of s.matchAll(/\u00a7([0-9a-fk-or])/g)) if (LOW_CONTRAST[m[1]] || (!isName && LOW_CONTRAST_BODY[m[1]])) bad.add(m[1]);
    for (const c of bad) out.push(`colour code ${c} (${LOW_CONTRAST[c] || LOW_CONTRAST_BODY[c]}) is unreadable on the book's parchment page (use {5} headings, {9} highlights, {1} key lines, {8} asides, {2} done; {3} or {6} only in names)`);
    const g = ODD_GLYPHS.exec(s);
    if (g) out.push(`'${g[0]}' (U+${g[0].charCodeAt(0).toString(16).toUpperCase().padStart(4, '0')}) renders as an odd glyph in Minecraft's font; write it in words`);
    if (isName) return out; // a title is one line: never wrapped, so bold cannot be cut
    if (wrapSafe(s) !== s) out.push('text is not wrap-safe (a multi-word bold run or a colour code glued to a number would be cut or split where BetterQuesting wraps); run it through wrapSafe / gen-questbook.ps1 WrapSafe');
    // bold opens at {l} and closes at {r} or any colour code (Minecraft's rule); BetterQuesting
    // carries an open format onto the next line, so a run is measured per line it covers
    let bold = false, run = '';
    const check = () => { if (bold && run.trim().length > BOLD_MAX) out.push(`bold run of ${run.trim().length} characters (max ${BOLD_MAX}) is cut off where BetterQuesting wraps: "${run.trim()}"`); run = ''; };
    for (let i = 0; i < s.length; i++) {
        if (s[i] === '\n') { check(); continue; }
        if (s[i] === '\u00a7' && i + 1 < s.length) {
            const c = s[++i];
            if (c === 'l') { if (!bold) { bold = true; run = ''; } } else if (/[0-9a-fr]/.test(c)) { check(); bold = false; }
            continue;
        }
        if (bold) run += s[i];
    }
    check();
    return out;
}

function makeFormatter(ctx) {
    // a text array may paste a shared block: a line '@name' is replaced by data.texts.name
    const expand = (text, where, depth = 0) => {
        if (!Array.isArray(text)) text = [String(text)];
        const out = [];
        for (const ln of text) {
            const m = /^@([a-z0-9_]+)$/.exec(ln);
            if (!m) { out.push(ln); continue; }
            const t = ctx.texts && ctx.texts[m[1]];
            if (t === undefined || depth > 4) { ctx.error(`${where}: unknown text block @${m[1]}`); continue; }
            out.push(...expand(t, where, depth + 1));
        }
        return out;
    };
    // fmt: a description (wrap-safe); fmt.title: a title, which is never wrapped and stays as typed
    const fmt = (text, where) => wrapSafe(raw(text, where));
    fmt.title = raw;
    return fmt;
    function raw(text, where) {
        const s = expand(text, where).join('\n');
        return s.replace(/\{([^{}\s]+)\}/g, (m, tok) => {
            if (/^[0-9a-fk-or]$/.test(tok)) return '\u00a7' + tok;
            if (GLYPHS[tok] !== undefined) return GLYPHS[tok];
            let mm;
            if ((mm = /^u\+([0-9a-fA-F]{4})$/.exec(tok))) return String.fromCharCode(parseInt(mm[1], 16));
            if ((mm = /^bom:([a-z0-9_]+):(.)$/.exec(tok))) return fmtNum(ctx.bomCount(mm[1], mm[2], where));
            if ((mm = /^bomdims:([a-z0-9_]+)$/.exec(tok))) return ctx.bomDims(mm[1], where);
            ctx.error(`${where}: unknown text token ${m}`);
            return m;
        });
    }
}

// ------------------------------------------------------------------------------- source tree
// Our registry names come out of the Java of --src, so a renamed item fails here, not in game.
function scanSource(src) {
    const java = walkFiles(path.join(src, 'src/main/java'), '.java');
    const items = new Set(), fluids = new Set(), mtes = new Map(), blocks = new Map();
    for (const f of java) {
        const t = fs.readFileSync(f, 'utf8');
        for (const m of t.matchAll(/BTSItems\.define\(\s*"([A-Za-z0-9_]+)"/g)) items.add(m[1]);
        for (const m of t.matchAll(/defineWithTooltip\(\s*"([A-Za-z0-9_]+)"/g)) items.add(m[1]);
        for (const m of t.matchAll(/GameRegistry\.registerItem\(\s*\w+\s*,\s*"([A-Za-z0-9_]+)"/g)) items.add(m[1]);
        for (const m of t.matchAll(/GameRegistry\.registerBlock\(\s*\w+\s*,\s*[\w.]+\s*,\s*"([A-Za-z0-9_]+)"/g)) items.add(m[1]);
        for (const m of t.matchAll(/"(bts[a-z0-9]+)"/g)) fluids.add(m[1]);
        // SLCircuitItems registers through its own private define("name") (circuits v2, items/SLCircuitItems.java)
        if (f.endsWith(path.sep + 'SLCircuitItems.java')) for (const m of t.matchAll(/\bdefine\(\s*"([A-Za-z0-9_]+)"/g)) items.add(m[1]);
    }
    // Config's boolean flags: the names a pack edit (or one of its ops) may be gated on with "config".
    const configFlags = new Set();
    // MTE ids: BTSMachines registers base + n with the unlocalised name next to it.
    const bm = java.find(f => f.endsWith(path.join('machines', 'BTSMachines.java')));
    const cfg = java.find(f => f.endsWith(path.sep + 'Config.java'));
    let base = 28100;
    if (cfg) {
        const ct = fs.readFileSync(cfg, 'utf8');
        const m = /mteBaseId\s*=\s*(\d+)/.exec(ct);
        if (m) base = +m[1];
        for (const b of ct.matchAll(/public static boolean (\w+)/g)) configFlags.add(b[1]);
    }
    if (bm) {
        const t = fs.readFileSync(bm, 'utf8');
        for (const m of t.matchAll(/\(\s*(?:Config\.mteBaseId|base)\s*\+\s*(\d+)\s*,\s*"([\w.]+)"/g)) mtes.set(base + +m[1], m[2]);
    }
    // casing metas: BlockSLCasing / BlockSLCasing2 constants
    for (const [cls, reg] of [['BlockSLCasing.java', 'slCasing'], ['BlockSLCasing2.java', 'slCasing2']]) {
        const f = java.find(x => x.endsWith(path.sep + cls));
        if (!f) continue;
        const metas = new Set();
        for (const m of fs.readFileSync(f, 'utf8').matchAll(/public static final int (\w+) = (\d+);/g)) {
            if (!/HOLE/.test(m[1])) metas.add(+m[2]);
        }
        blocks.set(reg, metas);
    }
    let head = '';
    try {
        const g = path.join(src, '.git');
        let gitDir = g;
        if (fs.statSync(g).isFile()) gitDir = path.resolve(src, fs.readFileSync(g, 'utf8').replace(/^gitdir:\s*/, '').trim());
        const h = fs.readFileSync(path.join(gitDir, 'HEAD'), 'utf8').trim();
        if (h.startsWith('ref:')) {
            const ref = h.slice(5).trim();
            const common = fs.existsSync(path.join(gitDir, 'commondir'))
                ? path.resolve(gitDir, fs.readFileSync(path.join(gitDir, 'commondir'), 'utf8').trim()) : gitDir;
            const rf = [path.join(gitDir, ref), path.join(common, ref)].find(p => fs.existsSync(p));
            head = rf ? fs.readFileSync(rf, 'utf8').trim().slice(0, 7) + ' ' + ref.replace('refs/heads/', '') : ref;
        } else head = h.slice(0, 7);
    } catch (e) { head = '?'; }
    const manifestFile = path.join(src, 'src/main/resources/assets/beyondthestars/structures/manifest.json');
    const manifest = fs.existsSync(manifestFile) ? JSON.parse(fs.readFileSync(manifestFile, 'utf8')) : null;
    return { items, fluids, mtes, blocks, manifest, head, configFlags };
}

// ------------------------------------------------------------------------------------ the pack
function readPack(dir) {
    const quests = new Map(), lines = new Map(), itemsUsed = new Set(), fluidsUsed = new Set();
    const noteItem = st => { if (st && st['id:8']) itemsUsed.add(`${st['id:8']}:${st['Damage:2'] || 0}`); };
    for (const f of walkFiles(path.join(dir, 'Quests'), '.json')) {
        const j = readTypedJson(f);
        const id = b64Of(j['questIDHigh:4'], j['questIDLow:4']);
        const p = j['properties:10']['betterquesting:10'];
        quests.set(id, { id, json: j, name: strip(p['name:8']), file: f, lines: [] });
        noteItem(p['icon:10']);
        for (const t of Object.values(j['tasks:9'] || {})) {
            for (const it of Object.values(t['requiredItems:9'] || {})) noteItem(it);
            for (const fl of Object.values(t['requiredFluids:9'] || {})) fluidsUsed.add(fl['FluidName:8']);
        }
        for (const r of Object.values(j['rewards:9'] || {})) for (const it of Object.values(r['rewards:9'] || {})) noteItem(it);
    }
    for (const d of fs.readdirSync(path.join(dir, 'QuestLines'))) {
        const ld = path.join(dir, 'QuestLines', d);
        const L = readTypedJson(path.join(ld, 'QuestLine.json'));
        const id = b64Of(L['questLineIDHigh:4'], L['questLineIDLow:4']);
        const entries = [];
        for (const e of fs.readdirSync(ld)) {
            if (e === 'QuestLine.json') continue;
            const E = readTypedJson(path.join(ld, e));
            const qid = b64Of(E['questIDHigh:4'], E['questIDLow:4']);
            entries.push({ id: qid, x: E['x:3'], y: E['y:3'], w: E['sizeX:3'], h: E['sizeY:3'] });
            if (quests.has(qid)) quests.get(qid).lines.push(id);
        }
        lines.set(id, { id, dir: d, name: strip(L['properties:10']['betterquesting:10']['name:8']), entries });
    }
    let packVersion = null;
    try { packVersion = JSON.parse(fs.readFileSync(path.join(dir, 'QuestSettings.json'), 'utf8'))['betterquesting:10']['pack_version:3']; } catch (e) { /* none */ }
    const order = fs.existsSync(path.join(dir, 'QuestLinesOrder.txt'))
        ? fs.readFileSync(path.join(dir, 'QuestLinesOrder.txt'), 'utf8').split(/\r?\n/).filter(Boolean).map(l => l.split(':')[0].trim()) : [];
    return { quests, lines, itemsUsed, fluidsUsed, packVersion, order };
}

// ------------------------------------------------------------------------------------ build
function build(data, opts) {
    const errors = [], warnings = [];
    const error = m => errors.push(m), warn = m => warnings.push(m);
    const { src, pack, registry, ledgerText, plantDir } = opts;
    const HIGH = data.idHigh;

    // ---- features
    const features = Object.assign({}, data.features, opts.featureOverrides || {});
    const built = list => (list || []).every(f => {
        if (!(f in features)) { error(`unknown feature '${f}'`); return false; }
        return features[f] === true;
    });
    const blocked = list => (list || []).some(f => {
        if (!(f in features)) { error(`unknown feature '${f}'`); return false; }
        return features[f] === true;
    });

    // ---- manifest-derived bills
    const manifest = src.manifest;
    const bomCount = (piece, ch, where) => {
        const p = manifest && manifest.pieces[piece];
        if (!p) { error(`${where}: manifest has no piece '${piece}'`); return 0; }
        const e = p.bom.find(b => b.ch === ch);
        if (!e) { error(`${where}: piece '${piece}' has no element '${ch}'`); return 0; }
        return e.count;
    };
    const bomDims = (piece, where) => {
        const p = manifest && manifest.pieces[piece];
        if (!p) { error(`${where}: manifest has no piece '${piece}'`); return '?'; }
        return `${p.w} ${GLYPHS.times} ${p.h} ${GLYPHS.times} ${p.d}`;
    };
    const fmt = makeFormatter({ bomCount, bomDims, error, texts: data.texts });

    // ---- item references
    // "64x ref" | "ref"; ref = mte:N | ore:name | mod:name[:meta] ; fluids: "1000x fluid:name"
    function parseStack(s, where) {
        const m = /^(?:(\d+)x\s+)?(.+)$/.exec(String(s).trim());
        const count = m[1] ? +m[1] : 1;
        const ref = m[2];
        if (ref.startsWith('fluid:')) return { kind: 'fluid', name: ref.slice(6), count };
        if (ref.startsWith('mte:')) return { kind: 'item', id: 'gregtech:gt.blockmachines', meta: +ref.slice(4), count, ref };
        if (ref.startsWith('ore:')) {
            const ore = ref.slice(4), r = registry.ore[ore];
            if (!r) { error(`${where}: ore '${ore}' is not in questbook/registry.json`); return { kind: 'item', id: 'minecraft:stone', meta: 0, count, ore, ref }; }
            const rep = parseStack(r.rep, where);
            return { kind: 'item', id: rep.id, meta: rep.meta, count, ore, ref };
        }
        const parts = ref.split(':');
        let meta = 0;
        if (parts.length >= 3 && /^\d+$/.test(parts[parts.length - 1])) meta = +parts.pop();
        if (parts.length !== 2) { error(`${where}: cannot parse item '${ref}'`); }
        return { kind: 'item', id: parts.join(':'), meta, count, ref };
    }
    // Every item and fluid a world can see must exist: ours by the Java of --src, others by the
    // pack using it somewhere, or by a verified entry in questbook/registry.json.
    function checkExists(st, where) {
        if (st.count < 1) error(`${where}: stack ${st.ref || st.name} has count ${st.count}`);
        if (st.kind === 'fluid') {
            if (st.name.startsWith('bts')) { if (!src.fluids.has(st.name)) error(`${where}: fluid '${st.name}' is not registered in ${opts.srcRoot}`); return; }
            if (pack.fluidsUsed.has(st.name) || registry.fluids[st.name]) return;
            error(`${where}: fluid '${st.name}' is neither used by the pack nor listed in questbook/registry.json`);
            return;
        }
        if (st.ore) return; // checked when parsed (registry.json lists every ore with its representative)
        const [mod, name] = st.id.split(':');
        if (mod === 'beyondthestars') {
            if (!src.items.has(name)) { error(`${where}: beyondthestars:${name} is not registered in ${opts.srcRoot}`); return; }
            if (src.blocks.has(name) && !src.blocks.get(name).has(st.meta)) error(`${where}: ${name} has no live meta ${st.meta}`);
            return;
        }
        if (st.id === 'gregtech:gt.blockmachines' && st.meta >= 28100 && st.meta < 28140) {
            if (!src.mtes.has(st.meta)) error(`${where}: MTE ${st.meta} is not registered in BTSMachines of ${opts.srcRoot}`);
            return;
        }
        const key = `${st.id}:${st.meta}`;
        if (pack.itemsUsed.has(key) || registry.items[key]) return;
        error(`${where}: item ${key} is neither used by the pack nor listed in questbook/registry.json`);
    }
    const itemNbt = st => ({ 'Count:3': st.count, 'Damage:2': st.meta, 'OreDict:8': st.ore || '', 'id:8': st.id });

    // ---- bills of materials -> optional retrieval stacks
    function bomStacks(spec, where) {
        // spec: "piece" | "piece-minus-other"
        const [piece, minus] = spec.split('-minus-');
        const tally = (name) => {
            const p = manifest && manifest.pieces[name];
            if (!p) { error(`${where}: manifest has no piece '${name}'`); return new Map(); }
            const fam = data.bomItems[p.machine] ? p.machine : p.machine.split('_')[0];
            const table = data.bomItems[fam];
            if (!table) { error(`${where}: no bomItems table for machine '${p.machine}'`); return new Map(); }
            const out = new Map();
            for (const b of p.bom) {
                const row = table[b.role];
                if (row === undefined) { error(`${where}: bomItems.${fam} has no entry for role '${b.role}' (${b.block})`); continue; }
                if (row === null) continue; // a socket or the controller: taught in the text
                if (row.expect && !b.block.includes(row.expect)) {
                    error(`${where}: manifest role ${p.machine}/${b.role} is now '${b.block}', bomItems expects '${row.expect}'`);
                }
                out.set(row.item, (out.get(row.item) || 0) + b.count);
            }
            return out;
        };
        const t = tally(piece);
        if (minus) for (const [k, v] of tally(minus)) t.set(k, (t.get(k) || 0) - v);
        const stacks = [];
        for (const [ref, n] of [...t.entries()].sort((a, b) => b[1] - a[1])) {
            if (n <= 0) continue;
            const st = parseStack(`${n}x ${ref}`, where);
            checkExists(st, where);
            stacks.push(st);
        }
        if (!stacks.length) error(`${where}: bill '${spec}' is empty`);
        return stacks;
    }

    // ---- tasks and rewards (BQ typed keys)
    function compileTasks(tasks, where, checkItems) {
        const out = {};
        tasks.forEach((t, i) => {
            const w = `${where} task ${i}`;
            let nbt;
            if (t.type === 'checkbox') nbt = { 'index:3': i, 'taskID:8': 'bq_standard:checkbox' };
            else if (t.type === 'retrieval' || t.type === 'optional_retrieval') {
                let stacks = [];
                if (t.bom) stacks = checkItems ? bomStacks(t.bom, w) : [];
                for (const s of t.items || []) {
                    const st = parseStack(s, w);
                    if (st.kind !== 'item') error(`${w}: a fluid in an item task`);
                    if (checkItems) checkExists(st, w);
                    stacks.push(st);
                }
                if (!stacks.length && checkItems) error(`${w}: retrieval with no items`);
                const req = {};
                stacks.forEach((st, k) => { req[`${k}:10`] = itemNbt(st); });
                nbt = {
                    'autoConsume:1': 0, 'consume:1': 0, 'groupDetect:1': 0, 'ignoreNBT:1': 1, 'index:3': i, 'partialMatch:1': 1,
                    'requireOnlyOneItem:1': 0, 'requiredItems:9': req, 'taskID:8': 'bq_standard:' + t.type,
                };
            } else if (t.type === 'fluid') {
                const req = {};
                (t.fluids || []).forEach((s, k) => {
                    const st = parseStack(s, w);
                    if (st.kind !== 'fluid') error(`${w}: an item in a fluid task`);
                    if (checkItems) checkExists(st, w);
                    req[`${k}:10`] = { 'Amount:3': st.count, 'FluidName:8': st.name };
                });
                nbt = { 'autoConsume:1': 0, 'consume:1': 0, 'groupDetect:1': 0, 'ignoreNBT:1': 1, 'index:3': i, 'requiredFluids:9': req, 'taskID:8': 'bq_standard:fluid' };
            } else if (t.type === 'tbd') {
                if (checkItems) error(`${w}: task still 'tbd' (${t.what}) but its quest's features are built - write the real task`);
                nbt = { 'index:3': i, 'taskID:8': 'bq_standard:checkbox' };
            } else error(`${w}: unknown task type '${t.type}'`);
            out[`${i}:10`] = nbt;
        });
        return out;
    }
    function compileRewards(rewards, where) {
        const out = {};
        (rewards || []).forEach((s, i) => {
            const st = parseStack(s, `${where} reward ${i}`);
            checkExists(st, `${where} reward ${i}`);
            out[`${i}:10`] = { 'ignoreDisabled:1': 0, 'index:3': i, 'rewardID:8': 'bq_standard:item', 'rewards:9': { '0:10': itemNbt(st) } };
        });
        return out;
    }

    // ---- chapters
    const chapterByKey = new Map();
    for (const [key, c] of Object.entries(data.packChapters)) {
        const line = pack.lines.get(c.id);
        if (!line) { error(`pack chapter '${key}' (${c.id}) is not in ${opts.packDir}`); continue; }
        if (line.name !== c.name) error(`pack chapter '${key}' is now named '${line.name}', expected '${c.name}'`);
        chapterByKey.set(key, { key, ours: false, b64: c.id, dir: line.dir, name: line.name, range: c.range, packLine: line });
    }
    for (const c of data.chapters) {
        const b64 = b64Of(HIGH, c.low);
        if (/[+/]/.test(b64)) error(`chapter ${c.key}: id ${c.low} encodes with + or /`);
        chapterByKey.set(c.key, { key: c.key, ours: true, low: c.low, b64, dir: `${c.dirStem}-${b64}`, name: c.name, range: c.range, def: c });
    }

    // ---- our quests
    const ours = new Map(); // low -> quest record
    const editById = new Map(data.packEdits.map(e => [e.id, e]));
    function resolvePrereq(ref, where) {
        let m;
        if (typeof ref === 'number') return { b64: b64Of(HIGH, ref), type: 0, ours: ref };
        if ((m = /^(\d+)(?::(\d))?$/.exec(ref))) return { b64: b64Of(HIGH, +m[1]), type: +(m[2] || 0), ours: +m[1] };
        if ((m = /^(E\d+)(?::(\d))?$/.exec(ref))) {
            const e = editById.get(m[1]);
            if (!e) { error(`${where}: prerequisite ${ref} names no pack edit`); return null; }
            return { b64: e.quest, type: +(m[2] || 0) };
        }
        if ((m = /^pack:([A-Za-z0-9_-]{22}==)(?::(\d))?$/.exec(ref))) return { b64: m[1], type: +(m[2] || 0) };
        error(`${where}: cannot parse prerequisite '${ref}'`);
        return null;
    }
    const plantLows = new Set(opts.plantLows || []);
    const knownQuest = b64 => pack.quests.has(b64) || [...ours.values()].some(q => q.b64 === b64) ||
        [...plantLows].some(l => b64Of(HIGH, l) === b64);

    const planned = fmt(data.placeholderNote, 'placeholderNote');
    for (const q of data.quests) {
        const where = `quest ${q.low} (${q.key})`;
        if (ours.has(q.low)) { error(`${where}: duplicate id ${q.low}`); continue; }
        const ch = chapterByKey.get(q.chapter);
        if (!ch) { error(`${where}: unknown chapter '${q.chapter}'`); continue; }
        if (!ch.range || q.low < ch.range[0] || q.low > ch.range[1]) error(`${where}: id outside chapter ${q.chapter}'s range ${ch.range}`);
        const b64 = b64Of(HIGH, q.low);
        if (/[+/]/.test(b64)) error(`${where}: id encodes with + or / (${b64})`);
        if (!/^[a-z0-9_]+$/.test(q.key || '')) error(`${where}: missing or malformed key`);
        const placeholder = !!(q.needs && q.needs.length && !built(q.needs));
        const rec = { q, where, b64, chapter: q.chapter, placeholder, size: q.size || 24 };
        // name: a placeholder is grey and says so, so the book shows at a glance what is not built
        const baseName = fmt.title(q.name, where);
        rec.name = placeholder ? '\u00a77\u00a7l\u00a7n' + strip(baseName) + '\u00a7r \u00a78(planned)' : baseName;
        rec.stem = q.stem || strip(baseName).replace(/[^A-Za-z0-9]/g, '').slice(0, 16);
        const descParts = [fmt(q.desc, where)];
        if (placeholder) descParts.push(planned);
        rec.desc = descParts.join('\n\n');
        if (/\{[^}]*\}/.test(rec.desc.replace(/\[[^\]]*\]/g, ''))) warn(`${where}: description still contains a {token}`);
        // tasks
        if (!q.tasks || !q.tasks.length) error(`${where}: no tasks (a placeholder still lists its real ones)`);
        if (placeholder) {
            compileTasks(q.tasks || [], where, false); // syntax only: the items need not exist yet
            rec.tasks = { '0:10': { 'index:3': 0, 'taskID:8': 'bq_standard:checkbox' } };
            rec.realTasks = q.tasks;
            rec.rewards = {};
        } else {
            rec.tasks = compileTasks(q.tasks || [], where, true);
            rec.rewards = compileRewards(q.rewards, where);
        }
        const iconSrc = (!placeholder && q.iconBuilt) ? q.iconBuilt : q.icon;
        if (!iconSrc) error(`${where}: no icon`);
        const icon = parseStack(iconSrc || 'minecraft:stone', `${where} icon`);
        checkExists(icon, `${where} icon`);
        rec.icon = itemNbt(Object.assign({}, icon, { count: 1 }));
        rec.countAsQuest = placeholder ? 0 : (q.countAsQuest === 0 ? 0 : 1);
        rec.main = q.main ? 1 : 0;
        for (const k of ['x', 'y']) if (typeof q[k] !== 'number') error(`${where}: no ${k}`);
        if (q.offPos && !q.offPos.every(n => typeof n === 'number')) error(`${where}: bad offPos`);
        ours.set(q.low, rec);
    }
    for (const rec of ours.values()) {
        rec.prereqs = [];
        for (const r of rec.q.prereqs || []) {
            const p = resolvePrereq(r, rec.where);
            if (!p) continue;
            if (!knownQuest(p.b64)) error(`${rec.where}: prerequisite ${r} (${p.b64}) exists neither in the pack nor in our quests`);
            rec.prereqs.push(p);
        }
        if (rec.q.onlyWithout && !editById.has(rec.q.onlyWithout)) error(`${rec.where}: onlyWithout names no edit '${rec.q.onlyWithout}'`);
    }

    // ---- ledger (append-only)
    const ledger = new Map();
    for (const line of (ledgerText || '').split(/\r?\n/)) {
        const m = /^(\d+)\s+(\S+)/.exec(line);
        if (m) ledger.set(+m[1], m[2]);
    }
    const nowKeys = new Map();
    for (const c of data.chapters) nowKeys.set(c.low, 'chapter:' + c.key);
    for (const rec of ours.values()) nowKeys.set(rec.q.low, rec.q.key);
    for (const l of plantLows) nowKeys.set(l, 'plant');
    nowKeys.set(PLANT_LINE_LOW, 'chapter:plant');
    for (const [low, key] of ledger) {
        if (!nowKeys.has(low)) error(`id-ledger: ${low} (${key}) was shipped and is gone - ids may never disappear`);
        else if (key !== 'plant' && !key.startsWith('plant:') && nowKeys.get(low) !== key) {
            error(`id-ledger: ${low} was shipped as '${key}' and is now '${nowKeys.get(low)}' - an id may never change meaning`);
        }
    }
    const ledgerAppend = [...nowKeys.entries()].filter(([l]) => !ledger.has(l)).sort((a, b) => a[0] - b[0]);

    // ---- pack edits
    const compiledEdits = [];
    const applied = new Map(); // quest b64 -> {edit, ops (resolved), original}
    const drift = [];
    for (const e of data.packEdits) {
        const where = `pack edit ${e.id}`;
        if (compiledEdits.some(x => x.id === e.id)) error(`${where}: duplicate edit id`);
        const pq = pack.quests.get(e.quest);
        if (!pq) { drift.push(`${e.id}: quest ${e.quest} ('${e.expectName}') is not in the pack`); continue; }
        if (pq.name !== e.expectName) { drift.push(`${e.id}: quest ${e.quest} is named '${pq.name}', expected '${e.expectName}'`); continue; }
        const exCh = chapterByKey.get(e.expectChapter);
        if (!exCh || exCh.ours) { error(`${where}: expectChapter '${e.expectChapter}' is not a pack chapter key`); continue; }
        if (!pq.lines.includes(exCh.b64)) { drift.push(`${e.id}: '${e.expectName}' is no longer in ${exCh.name}`); continue; }
        if (applied.has(e.quest)) error(`${where}: quest ${e.quest} is already edited by ${applied.get(e.quest).edit.id}`);
        let ops = e.ops;
        if (e.variants) {
            const v = e.variants.find(v => built(v.needs) && !blocked(v.unless));
            if (!v) { warn(`${where}: no variant matches the features; edit not applied`); continue; }
            ops = v.ops;
        }
        if (!ops || !ops.length) error(`${where}: no ops`);
        const orig = pq.json, op0 = orig['properties:10']['betterquesting:10'];
        let desc = op0['desc:8'] || '';
        let prereqs = Object.values(orig['preRequisites:9'] || {}).map(p => ({ b64: b64Of(p['questIDHigh:4'], p['questIDLow:4']), type: p['type:1'] || 0 }));
        const taskItems = new Set();
        for (const t of Object.values(orig['tasks:9'] || {})) for (const it of Object.values(t['requiredItems:9'] || {})) taskItems.add(`${it['id:8']}:${it['Damage:2']}`);
        const out = [];
        const moves = [];
        // "config" gates the whole edit; an op's own "config" gates only that op (text that is true only under a
        // flag, e.g. the circuit raise's warning). Both must name boolean fields of Config.java.
        const checkFlags = (list, w) => {
            if (list === undefined) return;
            if (!Array.isArray(list) || !list.every(f => typeof f === 'string')) { error(`${w}: config must be a list of flag names`); return; }
            for (const f of list) if (src.configFlags && !src.configFlags.has(f)) error(`${w}: config flag '${f}' is not a boolean in Config.java`);
        };
        checkFlags(e.config, where);
        for (const op of ops || []) {
            checkFlags(op.config, `${where} op ${op.op}`);
            if (op.config && op.op !== 'appendText') error(`${where} op ${op.op}: only appendText may carry its own config`);
            const before = out.length;
            const w = `${where} op ${op.op}`;
            switch (op.op) {
            case 'setName': out.push({ op: 'setName', name: fmt.title(op.name, w) }); break;
            case 'setDesc': desc = fmt(op.desc, w); out.push({ op: 'setDesc', desc }); break;
            case 'setIcon': {
                const st = parseStack(op.item, w); checkExists(st, w);
                out.push({ op: 'setIcon', icon: itemNbt(Object.assign({}, st, { count: 1 })) }); break;
            }
            case 'replaceText': {
                if (!desc.includes(op.find)) drift.push(`${e.id}: replaceText find text is not in '${e.expectName}': "${op.find}"`);
                desc = desc.split(op.find).join(fmt(op.with, w));
                out.push({ op: 'replaceText', find: op.find, with: fmt(op.with, w) }); break;
            }
            case 'appendText': { const t = fmt(op.text, w); desc += '\n\n' + t; out.push({ op: 'appendText', text: t }); break; }
            case 'setTasks': out.push({ op: 'setTasks', tasks: compileTasks(op.tasks, w, true) }); break;
            case 'setRewards': out.push({ op: 'setRewards', rewards: compileRewards(op.rewards, w) }); break;
            case 'swapItem': {
                const from = parseStack(op.from, w), to = parseStack(op.to, w);
                checkExists(to, w);
                if (!taskItems.has(`${from.id}:${from.meta}`)) drift.push(`${e.id}: swapItem: ${op.from} is not in '${e.expectName}' tasks`);
                out.push({ op: 'swapItem', from: { id: from.id, damage: from.meta }, to: { id: to.id, damage: to.meta } }); break;
            }
            case 'setPrereqs': {
                const ps = op.prereqs.map(r => resolvePrereq(r, w)).filter(Boolean);
                for (const p of ps) if (!knownQuest(p.b64)) error(`${w}: prerequisite ${p.b64} does not exist`);
                prereqs = ps; out.push({ op: 'setPrereqs', prereqs: ps.map(p => ({ b64: p.b64, type: p.type })) }); break;
            }
            case 'addPrereq': {
                const p = resolvePrereq(op.prereq, w);
                if (p && !knownQuest(p.b64)) error(`${w}: prerequisite ${p.b64} does not exist`);
                if (p) { prereqs.push(p); out.push({ op: 'addPrereq', prereq: { b64: p.b64, type: p.type } }); }
                break;
            }
            case 'removePrereq': {
                const p = resolvePrereq(op.prereq, w);
                if (p && !prereqs.some(x => x.b64 === p.b64)) drift.push(`${e.id}: removePrereq: ${p.b64} is not a prerequisite of '${e.expectName}'`);
                if (p) { prereqs = prereqs.filter(x => x.b64 !== p.b64); out.push({ op: 'removePrereq', prereq: { b64: p.b64 } }); }
                break;
            }
            case 'replacePrereq': {
                const f = resolvePrereq(op.from, w), t = resolvePrereq(op.to, w);
                if (!f || !t) break;
                const hit = prereqs.find(x => x.b64 === f.b64);
                if (!hit) drift.push(`${e.id}: replacePrereq: ${f.b64} is not a prerequisite of '${e.expectName}'`);
                if (!knownQuest(t.b64)) error(`${w}: prerequisite ${t.b64} does not exist`);
                prereqs = prereqs.map(x => (x.b64 === f.b64 ? { b64: t.b64, type: hit ? hit.type : t.type } : x));
                out.push({ op: 'replacePrereq', from: { b64: f.b64 }, to: { b64: t.b64 } }); break;
            }
            case 'move': {
                const to = chapterByKey.get(op.to);
                if (!to) { error(`${w}: unknown chapter '${op.to}'`); break; }
                const size = op.size || 24;
                moves.push({ from: exCh.key, to: op.to, x: op.x, y: op.y, size });
                out.push({ op: 'move', from: exCh.b64, to: to.b64, x: op.x, y: op.y, sizeX: size, sizeY: size }); break;
            }
            default: error(`${w}: unknown op`);
            }
            if (op.config && out.length > before) out[out.length - 1].config = op.config.slice();
        }
        const rec = {
            id: e.id, quest: e.quest, expectName: e.expectName, expectChapter: exCh.b64, config: e.config || [],
            progress: e.progress || 'keep', ops: out,
        };
        compiledEdits.push(rec);
        applied.set(e.quest, { edit: e, rec, prereqs, moves, origLines: pq.lines.slice(), desc });
    }
    if (pack.packVersion !== null && data.packVersion !== pack.packVersion) {
        warn(`the pack is pack_version ${pack.packVersion}; the edits were written against ${data.packVersion}`);
    }
    for (const d of drift) error('pack drift: ' + d);

    // Where a quest of ours sits in a world WITHOUT the pack edits (and in the DefaultQuests mirror,
    // which is exactly such a world): its offPos, else shifted clear of the unedited pack by the
    // chapter's offShift. Our own chapters and the fallback quests never shift.
    function offPosOf(rec) {
        if (rec.q.offPos) return rec.q.offPos;
        const pc = data.packChapters[rec.chapter];
        const sh = (pc && pc.offShift && !rec.q.onlyWithout) ? pc.offShift : [0, 0];
        return [rec.q.x + sh[0], rec.q.y + sh[1]];
    }

    // ---- layouts and tidiness
    // A layout = chapter -> [{id, x, y, w, h, touched, label}] plus the prerequisite map in force.
    // 'touched' = placed by us (our quest, or a pack quest we moved). The pack's own icons and
    // lines that we did not change are the pack's design and are never judged.
    function layout(scenario) {
        const editsOn = scenario !== 'off';
        const L = new Map();
        const put = (key, ent) => { if (!L.has(key)) L.set(key, []); L.get(key).push(ent); };
        const label = id => {
            const r = [...ours.values()].find(x => x.b64 === id);
            if (r) return `${r.q.low} ${strip(r.name)}`;
            const p = pack.quests.get(id);
            return p ? `pack '${p.name}'` : id;
        };
        for (const [key, ch] of chapterByKey) {
            if (ch.ours) continue;
            for (const en of ch.packLine.entries) {
                const ap = editsOn ? applied.get(en.id) : null;
                if (ap && ap.moves.length) continue; // re-placed below
                put(key, { id: en.id, x: en.x, y: en.y, w: en.w, h: en.h, touched: false, label: label(en.id) });
            }
        }
        if (editsOn) {
            for (const [id, ap] of applied) {
                if (!ap.moves.length) {
                    continue; // not moved: the entry stays; only its prerequisites may differ (lines)
                }
                for (const mv of ap.moves) put(mv.to, { id, x: mv.x, y: mv.y, w: mv.size, h: mv.size, touched: true, moved: true, label: label(id) });
            }
        }
        for (const rec of ours.values()) {
            const fallback = rec.q.onlyWithout;
            if (fallback && editsOn && applied.has(editById.get(fallback).quest)) continue;
            let x = rec.q.x, y = rec.q.y;
            if (!editsOn) [x, y] = offPosOf(rec);
            put(rec.chapter, { id: rec.b64, x, y, w: rec.size, h: rec.size, touched: true, ours: true, label: label(rec.b64) });
        }
        const packPrereqs = id => {
            const p = pack.quests.get(id);
            return p ? Object.values(p.json['preRequisites:9'] || {}).map(pp => ({ b64: b64Of(pp['questIDHigh:4'], pp['questIDLow:4']), type: pp['type:1'] || 0 })) : [];
        };
        const prereqOf = id => {
            const r = [...ours.values()].find(x => x.b64 === id);
            if (r) return r.prereqs;
            if (editsOn && applied.has(id)) return applied.get(id).prereqs;
            return packPrereqs(id);
        };
        // a line we drew: one end is ours or moved, or the prerequisite itself is our edit's
        const lineIsOurs = (s, e, p) => s.touched || e.touched || !packPrereqs(e.id).some(x => x.b64 === p.b64 && x.type === p.type);
        return { L, prereqOf, lineIsOurs };
    }
    // A quest that (through its prerequisites) needs itself can never unlock. Walk the whole
    // post-edit graph - every pack quest plus ours - and name every cycle that runs through our
    // content or our edits. Only AND quests are followed: an OR / XOR quest (the pack's 'The
    // Choice' ring, the secret Stargate pair) unlocks through any one branch, so it breaks a cycle.
    function checkCycles(scenario) {
        const { prereqOf } = layout(scenario);
        const nodes = new Set([...pack.quests.keys(), ...[...ours.values()].map(r => r.b64)]);
        const name = id => {
            const r = [...ours.values()].find(x => x.b64 === id);
            return r ? `${r.q.low}` : `'${(pack.quests.get(id) || { name: id }).name}'`;
        };
        const state = new Map(); // 1 = on the stack, 2 = done
        const stack = [];
        const seen = new Set();
        const visit = id => {
            state.set(id, 1);
            stack.push(id);
            const pq = pack.quests.get(id);
            const logic = pq ? (pq.json['properties:10']['betterquesting:10']['questLogic:8'] || 'AND') : 'AND';
            for (const p of (logic === 'AND' ? prereqOf(id) : [])) {
                if (!nodes.has(p.b64)) continue;
                const st = state.get(p.b64);
                if (st === 1) {
                    const ids = stack.slice(stack.indexOf(p.b64));
                    const cyc = ids.map(name);
                    const key = [...cyc].sort().join('|');
                    const ourCycle = ids.some(x => !pack.quests.has(x) || applied.has(x));
                    if (ourCycle && !seen.has(key)) { seen.add(key); error(`layout '${scenario}': prerequisite cycle (nothing in it can ever unlock): ${cyc.join(' <- ')} <- ${name(p.b64)}`); }
                } else if (!st) visit(p.b64);
            }
            stack.pop();
            state.set(id, 2);
        };
        for (const id of nodes) if (!state.get(id)) visit(id);
    }
    function segHitsRect(x1, y1, x2, y2, r) {
        // Liang-Barsky against the icon grown by LINE_CLEARANCE: BetterQuesting draws a line 4 units
        // wide (8 for a main quest), so one that only grazes an icon's edge still looks like it hits it
        const m = LINE_CLEARANCE;
        const xmin = r.x - m, ymin = r.y - m, xmax = r.x + r.w + m, ymax = r.y + r.h + m;
        let t0 = 0, t1 = 1;
        const dx = x2 - x1, dy = y2 - y1;
        for (const [p, q] of [[-dx, x1 - xmin], [dx, xmax - x1], [-dy, y1 - ymin], [dy, ymax - y1]]) {
            if (p === 0) { if (q < 0) return false; continue; }
            const t = q / p;
            if (p < 0) { if (t > t1) return false; if (t > t0) t0 = t; } else { if (t < t0) return false; if (t < t1) t1 = t; }
        }
        return t0 < t1;
    }
    const views = {}; // scenario -> chapter key -> { name, ents, lines } for --svg
    // The pack's own crossings, per pack chapter, in its untouched layout: we never ADD a crossing,
    // and we are never blamed for one the pack already draws (even after nudging one of its quests).
    const packCrossings = new Map();
    for (const [key, ch] of chapterByKey) {
        if (ch.ours) continue;
        const set = new Set();
        const ents = ch.packLine.entries;
        const byId = new Map(ents.map(e => [e.id, e]));
        for (const e of ents) {
            const pq = pack.quests.get(e.id);
            if (!pq) continue;
            for (const pp of Object.values(pq.json['preRequisites:9'] || {})) {
                const sid = b64Of(pp['questIDHigh:4'], pp['questIDLow:4']);
                const s0 = byId.get(sid);
                if (!s0) continue;
                for (const o of ents) {
                    if (o === e || o === s0) continue;
                    if (segHitsRect(s0.x + s0.w / 2, s0.y + s0.h / 2, e.x + e.w / 2, e.y + e.h / 2, o)) set.add(`${sid}|${e.id}|${o.id}`);
                }
            }
        }
        packCrossings.set(key, set);
    }
    function checkTidy(scenario, strictLines) {
        const { L, prereqOf, lineIsOurs } = layout(scenario);
        const tag = `layout '${scenario}'`;
        views[scenario] = {};
        const chapterNames = new Map([...chapterByKey].map(([k, c]) => [k, c.name]));
        for (const [key, ents] of L) {
            const cn = chapterNames.get(key);
            const byId = new Map();
            const view = { name: cn, ents, lines: [] };
            views[scenario][key] = view;
            for (const e of ents) {
                if (byId.has(e.id)) error(`${tag}: ${cn}: ${e.label} is placed twice`);
                byId.set(e.id, e);
                if (e.touched && (e.x % GRID || e.y % GRID)) error(`${tag}: ${cn}: ${e.label} at ${e.x},${e.y} is off the ${GRID}-unit grid`);
                if (e.touched && (e.x < 0 || e.y < 0)) error(`${tag}: ${cn}: ${e.label} at ${e.x},${e.y} is at a negative position`);
            }
            for (let i = 0; i < ents.length; i++) for (let j = i + 1; j < ents.length; j++) {
                const a = ents[i], b = ents[j];
                if (!a.touched && !b.touched) continue;
                if (a.x < b.x + b.w && b.x < a.x + a.w && a.y < b.y + b.h && b.y < a.y + a.h) {
                    error(`${tag}: ${cn}: ${a.label} [${a.x},${a.y} s${a.w}] overlaps ${b.label} [${b.x},${b.y} s${b.w}]`);
                }
            }
            for (const e of ents) {
                for (const p of prereqOf(e.id)) {
                    const s = byId.get(p.b64);
                    if (!s || p.type === 2) continue; // other chapter, or HIDDEN: no line drawn
                    const ax = s.x + s.w / 2, ay = s.y + s.h / 2, bx = e.x + e.w / 2, by = e.y + e.h / 2;
                    const ourLine = lineIsOurs(s, e, p);
                    const vl = { ax, ay, bx, by, type: p.type, ours: ourLine, bad: false };
                    view.lines.push(vl);
                    const known = packCrossings.get(key);
                    for (const o of ents) {
                        if (o === e || o === s) continue;
                        if (!ourLine && !o.touched && !e.touched && !s.touched) continue;
                        if (known && known.has(`${s.id}|${e.id}|${o.id}`)) continue;
                        if (segHitsRect(ax, ay, bx, by, o)) {
                            const msg = `${tag}: ${cn}: the ${p.type === 1 ? 'implicit (hover-only) ' : ''}line ${s.label} -> ${e.label} crosses ${o.label}`;
                            if (strictLines && p.type === 0) error(msg); else warn(msg);
                            vl.bad = true; o.hit = true;
                        }
                    }
                }
            }
        }
        return L;
    }
    const layoutOn = checkTidy('on', true);
    // ---- pack chapter edits: a pack chapter our edits leave empty is taken out of the book
    // (BQBridge restores it on uninstall, with edits off, or when one of its quests stays)
    const compiledChapterEdits = [];
    for (const ce of data.packChapterEdits || []) {
        const where = `pack chapter edit ${ce.id}`;
        const ch = chapterByKey.get(ce.chapter);
        if (!ch || ch.ours) { error(`${where}: '${ce.chapter}' is not a pack chapter key`); continue; }
        if (ce.op !== 'hideWhenEmpty') { error(`${where}: unknown op '${ce.op}'`); continue; }
        if (ch.name !== ce.expectName) drift.push(`${ce.id}: chapter ${ch.b64} is named '${ch.name}', expected '${ce.expectName}'`);
        const left = layoutOn.get(ce.chapter) || [];
        if (left.length) error(`${where}: ${ch.name} still holds ${left.length} quest(s) with the edits applied (${left.slice(0, 4).map(e => e.label).join(', ')}): move them, or drop the edit`);
        // a hidden chapter must not be named in any pack quest the player still reads (the pack
        // sends players to "the Endgame Goals tab"): point the text at the new home with a replaceText
        const named = new RegExp('\\b' + ch.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\b', 'i');
        for (const [qid, pq] of pack.quests) {
            const ap = applied.get(qid);
            const text = strip(ap ? ap.desc : (pq.json['properties:10']['betterquesting:10']['desc:8'] || ''));
            if (named.test(text)) error(`${where}: pack quest '${pq.name}' (${qid}) still names the hidden chapter '${ch.name}': add a replaceText edit`);
        }
        if (compiledChapterEdits.some(x => x.id === ce.id)) error(`${where}: duplicate id`);
        compiledChapterEdits.push({ id: ce.id, line: ch.b64, expectName: ce.expectName, op: ce.op });
    }
    for (const d of drift.filter(d => /^C\d+:/.test(d))) error('pack drift: ' + d);
    checkCycles('on');
    checkCycles('off');
    if (!opts.skipFutureLayout) {
        // the same book once every planned feature has shipped (stage variants flip)
        const saved = Object.assign({}, features);
        for (const k of Object.keys(features)) features[k] = true;
        const keep = new Map(applied);
        // re-resolve the stage variants under the future features (moves only)
        for (const e of data.packEdits) {
            if (!e.variants || !applied.has(e.quest)) continue;
            const v = e.variants.find(v => built(v.needs) && !blocked(v.unless));
            const ap = applied.get(e.quest);
            const moves = [];
            let prereqs = ap.prereqs;
            for (const op of (v ? v.ops : [])) {
                if (op.op === 'move') moves.push({ from: e.expectChapter, to: op.to, x: op.x, y: op.y, size: op.size || 24 });
                if (op.op === 'setPrereqs') prereqs = op.prereqs.map(r => resolvePrereq(r, e.id)).filter(Boolean);
                if (op.op === 'removePrereq') { const p = resolvePrereq(op.prereq, e.id); prereqs = prereqs.filter(x => x.b64 !== p.b64); }
            }
            applied.set(e.quest, Object.assign({}, ap, { moves, prereqs }));
        }
        checkTidy('future', true);
        checkCycles('future');
        for (const [k, v] of keep) applied.set(k, v);
        Object.assign(features, saved);
    }
    checkTidy('off', false);

    // ---- outputs
    const files = new Map(); // repo-relative path -> content
    const line = (h, l) => ({ 'questIDHigh:4': BigInt(h), 'questIDLow:4': BigInt(l) });
    function questNbt(rec) {
        const pre = {};
        rec.prereqs.forEach((p, i) => {
            const id = idOfB64(p.b64);
            pre[`${i}:10`] = Object.assign({ 'questIDHigh:4': id.high, 'questIDLow:4': id.low }, p.type ? { 'type:1': p.type } : {});
        });
        return {
            'preRequisites:9': pre,
            'properties:10': {
                'betterquesting:10': {
                    'autoClaim:1': 0, 'completion_animation:8': 'default', 'completion_particle:8': 'default',
                    'confetti_icon:10': { 'Count:3': 0, 'Damage:2': 0, 'OreDict:8': '', 'id:8': 'minecraft:stick' },
                    'countAsQuest:1': rec.countAsQuest, 'desc:8': rec.desc, 'globalShare:1': 0, 'icon:10': rec.icon,
                    'isMain:1': rec.main, 'isSilent:1': 0, 'lockedProgress:1': 0, 'name:8': rec.name,
                    'notification_duration:5': new F32(-1), 'notification_effect:3': -1, 'notification_fade_in:5': new F32(-1),
                    'notification_fade_out:5': new F32(-1), 'notification_icon_offset_y:3': -2147483648,
                    'notification_icon_scale:5': new F32(-1), 'notification_pos_x:3': -2147483648, 'notification_pos_y:3': -2147483648,
                    'notification_show_icon:8': 'default', 'notification_style:8': 'default', 'notification_subtitle:8': '',
                    'notification_subtitle_scale:5': new F32(-1), 'notification_title:8': '', 'notification_title_scale:5': new F32(-1),
                    'particle_count:3': -1, 'questLogic:8': rec.q.questLogic || 'AND', 'repeatTime:3': -1, 'repeat_relative:1': 1,
                    'simultaneous:1': 0, 'snd_complete:8': 'random.levelup', 'snd_update:8': 'random.levelup', 'taskLogic:8': 'AND',
                    'visibility:8': 'NORMAL',
                },
            },
            'questIDHigh:4': BigInt(HIGH), 'questIDLow:4': BigInt(rec.q.low),
            'rewards:9': rec.rewards, 'tasks:9': rec.tasks,
        };
    }
    const placement = (low, x, y, s) => Object.assign(line(HIGH, low), { 'sizeX:3': s, 'sizeY:3': s, 'x:3': x, 'y:3': y });
    function chapterNbt(c) {
        const def = c.def;
        const icon = parseStack(def.icon, `chapter ${c.key} icon`);
        checkExists(icon, `chapter ${c.key} icon`);
        return {
            'properties:10': {
                'betterquesting:10': {
                    'bg_image:8': '', 'bg_size:3': 256, 'desc:8': fmt(def.desc, `chapter ${c.key}`),
                    'icon:10': itemNbt(Object.assign({}, icon, { count: 1 })), 'name:8': fmt.title(def.nameStyled || def.name, `chapter ${c.key}`),
                    'visibility:8': 'UNLOCKED',
                },
            },
            'questLineIDHigh:4': BigInt(HIGH), 'questLineIDLow:4': BigInt(c.low),
        };
    }
    const dq = 'questbook/DefaultQuests';
    const overlayChapters = [], overlayQuests = [], overlayGrafts = [];
    for (const c of data.chapters) {
        const ch = chapterByKey.get(c.key);
        const nbt = chapterNbt(ch);
        files.set(`${dq}/QuestLines/${ch.dir}/QuestLine.json`, stringify(nbt) + '\n');
        const entries = [];
        for (const rec of [...ours.values()].filter(r => r.chapter === c.key)) {
            const pl = placement(rec.q.low, rec.q.x, rec.q.y, rec.size);
            entries.push(pl);
            files.set(`${dq}/QuestLines/${ch.dir}/${rec.stem}-${rec.b64}.json`, stringify(pl) + '\n');
            files.set(`${dq}/Quests/${ch.dir}/${rec.stem}-${rec.b64}.json`, stringify(questNbt(rec)) + '\n');
        }
        overlayChapters.push({ key: c.key, b64: ch.b64, order: 'afterPackLine', after: c.orderAfter, line: nbt, entries });
    }
    // our quests in pack chapters: ADDED files with pinned stems; DefaultQuests holds the
    // edits-off layout (the unedited pack), the bundle carries both positions
    for (const rec of [...ours.values()].filter(r => !chapterByKey.get(r.chapter).ours)) {
        const ch = chapterByKey.get(rec.chapter);
        if (!rec.q.stem) error(`${rec.where}: a quest grafted into a pack chapter needs a pinned 'stem'`);
        const [ox, oy] = offPosOf(rec);
        files.set(`${dq}/QuestLines/${ch.dir}/${rec.stem}-${rec.b64}.json`, stringify(placement(rec.q.low, ox, oy, rec.size)) + '\n');
        files.set(`${dq}/Quests/${ch.dir}/${rec.stem}-${rec.b64}.json`, stringify(questNbt(rec)) + '\n');
        overlayGrafts.push({ quest: rec.q.low, line: ch.b64, on: { x: rec.q.x, y: rec.q.y, size: rec.size }, off: { x: ox, y: oy, size: rec.size } });
    }
    for (const rec of ours.values()) {
        overlayQuests.push(Object.assign({ low: rec.q.low, key: rec.q.key, placeholder: rec.placeholder },
            rec.placeholder ? { needs: rec.q.needs } : {}, rec.q.onlyWithout ? { onlyWithout: rec.q.onlyWithout } : {},
            { nbt: questNbt(rec) }));
    }
    // the plant chapter and its entry quest, read back from what gen-questbook.ps1 just wrote
    let plant = null;
    if (plantDir) {
        const pl = plantDir.lineDir, pq = plantDir.questDir;
        const lineNbt = readTypedJson(path.join(pl, 'QuestLine.json'));
        const entries = fs.readdirSync(pl).filter(f => f !== 'QuestLine.json').sort().map(f => readTypedJson(path.join(pl, f)));
        const quests = fs.readdirSync(pq).sort().map(f => readTypedJson(path.join(pq, f)));
        plant = { key: 'plant', b64: b64Of(HIGH, PLANT_LINE_LOW), order: 'last', line: lineNbt, entries };
        for (const q of quests) overlayQuests.push({ low: Number(q['questIDLow:4']), key: 'plant', placeholder: false, nbt: q });
        if (plantDir.entry) {
            const ent = readTypedJson(plantDir.entry.placement);
            const q = readTypedJson(plantDir.entry.quest);
            overlayQuests.push({ low: Number(q['questIDLow:4']), key: 'plant_entry', placeholder: false, nbt: q });
            overlayGrafts.push({ quest: Number(q['questIDLow:4']), line: plantDir.entry.line, on: { x: ent['x:3'], y: ent['y:3'], size: ent['sizeX:3'] }, off: { x: ent['x:3'], y: ent['y:3'], size: ent['sizeX:3'] } });
        }
        overlayChapters.push(plant);
    }
    overlayQuests.sort((a, b) => a.low - b.low);

    const order = ['# Beyond the Stars: where our chapters go in QuestLinesOrder.txt (README merge step 6).',
        `# Insert these three lines directly AFTER the pack's "${data.packChapters.umv.name}" line (${data.packChapters.umv.id}),`,
        '# in this order; if that line is missing, append them at the end.'];
    for (const c of data.chapters) order.push(`${chapterByKey.get(c.key).b64}: ${c.name}`);
    order.push('# Append this line at the very END of the file (the plant chapter stays parked last).');
    order.push(`${b64Of(HIGH, PLANT_LINE_LOW)}: Beyond the Stars`);
    files.set(`${dq}/OrderLines.txt`, order.join('\n') + '\n');

    // ---- every player-facing text in the bundle, against how the book draws it (PLAYER_TEXT_RULES)
    const lint = (text, where, isName) => { for (const p of playerTextProblems(text, isName)) error(`${where}: ${p}`); };
    const bq = nbt => (nbt['properties:10'] || {})['betterquesting:10'] || {};
    for (const c of overlayChapters) {
        const p = bq(c.line), where = `chapter ${c.key}${c.key === 'plant' ? ' (questbook/gen-questbook.ps1)' : ''}`;
        lint(p['name:8'], where + ' name', true);
        lint(p['desc:8'], where + ' description', false);
    }
    for (const oq of overlayQuests) {
        const p = bq(oq.nbt);
        const where = `quest ${oq.low} '${strip(p['name:8'])}'${oq.key.startsWith('plant') ? ' (questbook/gen-questbook.ps1)' : ''}`;
        lint(p['name:8'], where + ' name', true);
        lint(p['desc:8'], where, false);
    }
    for (const ce of compiledEdits) {
        for (const op of ce.ops) {
            const where = `pack edit ${ce.id} ${op.op}`;
            if (op.op === 'setName') lint(op.name, where, true);
            if (op.op === 'setDesc') lint(op.desc, where, false);
            if (op.op === 'appendText') lint(op.text, where, false);
            if (op.op === 'replaceText') lint(op.with, where, false);
        }
    }

    const bundleCore = {
        format: 1, idHigh: HIGH, packVersion: data.packVersion,
        source: { data: 'questbook/singularity.json', src: opts.srcLabel, srcHead: src.head, manifestSha: manifest ? manifest.sha : null },
        features, orderAfter: data.packChapters.umv.id, chapters: overlayChapters, grafts: overlayGrafts, quests: overlayQuests,
        packEdits: compiledEdits, packChapterEdits: compiledChapterEdits,
    };
    // the version is the CONTENT's hash: where it was generated from (source) does not change it
    const bundleVersion = sha1(stringify(Object.assign({}, bundleCore, { source: null })));
    files.set(OVERLAY_REL, stringify(Object.assign({ bundleVersion }, bundleCore)) + '\n');

    // BTSQuestIds.java
    const lows = key => [...ours.values()].filter(r => r.chapter === key && !r.q.onlyWithout).map(r => r.q.low).sort((a, b) => a - b);
    // spotless's (Eclipse) array wrap: fill each line to column 120, continue with 8 spaces
    const wrapArray = (head, items) => {
        const lines = [];
        let cur = `${head}{ `;
        items.forEach((it, i) => {
            const piece = it + (i < items.length - 1 ? ',' : ' };');
            if ((cur + piece).length > 120 && cur.trim() !== head.trim() + '{') { lines.push(cur.replace(/ $/, '')); cur = '        '; }
            cur += piece + ' ';
        });
        lines.push(cur.replace(/ $/, ''));
        return lines.join('\n');
    };
    const arr = (name, xs, doc) => `    /** ${doc} */\n${wrapArray(`    public static final long[] ${name} = `, xs.map(x => `${x}L`))}\n`;
    const java = [
        '// GENERATED FILE - DO NOT EDIT.',
        '//',
        '// Rendered by tools/gen-singularity.js out of questbook/singularity.json (and, for the plant',
        '// chapter, out of the questbook/DefaultQuests files gen-questbook.ps1 writes). Every quest line and',
        '// quest id Beyond the Stars owns, so /btsquests counts one declared list and no hand-kept copy.',
        '// Regenerate with: powershell -NoProfile -ExecutionPolicy Bypass -File questbook/gen-bom.ps1',
        'package com.nateyburr.beyondthestars;',
        '',
        '/** Every questbook id Beyond the Stars owns (questIDHigh {@link #ID_HIGH}), per chapter. */',
        'public final class BTSQuestIds {',
        '',
        '    private BTSQuestIds() {}',
        '',
        `    public static final long ID_HIGH = ${HIGH}L;`,
        '',
        `    public static final long LINE_PLANT = ${PLANT_LINE_LOW}L;`,
        ...data.chapters.map(c => `    public static final long LINE_${c.key.toUpperCase()} = ${c.low}L;`),
        '',
        `    /** The plant chapter's entry quest, grafted into the pack's "How to Generate Power". */`,
        `    public static final long ENTRY_QUEST = ${ENTRY_LOW}L;`,
        '',
        arr('PLANT_QUESTS', [...plantLows].filter(l => l !== ENTRY_LOW).sort((a, b) => a - b), 'The plant chapter (line 47990).'),
        ...['uiv', 'umv'].map(k => arr(`${k.toUpperCase()}_QUESTS`, lows(k), `Ours, grafted into the pack's ${data.packChapters[k].name}.`)),
        ...data.chapters.map(c => arr(`${c.key.toUpperCase()}_QUESTS`, lows(c.key), `Chapter ${c.name} (line ${c.low}).`)),
        arr('FALLBACK_QUESTS', [...ours.values()].filter(r => r.q.onlyWithout).map(r => r.q.low).sort((a, b) => a - b),
            'Shown only while their pack edit is NOT applied (pack edits off, or drifted).'),
        arr('PLACEHOLDER_QUESTS', [...ours.values()].filter(r => r.placeholder).map(r => r.q.low).sort((a, b) => a - b),
            'Checkbox placeholders in this build: their content is not in the mod yet.'),
        `    /** Every pack edit in the bundle, in application order. */`,
        wrapArray('    public static final String[] PACK_EDITS = ', compiledEdits.map(e => `"${e.id}"`)),
        '}',
    ].join('\n') + '\n';
    files.set(IDS_JAVA_REL, java);

    const ledgerOut = (ledgerText || '# low key - APPEND-ONLY. tools/gen-singularity.js appends new ids and throws if one changes meaning or disappears.\n') +
        ledgerAppend.map(([l, k]) => `${l} ${k}\n`).join('');
    files.set('questbook/id-ledger.txt', ledgerOut);

    const summary = {
        chapters: data.chapters.length,
        quests: ours.size,
        placeholders: [...ours.values()].filter(r => r.placeholder).length,
        fallbacks: [...ours.values()].filter(r => r.q.onlyWithout).length,
        edits: compiledEdits.length,
        bundleVersion,
        layoutOn,
        drift,
        views,
    };
    return { errors, warnings, files, summary };
}

// ------------------------------------------------------------------------------------ main
// --svg <dir>: one picture per chapter and layout. Ours = blue, moved pack quests = gold, the
// pack's own = grey; a line that crosses an icon is red. For eyeballing tidiness (unit Q6).
function writeSvgs(views, dir) {
    fs.mkdirSync(dir, { recursive: true });
    const esc = t => String(t).replace(/&/g, '&amp;').replace(/</g, '&lt;');
    for (const [scn, chs] of Object.entries(views)) {
        for (const [key, v] of Object.entries(chs)) {
            const maxX = Math.max(...v.ents.map(e => e.x + e.w)) + 24, maxY = Math.max(...v.ents.map(e => e.y + e.h)) + 24;
            const S = 1.6;
            const out = [`<svg xmlns="http://www.w3.org/2000/svg" width="${maxX * S}" height="${maxY * S}" viewBox="0 0 ${maxX} ${maxY}" font-family="sans-serif">`,
                `<rect width="100%" height="100%" fill="#1e1e24"/>`];
            for (let gx = 0; gx < maxX; gx += 24) out.push(`<line x1="${gx}" y1="0" x2="${gx}" y2="${maxY}" stroke="#2a2a33" stroke-width="0.5"/>`);
            for (let gy = 0; gy < maxY; gy += 24) out.push(`<line x1="0" y1="${gy}" x2="${maxX}" y2="${gy}" stroke="#2a2a33" stroke-width="0.5"/>`);
            for (const l of v.lines) {
                const col = l.bad ? '#ff3b3b' : (l.ours ? '#7fb2ff' : '#555');
                out.push(`<line x1="${l.ax}" y1="${l.ay}" x2="${l.bx}" y2="${l.by}" stroke="${col}" stroke-width="${l.bad ? 2 : 1}"${l.type === 1 ? ' stroke-dasharray="3,3"' : ''}/>`);
            }
            for (const e of v.ents) {
                const fill = e.ours ? '#2f5d9e' : (e.touched ? '#9e7a2f' : '#44444c');
                out.push(`<rect x="${e.x}" y="${e.y}" width="${e.w}" height="${e.h}" fill="${fill}" stroke="${e.hit ? '#ff3b3b' : '#ddd'}" stroke-width="${e.hit ? 1.5 : 0.5}"/>`);
                const lab = e.label.replace(/^pack '/, '').replace(/'$/, '').replace(/ \(planned\)$/, '*');
                out.push(`<text x="${e.x + e.w / 2}" y="${e.y + e.h + 7}" font-size="5.5" fill="#eee" text-anchor="middle">${esc(lab.slice(0, 34))}</text>`);
            }
            out.push(`<text x="4" y="10" font-size="9" fill="#fff">${esc(v.name)} - layout '${scn}'</text></svg>`);
            fs.writeFileSync(path.join(dir, `${scn}-${key}.svg`), out.join('\n'));
        }
    }
}

function parseArgs(argv) {
    const o = { featureOverrides: {} };
    for (let i = 2; i < argv.length; i++) {
        const a = argv[i];
        if (a === '--src') o.src = argv[++i];
        else if (a === '--pack') o.pack = argv[++i];
        else if (a === '--check') o.check = true;
        else if (a === '--selftest') o.selftest = true;
        else if (a === '--svg') o.svg = argv[++i];
        else if (a === '--verbose') o.verbose = true;
        else if (a === '--feature') {
            const [k, v] = argv[++i].split('=');
            o.featureOverrides[k] = v === 'true';
        } else throw new Error(`unknown argument ${a}`);
    }
    return o;
}

function loadInputs(args) {
    const dataFile = path.join(QB, 'singularity.json');
    const raw = fs.readFileSync(dataFile, 'utf8');
    const bad = [...raw].findIndex(ch => ch.charCodeAt(0) > 126 || (ch.charCodeAt(0) < 32 && ch !== '\n' && ch !== '\r'));
    if (bad >= 0) throw new Error(`questbook/singularity.json is not ASCII at offset ${bad} (line ${raw.slice(0, bad).split('\n').length}); use {tokens}`);
    const data = JSON.parse(raw);
    const registry = JSON.parse(fs.readFileSync(path.join(QB, 'registry.json'), 'utf8'));
    // relative paths are relative to the repo root, wherever the script is run from (gen-bom.ps1 runs in questbook/)
    const srcRoot = path.resolve(REPO, args.src || REPO);
    const packDir = path.resolve(REPO, args.pack || DEFAULT_PACK);
    if (!fs.existsSync(path.join(packDir, 'Quests'))) throw new Error(`no pack DefaultQuests at ${packDir} (use --pack)`);
    const src = scanSource(srcRoot);
    if (!src.manifest) throw new Error(`no structures/manifest.json under ${srcRoot}`);
    const pack = readPack(packDir);
    const ledgerFile = path.join(QB, 'id-ledger.txt');
    const ledgerText = fs.existsSync(ledgerFile) ? fs.readFileSync(ledgerFile, 'utf8') : '';
    // the plant chapter, as gen-questbook.ps1 wrote it
    const dq = path.join(QB, 'DefaultQuests');
    const plantB64 = b64Of(data.idHigh, PLANT_LINE_LOW);
    const findDir = (sub, suffix) => {
        const d = path.join(dq, sub);
        if (!fs.existsSync(d)) return null;
        const hit = fs.readdirSync(d).find(n => n.endsWith('-' + suffix));
        return hit ? path.join(d, hit) : null;
    };
    const lineDir = findDir('QuestLines', plantB64), questDir = findDir('Quests', plantB64);
    if (!lineDir || !questDir) throw new Error('the plant chapter is not in questbook/DefaultQuests - run questbook/gen-bom.ps1 (it runs gen-questbook.ps1, then this)');
    const plantLows = fs.readdirSync(questDir).map(f => Number(readTypedJson(path.join(questDir, f))['questIDLow:4']));
    const entryB64 = b64Of(data.idHigh, ENTRY_LOW);
    let entry = null;
    for (const sub of fs.readdirSync(path.join(dq, 'Quests'))) {
        const f = path.join(dq, 'Quests', sub, `UnitOne-${entryB64}.json`);
        if (fs.existsSync(f)) {
            const lineB64 = sub.slice(sub.indexOf('-') + 1);
            entry = { quest: f, placement: path.join(dq, 'QuestLines', sub, `UnitOne-${entryB64}.json`), line: lineB64 };
        }
    }
    if (entry) plantLows.push(ENTRY_LOW);
    return {
        data, registry, src, pack, ledgerText, srcRoot, packDir,
        plantDir: { lineDir, questDir, entry }, plantLows,
        srcLabel: (path.relative(REPO, srcRoot) || '.').replace(/\\/g, '/'),
    };
}

function runBuild(inp, data, extra) {
    return build(data, Object.assign({
        src: inp.src, pack: inp.pack, registry: inp.registry, ledgerText: inp.ledgerText, plantDir: inp.plantDir,
        plantLows: inp.plantLows, srcRoot: inp.srcRoot, packDir: inp.packDir, srcLabel: inp.srcLabel,
    }, extra));
}

// Paths this generator owns inside questbook/DefaultQuests: cleaned before writing so a renamed
// quest leaves no stale file (the repo copies of pack folders only ever hold OUR files).
function ownedDirs(data, inp) {
    const out = [];
    for (const c of data.chapters) out.push(`${c.dirStem}-${b64Of(data.idHigh, c.low)}`);
    for (const k of ['uiv', 'umv']) {
        const line = inp.pack.lines.get(data.packChapters[k].id);
        if (line) out.push(line.dir);
    }
    return out;
}

function main() {
    const args = parseArgs(process.argv);
    const inp = loadInputs(args);
    if (args.selftest) return selftest(inp);
    const res = runBuild(inp, inp.data, { featureOverrides: args.featureOverrides });
    // Hover-only (implicit) crossings and the edits-off layout are informational: counted, and
    // listed with --verbose. Everything else is printed.
    const quiet = w => /implicit \(hover-only\)/.test(w) || /^layout 'off'/.test(w);
    for (const w of res.warnings) if (args.verbose || !quiet(w)) console.log('warning: ' + w);
    const hidden = res.warnings.filter(quiet).length;
    if (hidden && !args.verbose) console.log(`note: ${hidden} informational layout notes (hover-only lines, the edits-off layout); --verbose lists them`);
    if (args.svg) writeSvgs(res.summary.views, path.resolve(args.svg));
    const driftFile = path.join(QB, 'pack-drift.txt');
    if (res.summary.drift.length && !args.check) {
        fs.writeFileSync(driftFile, `# pack drift against ${inp.packDir} (pack_version ${inp.pack.packVersion})\n` + res.summary.drift.join('\n') + '\n');
    } else if (!res.summary.drift.length && fs.existsSync(driftFile) && !args.check) fs.unlinkSync(driftFile);
    if (res.errors.length) {
        console.error(`gen-singularity: ${res.errors.length} error(s):`);
        for (const e of res.errors) console.error('  - ' + e);
        if (res.errors.some(e => /not registered in/.test(e))) {
            console.error(`hint: ids are checked against the Java of ${inp.srcLabel}. A branch that lacks a merged feature ` +
                '(a worktree cut before the feature reached master) is checked against a tree that has it with ' +
                '--src <tree> (gen-bom.ps1: set BTS_QB_SRC), or the feature flag is set false in singularity.json.');
        }
        process.exit(1);
    }
    const dq = path.join(QB, 'DefaultQuests');
    if (args.check) {
        let stale = 0;
        for (const [rel, content] of res.files) {
            const f = path.join(REPO, rel);
            const cur = fs.existsSync(f) ? fs.readFileSync(f, 'utf8') : null;
            // The overlay records the commit it was generated from (srcHead); every later commit moves
            // HEAD, so that one field is not drift. Anything else in the file still is.
            const norm = s => (rel.endsWith('.java') ? s.replace(/\s+/g, '')
                : rel === OVERLAY_REL ? s.replace(/("srcHead":\s*)"[^"]*"/, '$1""') : s);
            if (cur === null || norm(cur) !== norm(content)) { console.error('stale: ' + rel); stale++; }
        }
        if (stale) process.exit(1);
        console.log(`gen-singularity --check: ${res.files.size} generated files are up to date (bundle ${res.summary.bundleVersion.slice(0, 12)}).`);
        return;
    }
    for (const d of ownedDirs(inp.data, inp)) {
        for (const sub of ['QuestLines', 'Quests']) fs.rmSync(path.join(dq, sub, d), { recursive: true, force: true });
    }
    fs.rmSync(path.join(dq, 'OrderLine.txt'), { force: true });
    for (const [rel, content] of res.files) {
        const f = path.join(REPO, rel);
        fs.mkdirSync(path.dirname(f), { recursive: true });
        fs.writeFileSync(f, content);
    }
    const s = res.summary;
    console.log(`gen-singularity: ${s.chapters} chapters, ${s.quests} quests of ours (${s.placeholders} checkbox placeholders, ${s.fallbacks} fallbacks), ` +
        `${s.edits} pack edits; bundle ${s.bundleVersion.slice(0, 12)}; source ${inp.srcLabel} (${inp.src.head}); pack ${inp.packDir} (pack_version ${inp.pack.packVersion}).`);
    console.log(`  wrote ${res.files.size} files: questbook/DefaultQuests (ladder chapters + grafts + OrderLines.txt), ${OVERLAY_REL}, ${IDS_JAVA_REL}, questbook/id-ledger.txt`);
}

// Break the data on purpose and prove each assertion fires (docs/questbook-integration.md §6 Q2).
function selftest(inp) {
    const clone = () => JSON.parse(JSON.stringify(inp.data));
    const q = (d, low) => d.quests.find(x => x.low === low);
    const cases = [
        ['clean data builds', d => d, null],
        ['two quests overlap', d => { const a = q(d, 48141), b = q(d, 48140); a.x = b.x + 12; a.y = b.y; return d; }, /overlaps/],
        ['a line crosses a quest', d => { q(d, 48116).prereqs.push(48119); return d; }, /crosses/],
        ['a drifted pack name', d => { d.packEdits[0].expectName = 'Not The Pack Name'; return d; }, /pack drift/],
        ['an unknown item', d => { q(d, 48100).tasks[0].items = ['1x beyondthestars:noSuchItem']; return d; }, /not registered/],
        ['an unknown fluid', d => { q(d, 48116).tasks[0].fluids = ['1000x fluid:btsnosuchfluid']; return d; }, /not registered/],
        ['a duplicate id', d => { d.quests.push(Object.assign({}, q(d, 48100), { key: 'dupe' })); return d; }, /duplicate id/],
        ['an id outside its range', d => { q(d, 48220).low = 48300; return d; }, /outside chapter/],
        ['off the grid', d => { q(d, 48111).x += 5; return d; }, /grid/],
        ['a missing prerequisite', d => { q(d, 48100).prereqs.push('pack:zzzzzzzzzzzzzzzzzzzzzw=='); return d; }, /exists neither/],
        ['a replaceText that no longer matches', d => { d.packEdits.find(e => e.id === 'E04').ops[0].find = 'no such sentence'; return d; }, /replaceText/],
        ['an id changing meaning in the ledger', d => { q(d, 48100).key = 'renamed_key'; return d; }, /change meaning/],
        ['a shipped id disappearing', d => { d.quests = d.quests.filter(x => x.low !== 48193); return d; }, /is gone/],
        ['a prerequisite cycle', d => { q(d, 48114).prereqs.push(48117); return d; }, /prerequisite cycle/],
        ['an op gated on a flag Config does not have', d => {
            d.packEdits.find(e => e.id === 'E38').ops[0].config = ['noSuchConfigFlag']; return d;
        }, /not a boolean in Config/],
        ['a placeholder shipping with a tbd task', d => { d.features.maxParts = true; return d; }, /still 'tbd'/],
        ['a bill role reassigned in the manifest', d => { d.bomItems.eye.band_shell.expect = 'NotThisBlock'; return d; }, /bomItems expects/],
        ['a long bold run', d => { q(d, 48100).desc = ['{5}{l}This whole sentence is bold and will be cut off.{r}']; return d; }, /bold run of/],
        ['a low-contrast colour', d => { q(d, 48100).desc = ['A {b}light aqua{r} word.']; return d; }, /unreadable on the book/],
        ['gold in a description', d => { q(d, 48100).desc = ['A {6}gold{r} key line.']; return d; }, /code 6 \(gold\) is unreadable/],
        ['an odd glyph', d => { q(d, 48100).desc = ['{u+221a}M']; return d; }, /odd glyph/],
        ['a pack quest naming a hidden chapter', d => { d.packEdits = d.packEdits.filter(x => x.id !== 'E66'); return d; }, /names the hidden chapter/],
        ['a hidden pack chapter that still holds a quest', d => { const e = d.packEdits.find(x => x.id === 'E64'); e.ops = e.ops.filter(o => o.op !== 'move'); return d; }, /still holds/],
    ];
    let failed = 0;
    for (const [name, mut, expect] of cases) {
        const d = mut(clone());
        const res = runBuild(inp, d, { skipFutureLayout: false });
        const hit = expect ? res.errors.some(e => expect.test(e)) : res.errors.length === 0;
        console.log(`${hit ? 'ok  ' : 'FAIL'}  ${name}${expect ? ` -> ${(res.errors.find(e => expect.test(e)) || res.errors[0] || 'no error').slice(0, 150)}` : res.errors.length ? ' -> ' + res.errors[0] : ''}`);
        if (!hit) failed++;
    }
    // wrapSafe: what it turns the two round-2 failures into, that it is idempotent, and that the
    // lint rejects text that skipped it (the way a hand-edited plant chapter would arrive)
    const S = SEC;
    const ws = [
        [`${S}2${S}lDONE WHEN${S}r you hold it`, `${S}2${S}lDONE ${S}lWHEN${S}r you hold it`],
        [`and ${S}91.51 ${GLYPHS.times} 10${GLYPHS.sup11} EU${S}r (about`, `and${S}9 1.51 ${GLYPHS.times} 10${GLYPHS.sup11} EU${S}r (about`],
        [`a ${S}9${S}l4 h Resonant${S}r, a`, `a${S}9${S}l ${S}l4 ${S}lh ${S}lResonant${S}r, a`],
        [`${S}5${S}lBUILD${S}r\n- plain ${S}9words${S}r here`, `${S}5${S}lBUILD${S}r\n- plain ${S}9words${S}r here`],
        [`Derrick${S}r (${S}21,920 blocks)`, `Derrick${S}r ${S}2(1,920 blocks)`],
        [`run /btsmigrate ${S}8(operator)${S}r. It reads`, `run /btsmigrate ${S}8(operator).${S}r It reads`],
    ];
    for (const [input, want] of ws) {
        const got = wrapSafe(input), again = wrapSafe(got);
        const ok = got === want && again === got && playerTextProblems(input, false).some(p => /wrap-safe/.test(p)) === (input !== want);
        console.log(`${ok ? 'ok  ' : 'FAIL'}  wrapSafe ${JSON.stringify(input.replace(/§/g, '&'))} -> ${JSON.stringify(got.replace(/§/g, '&'))}`);
        if (!ok) failed++;
    }
    if (failed) { console.error(`selftest: ${failed} case(s) failed`); process.exit(1); }
    console.log('selftest: every assertion fires on its broken input, and the real data builds clean.');
}

if (require.main === module) main();
else module.exports = { build, loadInputs, runBuild, parseArgs, b64Of };
