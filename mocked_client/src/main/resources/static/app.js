/*
  AI-assisted.
  Host stand for The Marlowe: open a book, quote parties, call the next table.
*/

const state = {
  mode: "",
  waitlistId: "",
  entryId: "",
  profiles: {},
  entries: [],
  rooms: [],
  bookName: "",
  partySize: 2
};

function roomsKey(mode) {
  return "hostRooms:" + mode;
}

function bookKey(mode) {
  return "hostBook:" + mode;
}

function readLegacyProfiles() {
  try {
    const profiles = JSON.parse(localStorage.getItem("hostProfiles") || "{}");
    const older = JSON.parse(localStorage.getItem("hostNames") || "{}");
    Object.keys(older).forEach((id) => {
      if (!profiles[id]) {
        profiles[id] = { name: older[id], size: null, note: "" };
      }
    });
    return profiles;
  } catch (error) {
    return {};
  }
}

function emptyRooms() {
  return { activeId: "", rooms: [], profiles: {}, entryId: "" };
}

function roomsFromBook(book) {
  const rooms = [];
  if (book && book.waitlistId) {
    rooms.push({ id: book.waitlistId, name: book.bookName || "Dining room" });
  }
  return {
    activeId: book && book.waitlistId ? book.waitlistId : "",
    rooms,
    profiles: (book && book.profiles) || {},
    entryId: (book && book.entryId) || ""
  };
}

function normalizeRooms(parsed) {
  return {
    activeId: parsed.activeId || "",
    rooms: Array.isArray(parsed.rooms) ? parsed.rooms : [],
    profiles: parsed.profiles || {},
    entryId: parsed.entryId || ""
  };
}

function loadBrowserRooms(mode) {
  const saved = localStorage.getItem(roomsKey(mode));
  if (saved) {
    try {
      return normalizeRooms(JSON.parse(saved));
    } catch (error) {
      return emptyRooms();
    }
  }
  const previous = localStorage.getItem(bookKey(mode));
  if (!previous) {
    return emptyRooms();
  }
  try {
    return roomsFromBook(JSON.parse(previous));
  } catch (error) {
    return emptyRooms();
  }
}

function migrateLegacy(mode) {
  if (localStorage.getItem(roomsKey(mode)) || localStorage.getItem(bookKey(mode))) {
    return;
  }
  if (!localStorage.getItem("hostWaitlistId")) {
    return;
  }
  localStorage.setItem(roomsKey(mode), JSON.stringify(roomsFromBook({
    waitlistId: localStorage.getItem("hostWaitlistId") || "",
    bookName: localStorage.getItem("hostBookName") || "",
    entryId: localStorage.getItem("hostEntryId") || "",
    profiles: readLegacyProfiles()
  })));
  localStorage.removeItem("hostWaitlistId");
  localStorage.removeItem("hostBookName");
  localStorage.removeItem("hostEntryId");
}

function forgetBrowserRooms(mode) {
  localStorage.removeItem(roomsKey(mode));
  localStorage.removeItem(bookKey(mode));
}

async function putBook(mode, book) {
  const response = await fetch("/api/book/" + mode, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(book)
  });
  if (!response.ok) {
    throw new Error("The stand could not save its book (" + response.status + ").");
  }
}

async function loadRooms(mode) {
  const response = await fetch("/api/book/" + mode, { headers: { Accept: "application/json" } });
  const saved = response.ok ? await response.json() : {};
  if (Array.isArray(saved.rooms)) {
    return normalizeRooms(saved);
  }
  const older = loadBrowserRooms(mode);
  if (older.rooms.length) {
    await putBook(mode, older);
    forgetBrowserRooms(mode);
  }
  return older;
}

let saving = Promise.resolve();

function saveRooms() {
  if (!state.mode) {
    return saving;
  }
  const mode = state.mode;
  const book = {
    activeId: state.waitlistId,
    rooms: state.rooms,
    profiles: state.profiles,
    entryId: state.entryId
  };
  saving = saving.then(() => putBook(mode, book)).catch(showError);
  return saving;
}

async function applyRooms(mode) {
  const stored = await loadRooms(mode);
  state.rooms = stored.rooms;
  state.profiles = stored.profiles || {};
  state.entryId = stored.entryId || "";
  const active = state.rooms.find((room) => room.id === stored.activeId) || state.rooms[0];
  state.waitlistId = active ? active.id : "";
  state.bookName = active ? active.name : "";
  state.entries = [];
  $("waitlistId").value = state.waitlistId;
  $("bookName").textContent = state.bookName || "Tonight";
  drawRooms();
}

const $ = (id) => document.getElementById(id);

function tick() {
  const now = new Date();
  $("clock").textContent = now.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
  $("today").textContent = now.toLocaleDateString([], {
    weekday: "long",
    month: "long",
    day: "numeric"
  });
}

async function boot() {
  tick();
  setInterval(tick, 1000);
  const stamp = new Date().toISOString().slice(11, 19).replaceAll(":", "");
  $("waitlistName").value = "Dining room " + stamp;
  const session = await (await fetch("/api/session")).json();
  paintSession(session);
  migrateLegacy(session.mode);
  await applyRooms(session.mode);
  await refreshHealth();
  if (state.waitlistId) {
    showFloor(true);
    await refresh();
  }
}

function paintSession(session) {
  state.mode = session.mode;
  $("mode").textContent = serviceLabel(session.mode);
  $("instance").textContent = session.instanceId;
  $("upstream").textContent = session.baseUrl;
  const cloud = session.mode === "gcp";
  $("useLocal").classList.toggle("on", !cloud);
  $("useCloud").classList.toggle("on", cloud);
  $("useLocal").setAttribute("aria-pressed", String(!cloud));
  $("useCloud").setAttribute("aria-pressed", String(cloud));
}

function serviceLabel(mode) {
  return mode === "gcp" ? "Cloud" : "Local";
}

async function refreshHealth() {
  const health = await call("GET", "/health");
  const up = health.status === 200 && health.json && health.json.status === "UP";
  $("health").textContent = up ? "Kitchen link up" : "Kitchen link down";
  $("health").classList.remove("ok", "bad");
  $("health").classList.add(up ? "ok" : "bad");
  return up;
}

async function switchTarget(mode) {
  if (mode === state.mode) {
    return;
  }
  await saveRooms();
  const response = await fetch("/api/upstream", {
    method: "POST",
    headers: { "Content-Type": "application/json", "Accept": "application/json" },
    body: JSON.stringify({ mode })
  });
  const session = await response.json();
  if (!response.ok) {
    say(session.message || "Could not switch the service.");
    return;
  }
  paintSession(session);
  await applyRooms(session.mode);
  const up = await refreshHealth();
  if (!up) {
    showFloor(false);
    clearBoard();
    say(serviceLabel(session.mode) + " is not answering. Start it, then switch again.");
    return;
  }
  if (state.waitlistId) {
    showFloor(true);
    await refresh();
    say("Showing the " + serviceLabel(session.mode).toLowerCase() + " book.");
    return;
  }
  showFloor(false);
  clearBoard();
  say(serviceLabel(session.mode) + " service. Open a book on this stand.");
}

function clearBoard() {
  $("waitingCount").textContent = "—";
  $("coverCount").textContent = "—";
  $("nextQuote").textContent = "—";
  $("rows").replaceChildren();
  $("readyBanner").hidden = true;
}

function showFloor(open) {
  $("welcome").hidden = open;
  $("floor").hidden = !open;
  if (state.bookName) {
    $("bookName").textContent = state.bookName;
  }
}

async function call(method, path, body) {
  const options = { method, headers: { Accept: "application/json" } };
  if (body !== undefined) {
    options.headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(body);
  }
  const response = await fetch("/api/proxy" + path, options);
  const text = await response.text();
  let json = null;
  if (text) {
    try {
      json = JSON.parse(text);
    } catch (error) {
      json = null;
    }
  }
  $("trace").textContent = method + " " + path + "\n" + response.status + "\n"
      + (text || "(empty)");
  return { status: response.status, json, text };
}

let toastTimer = 0;

function say(text) {
  const toast = $("message");
  toast.textContent = text;
  toast.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove("show"), 4000);
}

function explain(result, fallback) {
  const message = result.json && (result.json.message || result.json.error);
  say(message ? fallback + " " + message : fallback + " (" + result.status + ").");
}

function rememberWaitlist(id) {
  state.waitlistId = id;
  $("waitlistId").value = id;
  saveRooms();
}

function rememberBook(name) {
  state.bookName = name || state.bookName;
  const room = state.rooms.find((item) => item.id === state.waitlistId);
  if (room && state.bookName) {
    room.name = state.bookName;
  }
  if (state.bookName) {
    $("bookName").textContent = state.bookName;
    saveRooms();
    drawRooms();
  }
}

function rememberEntry(id) {
  state.entryId = id || "";
  saveRooms();
}

function rememberProfile(id, profile) {
  state.profiles[id] = profile;
  saveRooms();
}

function profileFor(entry) {
  return state.profiles[entry.entryId] || {
    name: entry.name || "Guest",
    size: null,
    note: ""
  };
}

function guestName(entry) {
  const profile = profileFor(entry);
  if (profile.name && profile.name !== "Guest") {
    return profile.name;
  }
  return entry.name || "Guest";
}

async function openRoom(name, maxLength, minutes) {
  if (!name) {
    say("Name the room.");
    return;
  }
  const result = await call("POST", "/waitlists", {
    name,
    maxLength: Number(maxLength),
    processingTimePerEntryMinutes: Number(minutes)
  });
  if (result.status === 200 && result.json && result.json.id) {
    const roomName = result.json.name || name;
    if (!state.rooms.some((room) => room.id === result.json.id)) {
      state.rooms.push({ id: result.json.id, name: roomName });
    }
    state.waitlistId = result.json.id;
    state.bookName = roomName;
    $("waitlistId").value = result.json.id;
    $("bookName").textContent = roomName;
    saveRooms();
    $("roomForm").hidden = true;
    if ($("roomName")) {
      $("roomName").value = "";
    }
    showFloor(true);
    say(roomName + " is open.");
    await refresh();
    return;
  }
  explain(result, "Could not open that room.");
}

async function createWaitlist() {
  await openRoom($("waitlistName").value.trim(), $("maxLength").value, $("minutes").value);
}

async function addRoom() {
  await openRoom($("roomName").value.trim(), $("roomMax").value, $("roomMinutes").value);
}

async function selectRoom(id) {
  const room = state.rooms.find((item) => item.id === id);
  if (!room) {
    return;
  }
  state.waitlistId = room.id;
  state.bookName = room.name;
  $("waitlistId").value = room.id;
  $("bookName").textContent = room.name;
  saveRooms();
  showFloor(true);
  drawRooms();
  await refresh();
}

async function removeRoom(id) {
  const room = state.rooms.find((item) => item.id === id);
  if (!room) {
    return;
  }
  if (!window.confirm("Take " + room.name + " off this stand?")) {
    return;
  }
  state.rooms = state.rooms.filter((item) => item.id !== id);
  if (state.waitlistId !== id) {
    saveRooms();
    drawRooms();
    say(room.name + " is off this stand.");
    return;
  }
  const next = state.rooms[0];
  if (!next) {
    state.waitlistId = "";
    state.bookName = "";
    state.entries = [];
    saveRooms();
    showFloor(false);
    clearBoard();
    say(room.name + " is off this stand.");
    return;
  }
  state.waitlistId = next.id;
  state.bookName = next.name;
  saveRooms();
  say(room.name + " is off this stand.");
  await selectRoom(next.id);
}

async function joinBook() {
  const waitlistId = $("waitlistId").value.trim();
  if (!waitlistId) {
    say("Paste the room id from the other stand.");
    return;
  }
  if (!state.rooms.some((room) => room.id === waitlistId)) {
    state.rooms.push({ id: waitlistId, name: "Room" });
  }
  state.waitlistId = waitlistId;
  saveRooms();
  showFloor(true);
  await refresh();
}

async function checkIn() {
  const waitlistId = $("waitlistId").value.trim();
  if (!waitlistId) {
    say("Open a book before adding a party.");
    return;
  }
  const party = $("partyName").value.trim();
  if (!party) {
    say("Whose name is on the book?");
    return;
  }
  rememberWaitlist(waitlistId);
  const holdNext = $("rush").checked;
  const result = await call("POST", "/waitlists/" + waitlistId + "/entries", {
    name: party,
    priority: holdNext ? 5 : 1
  });
  if (result.status === 200 && result.json) {
    rememberEntry(result.json.entryId);
    rememberProfile(result.json.entryId, {
      name: party,
      size: state.partySize,
      note: $("partyNote").value.trim()
    });
    say(party + " · party of " + state.partySize + " · " + quoteText(result.json) + ".");
    $("partyName").value = "";
    $("partyNote").value = "";
    $("rush").checked = false;
    await refresh();
    return;
  }
  explain(result, "Could not add that party.");
}

async function refresh() {
  const waitlistId = $("waitlistId").value.trim();
  if (!waitlistId) {
    say("Open a book first.");
    return;
  }
  rememberWaitlist(waitlistId);
  const result = await call("GET", "/waitlists/" + waitlistId);
  if (result.status !== 200 || !result.json) {
    explain(result, "Could not read the book.");
    showFloor(false);
    return;
  }
  state.entries = result.json.entries || [];
  rememberBook(result.json.name);
  const called = state.entries.find((entry) => entry.status === "CALLED");
  if (called) {
    rememberEntry(called.entryId);
  }
  showFloor(true);
  draw();
}

function quoteText(entry) {
  if (entry.status === "CALLED") {
    return "Table is ready";
  }
  if (entry.status === "SERVED") {
    return "Seated";
  }
  if (entry.status === "CANCELLED") {
    return "Left";
  }
  if (entry.position === 1 || entry.estimatedWaitTimeMinutes === 0) {
    return "Next up";
  }
  if (entry.estimatedWaitTimeMinutes == null) {
    return "Quoted";
  }
  return entry.estimatedWaitTimeMinutes + " min";
}

function draw() {
  const waiting = state.entries.filter((entry) => entry.status === "WAITING");
  const called = state.entries.filter((entry) => entry.status === "CALLED");
  const seated = state.entries.filter((entry) => entry.status === "SERVED");
  const left = state.entries.filter((entry) => entry.status === "CANCELLED");
  paintTickets($("rows"), waiting, true);
  paintReady(called[0]);
  paintPast($("seated"), seated);
  paintPast($("left"), left);
  $("seatedCount").textContent = String(seated.length);
  $("leftCount").textContent = String(left.length);
  $("waitingCount").textContent = String(waiting.length);
  const covers = waiting.reduce((sum, entry) => sum + (profileFor(entry).size || 0), 0);
  $("coverCount").textContent = covers ? String(covers) : "—";
  $("nextQuote").textContent = waiting.length ? quoteText(waiting[0]) : "Clear";
  $("seat").disabled = waiting.length === 0;
  drawRooms();
}

function drawRooms() {
  const host = $("rooms");
  if (!host) {
    return;
  }
  host.replaceChildren();
  state.rooms.forEach((room) => {
    const chip = document.createElement("div");
    chip.className = "tab" + (room.id === state.waitlistId ? " on" : "");
    const open = document.createElement("button");
    open.type = "button";
    open.className = "tab-name";
    open.textContent = room.name || "Room";
    open.addEventListener("click", () => selectRoom(room.id).catch(showError));
    const remove = document.createElement("button");
    remove.type = "button";
    remove.className = "tab-remove";
    remove.setAttribute("aria-label", "Remove " + (room.name || "room"));
    remove.textContent = "×";
    remove.addEventListener("click", () => removeRoom(room.id).catch(showError));
    chip.append(open, remove);
    host.append(chip);
  });
}

function paintReady(entry) {
  const banner = $("readyBanner");
  if (!entry) {
    banner.hidden = true;
    return;
  }
  banner.hidden = false;
  const profile = profileFor(entry);
  $("readyName").textContent = guestName(entry);
  const size = profile.size ? "Party of " + profile.size : "Party";
  $("readyMeta").textContent = size + (profile.note ? " · " + profile.note : "");
}

function paintTickets(host, entries, showQuote) {
  host.replaceChildren();
  if (!entries.length && showQuote) {
    const empty = document.createElement("p");
    empty.className = "empty";
    empty.textContent = "The book is clear.";
    host.append(empty);
    return;
  }
  entries.forEach((entry) => host.append(ticket(entry, showQuote)));
}

function paintPast(host, entries) {
  host.replaceChildren();
  entries.forEach((entry) => host.append(ticket(entry, false)));
}

function ticket(entry, showQuote) {
  const profile = profileFor(entry);
  const name = guestName(entry);
  const card = document.createElement("article");
  card.className = "ticket" + (entry.priority >= 5 ? " hold-next" : "");
  const avatar = document.createElement("div");
  avatar.className = "avatar";
  avatar.textContent = initials(name);
  const copy = document.createElement("div");
  const title = document.createElement("h3");
  title.textContent = name;
  const meta = document.createElement("p");
  const bits = [];
  if (profile.size) {
    bits.push("Party of " + profile.size);
  }
  if (entry.priority >= 5 && entry.status === "WAITING") {
    bits.push("Next table");
  }
  if (profile.note) {
    bits.push(profile.note);
  }
  if (entry.position && entry.status === "WAITING") {
    bits.push("No. " + entry.position);
  }
  meta.textContent = bits.join(" · ") || quoteText(entry);
  copy.append(title, meta);
  card.append(avatar, copy);
  if (showQuote) {
    const quote = document.createElement("div");
    quote.className = "quote";
    quote.textContent = quoteText(entry);
    const small = document.createElement("small");
    small.textContent = entry.position === 1 ? "Call them" : "Quoted";
    quote.append(small);
    card.append(quote);
  }
  return card;
}

function initials(name) {
  return name.split(/\s+/).filter(Boolean).slice(0, 2)
      .map((part) => part[0].toUpperCase()).join("") || "?";
}

async function seatNext() {
  const waitlistId = $("waitlistId").value.trim();
  if (!waitlistId) {
    say("Open a book first.");
    return;
  }
  const result = await call("POST", "/waitlists/" + waitlistId + "/serve-next");
  if (result.status === 204) {
    say("Nobody is waiting.");
    await refresh();
    return;
  }
  if (result.status === 200 && result.json) {
    rememberEntry(result.json.entryId);
    const known = state.profiles[result.json.entryId];
    say("Table ready for " + (known ? known.name : "the next party") + ".");
    await refresh();
    return;
  }
  explain(result, "Could not call the next party.");
}

async function mark(status) {
  const waitlistId = $("waitlistId").value.trim();
  if (!state.entryId) {
    say("Call a party before seating them.");
    return;
  }
  const known = state.profiles[state.entryId];
  const path = "/waitlists/" + waitlistId + "/entries/" + state.entryId;
  const result = await call("PATCH", path, { status });
  if (result.status === 200) {
    const name = known ? known.name : "That party";
    say(status === "SERVED" ? name + " is seated." : name + " left the book.");
    await refresh();
    return;
  }
  explain(result, "Could not update that party.");
}

$("useLocal").addEventListener("click", () => switchTarget("local").catch(showError));
$("useCloud").addEventListener("click", () => switchTarget("gcp").catch(showError));
$("create").addEventListener("click", () => createWaitlist().catch(showError));
$("showRoomForm").addEventListener("click", () => {
  $("roomForm").hidden = !$("roomForm").hidden;
});
$("addRoom").addEventListener("click", () => addRoom().catch(showError));
$("join").addEventListener("click", () => joinBook().catch(showError));
$("checkIn").addEventListener("click", () => checkIn().catch(showError));
$("refresh").addEventListener("click", () => refresh().catch(showError));
$("seat").addEventListener("click", () => seatNext().catch(showError));
$("served").addEventListener("click", () => mark("SERVED").catch(showError));
$("cancelled").addEventListener("click", () => mark("CANCELLED").catch(showError));

document.querySelectorAll("#sizes button").forEach((button) => {
  button.addEventListener("click", () => {
    state.partySize = Number(button.dataset.size);
    document.querySelectorAll("#sizes button").forEach((other) => {
      other.classList.toggle("on", other === button);
    });
  });
});

function showError(error) {
  say(error.message || "The stand lost the kitchen link.");
}

boot().catch(showError);
