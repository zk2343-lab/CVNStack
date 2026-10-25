/*
  AI-assisted.
  Independent REST actions and live PostgreSQL/Redis store tables.
*/

    const API = window.location.origin.includes("8765")
        ? "http://localhost:8080" : window.location.origin;
    const toolbox = [
      {
        id: "createWaitlist",
        title: "Create waitlist",
        method: "POST",
        path: "/waitlists",
        body: {
          name: "Support desk",
          maxLength: 10,
          processingTimePerEntryMinutes: 5
        },
        save: { waitlistId: "id" }
      },
      {
        id: "addEntry",
        title: "Add entry",
        method: "POST",
        path: "/waitlists/{waitlistId}/entries",
        body: { name: "Alice", priority: 1 },
        save: { entryId: "entryId" }
      },
      {
        id: "getWaitlist",
        title: "Get waitlist",
        method: "GET",
        path: "/waitlists/{waitlistId}"
      },
      {
        id: "getEntry",
        title: "Get entry",
        method: "GET",
        path: "/waitlists/{waitlistId}/entries/{entryId}"
      },
      {
        id: "serveNext",
        title: "Serve next",
        method: "POST",
        path: "/waitlists/{waitlistId}/serve-next",
        save: { entryId: "entryId" }
      },
      {
        id: "patchEntry",
        title: "Patch entry",
        method: "PATCH",
        path: "/waitlists/{waitlistId}/entries/{entryId}",
        body: { status: "SERVED" }
      },
      {
        id: "deleteEntry",
        title: "Delete entry",
        method: "DELETE",
        path: "/waitlists/{waitlistId}/entries/{entryId}"
      },
      {
        id: "flushRedis",
        title: "Flush Redis cache",
        method: "POST",
        path: "/debug/flush-redis"
      }
    ];

    const presets = {
      all: {
        title: "All actions",
        note: "Every independent HTTP action. Click any one.",
        scenario: "CVNStack is a waitlist management service. Client apps "
            + "create a waitlist, add people with priority 1–5 (5 is most "
            + "urgent), and move them WAITING → CALLED → SERVED, or cancel "
            + "them. PostgreSQL is the source of truth; Redis caches waiting "
            + "order and entry payloads. Click one HTTP action. A preset only "
            + "loads sample requests — it does not run them. Hover a "
            + "PostgreSQL waitlist, entry, or Redis key to highlight the "
            + "matching rows across both stores. A waitlist also lights its "
            + "entries. Click a row to keep that highlight; click elsewhere "
            + "to release it and hover again.",
        actions: toolbox
      },
      happyPath: {
        title: "Happy path set",
        note: "Create a list, add two people, look up Alice, serve next, then mark served.",
        scenario: "Happy path: create a waitlist, add Alice (priority 1) and "
            + "Bob (priority 5), look up the list and Alice, serve the next "
            + "person, then mark that entry SERVED. Bob should be called "
            + "first because 5 outranks 1. Watch PostgreSQL gain rows, then "
            + "Redis keep the waiting zset, entry hashes, and key set in sync.",
        actions: [
          {
            ...toolbox[0],
            body: {
              name: "Happy path",
              maxLength: 10,
              processingTimePerEntryMinutes: 5
            }
          },
          {
            ...toolbox[1],
            title: "Add Alice p1",
            body: { name: "Alice", priority: 1 }
          },
          {
            ...toolbox[1],
            id: "addBob",
            title: "Add Bob p5",
            body: { name: "Bob", priority: 5 }
          },
          { ...toolbox[2], title: "Get waitlist in order" },
          { ...toolbox[3], title: "Get Alice / current entry" },
          toolbox[4],
          {
            ...toolbox[5],
            title: "Patch current entry SERVED",
            body: { status: "SERVED" }
          }
        ]
      },
      cacheRebuild: {
        title: "Cache rebuild set",
        note: "Write rows, empty Redis, then GET so the cache rebuilds from PostgreSQL.",
        scenario: "Cache rebuild: write a waitlist and two entries so both "
            + "stores are full, flush Redis, then GET an entry. The read "
            + "should miss the empty cache, rebuild the waiting zset and "
            + "entry hash from PostgreSQL, and return the same person.",
        actions: [
          toolbox[0],
          {
            ...toolbox[1],
            title: "Add Alice",
            body: { name: "Alice", priority: 1 }
          },
          {
            ...toolbox[1],
            id: "addBob",
            title: "Add Bob",
            body: { name: "Bob", priority: 5 }
          },
          toolbox[7],
          { ...toolbox[3], title: "GET after flush (rebuild)" }
        ]
      },
      overlappingServe: {
        title: "Serve-next set",
        note: "Add two waiting entries, then click Serve next once or twice yourself.",
        scenario: "Serve-next set: add two waiting people (One at priority 5, "
            + "Two at priority 1), then click Serve next yourself. The first "
            + "click should call One and drop them from the Redis zset. A "
            + "second click calls Two. An empty queue returns 204.",
        actions: [
          toolbox[0],
          {
            ...toolbox[1],
            title: "Add One p5",
            body: { name: "One", priority: 5 }
          },
          {
            ...toolbox[1],
            id: "addTwo",
            title: "Add Two p1",
            body: { name: "Two", priority: 1 }
          },
          toolbox[4]
        ]
      },
      deleteShiftsRank: {
        title: "Delete set",
        note: "Add two entries, delete one, then GET the remaining row.",
        scenario: "Delete set: add two waiting people, delete the first saved "
            + "entry (Doomed), then GET the remaining person. Kept should "
            + "stay WAITING at position 1. The Redis zset and entry-key set "
            + "should drop Doomed.",
        actions: [
          toolbox[0],
          {
            ...toolbox[1],
            title: "Add Doomed",
            body: { name: "Doomed", priority: 1 }
          },
          {
            ...toolbox[1],
            id: "addKept",
            title: "Add Kept",
            body: { name: "Kept", priority: 1 }
          },
          toolbox[6],
          toolbox[3]
        ]
      },
      capacityExceeded: {
        title: "Capacity set",
        note: "Fill a one-seat waitlist, then try a second add.",
        scenario: "Capacity set: create a waitlist with maxLength 1, add "
            + "Alice, then try Bob. The second add should return 422 "
            + "CAPACITY_EXCEEDED. PostgreSQL and Redis should still show "
            + "only Alice — the rejected entry is not written to either store.",
        actions: [
          {
            ...toolbox[0],
            body: {
              name: "One seat",
              maxLength: 1,
              processingTimePerEntryMinutes: 5
            }
          },
          {
            ...toolbox[1],
            title: "Add Alice (fills the list)",
            body: { name: "Alice", priority: 1 }
          },
          {
            ...toolbox[1],
            id: "addBob",
            title: "Add Bob (should 422)",
            body: { name: "Bob", priority: 1 }
          },
          { ...toolbox[2], title: "Get waitlist after reject" }
        ]
      },
      priorityBump: {
        title: "Priority bump set",
        note: "Add two equal-priority people, bump the later one to 5, then GET the new order.",
        scenario: "Priority bump: add Casey and Drew at priority 1, bump the "
            + "current entry (Drew) to 5, then GET the waitlist. Drew should "
            + "jump ahead of Casey in the returned order and in the Redis "
            + "waiting zset. Priority 5 is the most urgent.",
        actions: [
          {
            ...toolbox[0],
            body: {
              name: "Priority desk",
              maxLength: 10,
              processingTimePerEntryMinutes: 5
            }
          },
          {
            ...toolbox[1],
            title: "Add Casey p1",
            body: { name: "Casey", priority: 1 }
          },
          {
            ...toolbox[1],
            id: "addDrew",
            title: "Add Drew p1",
            body: { name: "Drew", priority: 1 }
          },
          {
            ...toolbox[5],
            title: "Bump current entry to p5",
            body: { priority: 5 }
          },
          { ...toolbox[2], title: "Get waitlist after bump" }
        ]
      }
    };

    let current = "all";
    const ids = { waitlistId: "", entryId: "" };
    let pinnedPin = null;

    function interpolate(value, values) {
      return value.replace(/\{(\w+)\}/g, (_, name) => values[name] || "{" + name + "}");
    }

    function formatJson(value) {
      return value == null || value === "" ? "(no body)" : JSON.stringify(value, null, 2);
    }

    function escapeHtml(value) {
      return String(value).replace(/[&<>]/g, (ch) => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;"
      }[ch]));
    }

    function showCall(method, url, body, responseText) {
      const lines = [
        method + " " + url + " HTTP/1.1",
        "Host: " + new URL(API).host,
        "Accept: application/json"
      ];
      if (body !== undefined) {
        lines.push("Content-Type: application/json");
        lines.push("");
        lines.push(JSON.stringify(body, null, 2));
      }
      document.getElementById("requestView").textContent = lines.join("\n");
      let curl = "curl -sS -X " + method + " '" + url + "'";
      if (body !== undefined) {
        curl += " \\\n  -H 'Content-Type: application/json' \\\n  -d '"
            + JSON.stringify(body) + "'";
      }
      document.getElementById("curlView").textContent = curl;
      document.getElementById("responseView").textContent = responseText;
    }

    function decodeId(encoded) {
      try {
        const pad = encoded.length % 4 === 0 ? "" : "=".repeat(4 - (encoded.length % 4));
        return atob(encoded.replace(/-/g, "+").replace(/_/g, "/") + pad);
      } catch (ignored) {
        return "";
      }
    }

    function parseRedisKey(key) {
      const match = /^cvnstack:waitlist:([^:]+):(.*)$/.exec(key || "");
      if (!match) {
        return { waitlistId: "", entryId: "" };
      }
      const waitlistId = decodeId(match[1]);
      const entryMatch = /^entry:(.+)$/.exec(match[2]);
      return {
        waitlistId: waitlistId,
        entryId: entryMatch ? decodeId(entryMatch[1]) : ""
      };
    }

    function extractIds(text) {
      const ids = new Set();
      const raw = String(text || "");
      for (const match of raw.matchAll(
          /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/gi)) {
        ids.add(match[0]);
      }
      for (const match of raw.matchAll(
          /cvnstack:waitlist:([^:\s,\]]+)(?::entry:([^:\s,\]]+))?/g)) {
        const waitlistId = decodeId(match[1]);
        if (waitlistId) {
          ids.add(waitlistId);
        }
        if (match[2]) {
          const entryId = decodeId(match[2]);
          if (entryId) {
            ids.add(entryId);
          }
        }
      }
      return [...ids];
    }

    function table(headers, rows) {
      if (!rows.length) {
        return "<p class=\"empty\">No rows.</p>";
      }
      return "<div class=\"table-wrap\"><table><thead><tr>"
          + headers.map((h) => "<th>" + h + "</th>").join("")
          + "</tr></thead><tbody>" + rows.map((row) => {
            const attrs = Object.entries(row.attrs || {}).map(([name, value]) =>
                " data-" + name + "=\"" + escapeHtml(value == null ? "" : value) + "\"")
                .join("");
            return "<tr class=\"store-row\"" + attrs + ">" + row.cells.map((cell) =>
                "<td>" + escapeHtml(cell == null ? "" : cell) + "</td>").join("")
                + "</tr>";
          }).join("") + "</tbody></table></div>";
    }

    function idsOf(row, name) {
      return (row.dataset[name] || "").split(",").filter(Boolean);
    }

    function redisMentionsEntry(row, entryId) {
      return Boolean(entryId) && (row.dataset.entryId === entryId
          || idsOf(row, "valueIds").includes(entryId));
    }

    function highlightRelated(source) {
      const stores = document.querySelector(".stores");
      stores.classList.add("linking");
      const kind = source.dataset.kind;
      const focusEntry = kind === "entry" ? source.dataset.id : source.dataset.entryId;
      const focusWaitlist = kind === "waitlist" ? source.dataset.id : source.dataset.waitlistId;
      const mentioned = new Set(idsOf(source, "valueIds"));
      stores.querySelectorAll("tr.store-row").forEach((row) => {
        row.classList.remove("hl-source", "hl-entry", "hl-related");
        if (row === source) {
          row.classList.add("hl-source");
          return;
        }
        if (focusEntry) {
          if (row.dataset.kind === "waitlist" && row.dataset.id === focusWaitlist) {
            row.classList.add("hl-related");
          } else if (row.dataset.kind === "entry" && row.dataset.id === focusEntry) {
            row.classList.add("hl-entry");
          } else if (row.dataset.kind === "redis" && redisMentionsEntry(row, focusEntry)) {
            row.classList.add("hl-entry");
          }
          return;
        }
        if (kind === "waitlist") {
          if (row.dataset.waitlistId !== focusWaitlist) {
            return;
          }
          if (row.dataset.kind === "entry") {
            row.classList.add("hl-entry");
          } else if (row.dataset.kind === "redis") {
            row.classList.add("hl-related");
          }
          return;
        }
        if (row.dataset.kind === "waitlist" && row.dataset.id === focusWaitlist) {
          row.classList.add("hl-related");
        } else if (row.dataset.kind === "entry" && mentioned.has(row.dataset.id)) {
          row.classList.add("hl-entry");
        } else if (row.dataset.kind === "redis" && row.dataset.entryId
            && mentioned.has(row.dataset.entryId)) {
          row.classList.add("hl-entry");
        }
      });
    }

    function clearHighlight() {
      if (pinnedPin) {
        return;
      }
      const stores = document.querySelector(".stores");
      stores.classList.remove("linking");
      stores.querySelectorAll("tr.store-row").forEach((row) => {
        row.classList.remove("hl-source", "hl-entry", "hl-related");
      });
    }

    function rowPin(row) {
      if (!row || !row.dataset) {
        return "";
      }
      if (row.dataset.kind === "waitlist") {
        return "waitlist:" + row.dataset.id;
      }
      if (row.dataset.kind === "entry") {
        return "entry:" + row.dataset.id;
      }
      if (row.dataset.kind === "redis") {
        return "redis:" + (row.dataset.key || "");
      }
      return "";
    }

    function findRowByPin(pin) {
      if (!pin) {
        return null;
      }
      return [...document.querySelectorAll("tr.store-row")]
          .find((row) => rowPin(row) === pin) || null;
    }

    function releasePin() {
      pinnedPin = null;
      const stores = document.querySelector(".stores");
      stores.classList.remove("linking");
      stores.querySelectorAll("tr.store-row").forEach((row) => {
        row.classList.remove("hl-source", "hl-entry", "hl-related");
      });
    }

    function restorePinnedHighlight() {
      if (!pinnedPin) {
        return;
      }
      const row = findRowByPin(pinnedPin);
      if (!row) {
        releasePin();
        return;
      }
      highlightRelated(row);
    }

    function pinRow(row) {
      pinnedPin = rowPin(row);
      highlightRelated(row);
    }

    function bindStoreHover() {
      document.querySelectorAll("tr.store-row").forEach((row) => {
        row.addEventListener("mouseenter", () => {
          if (pinnedPin) {
            return;
          }
          highlightRelated(row);
        });
        row.addEventListener("mouseleave", () => {
          if (pinnedPin) {
            return;
          }
          clearHighlight();
        });
        row.addEventListener("click", (event) => {
          event.stopPropagation();
          pinRow(row);
        });
      });
    }

    async function refreshStores() {
      const response = await fetch(API + "/debug/stores");
      if (!response.ok) {
        document.getElementById("waitlistTable").textContent =
            "Could not read stores (" + response.status + ").";
        return;
      }
      const data = await response.json();
      document.getElementById("waitlistTable").innerHTML = table(
          ["id", "name", "maxLength", "minutes"],
          data.waitlists.map((row) => ({
            attrs: { kind: "waitlist", id: row.id },
            cells: [row.id, row.name, row.maxLength, row.processingTimePerEntryMinutes]
          })));
      document.getElementById("entryTable").innerHTML = table(
          ["id", "waitlistId", "name", "priority", "status", "updatedAt"],
          data.entries.map((row) => ({
            attrs: { kind: "entry", id: row.id, "waitlist-id": row.waitlistId },
            cells: [row.id, row.waitlistId, row.name, row.priority, row.status, row.updatedAt]
          })));
      document.getElementById("redisTable").innerHTML = table(
          ["key", "type", "value"],
          data.redis.map((row) => {
            const parsed = parseRedisKey(row.key);
            return {
              attrs: {
                kind: "redis",
                key: row.key,
                "waitlist-id": parsed.waitlistId,
                "entry-id": parsed.entryId,
                "value-ids": extractIds(row.value).join(",")
              },
              cells: [row.key, row.type, row.value]
            };
          }));
      document.getElementById("pgCount").textContent =
          data.waitlists.length + " lists · " + data.entries.length + " entries";
      document.getElementById("redisCount").textContent = data.redis.length + " keys";
      bindStoreHover();
      restorePinnedHighlight();
    }

    function readBody(textarea) {
      if (!textarea) {
        return undefined;
      }
      const text = textarea.value.trim();
      if (!text) {
        return undefined;
      }
      return JSON.parse(text);
    }

    async function sendAction(action, card) {
      const pathInput = card.querySelector(".path");
      const path = interpolate(pathInput.value.trim(), ids);
      const url = API + path;
      let body;
      try {
        body = readBody(card.querySelector("textarea"));
      } catch (error) {
        document.getElementById("responseView").textContent = "Invalid JSON: " + error.message;
        return;
      }
      card.classList.add("firing");
      showCall(action.method, url, body, "Waiting…");
      const options = { method: action.method, headers: { Accept: "application/json" } };
      if (body !== undefined) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
      }
      try {
        const response = await fetch(url, options);
        const text = await response.text();
        let parsed = text;
        try {
          parsed = text ? JSON.parse(text) : "";
        } catch (ignored) {
          parsed = text;
        }
        showCall(action.method, url, body, "HTTP/1.1 " + response.status + " "
            + response.statusText + "\n\n"
            + formatJson(parsed === "" ? null : parsed));
        if (action.save && parsed && typeof parsed === "object") {
          Object.entries(action.save).forEach(([name, field]) => {
            if (parsed[field]) {
              ids[name] = parsed[field];
            }
          });
        }
        await refreshStores();
      } catch (error) {
        document.getElementById("responseView").textContent = error.message;
      } finally {
        card.classList.remove("firing");
      }
    }

    async function wipeStores() {
      const button = document.getElementById("wipeStores");
      const url = API + "/debug/wipe-stores";
      button.classList.add("busy");
      ids.waitlistId = "";
      ids.entryId = "";
      releasePin();
      showCall("POST", url, undefined, "Waiting…");
      try {
        const response = await fetch(url, {
          method: "POST",
          headers: { Accept: "application/json" }
        });
        showCall("POST", url, undefined, "HTTP/1.1 " + response.status + " "
            + response.statusText + "\n\n(no body)");
        await refreshStores();
      } catch (error) {
        document.getElementById("responseView").textContent = error.message;
      } finally {
        button.classList.remove("busy");
      }
    }

    function renderActions() {
      const preset = presets[current];
      document.getElementById("actionTitle").textContent = preset.title;
      document.getElementById("presetScenario").textContent = preset.scenario;
      document.getElementById("actions").innerHTML = "";
      const note = document.createElement("p");
      note.className = "note";
      note.textContent = preset.note;
      document.getElementById("actions").appendChild(note);
      preset.actions.forEach((action, index) => {
        const card = document.createElement("article");
        card.className = "card";
        const hasBody = action.body !== undefined;
        card.innerHTML = "<h3><span class=\"method " + action.method + "\">"
            + action.method + "</span>" + escapeHtml(action.title) + "</h3>"
            + "<input class=\"path\" value=\"" + escapeHtml(action.path) + "\">"
            + (hasBody ? "<textarea>" + escapeHtml(JSON.stringify(action.body, null, 2))
                + "</textarea>" : "")
            + "<button class=\"send\" type=\"button\">Send this request</button>";
        card.querySelector(".send").addEventListener("click", () =>
            sendAction(action, card));
        card.dataset.index = String(index);
        document.getElementById("actions").appendChild(card);
      });
    }

    function selectPreset(id) {
      current = id;
      [...document.getElementById("presets").children].forEach((button) => {
        button.classList.toggle("active", button.dataset.id === id);
      });
      renderActions();
    }

    Object.entries(presets).forEach(([id, preset]) => {
      const button = document.createElement("button");
      button.type = "button";
      button.dataset.id = id;
      button.textContent = preset.title;
      button.addEventListener("click", () => selectPreset(id));
      document.getElementById("presets").appendChild(button);
    });

    document.getElementById("wipeStores").addEventListener("click", wipeStores);
    document.addEventListener("click", (event) => {
      if (!pinnedPin || event.target.closest("tr.store-row")) {
        return;
      }
      releasePin();
    });

    function panelTitle(panel) {
      return panel.querySelector("h2").childNodes[0].textContent.trim();
    }

    document.querySelectorAll(".panel-min").forEach((button) => {
      button.addEventListener("click", () => {
        const panel = button.closest(".panel");
        if (panel.classList.contains("maximized")) {
          document.querySelector("main").classList.remove("solo");
          panel.classList.remove("maximized");
          panel.querySelector(".panel-max").setAttribute("aria-pressed", "false");
          panel.querySelector(".panel-max").setAttribute("aria-label",
              "Expand " + panelTitle(panel));
        }
        const collapsed = panel.classList.toggle("collapsed");
        button.setAttribute("aria-expanded", collapsed ? "false" : "true");
        button.setAttribute("aria-label",
            (collapsed ? "Expand " : "Minimize ") + panelTitle(panel));
      });
    });

    document.querySelectorAll(".panel-max").forEach((button) => {
      button.addEventListener("click", () => {
        const panel = button.closest(".panel");
        const main = document.querySelector("main");
        const maximizing = !panel.classList.contains("maximized");
        document.querySelectorAll(".panel.maximized").forEach((open) => {
          open.classList.remove("maximized");
          const restore = open.querySelector(".panel-max");
          restore.setAttribute("aria-pressed", "false");
          restore.setAttribute("aria-label", "Expand " + panelTitle(open));
        });
        main.classList.toggle("solo", maximizing);
        if (maximizing) {
          panel.classList.remove("collapsed");
          panel.classList.add("maximized");
          panel.querySelector(".panel-min").setAttribute("aria-expanded", "true");
          panel.querySelector(".panel-min").setAttribute("aria-label",
              "Minimize " + panelTitle(panel));
        }
        button.setAttribute("aria-pressed", maximizing ? "true" : "false");
        button.setAttribute("aria-label",
            (maximizing ? "Restore " : "Expand ") + panelTitle(panel));
      });
    });

    selectPreset("all");
    refreshStores();
    setInterval(refreshStores, 5000);
