# Tweaks

Small one-click survival tweaks for a Paper server. Server-side only: players need no mod.

By tedtalksbits.

## Lever rotate

**Sneak + right-click a lever with both hands empty** to turn it one step: north, east, south, west, north. It's the
debug stick's `facing` change, available in survival.

- Works on floor, wall and ceiling levers. The lever stays on the floor, wall or ceiling (`face` doesn't change).
- The click doesn't flip the lever: on stays on, off stays off. A short click sound plays.
- With anything in either hand, sneak + click works as in vanilla (it places the block against the lever).
- Survival and creative only (not adventure). Permission `tweaks.leverrotate`, everyone has it by default.
- Protection plugins and spawn protection that stop you flipping a lever stop you turning it too.

### Sideways wall levers

On a wall, a lever's direction is the way it sticks out from the block it hangs on, so turning it hangs it on a
different neighbour in the same spot. Like the debug stick, the plugin doesn't check that neighbour: a wall lever can
end up hanging sideways on a lightning rod, glass or even air. That's the point (valves on a pipe made of lightning
rods, for example). What that means:

- **It stays as long as the block it now hangs on isn't changed.** Break or replace that block and the lever pops off
  and drops as an item, as a debug-stick lever would. Changing any other block next to it is fine.
- **A powered wall lever moves its power.** A wall lever strongly powers the block it hangs on, so after a turn the
  new block is powered and the old one isn't any more. On the floor and ceiling the power doesn't move.

## Install

1. Download `tweaks-plugin-<version>.jar` from the Releases page.
2. Put it in the server's `plugins/` folder and restart.

Paper 26.3 (Minecraft 26.3), Java 25.

## Config

`plugins/Tweaks/config.yml`. Each tweak can be switched off; restart the server after a change.

```yaml
lever-rotate:
  enabled: true
```

To keep it to some players, deny `tweaks.leverrotate` with a permissions plugin (or set it to `op`).

## Building

`gradlew build` (Java 25). The jar is `tweaks-plugin/build/libs/tweaks-plugin-<version>.jar`. See `AGENTS.md` for
tests, the real-server self-test and the dev loop.
