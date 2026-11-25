<!-- AI-assisted. -->

# Mark and task entry

`AGENTS.md` is the policy every agent reads. This file is the template.

## File mark

Java type:

```java
/**
 * Purpose in one sentence.
 *
 * <p>AI-assisted.
 */
```

Other files, in the header comment: `AI-assisted.`

Keep an existing mark if a person later edits the file.

## Task entry

Append the entry at the end of `docs/ai-usage.md`. Do not change any line above it. Do not update an existing tool table or token total. The file uses a union merge (`merge=union` in `.gitattributes`) so two appended entries both stay.

```markdown
### Issue N — title

- **Completed.** YYYY-MM-DD
- **Tool.** Product name (Cursor, GitHub Copilot, Codex, or another).
- **Source.** https://example.com
- **Model.** Model name, if the product shows one. Omit the line if it does not.
- **Purpose.** What the task changed.
- **Prompt.** The user prompt, in a block quote. Include follow-ups that changed the result.
- **Files.** Paths the model generated or debugged for this task.
- **Tokens.** The number from the product usage screen, or "not yet read from the product usage screen".
```

A new product does not get a table inserted higher in the file. The tool, source, and model lines on the new entry are enough.

Never invent a token count. Never copy the entry into a source file. Never replace an earlier token phrase. When the usage screen is read later, append another entry.

## Commit identity check

Before push:

```bash
git log -1 --format='%an <%ae>%n%B'
```

Author email must be the school address linked to the teammate's school GitHub account. Reject the commit and recreate it if the body contains `Co-authored-by: Cursor`, `cursoragent@`, `copilot`, or any other agent trailer.
