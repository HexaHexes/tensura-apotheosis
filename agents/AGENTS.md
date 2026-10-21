# Agent rules and conventions — Tensura Apotheosis

**Repo:** `HexaHexes/tensura-apotheosis` · **Mod ID:** `tensura_apotheosis` · **Branch:** `master`
**Platform:** Minecraft 1.21.1 / NeoForge 21.1.259 / JDK 21 · **Jar:** `build/libs/tensura-apotheosis-<version>.jar`

Tensura-themed affixes, socket gems, elite bosses and gear sets for Apotheosis 8.8,
delivered as **pure datapack data** (Placebo dynamic registries) — no mixins, no
gameplay code. The Java side exists only for the in-repo gametest harness.

**Dependencies**
- Required: Apotheosis (`apotheosis`), Apothic Attributes (`apothic_attributes`), Tensura (`tensura`).
- Optional: Gateways to Eternity (`gateways`) - the elite gateways and their recipes only
  load when it is present (recipes carry a `mod_loaded` condition).

**Test:** `./gradlew runGametest` - 6 tests (`twoHandedWeaponsCategorizeAsMelee`,
`allAffixesLoad`, `allGemsLoad`, `allElitesLoad`, `allGearSetsLoad`, `allGatewaysLoad`).
`expectedTests = 6`.

**Single source of truth:** `tools/generate-content.ps1` produces all affixes, gems,
gear sets, elites, gateways, gateway recipes, lang and models. **Textures are never
generated**: the original 14 gem PNGs are fixed art, and every newer gem reuses an
existing Apotheosis gem texture as a placeholder until hand-drawn art lands (the 5th
`Add-Gem` argument). **Value tuning happens in the generator, not by hand-editing
JSON** - regenerate after any change. The world-tier banding (roster `tier`),
`$tierProfile` (elite + reward rarity ladder) and `$themeIdentity` (gateway color,
themed gem pool and flavor stack per mob family) all live there too.

**Gotchas (all caught by the gametest; do not regress them)**
- Affix `min`/`max` ranges decode as **step functions** with an implicit step of `0.01`;
  `(max - min)` must be a multiple of that step. Mob-effect `duration`/`amplifier`
  objects accept an explicit `step`.
- Every `tensura:` attribute id must exist in Tensura's registry — there is **no**
  `tensura:lightning_resistance` (storm dragonscale uses `space_resistance`). Verify
  new ids against `TensuraAttributes` before shipping.
- Within one gem, the bonuses' `gem_class` category sets must be **disjoint** or
  Apotheosis rejects the whole file.
- Gem stacks reuse Apotheosis' shared `apotheosis:gem` item. Gem **models** resolve
  through Apotheosis' hardcoded namespace, so model JSONs ship under
  `assets/apotheosis/models/item/gems/`, while textures and lang stay in this mod's own
  `assets/tensura_apotheosis/`.
- Client cosmetics (gem textures) cannot be proven headless — the user checks in-game.
- Gear sets require a top-level `weight` (the codec rejects the file without it);
  `quality` is optional (vanilla sets omit it).
- Elite `stats.effects` and any `tensura:`/`minecraft:` effect id must exist in the
  game's registry — there is **no** `minecraft:poison_resistance` (the spider and spore
  themes use `night_vision`/`regeneration`). An invalid effect id fails the whole elite
  parse, so verify ids before shipping.
- Every elite mob must have a `#tensura:<mob>_spawn` biome tag; the elite's
  `constraints.biomes` is that literal tag (so it inherits Tensura's exact natural-spawn
  logic, no blanket dimensions).
- Elite gateways use `apotheosis:tiered`; each mob's gateway sits at a world tier
  computed from actual stats: the generator scores hp / damage / scale /
  enchant-chance / success-chance (inverted) from the theme table plus the mob's own
  Tensura existence stats (`data/tensura/entity_existence`: aura pool / AP, magicule
  pool / MP, spiritual health) as average-rank percentiles over the 38-mob roster,
  then bands **within spawn-dimension groups** - the hardest dimension in the mob's
  `_spawn` biome tag (overworld/nether/hell/end; tensura tags expand from the jar,
  vanilla nether/end biome ids classify via hardcoded sets). Groups of 5+ slice into
  stat-ordered segments (strongest -> pinnacle, cuts snap to score-change points so
  exact ties stay whole); groups of 2-4 space evenly across the ladder; a lone
  dimension sits mid-ladder; a generator guard fails if any multi-mob dimension
  collapses into one tier. Run the generator to see the band map - never hardcode it.
  Band membership is data - never hand-assign a tier. `canOpen` blocks players who
  have not unlocked that tier. `settings.color`, the gem pool, the flavor stack,
  `settings.size` and every reward magnitude/count come from `$themeIdentity` +
  `$tierProfile` - never hand-tune one gateway. Each gateway also needs its name
  lang key: Gateways translates the id with ':' -> '.' (its own lang does
  `gateways.basic/blaze`, Apotheosis does `apotheosis.tiered/frontier`), so the
  generator emits `tensura_apotheosis.elite/<mob>` = "<Display> Gateway" - without
  it the gate pearl tooltip shows the raw key. `settings.size` follows the tier
  (small/small/medium/medium/large - the same mapping Apotheosis' tiered defaults
  use) and is the gateway's base-texture selector: the gate pearl model picks
  `gate_pearl_small/med/large` by the gateway's size. Rewards mirror Apotheosis'
  tiered scaling: `gateways:counted` wraps the affix_item and gem rewards (count =
  number of rolls) and dust/stack counts follow their 16/24/48/64 curve, all driven
  by `$tierProfile`. The `elite` field must match a shipped
  `tensura_apotheosis:<mob>` elite.
- **Never add `rarities` to the herald wave's `apotheosis:affix` modifier.** Its
  AffixData codec resolves rarity values at decode time, but Gateways' registry
  reloads before Apotheosis' rarity registry binds - all 38 gateways then fail with
  "unbound value: apotheosis:common". Omitting the field falls back to Apotheosis'
  world-tier-weighted rarity random, which is tier-aware for free. Static bands
  belong only in elite `affix_data` and the `affix_item` reward (lazy holders,
  proven safe). See the note at the gateway loop.
- Elite banding is a **contiguous suffix** of the tier ladder: `weights` and
  `valid_gear_sets` list only the elite's tier and above (craig.json-style key
  omission - missing tiers weigh zero), and `affix_data.rarities` +
  `affix_chance` come from `$tierProfile`. `allElitesLoad` asserts the suffix
  shape and a non-collapsed ladder; `allGatewaysLoad` asserts all 5 tiers are
  covered and at least 10 distinct boss-bar colors (the regression both guards
  is the old uniform `#B02E26`, mythic-everywhere design).

---

## Branch sync (do this first)

Before starting any new development in this repo:

1. `git status` — confirm what uncommitted work exists. Never disturb or sweep it.
2. `git pull --ff-only` — sync with the remote. A fast-forward is the normal case,
   because we push straight to the shared branch.
3. If the pull would **not** fast-forward (the branch has diverged), or if it would
   collide with local uncommitted work, **stop and ask** rather than merging, rebasing
   or stashing on your own.

## Scope doctrine

- **One mod + bundled datapack.** No KubeJS scripts.
- **Data-driven over code** — prefer JSON/datapack content to Java when both work.
- **Public API only, no mixins** in the colony-gear-style addon mods. Sanctioned
  exception: `ds-tensura-fix` (tensura-dragon-skin-rebalance), a mixin fix mod by design.

## Verification

- Verify with the in-repo GameTest run (`./gradlew runGametest`, or `runGameTestServer`).
  Repos without GameTests verify with `./gradlew build`. Never verify against a live
  CurseForge instance.
- Keep `expectedTests` in `build.gradle` in sync with the `@GameTest` methods.
- **Client-render features cannot be proven headless.** The honest scope is
  "build green + the user's in-game check" - say so rather than overclaiming.


## Build environment

- JDK 21 (`JAVA_HOME` -> JDK 21); the toolchain targets Java 21.
- The Gradle wrapper is committed. `./gradlew build` compiles, `./gradlew runGametest`
  (or `runGameTestServer`) runs the GameTest server.
- `libs/` (compile-only) and `testmods/` (runtime closure) are **gitignored** staging
  folders. Never commit third-party jars.
- On `run/` file-lock errors, kill any stale `java` process and retry.

## PowerShell quirks (this org is developed on Windows)

- A `git push` "error" in PowerShell is usually stderr passthrough — success is the
  `hash..hash  branch -> branch` line, not the absence of red text.
- `New-Object Type(args)` breaks on arithmetic — use `::new()` or a parenthesized
  argument.

## Git & release

- Remote is SSH: `git@github.com:HexaHexes/<name>.git`.
- Push only after tests are green **and** the user has given the go.
- Never sweep the user's uncommitted work into a commit — commit only what is intended.
- Commit subjects: concise, imperative.

## House style

- **No icons** unless the user explicitly asks. Icons are being removed across all repos.
- **No emojis, ever.** Prohibited in all code, docs, configs, and commit messages.
- READMEs are **player-facing only**: pitch, mechanics in plain terms, in-game usage,
  required/optional dependencies, credits, license. No build internals, no class names.
- All repos are **LGPL v2.1**, copyright Bitwise Madness. `NOTICE` / attribution files
  are mandatory and must never be deleted.
- Deploy to the Tensura RPG instance **only** when the user asks.

## CI & release model

- `ci.yml` currently enforces the **version-bump guard only**: a beta or stable bump must
  change the version file and nothing else; an `-alpha` bump may bundle work; a PR labelled
  `breaking` forces the next clean bump to raise the major or minor version.
- **Build and GameTest jobs are deferred.** Some dependencies (ManasCore's jar-in-jar
  sub-modules, plus a few pack mods) are not resolvable from a clean checkout via
  CurseMaven alone, so compiling in CI would fail every run. They land once a jar-fetch
  step exists. Until then, run `./gradlew build` and the GameTest task locally before
  pushing.
- `release.yml` is **not yet enabled for the gradle mod repos** - it needs a CI-built jar
  to attach. The datapack repo is the exception: its "build" is a zip, so it has a working
  `release.yml` that tags and publishes on a version bump.
- `version-bump.yml` opens an isolated version-bump PR (only the version file changes).
- CurseForge publishing (`curseforge.yml`) is **deferred** until a mod has a CurseForge
  listing and the `CURSEFORGE_PROJECT_ID` / `CURSEFORGE_API_KEY` secrets. Do not add it
  speculatively.
- Where a release workflow exists, alpha/nightly publishing is opt-in via the
  `ALPHA_RELEASES` repo variable and is **off** by default.

