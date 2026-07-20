# Testing

## Commands

- Full verification: `./mvnw test`.
- Compile without tests: `./mvnw test-compile`.
- Focused test class: `./mvnw -Dtest=MarkdownConvertersTest test`.
- Focused test method: `./mvnw -Dtest=MarkdownConvertersTest#inbound_simple_roundtrip test`.
- Dependency scan: `./mvnw -Pdependency-check dependency-check:check`.

## Test Layout

- Markdown behavior: `MarkdownConvertersTest`.
- HTML behavior: `HtmlConvertersTest`.
- Plain Text behavior: `PlainTextConvertersTest`.
- JSON/model behavior: `JsonUtilTest`.
- Table utilities: `TableUtilTest`.
- Media utilities: `MediaUtilTest`.
- Sample fixtures: `SampleFixturesTest` with resources in `src/test/resources/samples/`.

## Expectations

- Tests use JUnit 5 and AssertJ from `spring-boot-starter-test`.
- No database, server, or external service is required for normal tests.
- Prefer structural assertions against ADF model objects for inbound converters.
- Use exact or focused string assertions for outbound formatting.
- Avoid tests that only check JSON substrings for important behavior like task state, marks, table content, or media attrs.
- Every implemented ADF node in `AdfNode.@JsonSubTypes` should deserialize through `JsonUtil`.
- Every mark factory in `Mark` should have rendering coverage or explicit unsupported-mark coverage.
- Cross-format round trips should use structural assertions where format loss is not expected.
