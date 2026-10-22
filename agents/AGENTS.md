# Agent rules and conventions — Tensura Apotheosis

**Repo:** `HexaHexes/tensura-apotheosis` · **Mod ID:** `tensura_apotheosis` · **Branch:** `master`
**Platform:** Minecraft 1.21.1 / NeoForge 21.1.259 / JDK 21 · **Jar:** `build/libs/tensura-apotheosis-<version>.jar`

Tensura-themed affixes, socket gems, elite bosses and gear sets for Apotheosis 8.8,
delivered as **datapack data** (Placebo dynamic registries) - no mixins. The Java side
holds one small runtime (`EliteNamePools`: draws a themed name per elite spawn from
per-mob pool data - Apotheosis' own name generation is global-config only and cannot
be themed per mob) plus the in-repo gametest harness. Public APIs only.

**Dependencies**
- Required: Apotheosis (`apotheosis`), Apothic Attributes (`apothic_attributes`), Tensura (`tensura`).
- Optional: Gateways to Eternity (`gateways`) - the elite gateways and their recipes only
  load when it is present (recipes carry a `mod_loaded` condition).

**Test:** `./gradlew runGametest` - 8 tests (`twoHandedWeaponsCategorizeAsMelee`,
`allAffixesLoad`, `allGemsLoad`, `allElitesLoad`, `eliteBonusLootLoads`,
`eliteNamePoolsLoad`, `allGearSetsLoad`, `allGatewaysLoad`).
`expectedTests = 8`.

**Single source of truth:** `tools/generate-content.ps1` produces all affixes, gems,
gear sets, elites, elite bonus loot tables, gateways, gateway recipes, lang and models. **Textures are never
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
- Elite combat stats come from the tier-scaled elite ladder (`$tierProfile.elite*`
  fields), not the theme table - the theme table only scores banding and keeps
  `effects`/`success_chance`. HP, damage, knockback, scale, enchant chance/levels and
  the AP/MP `add_multiplied_total` bonuses on `tensura:max_aura`/`tensura:max_magicule`
  (+50% at haven up to +500% at pinnacle) rise strictly by tier, and **adjacent tiers
  must never overlap** - `allElitesLoad`'s monotone guard fails otherwise.
- Elite `spawn_conditions` exclude `spawn_egg` **only** - the same pattern Apotheosis'
  own elites use; biome constraints, tier weights and `success_chance` all run through
  Apotheosis' native spawn pipeline (`ApothMobEvents`).
- Elite bonus loot lives in `data/tensura_apotheosis/loot_table/bonus/` - **singular**
  `loot_table` (the 1.21 datapack layout, not `loot_tables`). Each elite references two
  tables through `basicData.bonus_loot`: a world-tier table (gem dust, diamonds and an
  `apotheosis:random_gem` roll, scaled by the `$tierProfile.bonus*` weights) and a
  family table whose extra entries must carry an explicit `type: minecraft:item`.
  `eliteBonusLootLoads` asserts all 38 elites reference exactly 2 of the 19 tables and
  that every referenced file ships pools.
- Elite name pools ship from `tools/name-pools.json` (40-50 fantasy names per mob,
  globally unique - the generator asserts both) into
  `data/tensura_apotheosis/elite_names/<mob>.json`. The `EliteNamePools` runtime
  reloads them and draws one name per elite spawn (disk loads keep their saved name);
  the drawn name is tinted with the elite's top rarity color (`LootRarity.color()`),
  falling back to plain text when the rarity set is missing.
  `eliteNamePoolsLoad` asserts every elite has at least 40 loaded names.
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
  by `$tierProfile` - so are `settings.soundtrack` (frontier..pinnacle reuse
  Apotheosis' four tier discs; haven uses `minecraft:music_disc.cat` - vanilla
  disc sound events register with dotted paths) and the recipe cost
  (`recipeDiamonds`/`recipeDust`, pearls fixed at 2; a 3x3 grid holds 9 items,
  so an unknown count pair throws at generation). The pinnacle row's
  `chaseGateway` field names a bonus gate pearl added to every pinnacle
  gateway's rewards: currently `endless/horde`, a mixed-horde
  `gateways:endless` survival gateway emitted as a special entry after the
  roster loop (base_wave + after_every_n_waves modifiers escalating
  fodder -> bruisers -> daemons with stacked health scaling; gold color
  outside the family palette, reward-only - no recipe). The gametest counts
  39 gateways: 38 tiered + the endless chase one. The `elite` field must
  match a shipped `tensura_apotheosis:<mob>` elite.
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
- **Version bump on finish:** always bump the version when a new feature or bugfix
  is finished.
- **One feature or bugfix per branch:** develop each on its own feature branch and
  merge it into main/master through a pull request with a **squash merge**. The PR
  body must detail the work done, following `.github/pull_request_template.md`.

## House style

- **No icons** unless the user explicitly asks. Icons are being removed across all repos.
- **No emojis, ever.** Prohibited in all code, docs, configs, and commit messages.
- READMEs are **player-facing only**: pitch, mechanics in plain terms, in-game usage,
  required/optional dependencies, credits, license. No build internals, no class names.
- All repos are **LGPL v2.1**, copyright Bitwise Madness. `NOTICE` / attribution files
  are mandatory and must never be deleted.
- Deploy to the Tensura RPG instance **only** when the user asks.

## Naming

- **Java base package:** `org.bitwisemadness.<project-name>` - repo name lowercased,
  separators stripped (e.g. `ds-tensura-fix` -> `org.bitwisemadness.dstensurafix`).
  The Maven `group` in `build.gradle` matches (`org.bitwisemadness`).
- Keep repo name, folder name, `mod_id`, and the Java package aligned; the rename
  checklist in the template's `README.md` walks the whole set.
- **The template (`neoforge-mod-template`) is canonical for org-wide rules.** When a
  rule is added or changed in any repo's `agents/AGENTS.md`, mirror the change into
  the template so new repos inherit it.

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

