# Test plan: Tweaks

Plugin 0.1.0 (lever rotate). The automated real-server self-test passed on Paper 26.3-211: floor, ceiling and wall
levers, off and on, four turns each with lamps checked; your pipe corner (a wall lever between two lightning rods)
through seven turns, other blocks changed, a real chunk unload and reload, and the rod broken. **Nothing has been
seen in a real game yet**: the click itself, how it looks and sounds, a second player, protection.

## Setup (once, about 3 minutes)

1. Double-click `run\build-and-install.cmd`. It stops the test server (`..\blueprints-mc\run\local-paper`), builds
   and tests, installs `plugins\Tweaks.jar` (other projects' jars are left alone), then offers to start the server.
   Without RCON it asks you to type `stop` in the server window and carries on by itself.
2. Join in **survival**, with smooth quartz, a few lightning rods, levers, redstone lamps and a stack of stone.

## Tests

| # | Do this | Expect |
|---|---|---|
| 1 | Rebuild a corner of your pipe wall: lightning rods lying along a quartz wall, a gap between two of them. Place a lever on the wall in the gap, as normal. Empty both hands, sneak, right-click it | It turns sideways, pointing along one rod (facing east, so hanging on the west rod). A quiet click. It doesn't flip. |
| 2 | Keep sneak-clicking it | East, south (sticking out from the wall, hanging on nothing), west (along the other rod), north (back to normal). Choose the one you want. |
| 3 | Leave it pointing along a rod. Relog, then fly far enough away for the chunk to unload and come back, then restart the server | Still there, same direction, after each. |
| 4 | Place and break a block somewhere else next to the turned lever (above it, below it, the rod it doesn't hang on) | It stays. |
| 5 | Break the rod it hangs on | The lever pops off and drops as an item (expected, same as a debug-stick lever). |
| 6 | Floor lever wired to a lamp (lamp right next to it). Off: sneak-click four times. Flip it on, sneak-click four times | It spins on the spot N, E, S, W, N. The lamp stays off, then stays lit throughout. On stays on. |
| 7 | The same with a lever on a ceiling | Same. |
| 8 | A wall lever in a hole with quartz on all four sides and a lamp behind each quartz block. Flip it on, then sneak-click round | Exactly one lamp lit at a time: the one behind the block the lever now hangs on. The lever stays on and doesn't pop. |
| 9 | Hold a block (or anything, in either hand), sneak, right-click a lever | The block is placed against the lever, as in vanilla. The lever doesn't turn. |
| 10 | Plain right-click (not sneaking), empty hands | Flips on/off as usual. |
| 11 | Ask a second player to watch while you turn a lever | They see it turn (and hear the click). |
| 12 | **How it feels**: watch the lever closely as you sneak-click, a few times, off and on | Tell me if the lever flickers (looks flipped for a moment, then corrects) or anything looks wrong. Your client may draw a flip particle on an off lever. Also: is the sound right? It's the lever click, quieter and higher. |
| 13 | `/gamemode adventure`, sneak-click a lever | Nothing turns. Back to survival: works again. |
| 14 | Optional, spawn protection: as a **non-op** player within `spawn-protection` of spawn, plain-click and sneak-click a lever | If the plain click can't flip it, the sneak-click can't turn it either. |
| 15 | Optional: set `lever-rotate: enabled: false` in `plugins/Tweaks/config.yml`, restart | Sneak-click flips the lever like vanilla. The log says "Lever rotate: off". Set it back. |

## ⚠️ Known limits and decisions for you

1. **A turned lever hangs on whatever is behind it now**, even a rod, glass or air. It stays until that block changes,
   then pops off and drops (test 5). Same as the debug stick; it's what makes the sideways valves possible.
2. **A powered wall lever moves its power** to the block it now hangs on (test 8). Floor and ceiling: unchanged.
3. **Only the direction turns.** Moving a lever between floor, wall and ceiling (`face`) isn't in v0.1.0, as agreed.
   Buttons, repeaters, stairs and the like aren't either; noted as possible next tweaks in `AGENTS.md`.
4. **Power moving uses Minecraft internals** (no Bukkit API for neighbour updates). If a future Paper renames them,
   the plugin logs a warning once and levers still turn; lamps then only catch up at their next update. The CI
   self-test would catch that before a release.
5. **Server restart** isn't in the automated self-test (a chunk unload and reload is, which saves and loads the same
   way). Test 3 covers it.
6. **The client side** (test 12) can't be tested automatically. If the cancelled flip flickers badly, the next step is
   resending the block a tick later, not a client mod.
