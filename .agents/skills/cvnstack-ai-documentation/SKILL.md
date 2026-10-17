---
name: cvnstack-ai-documentation
description: >-
  Applies the CVNStack AI policy for Cursor, GitHub Copilot, Codex, and
  other coding agents. Use when generating or debugging code, tests, or
  docs, when a task or issue is finished, or when committing. Marks each
  touched file AI-assisted and records the task once in docs/ai-usage.md.
---

<!-- AI-assisted. -->

# CVNStack AI documentation

The shared policy is [AGENTS.md](../../../AGENTS.md). Apply it. Follow [reference.md](reference.md) for the exact mark and the log entry.

## While editing

On every file you generate or debug, tests included, add `AI-assisted` to the header. Leave the purpose comment in place. Do not write the tool, prompt, or token total in the file.

## When the task is complete

Append one entry to `docs/ai-usage.md` for that task. Name the product that actually ran (Cursor, GitHub Copilot, or another). One entry per completed task, under that product's section. Do not write the entry for work still in progress. Do not create another usage file.

## Git and GitHub identity

Follow [AGENTS.md](../../../AGENTS.md). Short form:

- Commit and open PRs as the human's **school** GitHub account for this repo. Never a personal account. Never an agent.
- Do not change global git config. Local repo `user.name` / `user.email` must be the school identity.
- No agent in author, committer, `Co-authored-by`, `Generated-by`, reviewers, assignees, or collaborators.
- After each commit, check the message body has no agent trailer before pushing. If the environment injects `Co-authored-by: Cursor`, recreate the commit with `git commit-tree` (or equivalent) so the trailer is absent.
