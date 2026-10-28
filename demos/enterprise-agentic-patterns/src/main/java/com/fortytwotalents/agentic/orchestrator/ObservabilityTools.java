package com.fortytwotalents.agentic.orchestrator;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import com.fortytwotalents.agentic.orchestrator.ObservabilityBackend.LogLine;
import com.fortytwotalents.agentic.orchestrator.ObservabilityBackend.MetricSample;

/**
 * Worker-private tools. The orchestrator never sees them: it talks to the analysts, the
 * analysts talk to the observability stack.
 */
public final class ObservabilityTools {

	private ObservabilityTools() {
	}

	public static final class LogTools {

		private final ObservabilityBackend backend;

		public LogTools(ObservabilityBackend backend) {
			this.backend = backend;
		}

		@Tool(description = "Search the centralised logs of one service")
		public List<LogLine> searchLogs(@ToolParam(description = "Service name") String service,
				@ToolParam(description = "Minimum level: INFO, WARN or ERROR") String minLevel) {
			return backend.logs(service, minLevel);
		}

	}

	public static final class MetricTools {

		private final ObservabilityBackend backend;

		public MetricTools(ObservabilityBackend backend) {
			this.backend = backend;
		}

		@Tool(description = "List the metric names available for a service")
		public List<String> listMetrics(@ToolParam(description = "Service name") String service) {
			return backend.metricNames(service);
		}

		@Tool(description = "Query a metric time series for a service")
		public List<MetricSample> queryMetric(@ToolParam(description = "Service name") String service,
				@ToolParam(description = "Metric name from listMetrics") String metric) {
			return backend.metric(service, metric);
		}

	}

}
