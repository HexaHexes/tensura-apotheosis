# Tensura Apotheosis

Adds a Tensura-themed set of **affixes, socket gems, elite bosses and gear** to
[Apotheosis](https://www.curseforge.com/minecraft/mc-mods/apotheosis) 8.8. Boss loot can
now roll Tensura-flavored magic items, Apotheosis' gem socketing gains a family of
Tensura gems, Tensura mobs get Apotheosis elite-boss variants (with summonable gateway
challenges to fight them directly), and Tensura equipment enters the boss loot pool.

## What it adds

**55 affixes** — magical weapon, armor, shield and utility effects that roll onto loot,
drawn straight from Tensura's systems (magicule/aura economy, the dodge suite, presence
concealment, chant and cost multipliers, and Tensura mob effects like frost, infection,
silence and petrification). A few examples:

- **Melee** — Flame Blade, Frost Blade, Armor Piercing, Keen, Soul Thief, plus damage-over-time
  (frostbite, infective, venomous) and crowd-control (paralyzing, silencing, terrifying,
  stone gaze) procs.
- **Ranged** — Hawk Eye, Quick Draw, Warped, plus arrow procs (frostbite, infective, venomous).
- **Armor** — Arcane Barrier, Iron Will, Warded, Sanctified, Ghost Step, Concealed,
  Regenerative, Untouchable, Restorative, and more.
- **Shield / Breaking / Any item** — Bulwark, Aegis, Reinforced, Deep Delver, Learned,
  Prodigy, Mastered, Danger Sense, Analyst, Entangling, and more.

Ultimate and transformation effects are deliberately left out — they'd be far too strong
on affixes.

**37 socket gems**, scaling from cracked to perfect purity:

- **Core (23):** magicule, aura, soul, predator, doppelganger, analyst, greedy, swift,
  barrier, shadow, vigil, stalwart, gale, earthen, fury, vigor, juggernaut, striker,
  precision, sprinter, brawler, mystic, executioner
- **Dragonscales (7):** flame, frost and storm, plus earth, darkness, wind and gravity
- **Holy (2):** Holy Core, Judgment Core
- **Spirit (3):** Spirit Core, Guardian Core, Mender Core
- **Ultima (2):** Ultima Gem and Sovereign Gem, top-tier gems combining crit, barrier,
  mastery and cooldown bonuses

**38 elite bosses** — rare, tiered variants of Tensura mobs, spawned by Apotheosis'
elite system:

- Each elite appears where its base mob naturally spawns, and only in that biome (no
  dimension-wide spawns). Spawned from eggs, spawners or summons never become elites —
  only natural spawns have a chance.
- The 38 are banded across Apotheosis' five world tiers by their actual stats — combat
  numbers (health, damage, size, enchantment, spawn rarity) combined with each mob's
  Tensura aura pool (AP), magicule pool (MP) and spiritual health — and stratified by
  spawn dimension: every non-overworld dweller (the underworld daemons, the nether's
  Salamander and Phantaspore, Barghest, the end's Winged Cat) is spread across the
  tiers instead of piling into one. The tiers cut both across and inside mob families:
  a haven band holds the low-MP lizards and insects, while a family's strongest member
  can sit far above its siblings (Arch Daemon's 140,000 MP against Lesser Daemon's
  8,000). A band's elite only spawns in worlds at or above its tier.
- Their gear rolls an increasing affix rarity ladder that tracks the tier: common and
  uncommon affixes at haven, up through rare and epic to mythic at summit and pinnacle.
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

**38 elite gateways** — summon the elites directly:

- Craft a gate pearl from ender pearls, diamonds and Apotheosis gem dust (one recipe per
  elite), place it on the ground, and a gateway opens.
- Every gateway is themed to its mob: the boss bar wears the family's color (slime green
  for a Slime gate, wasp amber for an Army Wasp gate, gold for a Unicorn gate), and the
  clear rewards draw from that family's gem pool plus a flavor stack — blaze powder from
  a Salamander gate, spider eyes from a Spider gate, dragon's breath from a Serpent gate.
- Each gateway runs two waves: a herald pack of the mob's lesser kin, then the elite
  itself as a full boss with boosted health and damage.
- Rewards scale with the gateway's world tier, counted roll by counted roll in the
  same shape as Apotheosis' own tiered gateways: affix items go from two
  common/uncommon rolls at haven to five guaranteed mythic rolls at pinnacle, gems
  from three cracked/normal rolls to ten perfect ones, and gem dust (8 → 64), flavor
  stacks and experience all grow with the tier. The gateway reads its tier too — the
  gate pearl's texture and the ring size follow small → medium → large as the tier
  climbs, the same mapping the default tiered gateways use. A gateway only opens once
  you have unlocked its tier — work up the built-in Apotheosis gateways to reach the
  higher bands. The gateway allows up to three failures before it collapses.
- Requires [Gateways to Eternity](https://www.curseforge.com/minecraft/mc-mods/gateways-to-eternity);
  without it, everything else still works.

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

To fight an elite directly, craft its gate pearl (ender pearls, diamonds and Apotheosis
gem dust — one recipe per elite, all 38 in the recipe book) and place it on the ground
to open the gateway. Needs [Gateways to Eternity](https://www.curseforge.com/minecraft/mc-mods/gateways-to-eternity).

## Dependencies

| | Mod | Why |
|---|---|---|
| **Required** | [Apotheosis](https://www.curseforge.com/minecraft/mc-mods/apotheosis) 8.8+ | The affix and gem system |
| **Required** | Apothic Attributes | Apotheosis' attribute library |
| **Required** | Tensura: Reincarnated | The themed effects and attributes |
| **Optional** | [Gateways to Eternity](https://www.curseforge.com/minecraft/mc-mods/gateways-to-eternity) | The 38 summonable elite gateways |

Made for the **Tensura RPG** pack (Minecraft 1.21.1, NeoForge).

## Credits

- Built on [Apotheosis](https://www.curseforge.com/minecraft/mc-mods/apotheosis) and Apothic Attributes.
- All Tensura flavor is drawn from Tensura: Reincarnated.

## License

GNU Lesser General Public License v2.1 — see [`LICENSE`](LICENSE).
Copyright (c) 2026 Bitwise Madness.
