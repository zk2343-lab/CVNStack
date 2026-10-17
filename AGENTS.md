<!-- AI-assisted. -->

# CVNStack agent policy

This file is the policy for every coding agent. Task skills live in `.agents/skills/`. Cursor, GitHub Copilot, Codex, and the other agents load that directory. There is no separate `.cursor` or `.github` copy.

## Scope

Read the GitHub issue before editing. Do that issue only. If a listed dependency is not in the tree, stop and name the missing issue. Module boundaries, status codes, and stores are in `.agents/skills/cvnstack-code-change/reference.md`.

Java 21, Spring Boot 3, Maven. `modules/waitlist-app` is the only process that starts until issue 17. Dependencies point inward. No production in-memory store. Libraries come from Maven.

Header comment on every new type, tests included: one sentence of purpose.

## AI usage

Two marks, and nothing else.

1. On each file you generate or debug, including tests, add `AI-assisted` to the existing header comment. Do not add the tool name, the prompt, or a token count to the file.
2. When the task is completed, append one entry to `docs/ai-usage.md`. Include the tool, where it came from, what it was used for, the prompt, the files, and the token total. If the usage screen was not read, write `not yet read from the product usage screen`. Never invent a number.

Do not open a second usage document.

## Git and GitHub identity

This is a school repository. Commits and pull requests must show only the human teammate who owns the work, on their **school** GitHub account (`zk2343-lab`, `wdingding0924-ux`, or `ygkang-cu`). Use the Columbia email linked to that account. Do not use a personal GitHub account. Do not use an agent email.

Agents never appear as a participant:

- Do not set an agent as author or committer.
- Do not add `Co-authored-by`, `Generated-by`, `Signed-off-by`, or any trailer that names Cursor, Copilot, Codex, or another agent.
- Do not add an agent as collaborator, reviewer, assignee, or team member.
- Do not call the GitHub collaborator API for an agent.

Do not change the human's **global** git config. For this repository only, the local `user.name` / `user.email` must be that teammate's school identity. When creating a commit, verify `git log -1 --format='%an <%ae>%n%B'` shows the school identity and no agent trailer before you push. If the tool injects `Co-authored-by: Cursor`, recreate the commit with `git commit-tree` so the published history has no agent trailer.
