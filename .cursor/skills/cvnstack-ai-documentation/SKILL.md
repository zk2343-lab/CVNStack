---
name: cvnstack-ai-documentation
description: >-
  Records AI assistance for CVNStack the way iteration 1 requires: a header
  mark on each generated or debugged file, and a session entry in
  docs/ai-usage.md with tool, source, purpose, prompt, and token total.
  Use when an agent writes or debugs code, tests, or docs, or when it
  commits or opens a pull request. Do not add an agent as a GitHub collaborator.
---

# CVNStack AI documentation

Iteration 1 scores this from evidence on `main`. The evidence is `docs/ai-usage.md` plus a header comment on every file a model generated or debugged, tests included. Follow [reference.md](reference.md) for the log shape.

## Mark the file

On a Java type, the header Javadoc ends its purpose sentence, then:

```java
/**
 * One sentence that says what this type is for.
 *
 * <p>AI-assisted: Cursor (Grok 4.7), YYYY-MM-DD.
 */
```

On XML, properties, and Markdown, put the same `AI-assisted:` sentence in a file comment at the top. Skill files keep their frontmatter first and put the comment immediately after it.

Mark a file a person later edits if a model generated or debugged it. Do not remove an existing mark.

## Log the session

Append one session to `docs/ai-usage.md` before the pull request. Include the tool, the product URL, the purpose, the user prompt, and the files touched.

The Cursor token total is on the usage screen, not in the chat. If you cannot read that screen, leave **not yet read from Cursor usage** and today's date. Never invent a token count. When someone does read the screen, replace that phrase with the number and the date it was read.

## Git and GitHub identity

The commit author is the human's existing git identity. Do not change git config.

Do not add `cursoragent`, Cursor, or any other agent as a collaborator, reviewer, assignee, or team member. Do not call the GitHub collaborator API. Do not put an agent in `Co-authored-by`, `Generated-by`, or the commit message. The header and `docs/ai-usage.md` are the only attribution.
