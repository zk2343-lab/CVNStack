# AI usage log reference

Course text: `docs/iteration-1-guidelines.md`, section "AI documentation".

## Tool row

Keep one row per tool. Add a session under that tool each time it is used. Do not delete earlier sessions.

```markdown
## Cursor

| | |
| --- | --- |
| Tool | Cursor desktop agent |
| Source | https://cursor.com |
| Model | Grok 4.7 |
| Token total | not yet read from Cursor usage |

### YYYY-MM-DD — short name

- **Purpose.** What the session changed.
- **Prompt.** The user prompt, in a block quote.
- **Files.** Paths a model generated or debugged.
- **Tokens.** Included in the tool total above, or "not yet read from Cursor usage" plus the date.
```

If a later tool is used (another editor, ChatGPT, Copilot), add a new `##` section with the same fields. The token total is per tool, not per session. Session notes say whether they are included in that total.

## Header

Java:

```java
/**
 * Purpose in one sentence.
 *
 * <p>AI-assisted: Cursor (Grok 4.7), YYYY-MM-DD.
 */
```

Other text files, first line:

```text
AI-assisted: Cursor (Grok 4.7), YYYY-MM-DD.
```

Use the model that actually ran. Do not leave a placeholder model name.

## What not to write

- A guessed token number.
- `Co-authored-by: cursoragent` or any agent trailer.
- An agent login in the commit author, the PR reviewer list, or the repository collaborator list.
- A second copy of this log outside `docs/ai-usage.md`.
