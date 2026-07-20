# AGENTS.md

## Scope
- This directory is the Maven project root; run project commands from here.
- Ignore generated/build output under `target/` when inspecting or editing code.
- IDE metadata (`.classpath`, `.project`, `.settings/`, `.factorypath`) is present; prefer Maven/source files over IDE config.

## Commands
- Full verification: `./mvnw test`.
- Focused test class: `./mvnw -Dtest=MarkdownConvertersTest test`.
- Focused test method: `./mvnw -Dtest=MarkdownConvertersTest#inbound_simple_roundtrip test`.
- Compile without tests: `./mvnw test-compile`.

## Project Shape
- Java 17, Spring Boot `4.1.0`, Maven wrapper `3.9.16`; Lombok annotation processing is configured in `pom.xml`.
- `AdfConverterApplication` is only the Spring Boot bootstrap; the useful library entrypoints are the factory facades `MarkdownConverters`, `HtmlConverters`, and `PlainTextConverters`.
- Inbound converters produce `com.tsystems.jira.adf.model.Document`; outbound converters consume `Document` and emit `String`.
- Main packages are `adf.model` for ADF nodes/marks, `adf.markdown`, `adf.html`, `adf.plaintext`, `adf.util`, `adf.api`, and lightweight `adf.config`/`adf.registry` support classes.
- ADF JSON polymorphism is centralized in `AdfNode` via Jackson `@JsonSubTypes`; add new node types there or JSON deserialize/serialize tests will miss them.
- Shared JSON behavior goes through `JsonUtil.mapper()`; it disables unknown-property failures and omits nulls.
- Markdown parsing uses flexmark with tables, task lists, autolinks, emoji, and strikethrough extensions; HTML parsing uses jsoup and strips `script`/`style`.

## Memory Bank
- `.roo/rules/memory-bank.md` describes RooCode workflow, not OpenCode workflow; do not blindly follow its instruction to read every memory file for every task.
- Use `.roo/memory-bank/projectbrief.md`, `productContext.md`, and `systemPatterns.md` for product intent: bidirectional ADF conversion for Markdown, HTML, and Plain Text.
- Use `.roo/memory-bank/features/testing.md` as testing goals only; current executable coverage is in `src/test/java`.
- Treat `.roo/memory-bank/activeContext.md`, `progress.md`, and `architecture/*` as historical/aspirational context. Some claims are stale: source now has Markdown, HTML, and Plain Text converters, while `.roo` still calls some converters examples.
- Treat `.roo/memory-bank/features/adf-converters.md` as a desired coverage list, not current truth. The implemented/deserializable node set is the `@JsonSubTypes` list in `AdfNode`.

## Testing Notes
- Tests use JUnit 5 and AssertJ from `spring-boot-starter-test`; no database, server, or external service is required.
- Existing converter tests assert fragments and round-trip sanity rather than exact full documents; preserve or add focused expectations for new ADF nodes/marks in the relevant format test.
- Add focused tests beside the affected converter (`MarkdownConvertersTest`, `HtmlConvertersTest`, `PlainTextConvertersTest`) or utility (`JsonUtilTest`, `TableUtilTest`, `MediaUtilTest`).
