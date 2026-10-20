# Agent rules and conventions â€” Tensura Apotheosis

**Repo:** `HexaHexes/tensura-apotheosis` Â· **Mod ID:** `tensura_apotheosis` Â· **Branch:** `master`
**Platform:** Minecraft 1.21.1 / NeoForge 21.1.259 / JDK 21 Â· **Jar:** `build/libs/tensura-apotheosis-<version>.jar`

Tensura-themed affixes and socket gems for Apotheosis 8.8, delivered as **pure
datapack data** (Placebo dynamic registries) â€” no mixins, no gameplay code. The Java
side exists only for the in-repo gametest harness.

**Dependencies (all required)**
- Apotheosis (`apotheosis`), Apothic Attributes (`apothic_attributes`), Tensura (`tensura`).

**Test:** `./gradlew runGametest` â€” 3 tests (`twoHandedWeaponsCategorizeAsMelee`,
`allAffixesLoad`, `allGemsLoad`). `expectedTests = 3`.

**Single source of truth:** `tools/generate-content.ps1` produces all affixes, gems,
lang and models; `tools/generate-textures.ps1` produces the 14 gem PNGs. **Value tuning
happens in the generator, not by hand-editing JSON** â€” regenerate after any change.

**Gotchas (all caught by the gametest; do not regress them)**
- Affix `min`/`max` ranges decode as **step functions** with an implicit step of `0.01`;
  `(max âˆ’ min)` must be a multiple of that step. Mob-effect `duration`/`amplifier`
  objects accept an explicit `step`.
- Every `tensura:` attribute id must exist in Tensura's registry â€” there is **no**
  `tensura:lightning_resistance` (storm dragonscale uses `space_resistance`). Verify
  new ids against `TensuraAttributes` before shipping.
- Within one gem, the bonuses' `gem_class` category sets must be **disjoint** or
  Apotheosis rejects the whole file.
- Gem stacks reuse Apotheosis' shared `apotheosis:gem` item. Gem **models** resolve
  through Apotheosis' hardcoded namespace, so model JSONs ship under
  `assets/apotheosis/models/item/gems/`, while textures and lang stay in this mod's own
  `assets/tensura_apotheosis/`.
- Client cosmetics (gem textures) cannot be proven headless â€” the user checks in-game.

---

## Branch sync (do this first)

Before starting any new development in this repo:

1. `git status` â€” confirm what uncommitted work exists. Never disturb or sweep it.
2. `git pull --ff-only` â€” sync with the remote. A fast-forward is the normal case,
   because we push straight to the shared branch.
3. If the pull would **not** fast-forward (the branch has diverged), or if it would
   collide with local uncommitted work, **stop and ask** rather than merging, rebasing
   or stashing on your own.

## Scope doctrine

- **One mod + bundled datapack.** No KubeJS scripts.
- **Data-driven over code** â€” prefer JSON/datapack content to Java when both work.
- **Public API only, no mixins** in the colony-gear-style addon mods. Sanctioned
  exception: `ds-tensura-fix` (tensura-dragon-skin-rebalance), a mixin fix mod by design.

## Verification

- The in-repo GameTest run is the **only** CI gate. Never verify against a live
  CurseForge instance.
- Keep `expectedTests` in `build.gradle` in sync with the `@GameTest` methods.
- **Client-render features cannot be proven headless.** The honest scope is
  "build green + the user's in-game check" â€” say so rather than overclaiming.

## Build environment

- JDK 21 (`JAVA_HOME` -> JDK 21); the toolchain targets Java 21.
- The Gradle wrapper is committed. `./gradlew build` compiles, `./gradlew runGametest`
  (or `runGameTestServer`) runs the GameTest server.
- `libs/` (compile-only) and `testmods/` (runtime closure) are **gitignored** staging
  folders. Never commit third-party jars.
- On `run/` file-lock errors, kill any stale `java` process and retry.

## PowerShell quirks (this org is developed on Windows)

- A `git push` "error" in PowerShell is usually stderr passthrough â€” success is the
  `hash..hash  branch -> branch` line, not the absence of red text.
- `New-Object Type(args)` breaks on arithmetic â€” use `::new()` or a parenthesized
  argument.

## Git & release

- Remote is SSH: `git@github.com:HexaHexes/<name>.git`.
- Push only after tests are green **and** the user has given the go.
- Never sweep the user's uncommitted work into a commit â€” commit only what is intended.
- Commit subjects: concise, imperative.

## House style

- **No icons** unless the user explicitly asks. Icons are being removed across all repos.
- READMEs are **player-facing only**: pitch, mechanics in plain terms, in-game usage,
  required/optional dependencies, credits, license. No build internals, no class names.
- All repos are **LGPL v2.1**, copyright Bitwise Madness. `NOTICE` / attribution files
  are mandatory and must never be deleted.
- Deploy to the Tensura RPG instance **only** when the user asks.

## CI & release model

- `ci.yml` runs on pull requests: a build job and a GameTest job. Both assert a
  non-zero result â€” a run that passes vacuously (0 tests) is a failure.
- `release.yml` runs on every push to the default branch, re-runs the tests, and â€” only
  when the version was bumped â€” tags and publishes a GitHub release with a cumulative
  changelog. Alpha/nightly publishing is opt-in via the `ALPHA_RELEASES` repo variable
  and is **off** by default.
- `version-bump.yml` opens an isolated version-bump PR (only the version file changes).
  Beta/stable bumps must not carry other changes; `-alpha` bumps may bundle work.
- CurseForge publishing (`curseforge.yml`) is **deferred** until a mod has a CurseForge
  listing and the `CURSEFORGE_PROJECT_ID` / `CURSEFORGE_API_KEY` secrets. Do not add it
  speculatively.

