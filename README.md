# Tensura Apotheosis

Adds a Tensura-themed set of **affixes, socket gems, elite bosses and gear** to
[Apotheosis](https://www.curseforge.com/minecraft/mc-mods/apotheosis) 8.8. Boss loot can
now roll Tensura-flavored magic items, Apotheosis' gem socketing gains a family of
Tensura gems, Tensura mobs get Apotheosis elite-boss variants, and Tensura equipment
enters the boss loot pool.

## What it adds

**51 affixes** — magical weapon, armor, shield and utility effects that roll onto loot,
drawn straight from Tensura's systems (magicule/aura economy, the dodge suite, presence
concealment, chant and cost multipliers, and Tensura mob effects like frost, infection,
silence and petrification). A few examples:

- **Melee** — Flame Blade, Frost Blade, Armor Piercing, Keen, Soul Thief, plus damage-over-time
  (frostbite, infective, venomous) and crowd-control (paralyzing, silencing, terrifying,
  stone gaze) procs.
- **Ranged** — Hawk Eye, Quick Draw, Warped, plus arrow procs.
- **Armor** — Arcane Barrier, Iron Will, Warded, Sanctified, Ghost Step, Concealed,
  Regenerative, Untouchable, and more.
- **Shield / Breaking / Any item** — Bulwark, Aegis, Deep Delver, Learned, Prodigy,
  Mastered, Danger Sense, Analyst, and more.

Ultimate and transformation effects are deliberately left out — they'd be far too strong
on affixes.

**14 socket gems**, scaling from cracked to perfect purity:

- **Core (8):** magicule, aura, soul, predator, doppelganger, analyst, greedy, swift
- **Unique (5):** flame, frost and storm dragonscale, Holy Core, Spirit Core
- **Ultima:** a top-tier gem combining crit, barrier, mastery and cooldown bonuses

**38 elite bosses** — rare, tiered variants of Tensura mobs, spawned by Apotheosis'
elite system:

- Each elite appears where its base mob naturally spawns, and only in that biome (no
  dimension-wide spawns). Spawned from eggs, spawners or summons never become elites —
  only natural spawns have a chance.
- Elites span Apotheosis' five rarity tiers (haven through pinnacle), from commons like
  Direwolf or Giant Ant to rare pinnacle threats like Arch Daemon and Tempest Serpent.
- They carry bonus health, damage, knockback resistance and size, and wear themed gear
  drawn from their identity (daemons wield staves and grimoires, spiders wear knight-spider
  carapace, beasts carry beast-horn and blade-tiger weapons, and so on).
- On a player kill the elite's held weapon always drops, so the loot is real.

**80 gear sets** — Tensura equipment wired into Apotheosis' boss gear:

- Five tiered sets (haven through pinnacle) join the vanilla boss loot pools, so the
  built-in Apotheosis bosses can now drop Tensura weapons and armor alongside the
  vanilla diamond/netherite gear.
- Themed sets (daemon, scaled, spider, insect, beast, aquatic, and more) dress the 38
  elites in gear that matches their identity and Tensura's own material progression —
  monster leather in the early tiers up to orichalcum and hihiirokane at the pinnacle.
- Pegasus, Unicorn and Hover Lizard wear horse barding (the only elites whose gear is
  visibly rendered); everyone else's elite gear is mechanical — real armor points and a
  real weapon, even where the model doesn't draw it.

## Two-handed weapons count as melee

Tensura's two-handed weapons (greatsword, katana, longsword, odachi, scythe, spear,
tachi and custom blades) receive all melee affixes and melee gem lines automatically —
no tagging needed.

## Getting it in-game

Nothing to configure. Tensura affixes roll onto Apotheosis boss and affix loot
naturally. To browse gems:

```
/give apotheosis:gem
```

Gems also appear in Apotheosis' creative tab. To find affix items, reroll or reforge a
dropped affix item (or use Apotheosis' own `/apotheosis` commands) until a Tensura name
shows up — *Flame Blade*, *Silencing "of Severance"*, *Concealed*, and so on.

## Dependencies

| | Mod | Why |
|---|---|---|
| **Required** | [Apotheosis](https://www.curseforge.com/minecraft/mc-mods/apotheosis) 8.8+ | The affix and gem system |
| **Required** | Apothic Attributes | Apotheosis' attribute library |
| **Required** | Tensura: Reincarnated | The themed effects and attributes |

Made for the **Tensura RPG** pack (Minecraft 1.21.1, NeoForge).

## Credits

- Built on [Apotheosis](https://www.curseforge.com/minecraft/mc-mods/apotheosis) and Apothic Attributes.
- All Tensura flavor is drawn from Tensura: Reincarnated.

## License

GNU Lesser General Public License v2.1 — see [`LICENSE`](LICENSE).
Copyright (c) 2026 Bitwise Madness.
