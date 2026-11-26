<!-- AI-assisted. -->

# Restaurant mocked client

This folder is a separate restaurant application for issue 41. It is not a page
served by the waitlist service. The host stand calls the waitlist HTTP API with
`java.net.http` and does not put the server sources on its classpath.

The same actions as the service visualizer are here, in restaurant terms:
create a waitlist, check a party in, read position and estimate, seat the next
party (`serve-next`), then mark that party served or cancelled. Priority **5**
is the party that needs the next table.

Each machine keeps one instance id in `~/.cvnstack/mocked-client-instance` and
sends it as `X-Client-Instance` on every call. A second id used by the local
port script is stored in `~/.cvnstack/mocked-client-instance-b`.

The waitlist API stores entries but not guest names, party sizes, notes, or a
list of rooms. The host stand saves those in
`~/.cvnstack/mocked-client-book-local.json` and
`~/.cvnstack/mocked-client-book-gcp.json`, so they survive a restart and show
up in any browser that opens the stand. Delete a file to start that service's
stand from an empty book.

## Build

Use JDK 21.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn -f mocked_client/pom.xml test
mvn -f mocked_client/pom.xml checkstyle:check
mvn -f mocked_client/pom.xml -DskipTests package
```

The runnable jar is `mocked_client/target/mocked-client.jar`.

## Local waitlist port

Start the waitlist service on port 8080, then either talk to that port directly:

```bash
mocked_client/scripts/call-local-port.sh 8080
```

or boot the host stand on this machine and let it call that port:

```bash
mocked_client/scripts/run-local.sh
```

Open <http://127.0.0.1:8090>. The stand listens only on loopback. Its upstream
starts at `http://localhost:8080`. The Local / Cloud control in the header
switches the upstream and reloads the book saved for that service.

`call-local-port.sh` does not start the host stand. `BASE_URL` replaces the
port when you want the same script to call another origin.

## Hosted service

Boot the host stand locally and point it at the deployed service:

```bash
mocked_client/scripts/run-gcp.sh
```

That uses:

```text
http://waitlist-management-service-5khm3u4xr0cvdjxqkvcf52uv.34-70-77-140.sslip.io
```

The same origin is the `--mode gcp` default for the jar. `--base-url` overrides
either mode.

```bash
java -jar mocked_client/target/mocked-client.jar health --mode gcp
java -jar mocked_client/target/mocked-client.jar create-waitlist \
  --mode gcp --name "Dining room 1" --max-length 10 --minutes 5
java -jar mocked_client/target/mocked-client.jar check-in \
  --mode gcp --waitlist WAITLIST_ID --name "Rush table" --priority 5
java -jar mocked_client/target/mocked-client.jar position \
  --mode gcp --waitlist WAITLIST_ID --entry ENTRY_ID
java -jar mocked_client/target/mocked-client.jar serve-next \
  --mode gcp --waitlist WAITLIST_ID
java -jar mocked_client/target/mocked-client.jar served \
  --mode gcp --waitlist WAITLIST_ID --entry ENTRY_ID
```

## Two machines

Run `run-gcp.sh` (or `run-local.sh`) on each machine. The instance line printed
at startup must differ. Paste the waitlist id from the first machine into the
second machine's waitlist field, check in another party, and refresh. When the
service stores the header, each row's Instance column is the machine that
checked that party in.

The end-to-end inputs and expected results are in
[docs/e2e-checklist.md](../docs/e2e-checklist.md).
