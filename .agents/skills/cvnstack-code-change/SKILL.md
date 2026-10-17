---
name: cvnstack-code-change
description: >-
  Implements CVNStack waitlist-service changes within one GitHub issue:
  Maven module boundaries, package names, Google Java Style, JUnit 5,
  Mockito, REST Assured, and pull requests. Use when editing Java, pom.xml,
  tests, Checkstyle, or docs for an issue or pull request in this repository,
  including from Cursor, GitHub Copilot, or another coding agent.
---

<!-- AI-assisted. -->

# CVNStack code changes

Do the issue in front of you. Read that GitHub issue before editing. Contracts live in [reference.md](reference.md); this skill only keeps edits inside them.

## Before editing

1. Branch off the current integration line. Do not commit on `main`.
2. If the issue lists a dependency that is not in the tree yet, stop and name the missing issue.
3. Read [reference.md](reference.md) when the change touches modules, packages, status codes, or stores.

## Rules that are easy to miss

- Java 21, Spring Boot 3, Maven. `modules/waitlist-app` is the only process that starts.
- Dependencies point inward. `waitlist-domain` and `waitlist-api` have no JDBC, Redis, or Spring MVC.
- No production in-memory store. Mocks and fakes stay in unit tests. Testcontainers talk to PostgreSQL or Redis.
- Header comment on every new type, tests included. The comment states the purpose. If you generated or debugged the file, add `AI-assisted` and nothing else about the session.
- Indent 2 spaces. Line length 100. Ruleset: `config/checkstyle/checkstyle.xml`.
- Unit tests use JUnit 5 and Mockito. API tests use REST Assured. One command: `mvn test`.
- Libraries come from Maven. Do not vendor jars or copy third-party source in.

## Check

Use a JDK 21 `JAVA_HOME` for Maven. Do not change the user shell profile.

```bash
mvn test
mvn checkstyle:check
```

## Pull request

Commit messages say what was added, removed, or changed. Author and committer must be the human's school GitHub identity for this repository. One approving review from another teammate is enough: Ding Wang (`wdingding0924-ux`) or Yoongu Kang (`ygkang-cu`). Do not list an agent on the pull request.

When the task is complete, add one entry to `docs/ai-usage.md` using the `cvnstack-ai-documentation` skill. The shared policy for every coding agent is `AGENTS.md`.
