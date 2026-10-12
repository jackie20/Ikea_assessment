# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
The codebase mixes two Panache styles: the Active Record pattern (Store and Product extend PanacheEntity,
so persistence calls sit on the entity itself) and the Repository pattern (Warehouse uses a separate
WarehouseRepository that extends PanacheRepository).

I would standardise on the Repository pattern across all entities for these reasons:

1. Testability — with the repository style you can mock WarehouseStore in unit tests without touching
   the database. Active Record entities are much harder to mock because the static persist/findById
   calls are bound to the class, so tests need a full Quarkus context (slower) or the JPA layer.

2. Separation of concerns — keeping persistence logic in a repository and domain logic in use cases
   is cleaner and aligns with the hexagonal architecture already established for the Warehouse module.

3. Consistency — mixing both patterns in the same codebase creates a steeper learning curve for new
   team members who have to understand two different mental models.

I would not make this change all at once; I would migrate Store and Product to the repository pattern
incrementally, one entity at a time, driven by new feature work or test improvements to limit risk.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Code-first (Product/Store):
  Pros: less ceremony, fast to start, no extra tooling, code is the single source of truth.
  Cons: the API contract is only discoverable by reading the code; documentation drifts unless you
  add annotations manually; breaking changes are easy to introduce accidentally.

Spec-first (Warehouse):
  Pros: the OpenAPI YAML is a clear, version-controlled contract that can be shared with consumers
  before implementation starts. Code generation from the spec enforces that the implementation stays
  in sync with the contract. Consumer teams, front-ends, and QA can generate mocks directly from the
  YAML. Linting and breaking-change detection tools (e.g., openapi-diff) work on the YAML.
  Cons: initial tooling setup costs time; generated code can be verbose or awkward; the generator
  version must be kept in sync with the runtime; adding custom behaviour (authentication, logging)
  around generated interfaces requires extra boilerplate.

My choice: spec-first for any API that crosses a team or system boundary, because the contract is the
most important artefact. Code-first is acceptable for purely internal endpoints where the team owns
both sides, but even then, generating the OpenAPI YAML from annotations (e.g., with SmallRye OpenAPI)
gives most of the contract benefits with minimal overhead.

For this codebase I would migrate Product and Store to a spec-first approach to align with the
Warehouse module and enable consistent API documentation.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority order:

1. Domain/unit tests — the use cases (Create, Archive, Replace) contain all the business rules and
   are pure Java, so they are cheap to write and fast to run. I would cover every validation branch
   with Mockito-based unit tests. These tests give the highest confidence per minute of investment.

2. Integration tests for the REST layer — a small number of @QuarkusTest tests that start the full
   application with an in-memory H2 or the Quarkus dev-services PostgreSQL container verify that
   JSON serialisation, HTTP status codes, and the full request path work as expected. I would keep
   the number of these tests small: one happy path and one error path per endpoint.

3. Repository tests — Panache queries are best verified against a real database to catch HQL syntax
   errors that mocks would not catch. @QuarkusTest with @TestTransaction handles rollback automatically.

I would explicitly skip: testing generated code (the OpenAPI generator output), trivial getters/setters,
and framework wiring that Quarkus already tests.

To maintain coverage over time:
- Enforce a JaCoCo minimum (70% line coverage here) as a build gate so regressions are caught in CI.
- Require tests for every new use case before merging (definition of done).
- Review coverage reports in PR reviews, not as a vanity metric but to catch missed edge cases.
- Prefer a few meaningful tests over many shallow tests that only inflate the percentage.
```
