# Project Context

## Purpose

`adf-converter` provides reusable bidirectional conversion between Atlassian Document Format (ADF) and common text formats:

- Markdown
- HTML
- Plain Text

Target users are developers integrating Atlassian content with systems that need non-ADF formats.

## Runtime Shape

- Java 17 Maven project using Spring Boot `4.1.0`.
- Maven wrapper version is `3.9.16`.
- `AdfConverterApplication` is only the Spring Boot bootstrap.
- Useful library entrypoints are factory facades: `MarkdownConverters`, `HtmlConverters`, and `PlainTextConverters`.
- Inbound converters produce `com.tsystems.jira.adf.model.Document`.
- Outbound converters consume `Document` and emit `String`.

## Main Packages

- `com.tsystems.jira.adf.model`: ADF nodes and marks.
- `com.tsystems.jira.adf.markdown`: Markdown inbound/outbound conversion.
- `com.tsystems.jira.adf.html`: HTML inbound/outbound conversion.
- `com.tsystems.jira.adf.plaintext`: Plain Text inbound/outbound conversion.
- `com.tsystems.jira.adf.util`: shared JSON, table, and media helpers.
- `com.tsystems.jira.adf.api`, `config`, `registry`: lightweight facade/config support.

## Source Of Truth

- Converter behavior in `src/main/java` and tests in `src/test/java` are authoritative.
- Deserializable ADF node types are registered in `AdfNode` via Jackson `@JsonSubTypes`.
- Mark support starts from `Mark` factory methods, then converter-specific rendering/parsing support.
- Shared JSON behavior goes through `JsonUtil.mapper()`; it ignores unknown JSON properties and omits null values.
- `TableUtil` handles table cell normalization.
- `MediaUtil` handles media placeholder and URL parsing.

## Parser Libraries

- Markdown parsing uses flexmark with tables, task lists, autolinks, emoji, and strikethrough extensions.
- HTML parsing uses jsoup and strips `script` and `style` elements.
- Plain Text parsing is intentionally heuristic and line-oriented.
