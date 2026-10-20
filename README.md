# Tensura Apotheosis

Tensura-themed **affixes and socket gems** for [Apotheosis](https://www.curseforge.com/minecraft/mc-mods/apotheosis) 8.8 (1.21.1, NeoForge).

The mod is **pure data**: no mixins, no gameplay code. Every affix and gem is a
JSON file loaded by Apotheosis' Placebo dynamic registries, gem stacks reuse
Apotheosis' shared `apotheosis:gem` item, and the Java side exists only for the
in-repo gametest harness.

## Content

**51 affixes** (rare→mythic by default, procs from uncommon; value curves mirror
Apotheosis' own affixes for the shared Apothic attributes):

| Group | Affixes |
|---|---|
| Melee attribute | flame_blade, frost_blade, armor_piercing, lethal, keen, soul_thief, predator's_edge |
| Melee procs | frostbite, infective, venomous, corrosive (DoTs) · paralyzing, silencing, terrifying, stone_gaze (hard-CC, mutually exclusive) |
| Ranged | hawk_eye, quick_draw, warped · frostbite, infective (arrow procs) |
| Armor | magicule_fortified/attuned, aura_attuned/blessed, spiritually_fortified, mending_soul, arcane_barrier, iron_will, warded, sanctified, indomitable, regenerative, untouchable, ghost_step, concealed · magicule_surge (hurt-self proc) |
| Shield | bulwark, aegis, wardstone |
| Breaker | deep_delver, learned, earth_affinity |
| Generic | swift_chant, efficient_casting, prodigy, mastered, quickened, danger_sense, analyst, far_sight, heatseeker |

Tensura's own systems carry the flavor: magicule/aura/spiritual economy,
dodge suite, presence concealment/sense, analysis, chant & cost multipliers,
and Tensura mob effects (`tensura:frost`, `tensura:infection`, `tensura:silence`,
`tensura:petrification`, ...) as proc lines. Ultimate/transformation effects are
deliberately excluded — far too strong for affixes.

**14 gems** (cracked→perfect purity scaling):

- **Core (8)**: magicule, aura, soul, predator, doppelganger, analyst, greedy, swift
- **Unique (5, `min_purity: flawed`, depth-tiered frontier→pinnacle weights)**: flame/frost/storm dragonscale, Holy Core, Spirit Core
- **Ultima** (`min_purity: flawless`, unique): crit + barrier + mastery + cooldown spread

Each gem's bonuses occupy **disjoint equipment classes** (Apotheosis rejects
overlapping gem classes within one gem).

## Two-handed weapons

Tensura's two-handed weapons (`TwoHandedSwordItem` — greatsword, katana, longsword,
odachi, scythe, spear, tachi, custom blades) all extend vanilla `SwordItem`, and
Apotheosis' `melee_weapon` category predicate is `SWORD_DIG` ability **or** base
attack damage > 1. They therefore receive all melee affixes and melee gem lines
with no extra tagging — pinned by the gametest below. Shield affixes are
inherently a one-handed playstyle (Tensura disables shields in two-handed stance).

## Verification

```
gradle runGametest          # requires JAVA_HOME -> JDK 21
```

Three GameTests (graded by the build, `expectedTests = 3`):

1. `twoHandedWeaponsCategorizeAsMelee` — every registered `TwoHandedSwordItem`
   resolves to `apotheosis:melee_weapon`.
2. `allAffixesLoad` — all 51 data files parse into `AffixRegistry`.
3. `allGemsLoad` — all 14 gems parse into `GemRegistry`, stack onto
   `apotheosis:gem`, and respect `min_purity`.

Client cosmetics (gem textures) can't be proven headless — check in-game.

## In-game checklist

```
/give apotheosis:gem
```
- Gems also appear in Apotheosis' creative tab (all purities ≥ min_purity).
- Affix items: any Apotheosis boss/affix loot now rolls the Tensura pool; to
  inspect directly, use Apotheosis' own commands (`/apotheosis` command tree)
  or reroll/reforge a dropped affix item until a Tensura name shows
  (Flame Blade, Silencing "of Severance", Concealed, ...).
- Gem models resolve through Apotheosis' hardcoded model namespace, so gem
  model JSONs ship under `assets/apotheosis/models/item/gems/` while textures
  and lang stay in `assets/tensura_apotheosis/`.

## Regenerating assets

Content is generated from single-source tables:

```
powershell -File tools/generate-content.ps1   # affixes + gems + lang + models
powershell -File tools/generate-textures.ps1  # 14 gem PNGs
```

Schema gotchas (both caught by the gametest, don't regress them):

- Affix `min`/`max` ranges are decoded as **step functions** with an implicit
  step of `0.01`; `(max - min)` must be a multiple of that step (the mob_effect
  `duration`/`amplifier` objects accept an explicit `step`).
- Every `tensura:` attribute id must exist in Tensura's registry — there is no
  `tensura:lightning_resistance` (the storm dragonscale uses space resistance).
  When adding ids, verify against `TensuraAttributes` before shipping.
- Within one gem, bonuses' `gem_class` category sets must be **disjoint** or
  Apotheosis rejects the whole file.

