# Workflow

## Before Editing

- Work from the Maven project root: `adf-converter/`.
- Ignore generated output under `target/`.
- Prefer Maven/source files over IDE metadata such as `.classpath`, `.project`, `.settings/`, and `.factorypath`.
- Check existing converter tests before changing conversion behavior.

## Change Approach

- Make the smallest correct change.
- Add or update focused tests for new converter behavior, model subtypes, marks, or config fields.
- Keep `ConverterConfig` limited to implemented behavior; do not add future-looking config unless behavior is implemented.
- Introduce shared parser/renderer abstractions only when duplication causes concrete maintenance bugs.
- Avoid broad architecture rewrites without coverage that protects current converter behavior.

## Converter Rules

- Inbound converters should return `Document` objects.
- Outbound converters should accept `Document` and return strings.
- New ADF node classes must be registered in `AdfNode.@JsonSubTypes` and covered by JSON serialization/deserialization tests.
- Inbound table cells should contain paragraph blocks, not direct text nodes.
- Media metadata should preserve `id`, `type`, and `collection` when present.

## Security And Dependencies

- Normal verification remains `./mvnw test`.
- Dependency scanning is opt-in: `./mvnw -Pdependency-check dependency-check:check`.
- `flexmark-all 0.64.8` is the latest Maven Central release but is relatively old; keep untrusted Markdown behavior covered by tests.
- Prefer dependency upgrades with a full test run.

## Historical Memory

- `.roo/**` is retained for now but is not the OpenCode workflow.
- Do not migrate stale Java snippets from `.roo/memory-bank/architecture/*.java`; they reference older packages and APIs.
- If `.roo/**` conflicts with source, tests, or `.opencode/project`, follow source/tests and this folder.
