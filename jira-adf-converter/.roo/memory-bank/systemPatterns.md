# System Patterns

Architecture

Central converter library with factory facades per format.

Primary Components

- ADF → Markdown
- Markdown → ADF
- ADF → HTML
- HTML → ADF
- ADF → Plain Text
- Plain Text → ADF

Current entrypoints

- `MarkdownConverters.inbound/outbound`
- `HtmlConverters.inbound/outbound`
- `PlainTextConverters.inbound/outbound`

Core model/utility patterns

- Inbound converters produce `com.tsystems.jira.adf.model.Document`.
- Outbound converters consume `Document` and emit `String`.
- ADF node polymorphism is registered in `AdfNode.@JsonSubTypes`.
- JSON behavior is centralized in `JsonUtil.mapper()`.
- Table normalization lives in `TableUtil`; media placeholder/URL parsing lives in `MediaUtil`.

Design Principles

- OOP
- SOLID
- Design Patterns
- Clean Architecture

Expected Evolution

Introduce shared parser/renderer abstractions only after more coverage exists and duplication causes concrete maintenance bugs.
