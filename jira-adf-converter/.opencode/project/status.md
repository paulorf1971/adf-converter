# Status

## Verified State

- Bidirectional Markdown, HTML, and Plain Text converters are implemented.
- ADF model and Jackson polymorphic registration are available through `AdfNode`.
- Sample documents are executable fixtures under `src/test/resources/samples/`.
- Latest verified normal test run: `./mvnw test` reports 43 tests, 0 failures.

## Current Dependencies

- Spring Boot parent: `4.1.0`.
- Java: `17`.
- flexmark: `0.64.8`.
- jsoup: `1.22.2`.
- Lombok: `1.18.46`.
- Maven compiler plugin: `3.15.0`.
- Jackson versions are managed by the Spring Boot parent through the Jackson BOM.
- OWASP Dependency-Check is available through the `dependency-check` Maven profile.

## Recent Decisions

- Unused `ConverterConfig` fields were removed; current config should only expose implemented behavior.
- Sample documents live in `src/test/resources/samples/` and are covered by `SampleFixturesTest`.
- Dependency upgrades should be verified with `./mvnw test`; rollback if verification fails.
- OpenCode project memory now lives under `.opencode/project/`; `.roo/**` is historical and should not be cleaned yet.

## Known Limitations

- Converters are independent/manual implementations with some duplicated logic.
- Plain Text conversion is heuristic and less expressive than Markdown or HTML.
- Plain Text mixed lists are split into separate list blocks; nested list structure is not modeled.
- `subsup` and `textColor` mark factories exist, but Markdown/HTML outbound do not render them specially unless support is added later.
- Desired but currently unmodeled ADF nodes include `expand`, `inlineCard`, `mention`, `nestedExpand`, and `rule`.
