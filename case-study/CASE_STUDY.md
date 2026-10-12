# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

The core challenge is that most costs are shared or indirect — a delivery truck that serves multiple Stores, or a warehouse team that handles products for several Stores simultaneously. Accurately splitting those costs requires agreed allocation rules (e.g., volume-weighted, revenue-weighted, activity-based) that must be applied consistently before any reporting is meaningful.

Key questions I would ask before designing the system:
- At what granularity does the business need cost visibility — per Warehouse, per Store, per product SKU, per shift?
- Which costs are directly attributable (clear ownership) versus shared (need an allocation key)?
- What is the accepted latency for cost data — real-time, daily, monthly close?
- Which source systems hold the raw data (WMS, TMS, HR/payroll, ERP) and how reliable and structured are their exports?

Data quality is usually the biggest practical barrier. If the source systems use different cost centre hierarchies, currencies, or timing conventions, any downstream allocation will inherit those inconsistencies. I would prioritise data normalisation and a clear audit trail (every allocated amount should be traceable back to its source transaction and allocation rule) before building reporting dashboards.

From a system design perspective, cost allocation logic should be modelled explicitly — not embedded in SQL aggregations scattered across reports — so that allocation rules can be changed centrally and retroactively recalculated when methodology changes.

---

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

Identification: cost optimization begins with a baseline — you cannot improve what you cannot measure. I would start by building a cost-per-unit-fulfilled metric broken down by Warehouse and Store. Areas significantly above the median are candidates for investigation. Typical drivers are: underutilised warehouse capacity (fixed costs spread over fewer units), high transportation costs from poorly located Stores relative to Warehouses, and labour inefficiency from manual processes.

Prioritisation: I would rank opportunities by (estimated saving × confidence) / implementation effort. Quick wins with low risk (e.g., renegotiating a carrier contract, consolidating shipments) should be taken first to build momentum and fund more complex initiatives. Structural changes (e.g., closing a Warehouse, relocating a Store) need a longer business case and involve one-off transition costs.

Implementation considerations:
- Changes to Warehouse or Store placement affect the fulfilment assignment logic in this system, so any optimisation that requires reassigning which Warehouse serves which Store must be reflected in the data model and business rules.
- A/B testing or phased rollout (e.g., piloting a new carrier in one region) is safer than big-bang changes, and gives data to validate the saving estimate before full commitment.
- Service quality metrics (order lead time, fulfilment rate) must be monitored alongside cost metrics to detect any quality degradation introduced by the optimisation.

---

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

Why integration matters: without a connection to the financial system of record, cost data in the Cost Control Tool will diverge from the figures used for P&L and auditing. Finance teams will maintain parallel spreadsheets to reconcile, trust in the tool will erode, and decisions will be made on inconsistent numbers.

Before designing the integration I would ask:
- Is the financial system the master for cost data, or does the Cost Control Tool receive raw events and allocate costs itself?
- What integration interfaces does the financial system expose — batch file export, REST API, message queue, ERP connector?
- What are the SLA requirements for data freshness — can we accept a nightly batch, or does a Store manager need today's accrued costs during the working day?
- How are correction and reversal transactions handled, and must they propagate back to Cost Control in near-real-time?

Design considerations:
- An event-driven integration (the financial system publishes cost events to a message broker, the Cost Control Tool subscribes) is more resilient than point-to-point API calls and naturally supports retry on failure.
- An idempotency key on each cost event prevents double-counting if the consumer reprocesses messages after a restart.
- A reconciliation job that compares financial system totals against Cost Control Tool totals on a scheduled basis catches silent data drift that event processing alone might miss.
- All integrated data should carry the source system's transaction reference so discrepancies can be traced.

---

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

Forecasting in fulfilment is difficult because costs have both fixed components (warehouse lease, headcount) and variable components (carrier rates, overtime) that respond non-linearly to volume spikes. A model that assumes linear cost scaling will systematically underestimate peak costs.

Key design considerations:

Driver-based budgeting: rather than extrapolating last year's actuals, build the budget from operational drivers (expected unit volume × cost-per-unit by category). This makes assumptions explicit and allows "what-if" simulations (e.g., what happens to cost if volume grows 20% but headcount is flat?).

Granularity vs. maintainability: a budget at SKU level is more accurate but much harder to maintain. I would budget at product category × Warehouse level as a pragmatic starting point, with the ability to drill down where a category represents significant spend.

Rolling forecast: a static annual budget becomes stale quickly in a dynamic fulfilment operation. A 12-month rolling forecast updated monthly with actuals-to-date and revised assumptions gives a more actionable picture than a fixed annual plan.

Data requirements: accurate forecasting requires historical actuals (ideally 24+ months to capture seasonality), a calendar of known events (promotions, peak seasons), and a way to model structural changes (new Warehouse openings, Store closures).

Integration with operational planning: the forecast is only useful if it feeds back into resource planning — staffing levels, carrier capacity reservations, and inventory replenishment — so the system needs to expose forecast data to the operational teams who act on it.

---

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

Why preserving cost history matters:

1. Audit and compliance — financial systems require a complete, unbroken record of where costs were incurred. Deleting or overwriting the old Warehouse record would leave gaps that auditors and regulators cannot accept.

2. Benchmarking — the new Warehouse should be compared against the old one to validate that the replacement actually achieved the expected cost improvement. Without the archived cost history that comparison is impossible.

3. Budget baseline — the budget for the new Warehouse is typically derived from the old Warehouse's actuals adjusted for new capacity, lease rates, and expected efficiency gains. Losing the actuals means the new budget is built on estimates rather than evidence.

How this relates to the data model:

In this system, the Replace operation archives the old Warehouse (sets `archivedAt`) rather than deleting it, and creates the new Warehouse with the same Business Unit Code. This design is correct for cost history because:
- The archived record retains its identity and can still be joined to historical cost transactions by `businessUnitCode` + time range.
- The `archivedAt` timestamp acts as a cost period boundary: costs before `archivedAt` belong to the old Warehouse, costs after belong to the new one.
- Reporting queries that need a full cost view across the replacement boundary can union active and archived records and use `createdAt`/`archivedAt` as period filters.

Operational considerations:

During the transition period (old Warehouse winding down, new one ramping up) both may be operating simultaneously with overlapping costs. The budget must account for this double-run cost explicitly. Tracking `createdAt` on the new Warehouse and `archivedAt` on the old one gives the exact overlap window for financial reconciliation.

---

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
