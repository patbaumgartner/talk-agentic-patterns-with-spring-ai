package com.fortytwotalents.agentic.routing;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fortytwotalents.agentic.core.Agents;
import com.fortytwotalents.agentic.core.TaskExecutionAgent;
import com.fortytwotalents.agentic.routing.RoutingWorkflow.Route;
import com.fortytwotalents.agentic.routing.RoutingWorkflow.Routed;
import com.fortytwotalents.agentic.routing.RoutingWorkflow.RoutingDecision;
import com.fortytwotalents.agentic.routing.Triage.BillingReply;
import com.fortytwotalents.agentic.routing.Triage.CaseOutcome;
import com.fortytwotalents.agentic.routing.Triage.ClaimIntake;
import com.fortytwotalents.agentic.routing.Triage.ComplaintCase;
import com.fortytwotalents.agentic.routing.Triage.CustomerEmail;

/**
 * Pattern: Graph / Routing Workflow. The first time the LLM picks the edge.
 *
 * Enterprise problem: an insurer's shared inbox receives claims, billing questions and
 * complaints in one stream. A cheap model classifies; each branch is specialised.
 * Complaints are regulated (FINMA / FCA style deadlines) and are therefore handled by
 * code, never auto-answered by an LLM.
 */
@Service
public class ClaimsInboxTriage {

	private static final AtomicInteger COMPLAINT_SEQUENCE = new AtomicInteger(100);

	private final RoutingWorkflow<CustomerEmail, CaseOutcome> workflow;

	private final Clock clock;

	public ClaimsInboxTriage(Agents agents, Clock clock) {
		this.clock = clock;

		TaskExecutionAgent<SelectorInput, RoutingDecision> router = TaskExecutionAgent
			.<SelectorInput, RoutingDecision>builder("Triage#Router")
			.chatClient(agents.fast().build())
			.promptTemplate("prompts/routing/route-customer-email.st")
			.params(in -> Map.of("routes", in.routes(), "email", in.email().subject() + "\n\n" + in.email().body()))
			.responseType(RoutingDecision.class)
			.build();

		TaskExecutionAgent<ClaimInput, ClaimIntake> claimIntake = TaskExecutionAgent
			.<ClaimInput, ClaimIntake>builder("Triage#ClaimIntake")
			.chatClient(agents.fast().build())
			.promptTemplate("prompts/routing/first-notice-of-loss.st")
			.params(in -> Map.of("lineOfBusiness", in.lineOfBusiness(), "requiredFacts", in.requiredFacts(), "email",
					in.email().body()))
			.responseType(ClaimIntake.class)
			.build();

		TaskExecutionAgent<CustomerEmail, BillingReply> billing = TaskExecutionAgent
			.<CustomerEmail, BillingReply>builder("Triage#Billing")
			.chatClient(agents.fast().build())
			.promptTemplate("prompts/routing/billing-reply.st")
			.params(email -> Map.of("email", email.body()))
			.responseType(BillingReply.class)
			.build();

		List<Route<CustomerEmail, CaseOutcome>> routes = List.of( //
				new Route<>("motor-claim",
						"Reports of a NEW loss involving a vehicle: accident, collision, glass damage, theft of a car",
						email -> claim(claimIntake, email, "MOTOR", "claims-motor",
								"policy number, date and place of the loss, other party details, police report number")),
				new Route<>("property-claim",
						"Reports of a NEW loss to a home or contents: water damage, fire, burglary, storm",
						email -> claim(claimIntake, email, "PROPERTY", "claims-property",
								"policy number, date of the loss, affected rooms and items, photos")),
				new Route<>("billing", "Questions about premiums, invoices, payment methods, direct debit, refunds",
						email -> {
							BillingReply reply = billing.apply(email);
							return new CaseOutcome("billing", reply.summary(), reply, reply.draftReply());
						}),
				new Route<>("complaint",
						"Expressions of dissatisfaction with our service, a claim decision or an employee. "
								+ "Takes precedence when an e-mail both complains and asks something else",
						this::openComplaint));

		Route<CustomerEmail, CaseOutcome> humanReview = new Route<>("human-review", "Fallback",
				email -> new CaseOutcome("human-review", "Router unsure or unknown route, needs a human", email, null));

		this.workflow = new RoutingWorkflow<>(routes,
				(email, available) -> router.apply(new SelectorInput(email, describe(available))), humanReview, 0.7);
	}

	public Routed<CaseOutcome> triage(CustomerEmail email) {
		return workflow.route(email);
	}

	private static CaseOutcome claim(TaskExecutionAgent<ClaimInput, ClaimIntake> intake, CustomerEmail email,
			String lineOfBusiness, String queue, String requiredFacts) {
		ClaimIntake claim = intake.apply(new ClaimInput(email, lineOfBusiness, requiredFacts));
		return new CaseOutcome(queue, claim.whatHappened(), claim, claim.draftReply());
	}

	/** Tool as Agent: regulated deadlines are computed, not generated. */
	private CaseOutcome openComplaint(CustomerEmail email) {
		LocalDate today = LocalDate.now(clock);
		ComplaintCase complaint = new ComplaintCase("CMP-" + COMPLAINT_SEQUENCE.incrementAndGet(), "Complaints Office",
				addBusinessDays(today, 5), today.plusWeeks(8));
		return new CaseOutcome("complaints", "Formal complaint from " + email.from(), complaint, null);
	}

	static LocalDate addBusinessDays(LocalDate start, int days) {
		LocalDate date = start;
		int added = 0;
		while (added < days) {
			date = date.plusDays(1);
			if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
				added++;
			}
		}
		return date;
	}

	private static String describe(List<Route<CustomerEmail, CaseOutcome>> routes) {
		return routes.stream().map(r -> "- " + r.name() + ": " + r.description()).collect(Collectors.joining("\n"));
	}

	record SelectorInput(CustomerEmail email, String routes) {
	}

	record ClaimInput(CustomerEmail email, String lineOfBusiness, String requiredFacts) {
	}

}
