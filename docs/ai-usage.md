<!-- AI-assisted: Cursor (Grok 4.7), 2026-10-16. -->

# AI usage

Iteration 1 requires a record of every AI tool: what it is, where it came from, what it was used for, the prompts, and the token total per tool. File marks are header comments. This log is that record. Agents update it by following `.cursor/skills/cvnstack-ai-documentation/`.

Token totals are copied from the product's usage screen. A session does not reveal its own token count, so a row says **not yet read from Cursor usage** until someone opens that screen and writes the number plus the date they read it. Do not estimate.

## Cursor

| | |
| --- | --- |
| Tool | Cursor desktop agent |
| Source | <https://cursor.com> |
| Model | Grok 4.7 |
| Token total | **not yet read from Cursor usage** |

### 2026-10-16 — iteration plan

- **Purpose.** Draft the iteration requirements and the task split.
- **Files.** `docs/iteration-1-guidelines.md`, `docs/mvp-task-breakdown.md`.
- **Prompt.** Course first-iteration brief and the CVNStack proposal, asking for the guidelines and the MVP task breakdown. The original chat text was not stored in the repository; the files record the source in their closing note.
- **Tokens.** Included in the Cursor total above once that total is read. Not yet read.

### 2026-10-16 — issue 1 scaffold and agent skills

- **Purpose.** Scaffold the Spring Boot project for [issue 1](https://github.com/zk2343-lab/CVNStack/issues/1), and add project skills so later agent edits follow the same module, style, and AI-log rules.
- **Prompt.**

  > can you help me to work on https://github.com/zk2343-lab/CVNStack/issues/1 While working on it, build ai skill references so team can have consistent guildine while using agent to work on code cahnge - don't add cursoragent as collaborator or leave any stupid chace, and instead follow Iteration 1 Guidelines' AI documentation.

- **Files.** Parent `pom.xml`, `modules/waitlist-app/`, `config/checkstyle/checkstyle.xml`, `.gitignore`, `README.md`, this log, `.cursor/skills/cvnstack-code-change/`, `.cursor/skills/cvnstack-ai-documentation/`, and the skill pointer in `docs/iteration-1-guidelines.md` and `docs/mvp-task-breakdown.md`.
- **Tokens.** Included in the Cursor total above. Not yet read from Cursor usage on 2026-10-16.
