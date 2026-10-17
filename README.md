<!-- AI-assisted: Cursor (Grok 4.7), 2026-10-16. -->

# CVNStack

Waitlist Management Service for COMS W4156. Client applications create a waitlist, add prioritized entries, and move those entries from waiting to called, served, or cancelled.

The process boots and answers `GET /health`. Waitlist routes come in later issues.

## Docs

- [Iteration 1 guidelines](docs/iteration-1-guidelines.md) — course requirements and definition of done
- [MVP task breakdown](docs/mvp-task-breakdown.md) — scope, package layout, and who owns each pull request
- [AI usage log](docs/ai-usage.md) — tools, prompts, and token totals
- Task board: <https://github.com/users/zk2343-lab/projects/1/views/1>

Team: Zengxiaoran Kang (zk2343-lab), Ding Wang (wdingding0924-ux), Yoongu Kang (ygkang-cu).

## Requirements

Java 21 and Maven 3.9 or newer. Point `JAVA_HOME` at a JDK 21 before the commands below.

Libraries come from Maven. No third-party source is copied into this repository.

## Build, test, and run

```bash
mvn test
mvn checkstyle:check
mvn -pl modules/waitlist-app spring-boot:run
```

Then open `http://localhost:8080/health`. A healthy process returns 200 and `{"status":"UP"}`.

`mvn test` runs JUnit 5, Mockito, and REST Assured. Those dependencies, plus the JaCoCo and Checkstyle plugins, are declared in the parent `pom.xml`. JaCoCo writes its report under `modules/waitlist-app/target/site/jacoco/` on `mvn verify`. Committing that report is a later issue. Checkstyle uses `config/checkstyle/checkstyle.xml`, a project ruleset based on the Google Java Style Guide. A clean report under `reports/checkstyle/` is issue 11.

## Layout

```text
pom.xml
config/checkstyle/checkstyle.xml
modules/waitlist-app/
```

Shared modules are issue 17. Package names are fixed now so later branches do not invent new ones:

| Module | Package |
| --- | --- |
| `waitlist-app` | `edu.columbia.cvnstack` |
| `waitlist-domain` | `edu.columbia.cvnstack.domain` |
| `waitlist-api` | `edu.columbia.cvnstack.api` |
| `waitlist-persistence` | `edu.columbia.cvnstack.persistence` |
| `waitlist-cache` | `edu.columbia.cvnstack.cache` |

There is no production in-memory store.

## Agents

Shared agent instructions live in `.cursor/skills/`. AI attribution goes in the file header and in `docs/ai-usage.md`. Do not add an agent account as a collaborator.
