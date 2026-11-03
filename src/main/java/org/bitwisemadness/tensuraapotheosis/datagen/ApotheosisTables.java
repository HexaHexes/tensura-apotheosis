package org.bitwisemadness.tensuraapotheosis.datagen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The shared tables and shapes every datagen section reads - the direct port
 * of the category tables, theme roster, world-tier profile and theme identity
 * blocks from the retired {@code tools/generate-content.ps1} (issue #29).
 *
 * <p>Everything here is immutable and order-stable: the sections emit JSON in
 * the exact order these tables declare, and the writer normalizes key order
 * anyway.</p>
 */
public final class ApotheosisTables {

    private ApotheosisTables() {
    }

    /** The five world tiers, weakest to strongest - the banding ladder. */
    public static final List<String> TIERS = List.of("haven", "frontier", "ascent", "summit", "pinnacle");

    /** Apotheosis gear categories, as the affix and gem tables reference them. */
    public static final List<String> MEA = List.of("apotheosis:melee_weapon", "apotheosis:trident");
    public static final List<String> BOW = List.of("apotheosis:bow");
    public static final List<String> BODY = List.of("apotheosis:chestplate", "apotheosis:leggings");
    public static final List<String> LB = List.of("apotheosis:leggings", "apotheosis:boots");
    public static final List<String> ARMO = List.of("apotheosis:helmet", "apotheosis:chestplate",
            "apotheosis:leggings", "apotheosis:boots");
    public static final List<String> SHLD = List.of("apotheosis:shield");
    public static final List<String> BRK = List.of("apotheosis:breaker");
    public static final List<String> ALL = List.of("apotheosis:bow", "apotheosis:breaker",
            "apotheosis:helmet", "apotheosis:chestplate", "apotheosis:leggings", "apotheosis:boots",
            "apotheosis:shield", "apotheosis:trident", "apotheosis:melee_weapon", "apotheosis:shears");

    /** A numeric range: {@code step} present means the value draws on a grid. */
    public record RangeVal(double min, double max, Double step) {
    }

    /** One rarity-keyed value row of a proc affix: duration/amplifier/cooldown. */
    public record ProcSpec(Object duration, Object amplifier, int cooldown) {
    }

    /** Placebo weight block shared by affix definitions and gem weights. */
    public record WeightSpec(double quality, int weight) {
    }

    /** Per-world-tier gem weight block for the unique-tier gems. */
    public record TierWeights(WeightSpec frontier, WeightSpec ascent, WeightSpec summit, WeightSpec pinnacle) {
    }

    /** A gem bonus line: attribute roll scoped to a gem class. */
    public record GemLine(String gemClass, String attribute, String operation, double[] purity) {
    }

    /** One world-tier profile row: drives elites, gateways, rewards and bonus loot. */
    public record TierProfile(List<String> eliteRarities, double affixChance, List<String> rewardRarities,
            List<String> purities, int dust, int heraldXp, int bossXp, int finalXp, int stackCount,
            int itemCount, int gemCount, String size, String soundtrack, int recipeDiamonds, int recipeDust,
            double[] eliteHp, double[] eliteDmg, double eliteKb, double[] eliteScale, double eliteEnch,
            int[] eliteEnchLvl, double elitePoolMult, int[] bonusDust, int bonusGemWeight,
            int bonusDiamondWeight, String chaseGateway) {
    }

    /** Armor material spec: a plain material, or a monster-leather grade. */
    public record ArmorSpec(String mat, String grade) {
    }

    /**
     * One mob-family theme: the gear tables (per world tier) that
     * {@code GearSetContent} emits, the combat stat bands and effects that
     * {@code EliteContent} bands on, and the barding override.
     */
    public record Theme(String key, List<ArmorSpec> armor, List<List<String>> weapons, String barding,
            List<String> effects, double[] hp, double[] dmg, double[] scale, double ench, double success) {
    }

    /** Theme identity for gateways and family bonus loot: bar color, reward gems, flavor stack. */
    public record ThemeIdentity(String color, List<String> gems, String stack) {
    }

    /** Gem classes mirror Apotheosis' canonical entries (key AND types), so tooltips reuse its lang. */
    public static final Map<String, Map.Entry<String, List<String>>> GEM_CLASSES = Map.of(
            "body", Map.entry("core_armor", List.of("apotheosis:chestplate", "apotheosis:leggings")),
            "helmet", Map.entry("helmet", List.of("apotheosis:helmet")),
            "boots", Map.entry("boots", List.of("apotheosis:boots")),
            "weapon", Map.entry("light_weapon", MEA),
            "bow", Map.entry("bow", BOW),
            "tool", Map.entry("tools", List.of("apotheosis:breaker", "apotheosis:shears")),
            "shield", Map.entry("shield", SHLD));

    /** Flat weight block for the common gems. */
    public static final WeightSpec FLAT_WEIGHTS = new WeightSpec(0.1, 100);

    /** Per-world-tier weight block for the unique-tier gems. */
    public static final TierWeights TIER_WEIGHTS = new TierWeights(
            new WeightSpec(0.05, 2), new WeightSpec(0.08, 6), new WeightSpec(0.1, 15), new WeightSpec(0.15, 30));

    /** Gem purity ladder keys, cracked through perfect. */
    public static final List<String> PURITIES = List.of(
            "cracked", "chipped", "flawed", "normal", "flawless", "perfect");

    /**
     * The fourteen mob-family themes. Armor and weapons index the five world
     * tiers (haven..pinnacle); barding themes override the chest slot with
     * horse armor; the stat bands score the tier banding.
     */
    public static final List<Theme> THEMES = List.of(
            new Theme("demon",
                    List.of(new ArmorSpec("leather", "d"), new ArmorSpec("leather", "c"),
                            new ArmorSpec("pure_magisteel", null), new ArmorSpec("orichalcum", null),
                            new ArmorSpec("hihiirokane", null)),
                    List.of(List.of("tensura:low_magic_staff"),
                            List.of("tensura:low_magic_staff", "tensura:iron_katana"),
                            List.of("tensura:medium_magic_staff", "tensura:low_magisteel_katana"),
                            List.of("tensura:high_magic_staff", "tensura:high_magisteel_katana"),
                            List.of("tensura:grimoire_a", "tensura:grimoire_special_a", "tensura:orichalcum_katana")),
                    "", List.of("minecraft:fire_resistance", "minecraft:night_vision"),
                    new double[] { 40, 70 }, new double[] { 0.4, 0.7 }, new double[] { 1.2, 1.5 }, 0.4, 0.01),
            new Theme("salamander",
                    List.of(new ArmorSpec("leather", "d"), new ArmorSpec("leather", "c"),
                            new ArmorSpec("high_magisteel", null), new ArmorSpec("adamantite", null),
                            new ArmorSpec("hihiirokane", null)),
                    List.of(List.of("tensura:wooden_scythe"), List.of("tensura:iron_scythe"),
                            List.of("tensura:low_magisteel_scythe"), List.of("tensura:adamantite_scythe"),
                            List.of("tensura:hihiirokane_scythe", "tensura:blade_tiger_scythe")),
                    "", List.of("minecraft:fire_resistance"),
                    new double[] { 35, 60 }, new double[] { 0.35, 0.6 }, new double[] { 1.1, 1.3 }, 0.35, 0.012),
            new Theme("scaled",
                    List.of(new ArmorSpec("leather", "d"), new ArmorSpec("leather", "c"),
                            new ArmorSpec("serpent_scalemail", null), new ArmorSpec("serpent_scalemail", null),
                            new ArmorSpec("charybdis_scalemail", null)),
                    List.of(List.of("tensura:wooden_spear"), List.of("tensura:iron_spear"),
                            List.of("tensura:low_magisteel_spear"), List.of("tensura:mithril_spear"),
                            List.of("tensura:orichalcum_spear", "tensura:vortex_spear")),
                    "", List.of("minecraft:fire_resistance"),
                    new double[] { 45, 80 }, new double[] { 0.4, 0.75 }, new double[] { 1.3, 1.7 }, 0.4, 0.01),
            new Theme("lizard",
                    List.of(new ArmorSpec("leather", "d"), new ArmorSpec("leather", "c"),
                            new ArmorSpec("serpent_scalemail", null), new ArmorSpec("serpent_scalemail", null),
                            new ArmorSpec("serpent_scalemail", null)),
                    List.of(List.of("tensura:wooden_short_sword"), List.of("tensura:iron_short_sword"),
                            List.of("tensura:low_magisteel_short_sword"), List.of("tensura:pure_magisteel_kodachi"),
                            List.of("tensura:orichalcum_kodachi")),
                    "", List.of(),
                    new double[] { 25, 45 }, new double[] { 0.25, 0.45 }, new double[] { 0.9, 1.1 }, 0.25, 0.015),
            new Theme("hover",
                    List.of(),
                    List.of(List.of("tensura:wooden_short_sword"), List.of("tensura:iron_short_sword"),
                            List.of("tensura:low_magisteel_short_sword"), List.of("tensura:pure_magisteel_kodachi"),
                            List.of("tensura:orichalcum_kodachi")),
                    "iron", List.of(),
                    new double[] { 25, 45 }, new double[] { 0.25, 0.45 }, new double[] { 0.9, 1.1 }, 0.25, 0.015),
            new Theme("spider",
                    List.of(new ArmorSpec("leather", "c"), new ArmorSpec("leather", "c"),
                            new ArmorSpec("knight_spider_carapace", null), new ArmorSpec("knight_spider_carapace", null),
                            new ArmorSpec("knight_spider_carapace", null)),
                    List.of(List.of("tensura:spider_dagger", "tensura:short_bow"),
                            List.of("tensura:spider_dagger", "tensura:short_bow"),
                            List.of("tensura:spider_dagger", "tensura:spider_bow"),
                            List.of("tensura:spider_dagger", "tensura:long_spider_bow"),
                            List.of("tensura:spider_dagger", "tensura:war_spider_bow")),
                    "", List.of("minecraft:night_vision"),
                    new double[] { 30, 55 }, new double[] { 0.3, 0.55 }, new double[] { 1.0, 1.3 }, 0.3, 0.012),
            new Theme("insect",
                    List.of(new ArmorSpec("leather", "c"), new ArmorSpec("leather", "c"),
                            new ArmorSpec("ant_carapace", null), new ArmorSpec("ant_carapace", null),
                            new ArmorSpec("ant_carapace", null)),
                    List.of(List.of("tensura:short_bow"),
                            List.of("tensura:short_bow", "tensura:centipede_dagger"),
                            List.of("tensura:ant_crossbow", "tensura:centipede_dagger"),
                            List.of("tensura:ant_crossbow", "tensura:centipede_dagger"),
                            List.of("tensura:ant_crossbow", "tensura:centipede_dagger")),
                    "", List.of(),
                    new double[] { 28, 50 }, new double[] { 0.28, 0.5 }, new double[] { 1.0, 1.2 }, 0.28, 0.014),
            new Theme("beast",
                    List.of(new ArmorSpec("leather", "d"), new ArmorSpec("leather", "c"),
                            new ArmorSpec("leather", "b"), new ArmorSpec("leather", "a"),
                            new ArmorSpec("leather", "special_a")),
                    List.of(List.of("tensura:wooden_great_sword"), List.of("tensura:iron_great_sword"),
                            List.of("tensura:beast_horn_spear", "tensura:low_magisteel_great_sword"),
                            List.of("tensura:blade_tiger_scythe", "tensura:mithril_great_sword"),
                            List.of("tensura:adamantite_great_sword", "tensura:blade_tiger_scythe")),
                    "", List.of(),
                    new double[] { 35, 65 }, new double[] { 0.35, 0.6 }, new double[] { 1.1, 1.4 }, 0.35, 0.012),
            new Theme("aquatic",
                    List.of(new ArmorSpec("leather", "c"), new ArmorSpec("leather", "c"),
                            new ArmorSpec("serpent_scalemail", null), new ArmorSpec("serpent_scalemail", null),
                            new ArmorSpec("serpent_scalemail", null)),
                    List.of(List.of("tensura:wooden_spear"), List.of("tensura:iron_spear"),
                            List.of("tensura:low_magisteel_spear"), List.of("tensura:silver_spear"),
                            List.of("tensura:orichalcum_spear")),
                    "", List.of("minecraft:water_breathing"),
                    new double[] { 30, 55 }, new double[] { 0.3, 0.5 }, new double[] { 1.1, 1.4 }, 0.3, 0.014),
            new Theme("armorsaurus",
                    List.of(new ArmorSpec("leather", "b"), new ArmorSpec("leather", "b"),
                            new ArmorSpec("armorsaurus_scalemail", null), new ArmorSpec("armorsaurus_scalemail", null),
                            new ArmorSpec("armorsaurus_scalemail", null)),
                    List.of(List.of("tensura:iron_great_sword"), List.of("tensura:iron_great_sword"),
                            List.of("tensura:adamantite_great_sword"), List.of("tensura:adamantite_great_sword"),
                            List.of("tensura:adamantite_great_sword", "tensura:hihiirokane_great_sword")),
                    "", List.of("minecraft:fire_resistance"),
                    new double[] { 50, 90 }, new double[] { 0.45, 0.8 }, new double[] { 1.4, 1.8 }, 0.45, 0.008),
            new Theme("slime",
                    List.of(new ArmorSpec("leather", "c"), new ArmorSpec("leather", "c"),
                            new ArmorSpec("pure_magisteel", null), new ArmorSpec("pure_magisteel", null),
                            new ArmorSpec("orichalcum", null)),
                    List.of(List.of("tensura:slime_staff"), List.of("tensura:slime_staff"),
                            List.of("tensura:slime_staff"), List.of("tensura:slime_staff"),
                            List.of("tensura:slime_staff", "tensura:high_magic_staff")),
                    "", List.of(),
                    new double[] { 40, 75 }, new double[] { 0.3, 0.55 }, new double[] { 1.2, 1.6 }, 0.3, 0.01),
            new Theme("spore",
                    List.of(new ArmorSpec("leather", "c"), new ArmorSpec("leather", "c"),
                            new ArmorSpec("mithril", null), new ArmorSpec("mithril", null),
                            new ArmorSpec("orichalcum", null)),
                    List.of(List.of("tensura:low_magic_staff"), List.of("tensura:low_magic_staff"),
                            List.of("tensura:medium_magic_staff"), List.of("tensura:high_magic_staff"),
                            List.of("tensura:grimoire_b")),
                    "", List.of("minecraft:regeneration"),
                    new double[] { 30, 55 }, new double[] { 0.3, 0.5 }, new double[] { 1.0, 1.2 }, 0.3, 0.014),
            new Theme("orc",
                    List.of(new ArmorSpec("leather", "c"), new ArmorSpec("leather", "c"),
                            new ArmorSpec("silver", null), new ArmorSpec("adamantite", null),
                            new ArmorSpec("adamantite", null)),
                    List.of(List.of("tensura:wooden_great_sword"), List.of("tensura:iron_great_sword"),
                            List.of("tensura:iron_great_sword", "tensura:golden_great_sword"),
                            List.of("tensura:adamantite_great_sword"),
                            List.of("tensura:adamantite_great_sword", "tensura:orichalcum_great_sword")),
                    "", List.of(),
                    new double[] { 45, 80 }, new double[] { 0.45, 0.75 }, new double[] { 1.3, 1.6 }, 0.4, 0.01),
            new Theme("unicorn",
                    List.of(new ArmorSpec("leather", "b"), new ArmorSpec("leather", "b"),
                            new ArmorSpec("pure_magisteel", null), new ArmorSpec("mithril", null),
                            new ArmorSpec("orichalcum", null)),
                    List.of(List.of("tensura:wooden_spear"), List.of("tensura:iron_spear"),
                            List.of("tensura:unicorn_horn_spear"),
                            List.of("tensura:unicorn_horn_spear", "tensura:mithril_spear"),
                            List.of("tensura:unicorn_horn_spear", "tensura:orichalcum_spear")),
                    "iron", List.of("minecraft:fire_resistance"),
                    new double[] { 40, 70 }, new double[] { 0.4, 0.65 }, new double[] { 1.2, 1.5 }, 0.4, 0.01));

    /** Theme lookup by key. */
    public static Theme theme(String key) {
        return THEMES.stream().filter(t -> t.key().equals(key)).findFirst().orElseThrow();
    }

    /** Barding material per world tier for the two barding themes (iron..diamond). */
    public static final List<String> BARDING_BY_TIER = List.of("iron", "iron", "iron", "golden", "diamond");

    /**
     * The world-tier profile: one row drives BOTH the elite affix ladder and
     * the gateway reward ladder, so gear rarity and gem quality rise together.
     * Adjacent elite stat ladders never overlap (haven HP caps at 45, frontier
     * starts at 50) - the contract the GameTest monotone-ladder guard enforces.
     */
    public static final Map<String, TierProfile> TIER_PROFILES;

    static {
        Map<String, TierProfile> profiles = new LinkedHashMap<>();
        profiles.put("haven", new TierProfile(List.of("apotheosis:common", "apotheosis:uncommon"), 0.45,
                List.of("apotheosis:common", "apotheosis:uncommon"), List.of("cracked", "normal"),
                8, 100, 300, 500, 4, 2, 3, "small", "minecraft:music_disc.cat", 1, 1,
                new double[] { 30, 45 }, new double[] { 0.4, 0.6 }, 0.3, new double[] { 0.4, 0.7 }, 0.25,
                new int[] { 10, 6 }, 0.5, new int[] { 2, 3 }, 5, 0, null));
        profiles.put("frontier", new TierProfile(List.of("apotheosis:uncommon", "apotheosis:rare"), 0.5,
                List.of("apotheosis:uncommon", "apotheosis:rare"), List.of("normal"),
                16, 150, 400, 650, 8, 3, 5, "small", "apotheosis:music_disc_shimmer", 2, 2,
                new double[] { 50, 80 }, new double[] { 0.7, 1.0 }, 0.5, new double[] { 0.6, 0.9 }, 0.35,
                new int[] { 18, 11 }, 1.0, new int[] { 3, 5 }, 10, 5, null));
        profiles.put("ascent", new TierProfile(List.of("apotheosis:rare", "apotheosis:epic"), 0.55,
                List.of("apotheosis:rare", "apotheosis:epic"), List.of("flawless"),
                24, 200, 500, 800, 12, 3, 5, "medium", "apotheosis:music_disc_flash", 2, 3,
                new double[] { 85, 120 }, new double[] { 1.1, 1.5 }, 0.7, new double[] { 0.8, 1.1 }, 0.5,
                new int[] { 28, 17 }, 2.0, new int[] { 4, 8 }, 15, 8, null));
        profiles.put("summit", new TierProfile(List.of("apotheosis:epic", "apotheosis:mythic"), 0.65,
                List.of("apotheosis:epic", "apotheosis:mythic"), List.of("flawless", "perfect"),
                48, 250, 650, 950, 24, 3, 5, "medium", "apotheosis:soundtrack_summit", 3, 3,
                new double[] { 130, 170 }, new double[] { 1.6, 2.2 }, 0.9, new double[] { 1.0, 1.4 }, 0.65,
                new int[] { 40, 24 }, 3.0, new int[] { 6, 12 }, 20, 12, null));
        profiles.put("pinnacle", new TierProfile(List.of("apotheosis:epic", "apotheosis:mythic"), 0.8,
                List.of("apotheosis:mythic"), List.of("perfect"),
                64, 300, 800, 1200, 48, 5, 10, "large", "apotheosis:soundtrack_pinnacle", 4, 3,
                new double[] { 180, 260 }, new double[] { 2.5, 3.5 }, 1.0, new double[] { 1.2, 1.6 }, 0.85,
                new int[] { 60, 36 }, 5.0, new int[] { 8, 16 }, 30, 18, "endless/horde"));
        TIER_PROFILES = java.util.Collections.unmodifiableMap(profiles);
    }

    /** Gateway boss-bar color, themed reward gems and flavor stack per family. */
    public static final Map<String, ThemeIdentity> THEME_IDENTITY;

    static {
        Map<String, ThemeIdentity> identity = new LinkedHashMap<>();
        identity.put("demon", new ThemeIdentity("#6A1B9A",
                List.of("core/shadow", "core/fury", "dragonscale/darkness"), "minecraft:amethyst_shard"));
        identity.put("salamander", new ThemeIdentity("#E64A19",
                List.of("dragonscale/flame", "core/vigor", "core/fury"), "minecraft:blaze_powder"));
        identity.put("scaled", new ThemeIdentity("#558B2F",
                List.of("dragonscale/flame", "dragonscale/earth", "dragonscale/wind", "core/juggernaut"),
                "minecraft:dragon_breath"));
        identity.put("lizard", new ThemeIdentity("#AED581",
                List.of("core/earthen", "core/predator", "core/vigor"), "minecraft:turtle_scute"));
        identity.put("hover", new ThemeIdentity("#4DD0E1",
                List.of("dragonscale/wind", "core/sprinter", "core/swift"), "minecraft:feather"));
        identity.put("spider", new ThemeIdentity("#616161",
                List.of("core/shadow", "core/predator", "core/executioner"), "minecraft:spider_eye"));
        identity.put("insect", new ThemeIdentity("#F9A825",
                List.of("core/swift", "core/precision", "core/predator"), "minecraft:honeycomb"));
        identity.put("beast", new ThemeIdentity("#8D6E63",
                List.of("core/predator", "core/vigor", "core/fury", "core/juggernaut"), "minecraft:bone"));
        identity.put("aquatic", new ThemeIdentity("#039BE5",
                List.of("core/swift", "core/striker", "dragonscale/storm"), "minecraft:prismarine_crystals"));
        identity.put("armorsaurus", new ThemeIdentity("#546E7A",
                List.of("core/barrier", "core/stalwart", "spirit/guardian"), "minecraft:iron_ingot"));
        identity.put("slime", new ThemeIdentity("#7CB342",
                List.of("core/vigor", "core/magicule", "core/mystic"), "minecraft:slime_ball"));
        identity.put("spore", new ThemeIdentity("#AB47BC",
                List.of("core/mystic", "core/analyst", "core/magicule"), "minecraft:brown_mushroom"));
        identity.put("orc", new ThemeIdentity("#C62828",
                List.of("core/juggernaut", "core/brawler", "core/fury"), "minecraft:golden_carrot"));
        identity.put("unicorn", new ThemeIdentity("#FFD54F",
                List.of("holy/core", "spirit/core", "core/vigil", "holy/judgment"), "minecraft:gold_ingot"));
        THEME_IDENTITY = java.util.Collections.unmodifiableMap(identity);
    }

    /** Elite mounts: curated few, spawned at transform time and ridden. */
    public static final Map<String, String> ELITE_MOUNTS = Map.of(
            "orc", "tensura:armorsaurus",
            "beast_gnome", "tensura:hound_dog",
            "hell_caterpillar", "tensura:hell_moth");

    /** Elite escorts: supporting entities spawned at elite-relative offsets. */
    public static final Map<String, List<Escort>> ELITE_ESCORTS = Map.of(
            "greater_daemon", List.of(new Escort("tensura:lesser_daemon", 0.0, 0.0, -1.5)),
            "arch_daemon", List.of(
                    new Escort("tensura:lesser_daemon", -1.5, 0.0, -1.0),
                    new Escort("tensura:lesser_daemon", 1.5, 0.0, -1.0)));

    /** An escort entry: entity id plus elite-relative offset. */
    public record Escort(String entity, double x, double y, double z) {
    }

    /** Family-flavored Tensura extras per theme; absent themes ship the flavor stack alone. */
    public static final Map<String, List<Extra>> FAMILY_EXTRAS = Map.of(
            "salamander", List.of(
                    new Extra("tensura:fire_elemental_shard", 15),
                    new Extra("tensura:elemental_essence", 8)));

    /** A family bonus-loot extra: item id and weight. */
    public record Extra(String name, int weight) {
    }

    /** The elite roster: mob id, theme key, display name - 38 mobs, guarded. */
    public static final List<RosterMob> ROSTER = List.of(
            new RosterMob("arch_daemon", "demon", "Arch Daemon"),
            new RosterMob("greater_daemon", "demon", "Greater Daemon"),
            new RosterMob("lesser_daemon", "demon", "Lesser Daemon"),
            new RosterMob("salamander", "salamander", "Salamander"),
            new RosterMob("basilisk", "scaled", "Basilisk"),
            new RosterMob("dragon_peacock", "scaled", "Dragon Peacock"),
            new RosterMob("feathered_serpent", "scaled", "Feathered Serpent"),
            new RosterMob("tempest_serpent", "scaled", "Tempest Serpent"),
            new RosterMob("leech_lizard", "lizard", "Leech Lizard"),
            new RosterMob("hover_lizard", "hover", "Hover Lizard"),
            new RosterMob("black_spider", "spider", "Black Spider"),
            new RosterMob("knight_spider", "spider", "Knight Spider"),
            new RosterMob("army_wasp", "insect", "Army Wasp"),
            new RosterMob("giant_ant", "insect", "Giant Ant"),
            new RosterMob("evil_centipede", "insect", "Evil Centipede"),
            new RosterMob("hell_caterpillar", "insect", "Hell Caterpillar"),
            new RosterMob("hell_moth", "insect", "Hell Moth"),
            new RosterMob("barghest", "beast", "Barghest"),
            new RosterMob("beast_gnome", "beast", "Beast Gnome"),
            new RosterMob("direwolf", "beast", "Direwolf"),
            new RosterMob("blade_tiger", "beast", "Blade Tiger"),
            new RosterMob("giant_bat", "beast", "Giant Bat"),
            new RosterMob("giant_bear", "beast", "Giant Bear"),
            new RosterMob("horned_bear", "beast", "Horned Bear"),
            new RosterMob("horned_rabbit", "beast", "Horned Rabbit"),
            new RosterMob("hound_dog", "beast", "Hound Dog"),
            new RosterMob("one_eyed_owl", "beast", "One Eyed Owl"),
            new RosterMob("winged_cat", "beast", "Winged Cat"),
            new RosterMob("giant_cod", "aquatic", "Giant Cod"),
            new RosterMob("giant_salmon", "aquatic", "Giant Salmon"),
            new RosterMob("landfish", "aquatic", "Landfish"),
            new RosterMob("spear_toro", "aquatic", "Spear Toro"),
            new RosterMob("armorsaurus", "armorsaurus", "Armorsaurus"),
            new RosterMob("slime", "slime", "Slime"),
            new RosterMob("phantaspore", "spore", "Phantaspore"),
            new RosterMob("orc", "orc", "Orc"),
            new RosterMob("pegasus", "unicorn", "Pegasus"),
            new RosterMob("unicorn", "unicorn", "Unicorn"));

    /** One roster entry: the mob, its family theme and its display name. */
    public record RosterMob(String mob, String theme, String display) {
    }

    /** Elite spawn weights per world tier, matching Apotheosis' own ladder. */
    public static final Map<String, WeightSpec> ELITE_SPAWN_WEIGHTS = Map.of(
            "haven", new WeightSpec(0.1, 100),
            "frontier", new WeightSpec(0.1, 100),
            "ascent", new WeightSpec(0.1, 80),
            "summit", new WeightSpec(0.12, 60),
            "pinnacle", new WeightSpec(0.15, 40));
}
