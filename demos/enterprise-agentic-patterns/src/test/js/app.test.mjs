import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { createContext, runInContext } from 'node:vm';

const source = readFileSync(new URL('../../main/resources/static/app.js', import.meta.url), 'utf8');
const sample = file => readFileSync(new URL(`../../../samples/${file}`, import.meta.url), 'utf8');

function createUi(fetch) {
    const elements = new Map();
    const handlers = new Map();
    const rendered = [];
    const document = {
        getElementById(id) {
            if (!elements.has(id)) {
                elements.set(id, {
                    style: { display: 'none' },
                    disabled: false,
                    hidden: id === 'runError',
                    textContent: '',
                    value: '{}',
                    addEventListener(event, handler) {
                        handlers.set(`${id}:${event}`, handler);
                    }
                });
            }
            return elements.get(id);
        },
        addEventListener() {}
    };
    const context = createContext({ document, fetch, performance });
    runInContext(source, context);
    context.displayResults = result => rendered.push(result);
    // The UI shows run errors inline in #runError instead of using alert().
    const errorText = () => {
        const box = elements.get('runError');
        if (!box || box.hidden) return [];
        const hint = elements.get('runErrorHint')?.textContent;
        return [`${elements.get('runErrorTitle').textContent}: ${elements.get('runErrorText').textContent}`
            + (hint ? `\n\n${hint}` : '')];
    };
    return {
        elements,
        get alerts() { return errorText(); },
        rendered,
        context,
        select(index) {
            context.patternIndex = index;
            const pattern = runInContext('currentPattern = patterns[patternIndex]', context);
            document.getElementById('inputJson').value = sample(pattern.samples[0].file);
            return pattern;
        },
        run: () => handlers.get('runDemoBtn:click')()
    };
}

for (let index = 0; index < 9; index++) {
    test(`pattern ${index + 1} uses its same-origin API without provider-specific configuration`, async () => {
        const requests = [];
        const result = { response: 'from the configured provider' };
        const ui = createUi(async (url, options) => {
            requests.push({ url, options });
            return Response.json(result);
        });
        const pattern = ui.select(index);

        await ui.run();

        assert.equal(requests.length, 1);
        assert.equal(requests[0].url, pattern.endpoint);
        assert.equal(requests[0].options.method, 'POST');
        assert.deepEqual(JSON.parse(requests[0].options.body),
            JSON.parse(ui.elements.get('inputJson').value));
        assert.deepEqual(ui.rendered, [result]);
        assert.deepEqual(ui.alerts, []);
        assert.equal(ui.elements.get('runDemoBtn').disabled, false);
    });
}

test('reports backend JSON errors without prescribing AWS credentials', async () => {
    const ui = createUi(async () => Response.json({
        message: 'OpenAI rejected the configured API key'
    }, { status: 401 }));
    ui.select(0);

    await ui.run();

    assert.match(ui.alerts[0], /API request failed: HTTP 401: OpenAI rejected the configured API key/);
    assert.match(ui.alerts[0], /active provider profile/);
    assert.doesNotMatch(ui.alerts[0], /AWS|Bedrock|8080/);
    assert.deepEqual(ui.rendered, []);
    assert.equal(ui.elements.get('runDemoBtn').disabled, false);
    assert.equal(ui.elements.get('statusIndicator').style.display, 'none');
});

test('preserves Problem Details responses from the backend', async () => {
    const ui = createUi(async () => Response.json({
        title: 'Unavailable',
        detail: 'The selected model does not support tool calling'
    }, { status: 503, headers: { 'Content-Type': 'application/problem+json' } }));
    ui.select(1);

    await ui.run();

    assert.match(ui.alerts[0], /HTTP 503: The selected model does not support tool calling/);
});

test('preserves plain-text HTTP errors', async () => {
    const ui = createUi(async () => new Response('Local model server is unavailable', { status: 502 }));
    ui.select(0);

    await ui.run();

    assert.match(ui.alerts[0], /HTTP 502: Local model server is unavailable/);
});

test('preserves standard Spring Boot error responses', async () => {
    const ui = createUi(async () => Response.json({
        status: 500,
        error: 'Internal Server Error',
        path: '/api/task-execution/invoices'
    }, { status: 500 }));
    ui.select(0);

    await ui.run();

    assert.match(ui.alerts[0], /HTTP 500: Internal Server Error/);
});

test('validation errors name the fields and do not blame the provider', async () => {
    const ui = createUi(async () => Response.json({
        type: 'about:blank',
        title: 'Invalid request',
        status: 400,
        detail: 'message must not be blank'
    }, { status: 400, headers: { 'Content-Type': 'application/problem+json' } }));
    ui.select(1);

    await ui.run();

    assert.match(ui.alerts[0], /^Invalid request/);
    assert.match(ui.alerts[0], /HTTP 400: message must not be blank/);
    assert.match(ui.alerts[0], /Fix the listed fields/);
    assert.doesNotMatch(ui.alerts[0], /credentials/);
});

test('reports malformed JSON error responses with their HTTP status', async () => {
    const ui = createUi(async () => new Response('{invalid', {
        status: 502,
        headers: { 'Content-Type': 'application/json' }
    }));
    ui.select(0);

    await ui.run();

    assert.match(ui.alerts[0], /HTTP 502: Invalid JSON error response:/);
});

test('uses the HTTP status text when an error response has no body', async () => {
    const ui = createUi(async () => new Response(null, { status: 503, statusText: 'Service Unavailable' }));
    ui.select(0);

    await ui.run();

    assert.match(ui.alerts[0], /HTTP 503: Service Unavailable/);
});

test('reports network errors with provider-neutral guidance', async () => {
    const ui = createUi(async () => { throw new TypeError('Failed to fetch'); });
    ui.select(0);

    await ui.run();

    assert.match(ui.alerts[0], /API request failed: Failed to fetch/);
    assert.match(ui.alerts[0], /backend is reachable/);
    assert.doesNotMatch(ui.alerts[0], /AWS|Bedrock|8080/);
});

test('invalid input JSON does not suggest a provider or call the backend', async () => {
    const ui = createUi(async () => assert.fail('Invalid input must not reach the backend'));
    ui.select(0);
    ui.elements.get('inputJson').value = '{invalid';

    await ui.run();

    assert.match(ui.alerts[0], /^Invalid input JSON:/);
    assert.doesNotMatch(ui.alerts[0], /credentials|provider|AWS|Bedrock/);
    assert.equal(ui.elements.get('runDemoBtn').disabled, false);
});

test('rendering errors are not blamed on the provider', async () => {
    const ui = createUi(async () => Response.json({ reply: 'Completed' }));
    ui.select(1);
    ui.context.displayResults = () => { throw new TypeError('Cannot render the result'); };

    await ui.run();

    assert.match(ui.alerts[0], /^Result visualization failed: Cannot render the result/);
    assert.doesNotMatch(ui.alerts[0], /credentials|provider|AWS|Bedrock/);
    assert.equal(ui.elements.get('statusIndicator').style.display, 'none');
    assert.equal(ui.elements.get('runDemoBtn').disabled, false);
});

test('every pattern sits in the slide stage: Mono 1-2, Graph 3-7, Orchestrator 8, Swarm 9', () => {
    const ui = createUi(async () => assert.fail('no request expected'));
    const stageNames = runInContext('patterns.map(p => stageOf(p.id).name)', ui.context);
    assert.deepEqual([...stageNames], [
        'Mono', 'Mono', 'Graph', 'Graph', 'Graph', 'Graph', 'Graph', 'Orchestrator', 'Swarm'
    ]);
});
