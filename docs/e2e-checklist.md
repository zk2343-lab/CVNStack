<!-- AI-assisted. -->

# End-to-end checklist (issue 41)

Two copies of the restaurant client, one waitlist service. The service tells
the copies apart with the `X-Client-Instance` header. Each machine saves one
id and sends that same id on every call. The service stores it on the waitlist
or entry the machine created and returns it as `clientInstance` on reads.
Later calls still send the header and do not replace the stored creator.

The client in this repo always sends the header. A service build from before
that header support still accepts the restaurant calls and omits
`clientInstance`. The hosted URL below was in that state on 2026-11-25:
`GET /health` returned 200 `{"status":"UP"}`, and create/add/serve-next/patch
succeeded without echoing `clientInstance`.

Hosted origin:

```text
http://waitlist-management-service-5khm3u4xr0cvdjxqkvcf52uv.34-70-77-140.sslip.io
```

Health: `GET /health` → 200 `{"status":"UP"}`.

## Local port, one shell

Waitlist service listening on port 8080 (Compose or `mvn spring-boot:run`).

```bash
mocked_client/scripts/call-local-port.sh 8080
```

The script does not start the host stand. It talks to `http://127.0.0.1:8080`.

| Step | Call | Expect |
| --- | --- | --- |
| Health | `GET /health` | 200, `status` `UP` |
| Machine A creates the book | `POST /waitlists` name `Dining room <epoch>`, `maxLength` 10, `processingTimePerEntryMinutes` 5 | 200 and an `id` |
| Machine A checks in Walk-in | `POST /waitlists/{id}/entries` `{"name":"Walk-in","priority":1}` | 200, `status` `WAITING` |
| Machine A checks in Rush table | same path, `priority` 5 | 200, `position` 1, `estimatedWaitTimeMinutes` 0 |
| Machine B checks in Window | same path, header is machine B, `priority` 1 | 200 and a different `entryId` |
| Read the rush party | `GET /waitlists/{id}/entries/{rushId}` | 200, `position` 1 |
| Seat next | `POST /waitlists/{id}/serve-next` | 200, `entryId` is the rush party, `status` `CALLED` |
| Served | `PATCH` rush `{"status":"SERVED"}` | 200 |
| Cancelled | `PATCH` Walk-in and Window `{"status":"CANCELLED"}` | 200 |

Machine A reads `~/.cvnstack/mocked-client-instance`. Machine B in this script
reads `~/.cvnstack/mocked-client-instance-b`. The script prints both ids before
the first call. They must differ. When the service returns `clientInstance`,
both ids appear on the rows each machine created.

## Host stand against the local port

```bash
mocked_client/scripts/run-local.sh
```

Open <http://127.0.0.1:8090>. The page shows mode `local`, upstream
`http://localhost:8080`, and this machine's instance id.

1. Create waitlist. The name field is already unique for this page load.
2. Check in `Walk-in` with the rush box off. Note position and estimate.
3. Check in `Rush table` with the rush box on. Position is 1 and the estimate
   is 0 minutes.
4. Seat next. The rush party becomes `CALLED`.
5. Select that row and mark served.
6. Select the walk-in and cancel it.

## Two machines against the hosted service

On each laptop:

```bash
mocked_client/scripts/run-gcp.sh
```

1. Confirm the two terminal lines `Instance ...` differ, and that both pages
   show upstream the hosted origin above.
2. On machine A, create a waitlist. Copy the waitlist id.
3. On machine A, check in `Walk-in` (priority 1) and `Rush table` (priority 5).
   Rush table is position 1, estimate 0.
4. On machine B, paste that waitlist id, check in `Window` (priority 1), and
   refresh. The board lists three parties.
5. On machine A, seat next. The called party is Rush table. Mark it served.
6. Cancel the parties that are still waiting.
7. When `clientInstance` is present, Rush table and Walk-in show machine A's
   id and Window shows machine B's id. A missing column value means this
   deployment is not storing the header yet. The pages still show two
   different instance ids, and every mutating call from each page sent that
   page's id.

Direct hosted run of the same HTTP script, without the host stand:

```bash
BASE_URL=http://waitlist-management-service-5khm3u4xr0cvdjxqkvcf52uv.34-70-77-140.sslip.io \
  mocked_client/scripts/call-local-port.sh
```

A second run with the same waitlist name returns 409 `CONFLICT`. The script
puts the epoch in the name so a repeat still creates a list.
