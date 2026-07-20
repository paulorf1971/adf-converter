# AGENTS.md

## Scope
- This directory is the Maven project root; run project commands from here.
- Ignore generated/build output under `target/` when inspecting or editing code.
- IDE metadata (`.classpath`, `.project`, `.settings/`, `.factorypath`) is present; prefer Maven/source files over IDE config.
- OpenCode project memory lives under `.opencode/project/`; start with `.opencode/project/README.md` when project context is needed.

## Commands
- Full verification: `./mvnw test`.
- Focused test class: `./mvnw -Dtest=MarkdownConvertersTest test`.
- Focused test method: `./mvnw -Dtest=MarkdownConvertersTest#inbound_simple_roundtrip test`.
- Compile without tests: `./mvnw test-compile`.
- Dependency scan: `./mvnw -Pdependency-check dependency-check:check`.

## Project Shape
- Java 17, Spring Boot `4.1.0`, Maven wrapper `3.9.16`; Lombok annotation processing is configured in `pom.xml`.
- `AdfConverterApplication` is only the Spring Boot bootstrap; the useful library entrypoints are the factory facades `MarkdownConverters`, `HtmlConverters`, and `PlainTextConverters`.
- Inbound converters produce `com.tsystems.jira.adf.model.Document`; outbound converters consume `Document` and emit `String`.
- Main packages are `adf.model` for ADF nodes/marks, `adf.markdown`, `adf.html`, `adf.plaintext`, `adf.util`, `adf.api`, and lightweight `adf.config`/`adf.registry` support classes.
- ADF JSON polymorphism is centralized in `AdfNode` via Jackson `@JsonSubTypes`; add new node types there or JSON deserialize/serialize tests will miss them.
- Shared JSON behavior goes through `JsonUtil.mapper()`; it disables unknown-property failures and omits nulls.
- Markdown parsing uses flexmark with tables, task lists, autolinks, emoji, and strikethrough extensions; HTML parsing uses jsoup and strips `script`/`style`.

## OpenCode Workflow
- Use `.opencode/project/context.md` for product and architecture context.
- Use `.opencode/project/workflow.md` for change workflow and source-of-truth rules.
- Use `.opencode/project/testing.md` for test conventions and commands.
- Use `.opencode/project/status.md` for current verified state, dependencies, decisions, and known limitations.
- `.roo/**` is historical RooCode memory; do not treat it as canonical when it conflicts with source, tests, `AGENTS.md`, or `.opencode/project/`.

## Testing Notes
- Tests use JUnit 5 and AssertJ from `spring-boot-starter-test`; no database, server, or external service is required.
- Existing converter tests assert fragments and round-trip sanity rather than exact full documents; preserve or add focused expectations for new ADF nodes/marks in the relevant format test.
- Add focused tests beside the affected converter (`MarkdownConvertersTest`, `HtmlConvertersTest`, `PlainTextConvertersTest`) or utility (`JsonUtilTest`, `TableUtilTest`, `MediaUtilTest`).
