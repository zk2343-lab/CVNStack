---
name: cvnstack-code-change
description: >-
  Implements CVNStack waitlist-service changes within one GitHub issue:
  Maven module boundaries, package names, Google Java Style, JUnit 5,
  Mockito, REST Assured, and pull requests. Use when editing Java, pom.xml,
  tests, Checkstyle, or docs for an issue or pull request in this repository.
---

# CVNStack code changes

Do the issue in front of you. Read that issue and its section in `docs/mvp-task-breakdown.md` before editing. Contracts live in that breakdown; this skill only keeps edits inside them.

## Before editing

1. Branch off the current integration line. Do not commit on `main`.
2. If the issue lists a dependency that is not in the tree yet, stop and name the missing issue.
3. Read [reference.md](reference.md) when the change touches modules, packages, status codes, or stores.

## Rules that are easy to miss

- Java 21, Spring Boot 3, Maven. `modules/waitlist-app` is the only process that starts.
- Dependencies point inward. `waitlist-domain` and `waitlist-api` have no JDBC, Redis, or Spring MVC.
- No production in-memory store. Mocks and fakes stay in unit tests. Testcontainers talk to PostgreSQL or Redis.
- Header comment on every new type, tests included. First sentence is the purpose. If a model wrote or debugged the file, the next paragraph is `AI-assisted: Cursor (Grok 4.7), YYYY-MM-DD.`
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

Commit messages say what was added, removed, or changed. Open a pull request for another teammate to review: Ding Wang (`wdingding0924-ux`) or Yoongu Kang (`ygkang-cu`).

Record the session with the `cvnstack-ai-documentation` skill. Attribution stays in the header and in `docs/ai-usage.md`.
