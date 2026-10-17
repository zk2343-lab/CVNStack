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

Add it under the tool that did the work. One token total per tool.

```markdown
### Issue N — title

- **Completed.** YYYY-MM-DD
- **Purpose.** What the task changed.
- **Prompt.** The user prompt, in a block quote. Include follow-ups that changed the result.
- **Files.** Paths the model generated or debugged for this task.
- **Tokens.** Included in the tool total above, or "not yet read from the product usage screen".
```

New tool:

```markdown
## Product name

| | |
| --- | --- |
| Tool | Product name |
| Source | https://example.com |
| Model | model name, if the product shows one |
| Token total | not yet read from the product usage screen |
```

Never invent a token count. Never copy the entry into a source file.

## Commit identity check

Before push:

```bash
git log -1 --format='%an <%ae>%n%B'
```

Author email must be the school address linked to the teammate's school GitHub account. Reject the commit and recreate it if the body contains `Co-authored-by: Cursor`, `cursoragent@`, `copilot`, or any other agent trailer.
