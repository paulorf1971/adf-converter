# Active Context

Current State

Library-style converter implementation exists for all planned directions:

- ADF -> Markdown and Markdown -> ADF
- ADF -> HTML and HTML -> ADF
- ADF -> Plain Text and Plain Text -> ADF

Latest verified status

- `./mvnw test` passes with 43 tests.
- Structural tests cover registered ADF node deserialization, supported/unsupported mark behavior, HTML task text/state, table cell block shape, media metadata parsing, date metadata preservation, Markdown ordered-list starts, blockquote line prefixing, Plain Text media placeholders, mixed list splitting, and fenced code handling.
- Sample fixtures live in `src/test/resources/samples/` and are exercised by `SampleFixturesTest`.
- `AGENTS.md` lives at the Maven project root and contains OpenCode-specific project guidance.

Current Focus

Maintain conversion fidelity and expand coverage when adding new ADF nodes, marks, or config behavior.

Immediate Priorities

1. Add focused tests with every new converter behavior or model subtype.
2. Keep `ConverterConfig` limited to implemented behavior.
3. Preserve `src/test/resources/samples/` as executable fixtures when supported formats change.
4. Continue refactoring duplicated renderer/parser logic only where it reduces real bugs.
