// Enterprise Agentic Patterns - Interactive Visualization
const patterns = [
    {
        id: 1,
        name: "Task Execution",
        subtitle: "Accounts payable: unstructured invoice to ERP-ready record",
        flowType: "Code-driven",
        whoDecides: "Code decides",
        endpoint: "/api/task-execution/invoices",
        flow: `graph TD
            A[📄 Unstructured Invoice] --> B[🤖 LLM Extractor<br/>TaskExecutionAgent]
            B --> C[📋 Typed Invoice Record]
            C --> D[✓ InvoiceValidator<br/>Plain Java Code]
            D --> E{Valid?}
            E -->|Yes| F[✅ AUTO_POST]
            E -->|No| G[⚠️ MANUAL_REVIEW]
            style A fill:#6366f1,stroke:#8b5cf6,color:#fff
            style B fill:#8b5cf6,stroke:#a855f7,color:#fff
            style C fill:#06b6d4,stroke:#22d3ee,color:#fff
            style D fill:#10b981,stroke:#34d399,color:#fff
            style F fill:#10b981,stroke:#34d399,color:#fff
            style G fill:#f59e0b,stroke:#fbbf24,color:#fff`,
        description: "<strong>Pattern:</strong> One prompt template in, one typed record out. The LLM reads and extracts, <strong>code decides</strong> if it can be auto-posted. Validation is deterministic business logic.",
        samples: [
            { name: "1-task-execution-invoice.json (valid)", file: "1-task-execution-invoice.json" },
            { name: "1-task-execution-invoice-with-errors.json (broken)", file: "1-task-execution-invoice-with-errors.json" }
        ]
    },
    {
        id: 2,
        name: "Mono (Agent + Tools)",
        subtitle: "IT service desk: unlock accounts, open tickets",
        flowType: "Agentic",
        whoDecides: "LLM + Policy in Tools",
        endpoint: "/api/mono/service-desk",
        flow: `graph TD
            A[👤 Employee Request] --> B[🧠 Mono Agent<br/>ChatClient + Tools]
            B --> C{Need Tool?}
            C -->|Yes| D[🔧 Tools<br/>lookupEmployee, getAccountStatus, unlockAccount, createTicket]
            D --> E[🛡️ Policy Enforcement in Code]
            E --> B
            C -->|No| F[💬 Response + Audit Trail]
            style A fill:#6366f1,stroke:#8b5cf6,color:#fff
            style B fill:#8b5cf6,stroke:#a855f7,color:#fff
            style D fill:#06b6d4,stroke:#22d3ee,color:#fff
            style E fill:#ef4444,stroke:#f87171,color:#fff
            style F fill:#10b981,stroke:#34d399,color:#fff`,
        description: "<strong>Pattern:</strong> One agent, multiple tools. LLM decides the flow, but <strong>policy lives in tools</strong>, not prompts. Tools record audit trails independently of what the model says.",
        samples: [
            { name: "2-mono-service-desk.json (normal)", file: "2-mono-service-desk.json" },
            { name: "2-mono-service-desk-security-hold.json (security hold)", file: "2-mono-service-desk-security-hold.json" }
        ]
    },
    {
        id: 3,
        name: "Chain",
        subtitle: "Legal: contract review against the playbook",
        flowType: "Sequential",
        whoDecides: "Code orchestrated",
        endpoint: "/api/chain/contract-review",
        flow: `graph LR
            A[📑 Contract Text] --> B[1️⃣ Extract Terms<br/>Fast Model]
            B --> C[2️⃣ Check vs Playbook<br/>Code Gate]
            C --> D{Deviations?}
            D -->|No| E[✅ APPROVED_PER_PLAYBOOK<br/>Stop Early]
            D -->|Yes| F[3️⃣ Assess Risk<br/>Smart Model]
            F --> G[4️⃣ Draft Memo<br/>Smart Model]
            G --> H[📊 Review Result]
            style A fill:#6366f1,stroke:#8b5cf6,color:#fff
            style B fill:#8b5cf6,stroke:#a855f7,color:#fff
            style C fill:#10b981,stroke:#34d399,color:#fff
            style E fill:#10b981,stroke:#34d399,color:#fff
            style F fill:#06b6d4,stroke:#22d3ee,color:#fff
            style G fill:#06b6d4,stroke:#22d3ee,color:#fff`,
        description: "<strong>Pattern:</strong> Sequential steps where each feeds the next. A code gate can short-circuit the chain early for efficiency and compliance.",
        samples: [
            { name: "3-chain-contract-review.json (deviations -> negotiate)", file: "3-chain-contract-review.json" },
            { name: "3-chain-contract-review-compliant.json (stops early)", file: "3-chain-contract-review-compliant.json" }
        ]
    },
    {
        id: 4,
        name: "Parallelization",
        subtitle: "Procurement: supplier due diligence in 4 dimensions",
        flowType: "Parallel Fan-out",
        whoDecides: "Code aggregates",
        endpoint: "/api/parallelization/supplier-due-diligence",
        flow: `graph TD
            A[🏢 Supplier Info] --> B[🎯 Orchestrator]
            B --> C1[💰 Financial Stability<br/>Worker 1]
            B --> C2[🔒 Info Security<br/>Worker 2]
            B --> C3[🌱 ESG<br/>Worker 3]
            B --> C4[⚖️ Legal/Compliance<br/>Worker 4]
            C1 --> D[🔀 Aggregate Results]
            C2 --> D
            C3 --> D
            C4 --> D
            D --> E[📊 Risk Calculation<br/>Code]
            E --> F[📝 Executive Summary<br/>LLM]
            style A fill:#6366f1,stroke:#8b5cf6,color:#fff
            style B fill:#8b5cf6,stroke:#a855f7,color:#fff
            style C1 fill:#06b6d4,stroke:#22d3ee,color:#fff
            style C2 fill:#06b6d4,stroke:#22d3ee,color:#fff
            style C3 fill:#06b6d4,stroke:#22d3ee,color:#fff
            style C4 fill:#06b6d4,stroke:#22d3ee,color:#fff
            style D fill:#f59e0b,stroke:#fbbf24,color:#fff
            style E fill:#10b981,stroke:#34d399,color:#fff`,
        description: "<strong>Pattern:</strong> Independent subtasks run in parallel on virtual threads. Code does aggregation and risk calculation; LLM only writes the summary. Elapsed time ≈ slowest dimension, not sum.",
        samples: [
            { name: "4-parallelization-supplier-due-diligence.json", file: "4-parallelization-supplier-due-diligence.json" }
        ]
    },
    {
        id: 5,
        name: "Evaluator-Optimizer",
        subtitle: "Compliance: GDPR / revFADP data breach notification",
        flowType: "Feedback Loop",
        whoDecides: "Code-bounded loop + LLM judge",
        endpoint: "/api/evaluator-optimizer/breach-notification",
        flow: `graph TD
            A[🚨 Breach Details] --> B[✍️ Writer<br/>Draft Notice]
            B --> C[⚖️ Evaluator/Judge<br/>Different Model]
            C --> D{Passes Checklist?}
            D -->|Yes| E[✅ Approved<br/>approvedForSending=true]
            D -->|No + Rounds < Max| F[🔄 Optimizer/Writer<br/>Fix Issues]
            F --> C
            D -->|No + Max Rounds| G[⚠️ Best Attempt<br/>Human Review]
            H[🛡️ Hard Rules<br/>Code Override] -.-> E
            H -.-> G
            style A fill:#6366f1,stroke:#8b5cf6,color:#fff
            style B fill:#8b5cf6,stroke:#a855f7,color:#fff
            style C fill:#f59e0b,stroke:#fbbf24,color:#fff
            style E fill:#10b981,stroke:#34d399,color:#fff
            style F fill:#06b6d4,stroke:#22d3ee,color:#fff
            style G fill:#ef4444,stroke:#f87171,color:#fff
            style H fill:#ef4444,stroke:#f87171,color:#fff`,
        description: "<strong>Pattern:</strong> Writer creates, separate judge evaluates against checklist. Code enforces hard rules the judge cannot override (DPO contact, word limit, banned phrases). Bounded loop with full audit trail.",
        samples: [
            { name: "5-evaluator-optimizer-breach-notification.json", file: "5-evaluator-optimizer-breach-notification.json" }
        ]
    },
    {
        id: 6,
        name: "Routing",
        subtitle: "Insurance: triage of the customer inbox",
        flowType: "Classification + Dispatch",
        whoDecides: "LLM routes, code owns",
        endpoint: "/api/routing/insurance-inbox",
        flow: `graph TD
            A[📧 Customer Email] --> B[🧭 Router<br/>LLM Classifier]
            B --> C{Route + Confidence}
            C -->|confidence >= 0.7 & valid| D[🎯 Route Handler]
            C -->|unknown or < 0.7| E[👤 human-review<br/>Fallback]
            D --> F1[🚗 motor-claim<br/>LLM Assistant]
            D --> F2[🏠 property-claim<br/>LLM Assistant]
            D --> F4[💳 billing<br/>LLM Assistant]
            D --> F3[📋 complaint<br/>Code Only - No LLM]
            F1 --> G[✅ Case Outcome]
            F2 --> G
            F4 --> G
            F3 --> G
            E --> G
            style A fill:#6366f1,stroke:#8b5cf6,color:#fff
            style B fill:#8b5cf6,stroke:#a855f7,color:#fff
            style E fill:#f59e0b,stroke:#fbbf24,color:#fff
            style F3 fill:#10b981,stroke:#34d399,color:#fff
            style G fill:#06b6d4,stroke:#22d3ee,color:#fff`,
        description: "<strong>Pattern:</strong> LLM sees only route names and descriptions, returns route+reason+confidence. Code owns dispatch, fallbacks, and SLAs. Some routes can be pure code (regulatory deadlines).",
        samples: [
            { name: "6-routing-motor-claim.json (LLM handler)", file: "6-routing-motor-claim.json" },
            { name: "6-routing-complaint.json (code handles)", file: "6-routing-complaint.json" },
            { name: "6-routing-unclear.json (low confidence -> human)", file: "6-routing-unclear.json" }
        ]
    },
    {
        id: 7,
        name: "Graph (DAG)",
        subtitle: "Banking: KYC / AML client onboarding",
        flowType: "Directed Acyclic Graph",
        whoDecides: "Code topology",
        endpoint: "/api/graph/kyc-onboarding",
        flow: `graph TD
            A[📋 Application Form + News] --> B[extract-applicant<br/>LLM]
            B --> C[sanctions-screening<br/>CODE - Deterministic]
            B --> D[adverse-media<br/>LLM]
            B --> E[source-of-funds<br/>LLM]
            C --> F[risk-decision<br/>LLM]
            D --> F
            E --> F
            F --> G{Code Override}
            G -->|Sanctions Hit| H[❌ REJECT<br/>Forced by Code]
            G -->|No Hit| I[📊 Final Decision]
            style A fill:#6366f1,stroke:#8b5cf6,color:#fff
            style B fill:#8b5cf6,stroke:#a855f7,color:#fff
            style C fill:#10b981,stroke:#34d399,color:#fff
            style D fill:#06b6d4,stroke:#22d3ee,color:#fff
            style E fill:#06b6d4,stroke:#22d3ee,color:#fff
            style F fill:#8b5cf6,stroke:#a855f7,color:#fff
            style H fill:#ef4444,stroke:#f87171,color:#fff
            style I fill:#06b6d4,stroke:#22d3ee,color:#fff`,
        description: "<strong>Pattern:</strong> Fixed DAG at build time, cycles rejected. Parallel batches execute together. Some nodes are deterministic code (sanctions screening) - regulated and reproducible with versioning. Code can override LLM decisions.",
        samples: [
            { name: "7-graph-kyc-onboarding.json (normal)", file: "7-graph-kyc-onboarding.json" },
            { name: "7-graph-kyc-sanctions-hit.json (code override)", file: "7-graph-kyc-sanctions-hit.json" }
        ]
    },
    {
        id: 8,
        name: "Orchestrator-Workers",
        subtitle: "SRE: incident root-cause analysis and postmortem",
        flowType: "Dynamic Orchestration",
        whoDecides: "LLM Orchestrator",
        endpoint: "/api/orchestrator/incident-analysis",
        flow: `graph TD
            A[🚨 Incident Alert] --> B[🎯 Incident Commander<br/>Smart Model Orchestrator]
            B --> C{Decides Delegation}
            C --> D1[📊 log_analyst<br/>Private Tools]
            C --> D2[📈 metrics_analyst<br/>Private Tools]
            C --> D3[🚀 deployment_history<br/>Plain Code]
            D1 --> B
            D2 --> B
            D3 --> B
            B --> E{Sufficient Info?}
            E -->|No| C
            E -->|Yes| F[📝 Postmortem<br/>Typed Result]
            style A fill:#6366f1,stroke:#8b5cf6,color:#fff
            style B fill:#8b5cf6,stroke:#a855f7,color:#fff
            style D1 fill:#06b6d4,stroke:#22d3ee,color:#fff
            style D2 fill:#06b6d4,stroke:#22d3ee,color:#fff
            style D3 fill:#10b981,stroke:#34d399,color:#fff
            style F fill:#10b981,stroke:#34d399,color:#fff`,
        description: "<strong>Pattern:</strong> Orchestrator has no direct access to logs/metrics. It dynamically delegates to specialized workers with private tools. Delegation path is recorded by code. Finds root cause among red herrings.",
        samples: [
            { name: "8-orchestrator-incident-analysis.json", file: "8-orchestrator-incident-analysis.json" }
        ]
    },
    {
        id: 9,
        name: "Swarm",
        subtitle: "Procurement: SaaS purchase approval across departments",
        flowType: "Peer-to-Peer",
        whoDecides: "LLM Peers + Code Guardrails",
        endpoint: "/api/swarm/saas-procurement",
        flow: `graph TD
            A[🛒 Procurement Request] --> B[🤝 Swarm of Peers]
            B --> C1[📥 intake<br/>Agent]
            B --> C2[🔒 security<br/>Agent]
            B --> C3[⚖️ legal<br/>Agent]
            B --> C4[💰 finance<br/>Agent]
            C1 <-->|handoff_to_agent| C2
            C2 <-->|handoff_to_agent| C3
            C3 <-->|handoff_to_agent| C4
            C4 <-->|handoff_to_agent| C1
            B --> D[📁 Shared Case File]
            D --> E[🛡️ Guardrails<br/>max handoffs, ping-pong, unknown peers]
            E --> F{Done or Escalate?}
            F -->|Consensus| G[✅ Decision]
            F -->|Escalate| H[👤 Human/Escalation Committee]
            style A fill:#6366f1,stroke:#8b5cf6,color:#fff
            style B fill:#8b5cf6,stroke:#a855f7,color:#fff
            style C1 fill:#06b6d4,stroke:#22d3ee,color:#fff
            style C2 fill:#06b6d4,stroke:#22d3ee,color:#fff
            style C3 fill:#06b6d4,stroke:#22d3ee,color:#fff
            style C4 fill:#06b6d4,stroke:#22d3ee,color:#fff
            style D fill:#f59e0b,stroke:#fbbf24,color:#fff
            style E fill:#ef4444,stroke:#f87171,color:#fff
            style G fill:#10b981,stroke:#34d399,color:#fff
            style H fill:#f59e0b,stroke:#fbbf24,color:#fff`,
        description: "<strong>Pattern:</strong> No boss - peers coordinate via handoffs. Each owns a system of record. Code enforces guardrails (handoff budget, ping-pong detection). Full path and case file tracked.",
        samples: [
            { name: "9-swarm-saas-procurement.json (normal)", file: "9-swarm-saas-procurement.json" },
            { name: "9-swarm-saas-procurement-blocked.json (risky data)", file: "9-swarm-saas-procurement-blocked.json" }
        ]
    }
];

// The four stages from the talk's stage map: autonomy grows from Mono to Swarm, and so does the guard-rail cost.
const stages = [
    { id: 1, name: "Mono", color: "#acca58", picks: "LLM picks the tools", decides: "The single agent",
        loop: "1 loop", patterns: [1, 2] },
    { id: 2, name: "Graph", color: "#00add7", picks: "Your code picks the edges", decides: "The topology (your code)",
        loop: "N loops with merge points", patterns: [3, 4, 5, 6, 7] },
    { id: 3, name: "Orchestrator", color: "#f28f00", picks: "One LLM picks the agents", decides: "A coordinator agent",
        loop: "1 outer + N inner loops", patterns: [8] },
    { id: 4, name: "Swarm", color: "#dc006b", picks: "Every peer picks the next", decides: "The peers themselves",
        loop: "Chained loops, peer handoffs", patterns: [9] }
];

const stageOf = patternId => stages.find(s => s.patterns.includes(patternId));

let currentPattern = null;
// Incremented on every pattern switch and run, so late responses never render into the wrong pattern.
let runToken = 0;
const sampleCache = new Map();

// --- Rendering helpers ------------------------------------------------------

// Model output is untrusted text: always escape before it goes into innerHTML.
function esc(value) {
    if (value === null || value === undefined || value === '') return '—';
    const text = typeof value === 'object' ? JSON.stringify(value) : String(value);
    return text.replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

const asArray = value => (Array.isArray(value) ? value : []);

const BADGE_CLASSES = {
    success: ['LOW', 'APPROVE', 'APPROVED_PER_PLAYBOOK', 'AUTO_POST', 'COMPLETED', 'YES', 'PASSED'],
    warning: ['MEDIUM', 'APPROVE_WITH_CONDITIONS', 'NEGOTIATE', 'MANUAL_REVIEW', 'ENHANCED_DUE_DILIGENCE',
        'MAX_HANDOFFS_REACHED', 'PING_PONG_DETECTED', 'UNKNOWN_AGENT', 'FALLBACK', 'NEEDS FIXES'],
    danger: ['HIGH', 'CRITICAL', 'REJECT', 'ESCALATE_TO_RISK_COMMITTEE', 'NO', 'HIT']
};

function badge(value, cls) {
    const text = value === null || value === undefined ? '—' : String(value);
    const kind = cls || Object.keys(BADGE_CLASSES).find(k => BADGE_CLASSES[k].includes(text.toUpperCase())) || 'info';
    return `<span class="badge badge-${kind}">${esc(text)}</span>`;
}

function item(label, valueHtml) {
    return `<div class="result-item"><span class="result-label">${esc(label)}</span>${valueHtml}</div>`;
}

const value = v => `<span class="result-value">${esc(v)}</span>`;
const yesNo = (flag, yesKind = 'success', noKind = 'info') => badge(flag ? 'YES' : 'NO', flag ? yesKind : noKind);
const paragraph = text => `<p style="color: var(--text-secondary); line-height: 1.7; white-space: pre-wrap; margin-top: 0.75rem;">${esc(text)}</p>`;
const listItems = items => `<div class="list-items">${items.map(i => `<div class="list-item">${i}</div>`).join('')}</div>`;

function card(title, bodyHtml) {
    const el = document.createElement('div');
    el.className = 'result-card';
    el.innerHTML = `<h4>${title}</h4>${bodyHtml}`;
    document.getElementById('visualizationContainer').appendChild(el);
}

function table(headers, rows) {
    return `<div class="table-container"><table class="data-table">
        <thead><tr>${headers.map(h => `<th>${esc(h)}</th>`).join('')}</tr></thead>
        <tbody>${rows.map(r => `<tr>${r.map(c => `<td>${c}</td>`).join('')}</tr>`).join('')}</tbody>
    </table></div>`;
}

function timeline(entries) {
    return `<div class="timeline">${entries.map(e => `
        <div class="timeline-item">
            <div class="timeline-number">${esc(e.number)}</div>
            <div class="timeline-content">
                <div class="timeline-title">${e.title}</div>
                ${e.desc ? `<div class="timeline-desc">${e.desc}</div>` : ''}
            </div>
        </div>`).join('')}</div>`;
}

function humanize(key) {
    return String(key).replace(/_/g, ' ').replace(/([a-z])([A-Z])/g, '$1 $2').replace(/^./, c => c.toUpperCase());
}

// Renders any flat object as a label/value grid; nested arrays become comma-separated lists.
function keyValueGrid(obj) {
    if (!obj || typeof obj !== 'object') return paragraph(obj);
    return `<div class="result-grid">${Object.entries(obj).map(([k, v]) => {
        const display = Array.isArray(v) ? (v.length ? v.join(', ') : '—') : typeof v === 'boolean' ? (v ? 'yes' : 'no') : v;
        return item(humanize(k), value(display));
    }).join('')}</div>`;
}

// ISO-8601 duration (e.g. PT3.697S) to milliseconds.
function durationMs(iso) {
    const m = /^PT(?:(\d+)H)?(?:(\d+)M)?(?:([\d.]+)S)?$/.exec(iso || '');
    if (!m) return null;
    return Math.round(((+m[1] || 0) * 3600 + (+m[2] || 0) * 60 + (+m[3] || 0)) * 1000);
}

// --- Navigation -------------------------------------------------------------

function initNavigation() {
    const navContainer = document.getElementById('patternNav');
    navContainer.innerHTML = '';

    stages.forEach(stage => {
        const group = document.createElement('div');
        group.className = 'nav-stage';
        group.style.setProperty('--stage', stage.color);
        group.style.setProperty('--cols', stage.patterns.length > 2 ? 2 : 1);
        group.innerHTML = `
            <div class="nav-stage-title" title="${stage.picks}">
                <span class="nav-stage-number">Stage ${stage.id}</span>
                <span class="nav-stage-name">${stage.name}</span>
            </div>`;
        stage.patterns.forEach(id => {
            const pattern = patterns.find(p => p.id === id);
            const btn = document.createElement('button');
            btn.className = 'nav-btn';
            btn.dataset.pattern = pattern.id;
            btn.innerHTML = `
                <span class="nav-number">${pattern.id}</span>
                <span class="nav-label">${pattern.name}</span>
            `;
            btn.onclick = () => selectPattern(pattern.id);
            group.appendChild(btn);
        });
        navContainer.appendChild(group);
    });
}

function initStageMap() {
    const map = document.getElementById('stageMap');
    map.innerHTML = stages.map(stage => `
        <div class="stage-card" style="--stage: ${stage.color}">
            <div class="stage-card-header">
                <span class="stage-card-number">Stage ${stage.id}</span>
                <span class="stage-card-name">${stage.name}</span>
            </div>
            <p class="stage-card-picks">${stage.picks}</p>
            <dl class="stage-card-facts">
                <dt>Who decides the flow</dt><dd>${stage.decides}</dd>
                <dt>Agentic loop</dt><dd>${stage.loop}</dd>
            </dl>
            <div class="stage-card-patterns">
                ${stage.patterns.map(id => {
                    const p = patterns.find(x => x.id === id);
                    return `<button class="stage-chip" data-pattern="${p.id}"><span class="nav-number">${p.id}</span>${p.name}</button>`;
                }).join('')}
            </div>
        </div>`).join('');
    map.querySelectorAll('.stage-chip').forEach(chip => {
        chip.onclick = () => selectPattern(Number(chip.dataset.pattern));
    });
}

function resetRunUi() {
    const btn = document.getElementById('runDemoBtn');
    btn.disabled = false;
    btn.innerHTML = '<span class="btn-icon">▶</span> Run Pattern';
    document.getElementById('statusIndicator').style.display = 'none';
}

function showRunError(title, message, hint) {
    document.getElementById('runErrorTitle').textContent = title;
    document.getElementById('runErrorText').textContent = message;
    const hintEl = document.getElementById('runErrorHint');
    hintEl.textContent = hint || '';
    hintEl.hidden = !hint;
    document.getElementById('runError').hidden = false;
}

function hideRunError() {
    document.getElementById('runError').hidden = true;
}

async function renderFlow() {
    const mermaidEl = document.getElementById('flowDiagram');
    mermaidEl.removeAttribute('data-processed');
    mermaidEl.textContent = currentPattern.flow;
    if (!window.mermaid) {
        return;
    }
    try {
        await mermaid.run({ nodes: [mermaidEl] });
    } catch (error) {
        console.error('Mermaid rendering failed', error);
    }
}

function selectPattern(patternId) {
    currentPattern = patterns.find(p => p.id === patternId);
    runToken++;
    resetRunUi();

    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.classList.toggle('active', Number(btn.dataset.pattern) === patternId);
    });

    const stage = stageOf(patternId);
    document.getElementById('patternSection').style.setProperty('--stage', stage.color);
    document.getElementById('stageBadge').textContent = `Stage ${stage.id} · ${stage.name}`;
    document.getElementById('stageBadge').title = stage.picks;
    document.querySelectorAll('#stageTrack .stage-step').forEach((step, idx) => {
        step.classList.toggle('active', idx + 1 === stage.id);
        step.classList.toggle('passed', idx + 1 < stage.id);
    });

    document.getElementById('introSection').style.display = 'none';
    document.getElementById('patternSection').style.display = 'block';

    document.getElementById('patternNumber').textContent = currentPattern.id;
    document.getElementById('patternName').textContent = currentPattern.name;
    document.getElementById('patternSubtitle').textContent = currentPattern.subtitle;
    document.getElementById('flowType').textContent = currentPattern.flowType;
    document.getElementById('whoDecides').textContent = currentPattern.whoDecides;
    document.getElementById('flowDescription').innerHTML = currentPattern.description;
    renderFlow();

    updateSamples();
    clearResults();

    document.getElementById('patternSection').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function updateSamples() {
    const sampleSelect = document.getElementById('sampleSelect');
    const samplesList = document.getElementById('samplesList');

    sampleSelect.innerHTML = '';
    samplesList.innerHTML = '';

    currentPattern.samples.forEach(sample => {
        const option = document.createElement('option');
        option.value = sample.file;
        option.textContent = sample.name;
        sampleSelect.appendChild(option);

        const link = document.createElement('a');
        link.className = 'sample-item';
        link.href = `samples/${sample.file}`;
        link.target = '_blank';
        link.rel = 'noopener';
        link.textContent = `samples/${sample.file}`;
        samplesList.appendChild(link);
    });

    sampleSelect.onchange = () => {
        runToken++;
        resetRunUi();
        clearResults();
        loadSample(sampleSelect.value);
    };
    loadSample(currentPattern.samples[0].file);
}

// Samples are the files in samples/, served by the backend, so UI and CLI demos use identical input.
async function loadSample(filename) {
    const input = document.getElementById('inputJson');
    const pattern = currentPattern;
    try {
        if (!sampleCache.has(filename)) {
            const response = await fetch(`samples/${filename}`);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            sampleCache.set(filename, await response.json());
        }
        if (pattern === currentPattern && document.getElementById('sampleSelect').value === filename) {
            input.value = JSON.stringify(sampleCache.get(filename), null, 2);
        }
    } catch (error) {
        if (pattern === currentPattern) {
            input.value = '';
            input.placeholder = `Could not load samples/${filename} (${error.message}). Paste the request JSON here.`;
        }
    }
}

async function apiErrorMessage(response) {
    let detail = (await response.text()).trim();
    if (detail && response.headers.get('Content-Type')?.includes('json')) {
        try {
            const body = JSON.parse(detail);
            detail = [body?.detail, body?.message, body?.error, body?.title]
                .find(value => typeof value === 'string' && value.trim()) || detail;
        } catch (error) {
            return `HTTP ${response.status}: Invalid JSON error response: ${error.message}`;
        }
    }
    return `HTTP ${response.status}: ${detail || response.statusText || 'Request failed'}`;
}

// --- Run --------------------------------------------------------------------

document.getElementById('runDemoBtn').addEventListener('click', async () => {
    if (!currentPattern) return;

    const token = ++runToken;
    const pattern = currentPattern;
    const btn = document.getElementById('runDemoBtn');
    const status = document.getElementById('statusIndicator');
    const statusText = document.getElementById('statusText');
    const started = performance.now();

    hideRunError();
    btn.disabled = true;
    btn.innerHTML = '<span class="btn-icon">⏳</span> Running...';
    status.style.display = 'flex';
    statusText.textContent = 'Calling API... (LLM patterns can take up to a minute)';
    let phase = 'input';

    try {
        const input = JSON.parse(document.getElementById('inputJson').value);
        if (input === null || typeof input !== 'object' || Array.isArray(input)) {
            throw new Error('the request must be a JSON object');
        }

        phase = 'request';
        const response = await fetch(pattern.endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(input)
        });
        if (!response.ok) {
            if (response.status === 400) phase = 'validation';
            throw new Error(await apiErrorMessage(response));
        }
        const result = await response.json();
        if (token !== runToken) return;

        phase = 'rendering';
        statusText.textContent = 'Processing results...';
        displayResults(result, Math.round(performance.now() - started));
    } catch (error) {
        if (token !== runToken) return;
        const title = {
            input: 'Invalid input JSON',
            validation: 'Invalid request',
            request: 'API request failed',
            rendering: 'Result visualization failed'
        }[phase];
        const hint = {
            validation: 'The backend rejected the input before calling the LLM. Fix the listed fields and run again.',
            request: 'Check that the backend is reachable and its active provider profile, model IDs and credentials (or local model server) are configured correctly. See the backend logs for details.'
        }[phase] || '';
        showRunError(title, error.message, hint);
    } finally {
        if (token === runToken) resetRunUi();
    }
});

function displayResults(result, roundTripMs) {
    const resultsSection = document.getElementById('resultsSection');
    const rawJson = document.getElementById('rawJson');
    document.getElementById('visualizationContainer').innerHTML = '';

    const visualizers = {
        1: visualizeInvoiceExtraction,
        2: visualizeServiceDesk,
        3: visualizeContractReview,
        4: visualizeSupplierDueDiligence,
        5: visualizeBreachNotification,
        6: visualizeRouting,
        7: visualizeKyc,
        8: visualizeIncident,
        9: visualizeSwarm
    };
    rawJson.textContent = JSON.stringify(result, null, 2);
    try {
        (visualizers[currentPattern.id] || visualizeGeneric)(result || {});
    } catch (error) {
        console.error('Visualization failed, falling back to raw JSON', error);
        document.getElementById('visualizationContainer').innerHTML = '';
        visualizeGeneric(result);
    }
    card('⏱️ Round Trip', `<div class="result-grid">${item('Browser → API → Browser', value(`${roundTripMs} ms`))}</div>`);

    resultsSection.style.display = 'block';
    resultsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// --- Pattern visualizations (field names mirror the Java response records) --

function visualizeInvoiceExtraction(result) {
    const invoice = result.invoice || {};
    card('📄 Invoice Extraction Result', `<div class="result-grid">
        ${item('Invoice Number', value(invoice.invoiceNumber))}
        ${item('Supplier', value(invoice.supplierName))}
        ${item('VAT / UID', value(invoice.supplierVatId))}
        ${item('Purchase Order', value(invoice.purchaseOrderNumber))}
        ${item('Issue Date', value(invoice.issueDate))}
        ${item('Due Date', value(invoice.dueDate))}
        ${item('Net / VAT', value(`${invoice.netAmount ?? '—'} / ${invoice.vatAmount ?? '—'} ${invoice.currency ?? ''}`))}
        ${item('Total', value(`${invoice.grossAmount ?? '—'} ${invoice.currency ?? ''}`))}
        ${item('Next Step (decided by code)', badge(result.nextStep))}
    </div>`);

    const lines = asArray(invoice.lineItems);
    if (lines.length) {
        card('🧾 Line Items', table(['Description', 'Qty', 'Unit Price', 'Total'],
            lines.map(l => [esc(l.description), esc(l.quantity), esc(l.unitPrice), esc(l.total)])));
    }

    const issues = asArray(result.validationIssues);
    card(issues.length ? '⚠️ Validation Issues (InvoiceValidator)' : '✅ Validation (InvoiceValidator)',
        issues.length ? listItems(issues.map(esc)) : paragraph('No issues found, ready for automatic posting.'));
}

function visualizeServiceDesk(result) {
    card('💬 Agent Response', paragraph(result.reply));

    const tickets = asArray(result.ticketIds);
    card('🎫 Tickets Created', tickets.length
        ? `<div class="path-list">${tickets.map(id => `<span class="path-item">${esc(id)}</span>`).join('')}</div>`
        : paragraph('No ticket created.'));

    const audit = asArray(result.auditTrail);
    card('📜 Audit Trail (Recorded by Tools)', audit.length
        ? timeline(audit.map((entry, idx) => ({ number: idx + 1, title: esc(entry) })))
        : paragraph('No tool was called.'));
}

function visualizeContractReview(result) {
    const steps = asArray(result.executedSteps);
    card('📑 Contract Review Result', `<div class="result-grid">
        ${item('Outcome', badge(result.outcome))}
        ${item('Overall Risk', result.risk ? badge(result.risk.overallRisk) : value('not assessed (stopped early)'))}
        ${item('Steps Executed', value(`${steps.length} of 4`))}
    </div>${steps.length ? timeline(steps.map((s, idx) => ({ number: idx + 1, title: esc(s) }))) : ''}`);

    if (result.terms) {
        const t = result.terms;
        card('🔎 Extracted Terms', keyValueGrid({
            counterparty: t.counterparty,
            contractType: t.contractType,
            annualContractValue: t.annualContractValue,
            liabilityCap: t.unlimitedLiability ? 'UNLIMITED' : t.liabilityCapAmount,
            paymentTermsDays: t.paymentTermsDays,
            terminationNoticeDays: t.terminationNoticeDays,
            autoRenewal: t.autoRenewal,
            governingLaw: t.governingLaw,
            personalDataProcessed: t.personalDataProcessed,
            dataTransferOutsideEea: t.dataTransferOutsideEea
        }));
    }

    const deviations = asArray(result.deviations);
    card(deviations.length ? '⚠️ Playbook Deviations (Code Gate)' : '✅ Playbook Gate (Code)', deviations.length
        ? table(['Rule', 'Expected', 'Actual', 'Severity'],
            deviations.map(d => [`<strong>${esc(d.rule)}</strong>`, esc(d.expected), esc(d.actual), badge(d.severity)]))
        : paragraph('No deviations from the playbook. The chain stopped early, no further LLM calls.'));

    if (result.risk) {
        const priorities = asArray(result.risk.negotiationPriorities);
        card('🎯 Risk Assessment', paragraph(result.risk.rationale)
            + (priorities.length ? listItems(priorities.map((p, idx) => `${idx + 1}. ${esc(p)}`)) : ''));
    }

    if (result.memo) {
        const redlines = asArray(result.memo.redlines);
        card(`📝 Negotiation Memo: ${esc(result.memo.subject)}`, paragraph(result.memo.body)
            + (redlines.length ? table(['Clause', 'Current Wording', 'Proposed Wording'],
                redlines.map(r => [`<strong>${esc(r.clause)}</strong>`, esc(r.currentWording), esc(r.proposedWording)])) : ''));
    }
}

function visualizeSupplierDueDiligence(result) {
    const elapsed = durationMs(result.elapsed);
    card(`🏢 Supplier Due Diligence: ${esc(result.supplierName)}`, `<div class="result-grid">
        ${item('Overall Risk (code: highest dimension)', badge(result.overallRisk))}
        ${item('Decision (code)', badge(result.decision))}
        ${item('Elapsed (parallel)', value(elapsed === null ? result.elapsed : `${elapsed} ms`))}
    </div>`);

    const assessments = asArray(result.assessments);
    if (assessments.length) {
        card('📊 Risk Dimensions (assessed in parallel)', table(['Dimension', 'Rating', 'Findings', 'Required Mitigations'],
            assessments.map(a => [
                `<strong>${esc(humanize(String(a.dimension || '').toLowerCase()))}</strong>`,
                badge(a.rating),
                asArray(a.findings).map(esc).join('<br>') || '—',
                asArray(a.requiredMitigations).map(esc).join('<br>') || '—'
            ])));
    }

    const conditions = asArray(result.conditions);
    if (conditions.length) {
        card('📋 Contract Conditions', listItems(conditions.map(esc)));
    }

    card('📝 Executive Summary (LLM)', paragraph(result.executiveSummary));
}

function visualizeBreachNotification(result) {
    const rounds = asArray(result.rounds);
    const verdict = result.finalVerdict || {};
    card('🚨 Breach Notification Result', `<div class="result-grid">
        ${item('Approved for Sending', badge(result.approvedForSending ? 'YES' : 'NO - HUMAN REVIEW', result.approvedForSending ? 'success' : 'danger'))}
        ${item('Rounds', value(rounds.length))}
        ${item('Final Score', value(verdict.score === undefined ? null : `${verdict.score}/100`))}
    </div>`);

    if (rounds.length) {
        card('🔄 Evaluation Rounds', timeline(rounds.map(r => {
            const v = r.verdict || {};
            const missing = asArray(v.missingRequirements);
            return {
                number: r.round,
                title: `Score ${esc(v.score)}/100 ${badge(v.passed ? 'PASSED' : 'NEEDS FIXES')}`,
                desc: (missing.length ? `<strong>Missing:</strong> ${missing.map(esc).join('; ')}<br>` : '') + esc(v.feedback)
            };
        })));
    }

    if (result.finalDraft) {
        card(`✉️ Final Draft: ${esc(result.finalDraft.subject)}`,
            `<div style="background: rgba(0,0,0,0.3); padding: 1rem; border-radius: 8px; white-space: pre-wrap; color: var(--text-secondary); line-height: 1.7; max-height: 400px; overflow-y: auto;">${esc(result.finalDraft.body)}</div>`);
    }
}

function visualizeRouting(result) {
    const decision = result.decision || {};
    const confidence = typeof decision.confidence === 'number' ? `${(decision.confidence * 100).toFixed(0)}%` : null;
    card('🧭 Routing Decision (LLM)', `<div class="result-grid">
        ${item('Selected Route', badge(decision.route, 'primary'))}
        ${item('Confidence', value(confidence))}
        ${item('Handled By (code)', badge(result.handledBy, result.fallback ? 'warning' : 'success'))}
        ${item('Fallback Used', yesNo(result.fallback, 'warning', 'success'))}
    </div>${paragraph(decision.reason)}`);

    const output = result.output;
    if (output) {
        card(`📋 Case Outcome: queue ${esc(output.queue)}`, paragraph(output.summary)
            + (output.details ? `<div style="margin-top: 0.75rem;">${keyValueGrid(output.details)}</div>` : ''));
        if (output.draftReply) {
            card('✉️ Draft Reply', paragraph(output.draftReply));
        }
    }
}

function visualizeKyc(result) {
    const decision = result.decision || {};
    const documents = asArray(decision.requiredDocuments);
    card('🏦 KYC Onboarding Result', `<div class="result-grid">
        ${item('Decision', badge(decision.outcome))}
        ${item('Risk Level', badge(decision.risk))}
        ${item('Overridden by Code', yesNo(result.overriddenByCode, 'danger'))}
    </div>${paragraph(decision.rationale)}${documents.length ? listItems(documents.map(d => `📎 ${esc(d)}`)) : ''}`);

    const batches = asArray(result.executionBatches);
    if (batches.length) {
        card('⚡ Execution Batches (DAG)', timeline(batches.map((batch, idx) => ({
            number: idx + 1,
            title: `Batch ${idx + 1}${asArray(batch).length > 1 ? ' (parallel)' : ''}`,
            desc: `<div class="path-list" style="margin-top: 0.5rem;">${asArray(batch).map(n => `<span class="path-item">${esc(n)}</span>`).join('')}</div>`
        }))));
    }

    if (result.applicant) {
        card('👤 Extracted Applicant', keyValueGrid(result.applicant));
    }

    if (result.sanctions) {
        const matches = asArray(result.sanctions.matches);
        card('🔍 Sanctions Screening (Deterministic Code)', `<div class="result-grid">
            ${item('Hit', yesNo(result.sanctions.hit, 'danger', 'success'))}
            ${item('List Version', value(result.sanctions.listVersion))}
        </div>${matches.length ? `<div style="margin-top: 0.75rem;">${listItems(matches.map(esc))}</div>` : ''}`);
    }

    if (result.adverseMedia) {
        const findings = asArray(result.adverseMedia.findings);
        card('📰 Adverse Media (LLM)', `<div class="result-grid">${item('Risk', badge(result.adverseMedia.risk))}</div>`
            + (findings.length ? `<div style="margin-top: 0.75rem;">${listItems(findings.map(esc))}</div>` : paragraph('No findings about this applicant.')));
    }

    if (result.sourceOfFunds) {
        card('💰 Source of Funds (LLM)', `<div class="result-grid">
            ${item('Plausible', yesNo(result.sourceOfFunds.plausible, 'success', 'danger'))}
            ${item('Risk', badge(result.sourceOfFunds.risk))}
        </div>${paragraph(result.sourceOfFunds.rationale)}`);
    }
}

function visualizeIncident(result) {
    const pm = result.postmortem || {};
    const confidence = typeof pm.confidence === 'number' ? `${(pm.confidence * 100).toFixed(0)}%` : null;
    card(`🚨 ${esc(pm.title || 'Incident Postmortem')}`, `<div class="result-grid">
        ${item('Severity', badge(pm.severity, 'danger'))}
        ${item('Confidence', value(confidence))}
    </div>${paragraph(pm.summary)}`);

    card('🎯 Root Cause', paragraph(pm.rootCause)
        + (pm.customerImpact ? `<div style="margin-top: 0.75rem;">${item('Customer Impact', value(pm.customerImpact))}</div>` : ''));

    const delegations = asArray(result.delegations);
    card('🧭 Orchestrator Delegations (recorded by code)', delegations.length
        ? timeline(delegations.map((d, idx) => ({
            number: idx + 1,
            title: `→ ${esc(d.worker)} <span class="badge badge-info">${esc(d.durationMs)} ms</span>`,
            desc: `<code style="white-space: pre-wrap;">${esc(d.input)}</code>`
        })))
        : paragraph('The orchestrator did not delegate to any worker.'));

    const factors = asArray(pm.contributingFactors);
    if (factors.length) {
        card('🧩 Contributing Factors & Ruled-out Hypotheses', listItems(factors.map(esc)));
    }

    const events = asArray(pm.timeline);
    if (events.length) {
        card('🕒 Timeline', table(['Time', 'Event'], events.map(e => [esc(e.time), esc(e.event)])));
    }

    const actions = asArray(pm.actionItems);
    if (actions.length) {
        card('✅ Action Items', table(['Action', 'Owner', 'Priority'],
            actions.map(a => [esc(a.action), esc(a.owner), badge(a.priority, 'warning')])));
    }
}

function visualizeSwarm(result) {
    const path = asArray(result.path);
    card('🤝 Swarm Procurement Result', `<div class="result-grid">
        ${item('Finished By', badge(result.finishedBy, 'primary'))}
        ${item('Handoffs', value(path.length))}
        ${item('Stop Reason (guardrails)', badge(result.stopReason))}
    </div>${paragraph(result.finalAnswer)}`);

    if (path.length) {
        const agents = [path[0].agent, ...path.map(h => h.handedOffTo)];
        card('🛤️ Agent Handoff Path',
            `<div class="path-list">${agents.map((a, idx) => `<span class="path-item">${esc(a)}</span>${idx < agents.length - 1 ? '<span class="path-arrow">→</span>' : ''}`).join('')}</div>`
            + `<div style="margin-top: 1rem;">${timeline(path.map(h => ({
                number: h.turn,
                title: `${esc(h.agent)} → ${esc(h.handedOffTo)}`,
                desc: esc(h.reason)
            })))}</div>`);
    }

    const caseFile = Object.entries(result.caseFile || {});
    if (caseFile.length) {
        card('📁 Shared Case File', timeline(caseFile.map(([agent, notes], idx) => ({
            number: idx + 1,
            title: esc(agent),
            desc: `<span style="white-space: pre-wrap;">${esc(notes)}</span>`
        }))));
    }
}

function visualizeGeneric(result) {
    card('✨ Result', `<pre style="color: var(--text-secondary); overflow-x: auto; font-size: 0.8125rem;">${esc(JSON.stringify(result, null, 2))}</pre>`);
}

function clearResults() {
    hideRunError();
    document.getElementById('resultsSection').style.display = 'none';
    document.getElementById('visualizationContainer').innerHTML = '';
    document.getElementById('rawJson').textContent = '';
}

document.getElementById('clearResultsBtn').addEventListener('click', clearResults);

// Ctrl/Cmd+Enter in the request editor runs the pattern.
document.getElementById('inputJson').addEventListener('keydown', event => {
    if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
        event.preventDefault();
        document.getElementById('runDemoBtn').click();
    }
});

// --- Init -------------------------------------------------------------------

document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initStageMap();
    document.getElementById('stageTrack').innerHTML = stages.map(s =>
        `<span class="stage-step" style="--stage: ${s.color}" title="${s.picks}">${s.name}</span>`).join('');
    if (window.mermaid) {
        mermaid.initialize({
            startOnLoad: false,
            theme: 'dark',
            themeVariables: {
                primaryColor: '#6366f1',
                primaryTextColor: '#f1f5f9',
                primaryBorderColor: '#8b5cf6',
                lineColor: '#64748b',
                secondaryColor: '#1e293b',
                tertiaryColor: '#0f172a'
            }
        });
    }
});
