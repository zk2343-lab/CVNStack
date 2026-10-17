<!-- AI-assisted. -->

# CVNStack

Waitlist Management Service for COMS W4156. Client applications create a waitlist, add prioritized entries, and move those entries from waiting to called, served, or cancelled.

The process boots and answers `GET /health`. Waitlist routes come in later issues.

Team: Zengxiaoran Kang (zk2343-lab), Ding Wang (wdingding0924-ux), Yoongu Kang (ygkang-cu).

Tasks live on the project board: <https://github.com/users/zk2343-lab/projects/1/views/1>

## Prerequisites

Install JDK 21 and Maven 3.9 or newer. `java -version` must report 21. If `mvn -version` shows a different Java, set `JAVA_HOME` to the JDK 21 home and run Maven again.

```bash
git clone https://github.com/zk2343-lab/CVNStack.git
cd CVNStack
```

The first Maven command downloads the libraries from Maven Central. No third-party source is copied into this repository. PostgreSQL and Redis are not required to build or start this scaffold; they arrive with a later issue.

## Build

From the repository root:

```bash
mvn -DskipTests package
```

That compiles every module and writes the runnable jar to `modules/waitlist-app/target/waitlist-app-0.1.0-SNAPSHOT.jar`.

## Start

With JDK 21 on the path:

```bash
mvn -pl modules/waitlist-app spring-boot:run
```

The process listens on port 8080. Open `http://localhost:8080/health`. A healthy process returns 200 and `{"status":"UP"}`.

After a build, the same jar starts without Maven:

```bash
java -jar modules/waitlist-app/target/waitlist-app-0.1.0-SNAPSHOT.jar
```

Stop the process with Ctrl-C.

## Test

```bash
mvn test
mvn checkstyle:check
mvn verify
```

`mvn test` runs JUnit 5, Mockito, and REST Assured. `mvn verify` also runs Checkstyle and writes the JaCoCo report under `modules/waitlist-app/target/site/jacoco/`. Those plugins are declared in the parent `pom.xml`. Checkstyle uses `config/checkstyle/checkstyle.xml`, a project ruleset based on the Google Java Style Guide. Committing the coverage report, and a clean report under `reports/checkstyle/`, are later issues.

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

Coding agents follow [AGENTS.md](AGENTS.md) and the skills in `.agents/skills/`. AI usage is recorded in [docs/ai-usage.md](docs/ai-usage.md). A file a model wrote or debugged is marked `AI-assisted` in its header.
