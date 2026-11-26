// priority: 0
// Companion to server_scripts/systems/retrogen.js (added 2026-11-25).
// Server scripts may not assign to `global`, and /reload would stack NativeEvents listeners,
// so the shared state and the single chunk-load listener live here (startup scripts run once per game start).
// The server script fills in global.retrogen.onLoad and drains global.retrogen.queue.

global.retrogen = { queue: [], queued: {}, stats: { retro: 0, fresh: 0, veins: 0 }, onLoad: null };

NativeEvents.onEvent(Java.loadClass("net.neoforged.neoforge.event.level.ChunkEvent$Load"), (event) => {
  const rg = global.retrogen;
  // ChunkEvent.Load also fires for client chunks; only the server side matters.
  if (!rg.onLoad || event.getLevel().isClientSide()) return;
  try {
    rg.onLoad(event);
  } catch (e) {
    console.warn("[retrogen] chunk load hook error: " + e);
  }
});
