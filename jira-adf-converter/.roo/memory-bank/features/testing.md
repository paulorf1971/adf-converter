# Testing

Required

Current executable coverage

- Run all tests with `./mvnw test` from project root.
- Current suite covers Markdown, HTML, Plain Text, JSON, sample fixtures, table utilities, media utilities, and Spring context smoke test.
- Latest verified run: 43 tests, 0 failures.
- Sample fixtures live in `src/test/resources/samples/` and are exercised by `SampleFixturesTest`.

Testing conventions

- Prefer structural assertions against ADF model objects for inbound converters.
- Use exact or focused string assertions for outbound formatting.
- Avoid tests that only check JSON substrings for important behavior like task state, marks, table content, or media attrs.

Coverage expectations

- Every converter direction should have focused tests for new behavior.
- Every implemented ADF node in `AdfNode.@JsonSubTypes` should deserialize through `JsonUtil`.
- Every mark factory in `Mark` should have either rendering coverage or explicit unsupported-mark coverage.
- Config tests should only cover implemented `ConverterConfig` fields.
- Cross-format round trips should use structural assertions where format loss is not expected.
