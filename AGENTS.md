# Tweaks: notes for agents

Start here, then read `README.md` (what it does, for players and admins). `docs/TEST_PLAN.md` and `docs/CI.md` cover
the rest.

## What this is

A small Paper plugin for the owner's friends-and-family survival server: one-click tweaks, each a listener with its
own permission and `config.yml` switch. Server-side only, no client mod. Credit: **tedtalksbits**. Separate from
every other project (Logic Blocks, Blueprints, Glowing Glass, Shulker Styles, Item Beacon); reuse their patterns by
reading their files, never edit them.

- `tweaks-plugin/`: the plugin (Java 25). `TweaksPlugin` reads the config and registers the tweaks that are on.
- `tweaks-plugin/.../LeverRotate.java`: tweak 1. `shouldRotate(event)` is the "should this click turn?" check;
  `rotate(block)` turns a lever one step and is what the self-test calls.
- `tweaks-plugin/.../Neighbours.java`: sends Minecraft's neighbour update by reflection (no Bukkit API for it).
- `paper-selftest/`: development-only plugin that builds levers on a real Paper server and checks them.
- `ci/`, `.github/workflows/`: CI and releases (`docs/CI.md`).
- `run/`: the dev loop (`DevInstall.java`).

## Status (2026-11-26)

- **v0.1.0 built, not released.** Lever rotate: 20 unit tests pass; the real-Paper self-test (47 checks) passes on
  Paper 26.3-211. CI written but not yet run on GitHub (the owner pushes).
- **Not seen in a real game yet**: the click itself (cancelled flip, client prediction, sound), a second player
  watching, spawn protection, protection plugins. That's `docs/TEST_PLAN.md`.

## Open items

1. Owner playtest (`docs/TEST_PLAN.md`), then release v0.1.0.
2. Possible next tweaks, not committed to, don't build unasked: changing `face` (floor/wall/ceiling) on levers; the
   same turn for buttons (same three properties); a general "wrench" for repeaters, comparators, observers, stairs.

## Decisions already made (don't reopen without asking)

| Decision | Why |
|---|---|
| A shared "Tweaks" plugin, lever rotate is its first tweak (not its own "Lever Rotate" project) | Owner's call, 2026-11-26: more one-click tweaks expected, saves repeating repo/CI/self-test/dev scaffolding |
| Each tweak has a `config.yml` switch (`<tweak>.enabled`) and its own permission `tweaks.<tweak>` | Owner's call: turn one off without removing the plugin |
| Lever: sneak + right-click with **both hands empty** | The one case where vanilla sneak-clicking a lever does anything (a plain flip), so nothing is lost; with an item vanilla places it, left alone |
| Everyone, survival and creative; not adventure (`tweaks.leverrotate`, default true) | Owner's call |
| Only `facing` cycles (N, E, S, W like the debug stick); `face` and `powered` stay | Owner's call for v1; covers his sideways pipe-valve levers |
| Support not checked; a wall lever may hang on a rod, glass or air | That's the point (his build); same as the debug stick |
| Written with `setBlockData(data, false)`, then (powered only) neighbour updates at the lever and at its old and new support | No physics means the lever never re-checks its support. Plain `setBlockData(data, true)` keeps the lever but leaves the power on the old block (self-test "compare" step shows it); a lever flip updates exactly these spots |
| Neighbour updates by reflection on Mojang names (`Level.updateNeighborsAt`); on failure log once and carry on | No Bukkit API; Logic Blocks does the same for signals. Levers still turn, power catches up at the next update |
| Listener at `HIGH`, skips when `useInteractedBlock() == DENY`, acts on the main-hand event only, cancels to stop the flip | Runs after protection plugins; the event fires once per hand |
| Release tag = plugin version; one jar | Same scheme as the other projects |

## Working rules

- Commit locally; **the owner pushes** to GitHub. Agents commit as `Claude <noreply@anthropic.com>` (pass it per
  commit with `git -c user.name=Claude -c user.email=noreply@anthropic.com`; don't change the repo's git config, the
  owner commits as himself). Never rewrite his commits.
- Reports: short and structured; anything needing the owner's decision at the top, marked ⚠️. No "•" separators.
- Verify on a real Paper server before claiming it works. MockBukkit is for unit tests only. Say plainly what was and
  wasn't seen in a real game.
- End each slice with `docs/TEST_PLAN.md` ("Do this / Expect" rows, then "⚠️ Known limits and decisions for you")
  and update this file.
- He playtests, then releases. Before saying a release is ready, check `version` in `build.gradle.kts` equals the tag
  he'll push, and look at `git tag` rather than notes.
- Kickoffs, mockups and drafts live in `C:\Users\tedan\Documents\software\Claude outputs\tweaks\`, never in the repo.
- A new tweak: its own listener class, a `<tweak>.enabled` switch in `config.yml`, a `tweaks.<tweak>` permission in
  `plugin.yml`, unit tests, self-test steps, README section, test plan rows.

## Building and testing

- `gradlew build` at the root (Java 25, Gradle 9.8.0). Jar: `tweaks-plugin/build/libs/tweaks-plugin-<v>.jar`.
- Self-test: `ci/run-selftest.sh <paper.jar> <new-folder>` (Linux, Git Bash, CI): throwaway flat-world server, runs
  with `-Dtweaks.selftest.exit=true`, fails unless `plugins/TweaksSelfTest/report.txt` ends `SELFTEST PASSED`.
  Offline: `PAPER_SEED_DIR=<existing server folder>`. `SELFTEST_ONLY=pipe,wall` for a subset.
- Dev loop: `run\build-and-install.cmd` (or `run/build-and-install.sh`) runs `run/DevInstall.java`: stops the local
  test server (RCON if enabled, else waits for `stop`), builds and tests, installs `plugins/Tweaks.jar` (removing
  stray `tweaks-plugin-*.jar`, leaving other projects' jars), offers to start the server. Defaults fit the owner
  (blueprints-mc's `run/local-paper`, JDK and Gradle cache); `TWEAKS_SERVER_DIR`, `TWEAKS_GRADLE_ARGS`, `JAVA_HOME`
  override. `--enable-rcon` once turns RCON on for the test server.
- CI: `docs/CI.md`.

## Cloud workspace pitfalls (if you work in a remote Linux container)

- Use the `offline-mc-build` skill. In short: the container can't reach Maven Central, Paper or Mojang; the device
  shell has Java 11 and no network. Tar the owner's Gradle home (`blueprints-mc\.tools\gradle-home`:
  `caches/modules-2/files-2.1`, `caches/modules-2/metadata-2.107`, `wrapper/dists/gradle-9.8.0-bin`) and
  `blueprints-mc\run\local-paper` (`paper-26.3-211.jar`, `cache`, `versions`, `libraries`) into `tweaks\.tools\stage\`,
  stage, unpack, `GRADLE_USER_HOME=<it>` and build with `--offline` (no init script needed with `metadata-2.107`).
- Hand commits back as a `git bundle` pulled with `--ff-only`; ask for delete permission on `software` first (git's
  lock files). Remove `.tools` afterwards.

## Gotchas found the hard way

- `TweaksPlugin` must not be `final`: MockBukkit subclasses the plugin class.
- A powered lever placed by `setBlockData` doesn't tell the lamps behind the block it hangs on (levers only update
  that block's neighbours when flipped). The self-test places the quartz supports last so the lamps re-check.
- The self-test refuses to run with two plugin jars in `build/libs` (stale version): `gradlew clean build`.
