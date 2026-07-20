# ADF Converters

Supported Directions

ADF → Markdown

ADF → HTML

ADF → Plain Text

Markdown → ADF

HTML → ADF

Plain Text → ADF

Implemented/deserializable ADF Nodes

- blockquote
- bulletList
- codeBlock
- date
- doc
- emoji
- hardBreak
- heading
- listItem
- media
- mediaGroup
- mediaInline
- mediaSingle
- orderedList
- panel
- paragraph
- status
- taskList
- taskItem
- decisionList
- decisionItem
- table
- tableRow
- tableCell
- tableHeader

Desired but not currently modeled in source

- expand
- inlineCard
- mention
- nestedExpand
- rule

Supported Marks

- code
- em
- link
- strike
- strong
- subsup
- textColor
- underline

Notes

- Treat this file as feature intent plus current source snapshot; verify exact support in `AdfNode.@JsonSubTypes`, `Mark`, converters, and tests before changing behavior.
- Inbound table cells are expected to contain paragraph blocks, not direct text nodes.
- Media metadata should preserve `id`, `type`, and `collection` when present.
- Plain Text inbound supports `[media:id]` and `[media:id?collection=x]` placeholders as media nodes.
- Plain Text mixed list kinds are parsed as separate list blocks; nested list structure is not currently modeled.
- `subsup` and `textColor` marks are modeled but do not have special Markdown/HTML rendering unless support is added later.
