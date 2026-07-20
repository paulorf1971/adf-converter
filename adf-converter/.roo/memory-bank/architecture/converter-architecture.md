# Converter Architecture

Current Implementations

- `AdfToMarkdownConverter` / `MarkdownToAdfConverter`
- `AdfToHtmlConverter` / `HtmlToAdfConverter`
- `AdfToPlainTextConverter` / `PlainTextToAdfConverter`

Factory facades

- `MarkdownConverters`
- `HtmlConverters`
- `PlainTextConverters`

Current architecture notes

- Converters are manual parser/renderer implementations around a shared ADF model.
- Markdown inbound uses flexmark; HTML inbound uses jsoup; Plain Text inbound is heuristic and line-oriented.
- `AdfNode.@JsonSubTypes` is the source of truth for deserializable node types.
- `TableUtil` normalizes cell attrs; `MediaUtil` builds/parses media placeholders.

Future Architecture

- Shared rendering helpers for lists, tables, marks, media, and dates may reduce duplication.
- Add abstractions after tests cover behavior well enough to prevent regressions.
- Avoid large architecture rewrites until duplicated logic causes concrete bugs.
