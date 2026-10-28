# Enterprise Agentic Patterns with Spring AI

Nine agentic patterns, nine enterprise problems, one Spring Boot app.

The companion demo [`sample-agents-and-dragons-java`](../sample-agents-and-dragons-java) teaches the patterns with
a fantasy game. This project shows the same patterns on problems people in the audience deal with on Monday morning:
invoices, contracts, incidents, KYC, GDPR. Each demo runs on its own and fits on one slide.

Stack: Java 25, Spring Boot 4.2, Spring AI 2.1, Amazon Bedrock, OpenAI, Anthropic, or Ollama, virtual threads.

## The demos

| # | Pattern | Enterprise problem | Who decides the flow | Endpoint |
|---|---------|--------------------|----------------------|----------|
| 1 | Task Execution | Accounts payable: unstructured invoice to ERP-ready record | Code | `POST /api/task-execution/invoices` |
| 2 | Mono (agent + tools) | IT service desk: unlock accounts, open tickets | LLM, policy in tools | `POST /api/mono/service-desk` |
| 3 | Chain | Legal: contract review against the playbook | Code | `POST /api/chain/contract-review` |
| 4 | Parallelization | Procurement: supplier due diligence in 4 dimensions | Code | `POST /api/parallelization/supplier-due-diligence` |
| 5 | Evaluator-Optimizer | Compliance: GDPR / revFADP data breach notification | Code-bounded loop, LLM judge | `POST /api/evaluator-optimizer/breach-notification` |
| 6 | Routing | Insurance: triage of the customer inbox | LLM picks, code owns the fallback | `POST /api/routing/insurance-inbox` |
| 7 | Graph (DAG) | Banking: KYC / AML client onboarding | Code (topology) | `POST /api/graph/kyc-onboarding` |
| 8 | Orchestrator-Workers | SRE: incident root-cause analysis and postmortem | LLM orchestrator | `POST /api/orchestrator/incident-analysis` |
| 9 | Swarm | Procurement: SaaS purchase approval across departments | LLM peers, code guard rails | `POST /api/swarm/saas-procurement` |

The order is deliberate: autonomy increases from top to bottom, and so does the cost of the guard rails you need.

## Running it

Prerequisites: JDK 25 and Maven. Pick one provider with a Spring profile. Bedrock is the default.

| Profile | Required | Optional |
|---------|----------|----------|
| `bedrock` (default) | AWS credentials (`AWS_PROFILE` or `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`) with access to Claude Haiku 5.5 and Opus 5.5 | `AWS_REGION` (`eu-central-1`), `BEDROCK_MODEL_ID` |
| `openai` | `OPENAI_API_KEY` | `OPENAI_BASE_URL` (Spring AI default: `https://api.openai.com/v1`) |
| `anthropic` | `ANTHROPIC_API_KEY` | `ANTHROPIC_BASE_URL` (`https://api.anthropic.com`) |
| `ollama` | A running Ollama with the models pulled | `OLLAMA_BASE_URL` (`http://localhost:11434`) |

The active profile selects the provider for **all nine patterns**, including the fast, smart and judge model tiers.
Setting an API key alone does not select a provider. For example, an OpenAI key must be paired with the `openai`
profile, either through `SPRING_PROFILES_ACTIVE=openai` or the Maven option below. Bedrock is used only when no
provider profile is selected; the other profiles do not require AWS credentials.

```bash
# Amazon Bedrock (Claude 5.5 needs cross-region inference profile ids: eu., us., global.)
export AWS_REGION=eu-central-1
mvn spring-boot:run

# OpenAI
export OPENAI_API_KEY=...
mvn spring-boot:run -Dspring-boot.run.profiles=openai

# Anthropic
export ANTHROPIC_API_KEY=...
mvn spring-boot:run -Dspring-boot.run.profiles=anthropic

# Ollama (the app never pulls models itself)
ollama pull qwen3:8b && ollama pull qwen3:14b
mvn spring-boot:run -Dspring-boot.run.profiles=ollama
```

The packaged jar works the same way: `java -jar target/enterprise-agentic-patterns.jar --spring.profiles.active=openai`.

Then fire the requests in [`requests.http`](requests.http), or with curl:

```bash
curl -s localhost:8080/api/graph/kyc-onboarding \
  -H 'Content-Type: application/json' -d @samples/7-graph-kyc-sanctions-hit.json | jq
```

Every demo has a sample in [`samples/`](samples). Where it makes the point, there is a second sample in which
something goes wrong (`*-with-errors`, `*-security-hold`, `*-sanctions-hit`, `*-blocked`, ...).

Swap models without touching code:

| Variable | Used for | `bedrock` | `openai` | `anthropic` | `ollama` |
|----------|----------|-----------|----------|-------------|----------|
| `AGENTIC_MODEL_FAST` | Extractors, routers, workers | `eu.anthropic.claude-haiku-5-5` | `gpt-4.1-mini` | `claude-haiku-4-5-20251001` | `qwen3:8b` |
| `AGENTIC_MODEL_SMART` | Writers, orchestrator, swarm peers | `eu.anthropic.claude-opus-5-5` | `gpt-4.1` | `claude-sonnet-4-6` | `qwen3:14b` |
| `AGENTIC_MODEL_JUDGE` | Evaluator (different model than the writer) | `eu.anthropic.claude-haiku-5-5` | `gpt-4.1-mini` | `claude-haiku-4-5-20251001` | `qwen3:8b` |

The models need tool calling and structured output. On Ollama, small models may fail at either.

Temperature depends on the provider. On `openai`, `anthropic` and `ollama` the fast and judge tiers send temperature 0
and the smart tier sends 0.3. Claude 5.5 on Bedrock rejects the parameter, so the `bedrock` profile sets
`agentic.temperature-supported=false` and no tier sends a temperature.

The app listens on `127.0.0.1` only. Set `SERVER_ADDRESS` (and `PORT`) to change the bind.

Set `logging.level.org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor=DEBUG` to show every prompt and
response on stage.

### Tests run offline

```bash
mvn verify
```

No AWS or API keys needed. The tests cover the pattern engines (graph scheduler, evaluator loop, router fallback, swarm guard
rails, bounded fan-out), all code gates, the samples, and the prompt templates. One test boots the whole application
with a canned `ChatModel`, and `ProviderProfilesTests` boots every provider profile and replays chat and tool
calls against a local HTTP stub.

The UI request and error-handling tests use Node.js's built-in test runner, without additional packages:

```bash
node --test src/test/js/app.test.mjs
```

The UI reports backend HTTP status and error details without assuming a provider. For request failures, check the
backend logs, the active profile, the model IDs and that provider's credentials (or the local model server).
Input JSON and result-visualization errors are reported separately, inline below the Run button.

Request records are checked with Bean Validation (`@Valid`, `@NotBlank`, `@Email`, ...) before any LLM call.
Missing, blank or wrongly typed fields return `400 Bad Request` as a problem detail that names the fields, e.g.
`{"title": "Invalid request", "detail": "message must not be blank"}`.

## The building blocks

All demos use four ideas from [`core/`](src/main/java/com/fortytwotalents/agentic/core):

- **`TaskExecutionAgent<I, O>`**: one prompt template in, one typed record out. It is a `java.util.function.Function`,
  so it composes into chains, fan-outs, graphs and tools. The `StructuredOutputValidationAdvisor` re-prompts the model
  when its JSON does not match the record's schema.
- **Records are the contracts**: `@JsonPropertyDescription` on record components ends up in the JSON schema the model
  sees. The record is the API between agents.
- **`Agents`**: `fast()`, `smart()` and `judge()` builders, so each step uses the right model. Builders come from
  Spring AI's auto-configured `ChatClient.Builder`, so every call is observed by Micrometer.
- **`Parallel.map`**: fan-out on virtual threads with a concurrency limit (provider rate limits) and a time budget.

## Pattern notes for the talk

### 1 Task Execution: invoice extraction

[`taskexecution/`](src/main/java/com/fortytwotalents/agentic/taskexecution): one LLM call extracts an `Invoice`
record from any layout. Then `InvoiceValidator`, plain Java, checks line totals, net + VAT = gross, dates and the
purchase order number. The `nextStep` is `AUTO_POST` or `MANUAL_REVIEW`. **The LLM reads, code decides.** Try the
`-with-errors` sample: the model faithfully extracts a broken invoice, and the code catches it.

### 2 Mono: IT service desk

[`mono/`](src/main/java/com/fortytwotalents/agentic/mono): one agent, four `@Tool`s, and the LLM decides the flow.
Run the `-security-hold` sample: the user claims CEO approval and asks to "ignore the usual checks". The model may
well try `unlockAccount`, but the tool enforces policy SEC-07 in code and refuses. The `auditTrail` in the response
is recorded by the tools, not reported by the model. **Put policy in tools, not in prompts.**

### 3 Chain: contract review

[`chain/`](src/main/java/com/fortytwotalents/agentic/chain): extract terms (fast model), then check them against
the `PolicyPlaybook` (code, "tool as agent"), then assess risk and draft a negotiation memo (smart model). If the
playbook finds no deviation, the chain stops after step 2. The compliant sample costs one LLM call instead of three.
`executedSteps` in the response shows which steps ran.

### 4 Parallelization: supplier due diligence

[`parallelization/`](src/main/java/com/fortytwotalents/agentic/parallelization): financial stability, information
security, ESG and legal/compliance are assessed in parallel by the same agent with different criteria. The overall
risk and the decision come from code (`RiskLevel.highest`, `Decision.from`), and an LLM writes only the executive
summary. Check `elapsed`: it is about the duration of the slowest dimension, not the sum.

### 5 Evaluator-Optimizer: breach notification

[`evaluatoroptimizer/`](src/main/java/com/fortytwotalents/agentic/evaluatoroptimizer): the writer drafts the customer
notice, a different model judges it against a GDPR Art. 34 / revFADP Art. 24 checklist, and the rewriter fixes what
the judge flagged. Code adds hard rules the judge cannot overrule: DPO contact verbatim, at most 350 words, no
"no risk" or "sophisticated attack". The loop is bounded (`agentic.limits.max-evaluation-rounds`). If it never
passes, you get the best-scoring draft with `approvedForSending=false` and every round in `rounds`, so a human takes over.

### 6 Routing: insurance inbox

[`routing/`](src/main/java/com/fortytwotalents/agentic/routing): the router sees only route names and descriptions,
and returns a route, a reason and a confidence. Code owns the rest. An unknown route or confidence below 0.7 goes to
`human-review`. The complaint route has no LLM at all: regulatory deadlines (acknowledge within 5 business days,
resolve within 8 weeks) are computed, not generated.

### 7 Graph: KYC onboarding

[`graph/`](src/main/java/com/fortytwotalents/agentic/graph):

```
                       ┌─> sanctions-screening (CODE) ─┐
extract-applicant (LLM)├─> adverse-media       (LLM)  ─┼─> risk-decision (LLM) ─> code override
                       └─> source-of-funds     (LLM)  ─┘
```

The DAG is fixed at build time and cycles are rejected. Ready nodes run in parallel batches; `executionBatches` in the
response shows `[[extract-applicant], [sanctions-screening, adverse-media, source-of-funds], [risk-decision]]`.
Sanctions screening is deterministic code: regulated, reproducible, and versioned (`listVersion`). With the
`-sanctions-hit` sample, code forces `REJECT` whatever the model proposed (`overriddenByCode`). The first sample has a
namesake in the news (a hairdresser called Claudia Weber), which the adverse media agent should ignore.

### 8 Orchestrator-Workers: incident analysis

[`orchestrator/`](src/main/java/com/fortytwotalents/agentic/orchestrator): the incident commander (smart model) has no
access to logs or metrics. It delegates to `log_analyst` and `metrics_analyst`, agents with their own private tools
that are exposed as tools via `FunctionToolCallback`, and to `deployment_history`, plain code. The LLM decides whom to
ask, in which order and how often. `delegations` in the response is recorded by code. The canned data has a red
herring (an inventory-service deployment with a cache warning) next to the real cause: payment-gateway v2.14.0 shrank
its Hikari pool from 20 to 5. The result is a typed `Postmortem` with timeline, action items and confidence.

### 9 Swarm: SaaS procurement

[`swarm/`](src/main/java/com/fortytwotalents/agentic/swarm): intake, security, legal and finance are peers, and
there is no boss. Each owns one system of record (vendor risk, DPA register, budget) and hands off with
`handoff_to_agent` (`returnDirect`, so the turn ends immediately). Everybody writes to a shared case file. The loop in
`Swarm` is plain code and owns the guard rails: handoff budget (`agentic.limits.max-handoffs`), ping-pong detection,
and unknown peers. When a guard trips, the case escalates to a human with the full `path` and `caseFile`. Approval
limits (team lead / head of department / CFO) are code in `checkBudget`.

## Production checklist mapping

| Concern | Where it shows up |
|---------|-------------------|
| Typed contracts between agents | Records with `@JsonPropertyDescription` in every demo, schema-validated |
| Deterministic code gates | `InvoiceValidator`, `PolicyPlaybook`, `SanctionsScreening`, breach-notice hard rules, complaint deadlines |
| Policy in tools, not prompts | `ServiceDeskTools.unlockAccount`, `BudgetTools.checkBudget` |
| Bounded loops and fan-out | `max-evaluation-rounds`, `max-handoffs`, `max-parallel-calls`, time budgets in `Parallel` |
| Human in the loop | `MANUAL_REVIEW`, `human-review` route, `ESCALATE_TO_RISK_COMMITTEE`, `ENHANCED_DUE_DILIGENCE`, swarm escalation |
| Audit trail by code | `auditTrail`, `executedSteps`, `delegations`, `executionBatches`, swarm `path` |
| Right model per job | `Agents.fast()` / `smart()` / `judge()` |
| Observability | Micrometer observations on every `ChatClient` call, `/actuator/metrics` |
| Testable without a model | Engines are LLM-agnostic; `mvn verify` runs offline |

All companies, people and data in the samples are fictitious. The sanctions list is a demo stand-in, not SECO, OFAC
or EU data.
