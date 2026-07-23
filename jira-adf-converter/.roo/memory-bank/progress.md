# Progress

Completed

✓ Maven project created

✓ Bidirectional Markdown, HTML, and Plain Text converters available

✓ ADF model and Jackson polymorphic registration available through `AdfNode`

✓ Focused converter and utility tests pass with Maven

✓ Sample documents are available as executable fixtures under `src/test/resources/samples/`

Current Status

Project compiles and test suite passes: `./mvnw test` currently reports 43 tests, 0 failures.

Recent fixes

- HTML inbound preserves mixed text and task item text/state.
- Inbound table cells from Markdown, HTML, and Plain Text now contain paragraph blocks.
- Selected model classes now support Jackson no-arg construction for polymorphic deserialization.
- HTML date rendering preserves original ADF timestamp via `data-adf-timestamp`.
- Media parsing handles `media:id?collection=x&type=file` and HTML `data-media-*` attrs.
- Media containers render all media children instead of only the first.
- Markdown ordered lists honor the `order` attr; blockquotes/panels prefix rendered lines.
- Plain Text inbound normalizes CRLF and preserves blank lines inside fenced code blocks.
- Unused `ConverterConfig` fields were removed; current config is limited to implemented behavior.
- Plain Text inbound parses media placeholders and separates mixed list kinds into separate list blocks.
- Fixture-backed sample tests cover JSON, Markdown, HTML, and Plain Text sample documents.

Remaining Work

- No open mandatory items from the previous list.

Known Limitations

- Converters are functional but still independent/manual implementations with some duplicated logic.
- Plain Text conversion remains intentionally heuristic and less expressive than Markdown or HTML.
- Plain Text mixed lists are split into separate list blocks; nested list structure is not modeled.
- `subsup` and `textColor` mark factories exist, but Markdown/HTML outbound treat unsupported marks according to `allowUnknownMarks` instead of rendering them specially.
