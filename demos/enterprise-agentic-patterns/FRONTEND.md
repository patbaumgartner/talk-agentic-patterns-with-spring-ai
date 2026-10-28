# Interactive Frontend for Enterprise Agentic Patterns

A modern, visual dashboard to explore and demo all 9 agentic patterns.

## Features

- **9 Pattern Visualizations** - Interactive flow diagrams for each pattern using Mermaid.js
- **Stage Map** - Patterns grouped into the four stages from the talk (Mono → Graph → Orchestrator → Swarm), from controllability to autonomy, with "who decides the flow" and the agentic loop per stage
- **Live Demos** - Run any pattern with sample data directly from the UI
- **Pattern-Specific Visualizations** - Custom result displays for each pattern type:
  - Task Execution: Invoice validation with issues
  - Mono: Agent responses with audit trails
  - Chain: Step-by-step execution tracking
  - Parallelization: Multi-dimension risk assessment
  - Evaluator-Optimizer: Iteration rounds visualization
  - Routing: Classification with confidence and SLAs
  - Graph (DAG): Execution batches and code overrides
  - Orchestrator-Workers: Delegation paths
  - Swarm: Peer handoffs and shared case file
- **Dark Theme UI** - Modern glassmorphism design
- **Sample Data** - Pre-loaded samples for each pattern including error cases

## Usage

1. Start the Spring Boot application:
```bash
mvn spring-boot:run
```

2. Open your browser to [http://localhost:8080](http://localhost:8080)

3. Select any pattern from the navigation to explore its flow and run demos

## Tech Stack

- Vanilla HTML/CSS/JavaScript (no build step needed)
- Mermaid.js for flow diagrams
- Served as static resources by Spring Boot
