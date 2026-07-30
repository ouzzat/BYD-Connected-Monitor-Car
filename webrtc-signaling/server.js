"use strict";

const http = require("http");
const { WebSocketServer, WebSocket } = require("ws");

const PORT = Number(process.env.PORT || 8080);
const MAX_MESSAGE_BYTES = 64 * 1024;
const ROOM_PATTERN = /^[A-Za-z0-9_-]{4,64}$/;
const ROLES = new Set(["car", "mobile"]);
const SIGNAL_TYPES = new Set(["ready", "offer", "answer", "candidate"]);
const rooms = new Map();

const server = http.createServer((request, response) => {
  response.setHeader("Content-Type", "application/json; charset=utf-8");
  response.setHeader("Cache-Control", "no-store");

  if (request.url === "/" || request.url === "/health") {
    response.writeHead(200);
    response.end(JSON.stringify({ status: "ok", service: "byd-webrtc-signaling" }));
    return;
  }

  response.writeHead(404);
  response.end(JSON.stringify({ error: "not_found" }));
});

const wss = new WebSocketServer({
  server,
  maxPayload: MAX_MESSAGE_BYTES,
  perMessageDeflate: false
});

function leave(client) {
  if (!client.room || !client.role) return;
  const room = rooms.get(client.room);
  if (!room) return;
  if (room.get(client.role) === client) room.delete(client.role);
  if (room.size === 0) rooms.delete(client.room);
  client.room = undefined;
  client.role = undefined;
}

function join(client, message) {
  const roomName = String(message.room || "");
  const role = String(message.role || "");
  if (!ROOM_PATTERN.test(roomName) || !ROLES.has(role)) {
    client.close(1008, "invalid room or role");
    return;
  }

  leave(client);
  const room = rooms.get(roomName) || new Map();
  const previous = room.get(role);
  if (previous && previous !== client) previous.close(1008, "role replaced");
  room.set(role, client);
  rooms.set(roomName, room);
  client.room = roomName;
  client.role = role;
  client.send(JSON.stringify({ type: "joined", room: roomName, role }));
}

function relay(client, message) {
  if (!client.room || !client.role || !SIGNAL_TYPES.has(message.type)) return;
  if (message.room !== client.room || message.from !== client.role) return;

  const targetRole = client.role === "car" ? "mobile" : "car";
  const target = rooms.get(client.room)?.get(targetRole);
  if (target?.readyState === WebSocket.OPEN) {
    target.send(JSON.stringify(message));
  }
}

wss.on("connection", (client) => {
  client.isAlive = true;
  client.on("pong", () => {
    client.isAlive = true;
  });

  client.on("message", (data, isBinary) => {
    if (isBinary || data.length > MAX_MESSAGE_BYTES) {
      client.close(1009, "message too large");
      return;
    }

    let message;
    try {
      message = JSON.parse(data.toString("utf8"));
    } catch {
      client.close(1007, "invalid json");
      return;
    }

    if (message.type === "join") join(client, message);
    else relay(client, message);
  });

  client.on("close", () => leave(client));
  client.on("error", () => leave(client));
});

const heartbeat = setInterval(() => {
  for (const client of wss.clients) {
    if (!client.isAlive) {
      client.terminate();
      continue;
    }
    client.isAlive = false;
    client.ping();
  }
}, 30_000);

wss.on("close", () => clearInterval(heartbeat));
server.listen(PORT, "0.0.0.0", () => {
  console.log(`BYD WebRTC signaling listening on port ${PORT}`);
});
