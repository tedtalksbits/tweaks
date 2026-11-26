# CI

GitHub Actions, `.github/workflows/ci.yml`. Runs on every push to `main`, on pull requests, and by hand (Actions tab >
CI > Run workflow). Pushes that only touch Markdown, `docs/` or `run/` are skipped. Nothing is deployed anywhere.

| Job | Does | Fails when |
|---|---|---|
| Plugin build and unit tests | `./gradlew build` at the root: plugin, self-test plugin, MockBukkit unit tests | Compile error or a unit test fails |
| Self-test on a real Paper server | Downloads the pinned Paper build, checks its SHA-256, runs `ci/run-selftest.sh` | The report doesn't end `SELFTEST PASSED`, or the server hangs (10 minutes) |
| Release (tags `v*` only) | All of the above, then publishes the plugin jar (see Releases) | Any CI job fails, or the tag doesn't match the plugin version |

Each run keeps the jars as artifacts (`tweaks-plugin`, `paper-selftest`) plus `selftest-report` (report and server
log). The self-test report is also printed on the run's summary page.

## Pins

The workflow's `env` block: `JAVA_VERSION`, `PAPER_VERSION`, `PAPER_BUILD`, `PAPER_SHA256`. To move to a new
Paper build, take the hash of the jar you tested locally (`certutil -hashfile paper-<v>-<b>.jar SHA256` on Windows,
`sha256sum` elsewhere) and change all three Paper values together. Change the Paper API and MockBukkit versions in
`tweaks-plugin/build.gradle.kts` and `paper-selftest/build.gradle.kts` to match.

## Running the self-test by hand

On Linux or in Git Bash, after `./gradlew build`:

```
ci/run-selftest.sh path/to/paper-26.3-211.jar /tmp/selftest-run
```

The second argument must be a folder that doesn't exist yet. Without internet access, add
`PAPER_SEED_DIR=path/to/an/existing/paper/server` so its `cache/`, `versions/` and `libraries/` are reused.
`SELFTEST_ONLY=pipe,wall` runs only the steps whose names start that way.

## Dependabot

`.github/dependabot.yml` checks the workflow's actions monthly and opens a PR when one has a new version; CI runs on
the PR, so merge it when it's green. It doesn't touch Gradle dependencies: Paper and MockBukkit move with the
Minecraft version (see Pins).

## Releases

`.github/workflows/release.yml`. The tag is the plugin version.

1. Bump `version` in `build.gradle.kts`.
2. Commit and push to `main`; wait for CI to go green.
3. Tag and push: `git tag v0.1.0` then `git push origin v0.1.0`.

The release workflow runs all of CI again, checks that the tag matches the plugin version and that the jar exists,
then publishes "Tweaks v0.1.0" with `tweaks-plugin-<version>.jar`. If anything fails, nothing is published; fix it,
delete the tag (`git push origin :v0.1.0`, `git tag -d v0.1.0`) and tag again. The self-test plugin is never released.
