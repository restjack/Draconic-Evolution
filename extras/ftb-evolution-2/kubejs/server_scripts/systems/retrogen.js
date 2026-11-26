// priority: 0
// Retrogen: adds ore features to chunks that were generated before the feature existed.
// Added 2026-11-25 for the unofficial Draconic Evolution 26.1.2 port (the world was pregenerated
// inside its borders before DE was installed, so it has no Draconium in the Overworld or End).
//
// How it works:
// - Every chunk that loads is queued. Brand-new chunks (isNewChunk) already got natural worldgen,
//   so they are only marked as done. Older chunks get the job's features at the mod's own rates,
//   placed with `/place feature`, then are marked done. Each chunk is processed once per job.
// - Done-markers live in the server's KubeJS persistent data: one 256-hex-char bitmap per
//   job/dimension/region (key "rg|<job>|<dim>|<rx>|<rz>").
// - To add a different ore later, add a NEW job with a new id. Don't edit an existing job,
//   because its chunks are already marked done and won't be revisited.
//
// ponytail: chunks that generated naturally after the feature was added but before this script
// existed are not "new" anymore and get a second helping. Measured on the test world: 0 such chunks
// in the Overworld/End and ~80 in the Nether. Ceiling: harmless extra veins.

const RETROGEN_JOBS = [
  {
    // v1 (2026-11-25 test) marked edge-of-view chunks done even when /place refused them; v2 redoes them.
    id: "draconium_v2",
    requires: "draconicevolution",
    dims: {
      // Rates copied from DE's placed_feature JSON (same in the 1.21.1 jar and the port).
      "minecraft:overworld": [{ feature: "draconicevolution:overworld_draconium_ore", chance: 1 / 10, count: 1, minY: -64, maxY: -48 }],
      "minecraft:the_nether": [{ feature: "draconicevolution:nether_draconium_ore", chance: 1 / 30, count: 1, minY: 4, maxY: 16 }],
      "minecraft:the_end": [{ feature: "draconicevolution:end_draconium_ore", chance: 1, count: 3, minY: 0, maxY: 70 }],
    },
  },
];

const RG_CHUNKS_PER_TICK = 16;
const RG_COMMANDS_PER_TICK = 24;

// Queue, stats and the single chunk-load listener live in startup_scripts/retrogen_hook.js
// (server scripts may not assign to `global`; mutating the object it holds is fine).
const RG = global.retrogen;

const rgDimId = (level) => {
  // KubeJS exposes `dimension` as a property (an id), not the vanilla dimension() method.
  // Accept either "minecraft:overworld" or "ResourceKey[minecraft:dimension / minecraft:overworld]".
  const s = String(level.dimension);
  const m = s.match(/([a-z0-9_.-]+:[a-z0-9_.\/-]+)\]?$/);
  return m ? m[1] : s;
};
const rgCoord = (v) => (typeof v === "function" ? v() : v);

// Guarded: RG is missing if the startup script didn't run (needs a full game restart after install).
if (RG) RG.onLoad = (event) => {
  const level = event.getLevel();
  if (level.isClientSide()) return;
  const dim = rgDimId(level);
  if (!RETROGEN_JOBS.some((j) => j.dims[dim])) return;
  const pos = event.getChunk().getPos();
  const cx = rgCoord(pos.x), cz = rgCoord(pos.z);
  const key = dim + "|" + cx + "|" + cz;
  if (RG.queued[key]) return;
  RG.queued[key] = true;
  RG.queue.push({ level: level, dim: dim, cx: cx, cz: cz, isNew: event.isNewChunk(), key: key });
};

// --- done-markers: one 1024-bit map per region, stored as 256 hex chars ---
const rgRegionKey = (job, dim, cx, cz) => "rg|" + job + "|" + dim + "|" + (cx >> 5) + "|" + (cz >> 5);
const rgBit = (cx, cz) => (cx & 31) + (cz & 31) * 32;

const rgIsDone = (data, job, dim, cx, cz) => {
  const hex = data.getStringOr(rgRegionKey(job, dim, cx, cz), "");
  if (!hex) return false;
  const bit = rgBit(cx, cz);
  return (parseInt(hex.substr((bit >> 3) * 2, 2), 16) & (1 << (bit & 7))) !== 0;
};

const rgMarkDone = (data, job, dim, cx, cz) => {
  const k = rgRegionKey(job, dim, cx, cz);
  let hex = data.getStringOr(k, "");
  if (!hex) hex = "0".repeat(256);
  const bit = rgBit(cx, cz);
  const i = (bit >> 3) * 2;
  const b = (parseInt(hex.substr(i, 2), 16) | (1 << (bit & 7))).toString(16);
  data.putString(k, hex.substr(0, i) + (b.length < 2 ? "0" + b : b) + hex.substr(i + 2));
};

const rgRand = (min, max) => min + Math.floor(Math.random() * (max - min + 1));

// Rhino (KubeJS) rejects `const` declared inside a loop body ("redeclaration of var"),
// so each loop body is its own function.
const rgPlaceVein = (server, c, s) => {
  // Stay 3 blocks inside the chunk so a vein doesn't reach into a neighbour that may not be loaded.
  const x = c.cx * 16 + rgRand(3, 12), z = c.cz * 16 + rgRand(3, 12), y = rgRand(s.minY, s.maxY);
  server.runCommandSilent("execute in " + c.dim + " run place feature " + s.feature + " " + x + " " + y + " " + z);
  RG.stats.veins++;
};

// `/place feature` refuses unless the target chunk AND all 8 neighbours are loaded
// (vanilla PlaceCommand.checkLoaded on a 3x3 chunk area), so wait for them.
const rgNeighboursLoaded = (level, cx, cz) => {
  for (let dx = -1; dx <= 1; dx++) for (let dz = -1; dz <= 1; dz++) if (!level.hasChunk(cx + dx, cz + dz)) return false;
  return true;
};

// Returns how many placement commands it ran, or -1 to retry the chunk later.
const rgProcessChunk = (server, data, c) => {
  let commands = 0;
  delete RG.queued[c.key];
  // Unloaded again before its turn: leave it unmarked, it is queued again on its next load.
  if (!c.level.hasChunk(c.cx, c.cz)) return 0;
  const jobs = RETROGEN_JOBS.filter((job) => job.dims[c.dim] && Platform.isLoaded(job.requires) && !rgIsDone(data, job.id, c.dim, c.cx, c.cz));
  // Nothing left to do here: drop it before the neighbour check, or done edge chunks would cycle forever.
  if (!jobs.length) return 0;
  if (!c.isNew && !rgNeighboursLoaded(c.level, c.cx, c.cz)) return -1;
  jobs.forEach((job) => {
    const specs = job.dims[c.dim];
    if (c.isNew) {
      RG.stats.fresh++;
    } else {
      RG.stats.retro++;
      specs.forEach((s) => {
        for (let t = 0; t < s.count; t++) {
          if (Math.random() < s.chance) {
            rgPlaceVein(server, c, s);
            commands++;
          }
        }
      });
    }
    rgMarkDone(data, job.id, c.dim, c.cx, c.cz);
  });
  return commands;
};

// One queue item. Edge-of-view chunks go to the back of the queue until their neighbours load
// (or they unload and are dropped; they are queued again on their next load).
const rgStep = (server, data) => {
  const c = RG.queue.shift();
  const r = rgProcessChunk(server, data, c);
  if (r >= 0) return r;
  RG.queue.push(c);
  RG.queued[c.key] = true;
  return 0;
};

// Startup-script state outlives a world (quit to title -> reopen). Leftover entries point at the dead
// level and would block or mis-mark chunks in the next world, so empty the queue on shutdown.
ServerEvents.unloaded(() => {
  if (!RG) return;
  RG.queue.length = 0;
  RG.queued = {};
});

ServerEvents.tick((event) => {
  if (!RG || !RG.queue.length) return;
  const server = event.server;
  const data = server.persistentData;
  let commands = 0;
  for (let n = 0; n < RG_CHUNKS_PER_TICK && RG.queue.length && commands < RG_COMMANDS_PER_TICK; n++) {
    commands += rgStep(server, data);
  }
  RG.stats.ticks = (RG.stats.ticks || 0) + 1;
  const st = RG.stats;
  // Log once as soon as work happens (proves the pipeline end to end), then every ~minute of busy ticks.
  if ((!st.logged && st.retro + st.fresh > 0) || st.ticks % 1200 === 0) {
    st.logged = true;
    console.info("[retrogen] retro chunks " + st.retro + ", new chunks skipped " + st.fresh + ", veins placed " + st.veins + ", queued " + RG.queue.length);
  }
});
