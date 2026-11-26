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

Append one entry at the end of `docs/ai-usage.md` for that task. Do not edit, delete, reorder, or reformat any earlier line. Name the product that actually ran (Cursor, GitHub Copilot, or another) inside the new entry. One entry per completed task. Do not write the entry for work still in progress. Do not create another usage file. Do not replace an older token line. A later reading is a new entry appended at the end.

## Git and GitHub identity

Follow [AGENTS.md](../../../AGENTS.md). Short form:

- Commit and open PRs as the human's **school** GitHub account for this repo. Never a personal account. Never an agent.
- Do not change global git config. Local repo `user.name` / `user.email` must be the school identity.
- No agent in author, committer, `Co-authored-by`, `Generated-by`, reviewers, assignees, or collaborators.
- After each commit, check the message body has no agent trailer before pushing. If the environment injects `Co-authored-by: Cursor`, recreate the commit with `git commit-tree` (or equivalent) so the trailer is absent.
