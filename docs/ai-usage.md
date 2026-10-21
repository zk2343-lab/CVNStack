<!-- AI-assisted. -->

# AI usage

This is the only AI usage record in the repository. When a task is completed, append one entry under the tool that did the work. A file the model generated or debugged is marked `AI-assisted` in its header. Prompts and token totals are not copied into that file.

Token totals come from the product's usage screen. If the screen has not been read, write **not yet read from Cursor usage** and do not estimate. When it is read, replace that phrase with the number and the date.

## Cursor

| | |
| --- | --- |
| Tool | Cursor desktop agent |
| Source | <https://cursor.com> |
| Model | Grok 4.7 (high); Composer default on later turns |
| Token total | **13,991,013** (input 739,466 + output 192,603 + cache read 13,058,944), read from Cursor usage API on 2026-10-16 for the three chats below. Request cost about $9.12 of included Pro usage. |

### Iteration plan

- **Completed.** 2026-10-16
- **Purpose.** Draft the iteration requirements and the task split.
- **Prompt.** The course first-iteration brief and the CVNStack proposal. The original chat text was not stored.
- **Files.** `docs/iteration-1-guidelines.md`, `docs/mvp-task-breakdown.md` (later removed; content moved to GitHub issues and agent skills)
- **Tokens.** About **6,368,067** (input 231,680 + output 92,995 + cache read 6,043,392). Included in the Cursor total above.

### Issue 1 — Scaffold the Spring Boot project

- **Completed.** 2026-10-16
- **Purpose.** Bootable Java 21 / Spring Boot 3 module with `GET /health`, parent test and quality plugins, Checkstyle ruleset, and agent skills that keep later edits on this log.
- **Prompts.**

  > can you help me to work on https://github.com/zk2343-lab/CVNStack/issues/1 While working on it, build ai skill references so team can have consistent guildine while using agent to work on code cahnge - don't add cursoragent as collaborator or leave any stupid chace, and instead follow Iteration 1 Guidelines' AI documentation.

  > also have work in drfat (don't pr or release just yet) I will review the work before commit

  > usage and of ai should be consolidated in one place (i am observing the AI generated referneces in many places across repo) and documented on each tasks upon completion. squash to one commit ahead of mainline with one short messagae

  > does this complhy to instruction? do we need to comment ai usage inline???? or it is fine as long as we have one place to document the suage. Additionally, I hope this experience can be streamlined and applied across all kinds of ai agent coders(cursor, copilot, etc). the idea is to add in skill so those ai code detect and apply same strategy

- **Files.** `pom.xml`, `modules/waitlist-app/`, `config/checkstyle/checkstyle.xml`, `.gitignore`, `README.md`, `AGENTS.md`, `.agents/skills/`, this file.
- **Tokens.** About **6,852,019** (input 320,315 + output 84,984 + cache read 6,446,720). Included in the Cursor total above.

### Remove the planning docs

- **Completed.** 2026-10-16
- **Purpose.** Drop the iteration guidelines and the task breakdown now that the agent skills and the GitHub project issues hold that material, and put build and start commands in the README.
- **Prompt.**

  > lets remove ./doc?

  > The instructions were added to agent skill, the tasks were added to project dashboard as issues.

  > Also add how to build and start service in README.md

- **Files.** Deleted `docs/iteration-1-guidelines.md` and `docs/mvp-task-breakdown.md`. Updated `README.md`, `AGENTS.md`, `.agents/skills/cvnstack-code-change/SKILL.md`, `.agents/skills/cvnstack-code-change/reference.md`, and this file.
- **Tokens.** About **770,927** (input 187,471 + output 14,624 + cache read 568,832), and still rising for later turns. Included in the Cursor total above.

### Clean commit identity on the issue 1 PR

- **Completed.** 2026-10-16
- **Purpose.** Stop agents and personal accounts from appearing on commits or pull requests; require the school GitHub identity; rewrite the issue 1 commit without a Cursor co-author trailer.
- **Prompt.**

  > On Pull request, it shows shalloe99cursoragent shalloe99 and cursoragent committed 2 minutes ago

  > i want only the author itself, no cursor or other ai agent mentioned as particiapnts (update in agent skill). and use school account when working on school project. clean out history for this bad one and resubmit

- **Files.** `AGENTS.md`, `.agents/skills/cvnstack-ai-documentation/SKILL.md`, `.agents/skills/cvnstack-ai-documentation/reference.md`, `.agents/skills/cvnstack-code-change/SKILL.md`, this file.
- **Tokens.** Included in the Cursor total above. Not re-read for this turn alone.

### Issues 11, 17, 2, 13 — modules, errors, Checkstyle, README

- **Completed.** 2026-10-17 (local branch; awaiting sign-off before commit)
- **Purpose.** Shared Maven modules with domain types/ports and pure ranking/estimate/transitions; API error codes, `ErrorResponse`, and `@ControllerAdvice`; clean Checkstyle reports under `reports/checkstyle/`; README/API docs aligned with the CVNStack proposal and MVP deviations.
- **Prompt.**

  > pull in latest change, work in a seperate local feature rbanch https://github.com/zk2343-lab/CVNStack/issues/11 https://github.com/zk2343-lab/CVNStack/issues/17 and https://github.com/zk2343-lab/CVNStack/issues/2
  > https://github.com/zk2343-lab/CVNStack/issues/13
  >
  > Remmeber to reference from our project design file Waitlist-Management-Service-CVNStack-Proposal (1).pdf . don't commit your changes - wait for my final sign off
- **Files.** `pom.xml`, `README.md`, `src/main/java/com/cvnstack/{service,model,error}/`, `src/test/...`, `reports/checkstyle/`, `.agents/skills/cvnstack-code-change/`, `AGENTS.md`, this file.
- **Tokens.** not yet read from the product usage screen

## Codex

| | |
| --- | --- |
| Tool | OpenAI Codex |
| Source | https://openai.com/codex/ |
| Model | `gpt-6.1-sol`, recorded in the local session log |
| Token total | **22,015,476** |

### Issue 19 - Redis cache for ranks, locks, and reads

- **Completed.** Local implementation and testing. Simplified and documented.
- **Purpose.** Implement the existing `CacheStorage` interface with Redis waiting order, entry caching, and serve-next locks. Add Redis container tests and unit tests. Prepare the local JDK 21 and Docker environment.
- **Prompt.**

  > Review the existing base code against the requirements and TODOs in issue #19. Check the existing CacheStorage interface and whether the implementation follows the intended direction. Discuss the changes with me before committing work, implement the approved work, and verify it with tests. Use standard Java where it keeps the code clear and efficient.

- **Files.** `src/main/java/com/cvnstack/service/RedisCacheStorage.java`, `src/test/java/com/cvnstack/service/RedisCacheStorageTest.java`, `src/test/java/com/cvnstack/service/RedisCacheStorageUnitTest.java`, `pom.xml`, `README.md`, this file.
- **Verification.** `mvn clean verify`: 49 tests passed, none skipped, and zero Checkstyle violations after removing the optional lock-duration setting.
- **Tokens.** Included in the Codex total above. It includes #19 preparation, environment setup, related status checks, implementation, tests, explanations, and revisions.
  | Recorded counter | Tokens |
  | --- | ---: |
  | Input, including cached input | 21,923,834 |
  | Cached input, already included above | 21,125,120 |
  | Output, including reasoning | 91,642 |
  | Total: input + output | **22,015,476** |
