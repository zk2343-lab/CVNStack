# Iteration 1 Guidelines

Team: **CVNStack**  
Repository: [zk2343-lab/CVNStack](https://github.com/zk2343-lab/CVNStack)  
Service: Waitlist Management Service  
Captured: 2026-10-16, from the course first-iteration brief

This file is the working copy of the first-iteration requirements. The task split that satisfies them is in [mvp-task-breakdown.md](mvp-task-breakdown.md). Only the tagged commit on `main` is graded.

## What this iteration is

Ship a minimum viable version of the waitlist service. It does not need every behavior from the proposal, but as many of the proposed API entry points as possible must be operational and demoable.

The six proposed entry points are the MVP surface:

| Method | Path | Capability |
| --- | --- | --- |
| POST | `/waitlists` | Create a waitlist |
| POST | `/waitlists/{id}/entries` | Add an entry and return position and estimated wait |
| GET | `/waitlists/{id}/entries/{entryId}` | Read status, position, and estimated wait |
| POST | `/waitlists/{id}/serve-next` | Call the next waiting entry |
| PATCH | `/waitlists/{id}/entries/{entryId}` | Update status or priority |
| DELETE | `/waitlists/{id}/entries/{entryId}` | Remove an entry |

Deviations from the proposal are allowed when they make the service easier to demo. Record every deviation in the README.

## Repository hygiene (10 pts)

- All implementation and test code for this iteration is on `main`.
- Commit messages, after the initial repository setup, say what was added, removed, or changed.
- Work lands through pull requests. Another team member reviews each pull request.
- Turn on branch protection where the plan allows it. This repository is public, so branch protection is available and should be enabled before the tagged release.
- Tag (or GitHub-release) the final commit that counts as iteration 1. Suggested tag: `iteration-1`.

## Style checking and self-documenting code (10 pts)

- The whole iteration-1 codebase, including tests, passes a style checker.
- Commit at least one style-checker report. The latest report, run against the tagged release, must be clean.
- The README names the checker and the ruleset.
- Non-trivial code uses mnemonic names, a header comment, and inline comments where the logic is not obvious.

Planned tool: **Checkstyle**, Maven plugin, ruleset based on **Google Java Style** (`config/checkstyle/checkstyle.xml`).

## Testing (35 pts)

### Unit tests (15 pts)

- Unit-test every major unit. Skip constructors, trivial accessors, and one-line helpers.
- Use test doubles where a unit would otherwise touch storage, time, or another component.
- Group related tests so they share setup and teardown and, where order matters, run in a defined order.
- The full unit-test collection runs with one command.
- Fix most bugs the tests find.

Planned tools: **JUnit 5** and **Mockito**. Configuration lives in `pom.xml`.

### API tests (15 pts)

- Exercise the MVP endpoints with an API testing tool.
- Keep the tests in this repository (or a linked workspace, with the link in the README).
- Fix most bugs the tests find.

Planned tool: **REST Assured** with Spring Boot's test slice, so `mvn test` runs them. A Postman collection is optional and is not a substitute for the in-repo suite.

### Coverage (5 pts)

- Commit a **branch** coverage report.
- The tests together must reach at least **55%** branch coverage. Iteration 2 will require 80%.

Planned tool: **JaCoCo** via Maven. Copy the report into `reports/jacoco/` before tagging, because `target/` is not what reviewers will look for.

### Explicitly not required until iteration 2

Static bug finding (PMD, SpotBugs), continuous integration, integration tests beyond the API suite, and a sample client. Any of these may be added now, but they are not part of the iteration-1 definition of done.

## External documentation (20 pts)

The README, or a document it links to, must cover:

- Every operational entry point: inputs, outputs, side effects, and every status and error code.
- Call-order constraints: which calls must happen first, and which sequences are rejected.
- Step-by-step scenarios of correct use.
- Contrived incorrect uses, with why they fail.
- How to build, run, and test. Every config file required to do that is in the repository.
- Which test and mock frameworks are used, and where their configuration files are.
- Which style checker and ruleset are used, and where the clean report is.
- Which coverage tool is used, how it is configured, and where the report is.

Third-party code:

- Prefer Maven to fetch libraries. Do not vendor jars.
- If any third-party source is copied into the tree, name it, give its path, and give the URL it came from.
- If none is copied in, say so.

Project management:

- Track each team member's tasks in GitHub Projects: <https://github.com/users/zk2343-lab/projects/1>
- Link that board from the README.
- The mentor and instructor need access if the board ever moves off GitHub.

## AI documentation (5 pts)

Document every AI tool the team uses:

- what it is and where it came from
- what it was used for
- the prompts
- total tokens used, per tool

Mark every AI-generated or AI-debugged file, including tests, even if a person edited it afterward. A header comment is the mark. If a tool's token total is only visible in the product's usage screen, record the number there is, and say when it was read.

Token totals are required. The course is collecting them to request university-supplied tokens next year.

## Submission

One team member submits the GitHub repository URL. Grading uses the tagged `main` commit that was last updated before the deadline. Other branches do not count.

## How points are awarded

Graders start from 0 and add points for evidence on the tagged `main` commit. They do not start from full credit and subtract.

| Area | Points | What earns the top band |
| --- | ---: | --- |
| Prerequisite | gate | Repository is reachable, and `main` has a tag whose commit is before the deadline. If either fails, the rest is not graded. |
| MVP functionality | 20 | Several entry points work, are demoable, and match the proposal or an explained deviation. |
| Repository hygiene | 10 | Tag present, meaningful commits, reviewed pull requests, branch protection where allowed. |
| Style and comments | 10 | Clean style report on the tagged release, tool and ruleset named, mnemonic names and comments. |
| Unit testing | 15 | Major units tested, doubles where needed, grouped tests, one command, bugs fixed. |
| API testing | 15 | MVP endpoints exercised by a real suite in the repo or a linked workspace, bugs fixed. |
| Branch coverage | 5 | Report in the repo, at least 55% branch coverage. |
| README / API docs | 10 | Entry points, errors, ordering, good and bad scenarios, build/run/test. |
| Third-party disclosure | 5 | Copied-in code identified and sourced, or an explicit statement that there is none. |
| Project management | 5 | Tasks per person on the GitHub Project (or an equivalent the mentor can open), linked from the README. |
| AI documentation | 5 | Tools, purpose, prompts, token totals, and in-code marks. A credible "no AI" statement also scores 5. |

## Definition of done

- [ ] The six endpoints above run locally against PostgreSQL and Redis and can be demonstrated.
- [ ] Shared modules, the consistency rules, and the GCP stack are in the repository. See the task breakdown.
- [ ] `mvn test` runs the unit tests and the API tests.
- [ ] `reports/jacoco/` shows branch coverage of at least 55%.
- [ ] `reports/checkstyle/` is a clean Checkstyle run of the tagged tree.
- [ ] README covers the API, scenarios, build/run/test, tools, and third-party code.
- [ ] AI use is logged and AI-touched files are marked.
- [ ] `main` is protected, pull requests have a review from another member, and `iteration-1` is tagged.

---

*Drafted with Cursor (Grok 4.7) on 2026-10-16 from the course first-iteration brief. The team owns edits after this commit. Token use is logged in the AI usage document when that task is done.*
