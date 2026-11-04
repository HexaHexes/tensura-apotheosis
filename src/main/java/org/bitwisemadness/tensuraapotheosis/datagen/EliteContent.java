package org.bitwisemadness.tensuraapotheosis.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.RosterMob;
import org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.TierProfile;
import org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.WeightSpec;

/**
 * The 38 Apotheosis elites, their stat-derived world-tier banding, and the
 * per-elite name pools.
 *
 * <p>Band membership is computed, never hand-picked: every roster mob is
 * scored on average-rank percentiles over the roster (theme combat stats plus
 * its own Tensura existence pools), sorted strongest-first with a deterministic
 * tiebreak chain, and placed in strata by spawn dimension so no dimension
 * piles into a single tier. The same facts come out of the staged Tensura jar
 * that the retired PowerShell generator read from gitignored libs/.</p>
 */
public final class EliteContent {

    private EliteContent() {
    }

    /** One scored roster mob: identity, raw stats, composite percentile. */
    private static final class Scored {
        final String mob;
        final String theme;
        final double composite;
        final double mp;
        final double aura;
        final double spiritual;
        final double hp;
        final double dmg;
        final double scale;
        final double ench;
        final double succ;

        Scored(String mob, String theme, double composite, double mp, double aura, double spiritual,
                double hp, double dmg, double scale, double ench, double succ) {
            this.mob = mob;
            this.theme = theme;
            this.composite = composite;
            this.mp = mp;
            this.aura = aura;
            this.spiritual = spiritual;
            this.hp = hp;
            this.dmg = dmg;
            this.scale = scale;
            this.ench = ench;
            this.succ = succ;
        }
    }

    private static final List<String> DIMENSION_ORDER = List.of("overworld", "nether", "hell", "end");
    private static final Set<String> MC_NETHER_BIOMES = Set.of(
            "minecraft:nether_wastes", "minecraft:soul_sand_valley", "minecraft:crimson_forest",
            "minecraft:warped_forest", "minecraft:basalt_deltas");
    private static final Set<String> MC_END_BIOMES = Set.of(
            "minecraft:the_end", "minecraft:end_highlands", "minecraft:end_midlands",
            "minecraft:end_barrens", "minecraft:small_end_islands");
    private static final Map<String, Integer> DIM_SEVERITY = Map.of(
            "overworld", 0, "nether", 1, "hell", 1, "end", 2);

    /** Mob id to world tier, computed once and reused by the gateway section. */
    private static Map<String, String> tiersByMob;

    /** Every generated elite/name-pool file, keyed by its path inside the jar. */
    public static Map<String, JsonObject> files(Path namePoolsFile) {
        Map<String, JsonObject> files = new LinkedHashMap<>();
        TensuraData tensura = TensuraData.read();
        Map<String, String> tiers = tiersByMob(tensura);
        for (RosterMob mob : ApotheosisTables.ROSTER) {
            files.put("data/tensura_apotheosis/apothic_elites/" + mob.mob() + ".json",
                    eliteJson(mob, tiers.get(mob.mob()), tensura));
        }
        for (Map.Entry<String, List<String>> pool : namePools(namePoolsFile).entrySet()) {
            JsonObject json = new JsonObject();
            JsonArray names = new JsonArray();
            pool.getValue().forEach(names::add);
            json.add("names", names);
            files.put("data/tensura_apotheosis/elite_names/" + pool.getKey() + ".json", json);
        }
        return files;
    }

    /** Lang keys: the per-elite display names. */
    public static void lang(Map<String, String> lang) {
        for (RosterMob mob : ApotheosisTables.ROSTER) {
            lang.put("elite.tensura_apotheosis." + mob.mob(), "Elite " + mob.display());
        }
    }

    /** The computed mob-to-world-tier banding, for the gateway section. */
    public static Map<String, String> tiersByMob() {
        return tiersByMob(TensuraData.read());
    }

    private static Map<String, String> tiersByMob(TensuraData tensura) {
        if (tiersByMob != null) {
            return tiersByMob;
        }
        List<Scored> scored = score(tensura);
        Map<String, String> tiers = new LinkedHashMap<>();
        Map<String, List<Scored>> groups = new LinkedHashMap<>();
        for (Scored mob : sorted(scored)) {
            String dimension = mobDimension(mob.mob, tensura);
            groups.computeIfAbsent(dimension, d -> new ArrayList<>()).add(mob);
        }
        for (List<Scored> members : groups.values()) {
            place(members, tiers);
        }
        // Tripwire: a dimension with 2+ mobs must span 2+ tiers - the whole
        // point of the stratification.
        for (Map.Entry<String, List<Scored>> group : groups.entrySet()) {
            Set<String> span = new HashSet<>();
            group.getValue().forEach(m -> span.add(tiers.get(m.mob)));
            if (group.getValue().size() >= 2 && span.size() < 2) {
                throw new IllegalStateException("dimension group '" + group.getKey() + "' ("
                        + group.getValue().size() + " mobs) collapsed into a single tier");
            }
        }
        tiersByMob = tiers;
        return tiers;
    }

    /** Raw stats: theme combat bands (midpoint + fixed values) plus existence pools. */
    private static List<Scored> raw(TensuraData tensura) {
        List<Scored> raw = new ArrayList<>();
        for (RosterMob roster : ApotheosisTables.ROSTER) {
            ApotheosisTables.Theme theme = ApotheosisTables.theme(roster.theme());
            TensuraData.Existence existence = tensura.existence().get(roster.mob());
            if (existence == null) {
                throw new IllegalStateException("roster " + roster.mob()
                        + ": no data/tensura/entity_existence file for AP/MP stats");
            }
            raw.add(new Scored(roster.mob(), roster.theme(),
                    0, existence.mp(), existence.aura(), existence.spiritual(),
                    (theme.hp()[0] + theme.hp()[1]) / 2,
                    (theme.dmg()[0] + theme.dmg()[1]) / 2,
                    (theme.scale()[0] + theme.scale()[1]) / 2,
                    theme.ench(), theme.success()));
        }
        return raw;
    }

    /** Average-rank percentile: a value shared by k mobs sits mid-block. */
    private static double percentile(List<Double> values, double v) {
        int below = 0;
        int equal = 0;
        for (double candidate : values) {
            if (candidate < v) {
                below++;
            } else if (candidate == v) {
                equal++;
            }
        }
        return (below + 0.5 * equal) / values.size();
    }

    private static List<Scored> score(TensuraData tensura) {
        List<Scored> raw = raw(tensura);
        List<Double> allHp = new ArrayList<>();
        List<Double> allDmg = new ArrayList<>();
        List<Double> allScale = new ArrayList<>();
        List<Double> allEnch = new ArrayList<>();
        List<Double> allSucc = new ArrayList<>();
        List<Double> allAura = new ArrayList<>();
        List<Double> allMp = new ArrayList<>();
        List<Double> allSpirit = new ArrayList<>();
        for (Scored p : raw) {
            allHp.add(p.hp);
            allDmg.add(p.dmg);
            allScale.add(p.scale);
            allEnch.add(p.ench);
            allSucc.add(p.succ);
            allAura.add(p.aura);
            allMp.add(p.mp);
            allSpirit.add(p.spiritual);
        }
        List<Scored> scored = new ArrayList<>();
        for (Scored p : raw) {
            double pctSucc = 1 - percentile(allSucc, p.succ); // inverted: rarer = stronger
            double composite = (percentile(allHp, p.hp) + percentile(allDmg, p.dmg)
                    + percentile(allScale, p.scale) + percentile(allEnch, p.ench)
                    + pctSucc + percentile(allAura, p.aura) + percentile(allMp, p.mp)
                    + percentile(allSpirit, p.spiritual)) / 8;
            scored.add(new Scored(p.mob, p.theme, composite, p.mp, p.aura, p.spiritual,
                    p.hp, p.dmg, p.scale, p.ench, p.succ));
        }
        return scored;
    }

    /** Strongest first, with the generator's deterministic tiebreak chain. */
    private static List<Scored> sorted(List<Scored> scored) {
        List<Scored> out = new ArrayList<>(scored);
        out.sort((a, b) -> {
            int c = Double.compare(b.composite, a.composite);
            if (c != 0) {
                return c;
            }
            c = Double.compare(b.mp, a.mp);
            if (c != 0) {
                return c;
            }
            c = Double.compare(b.aura, a.aura);
            if (c != 0) {
                return c;
            }
            c = Double.compare(b.spiritual, a.spiritual);
            if (c != 0) {
                return c;
            }
            c = Double.compare(b.hp, a.hp);
            if (c != 0) {
                return c;
            }
            c = Double.compare(b.dmg, a.dmg);
            if (c != 0) {
                return c;
            }
            c = Double.compare(b.scale, a.scale);
            if (c != 0) {
                return c;
            }
            c = Double.compare(b.ench, a.ench);
            if (c != 0) {
                return c;
            }
            c = Double.compare(a.succ, b.succ);
            if (c != 0) {
                return c;
            }
            return a.mob.compareTo(b.mob);
        });
        return out;
    }

    /** Hardest spawn dimension: recursive biome-tag expansion, cycle-safe. */
    private static String mobDimension(String mob, TensuraData tensura) {
        String tagName = mob + "_spawn";
        List<String> values = tensura.biomeTags().get(tagName);
        if (values == null) {
            throw new IllegalStateException("roster " + mob + ": no " + tagName + " biome tag in the Tensura jar");
        }
        Set<String> seen = new HashSet<>();
        seen.add("tensura:" + tagName);
        String best = "overworld";
        for (String entry : values) {
            String dimension = entryDimension(entry, 0, seen, tensura);
            if (severity(dimension) > severity(best)) {
                best = dimension;
            }
        }
        return best;
    }

    private static String entryDimension(String entry, int depth, Set<String> seen, TensuraData tensura) {
        if (depth > 8) {
            throw new IllegalStateException("biome tag expansion too deep at '" + entry + "'");
        }
        if (entry.startsWith("#")) {
            String tagPath = entry.substring(1);
            if (tagPath.startsWith("tensura:")) {
                String name = tagPath.substring("tensura:".length());
                List<String> values = tensura.biomeTags().get(name);
                if (values == null) {
                    throw new IllegalStateException("tensura biome tag '" + tagPath + "' missing from jar");
                }
                if (!seen.add(tagPath)) {
                    return "overworld";
                }
                String best = "overworld";
                for (String value : values) {
                    String dimension = entryDimension(value, depth + 1, seen, tensura);
                    if (severity(dimension) > severity(best)) {
                        best = dimension;
                    }
                }
                return best;
            }
            if (tagPath.matches("(^|[_/:])nether([_/:]|$)")) {
                return "nether";
            }
            if (tagPath.matches("(^|[_/:])end([_/:]|$)")) {
                return "end";
            }
            return "overworld";
        }
        if (MC_NETHER_BIOMES.contains(entry)) {
            return "nether";
        }
        if (MC_END_BIOMES.contains(entry)) {
            return "end";
        }
        if (entry.startsWith("tensura:")) {
            return tensura.biomeDimension().getOrDefault(entry, "overworld");
        }
        if (entry.matches("(^|[_/:])nether([_/:]|$)")) {
            return "nether";
        }
        if (entry.matches("(^|[_/:])end([_/:]|$)")) {
            return "end";
        }
        return "overworld";
    }

    private static int severity(String dimension) {
        return DIM_SEVERITY.getOrDefault(dimension, 0);
    }

    /**
     * Stratified placement within one spawn-dimension group: a lone mob sits
     * mid-ladder, 2-4 spread evenly, 5+ cut into contiguous stat-ordered
     * segments (cuts snap to score-change points so exact ties stay whole);
     * the strongest segment takes pinnacle.
     */
    private static void place(List<Scored> members, Map<String, String> tiers) {
        int k = members.size();
        if (k == 1) {
            tiers.put(members.get(0).mob, ApotheosisTables.TIERS.get(2));
            return;
        }
        if (k <= 4) {
            for (int s = 0; s < k; s++) {
                // [math]::Round is banker's; Math.rint matches, and none of the
                // k<=4 buckets ever lands on an exact .5 anyway.
                int bucket = (int) Math.rint((k - 1 - s) * 4.0 / (k - 1));
                tiers.put(members.get(s).mob, ApotheosisTables.TIERS.get(bucket));
            }
            return;
        }
        int base = k / 5;
        int rem = k % 5;
        int[] sizes = new int[5];
        for (int i = 0; i < 5; i++) {
            sizes[i] = base + (i < rem ? 1 : 0);
        }
        int[] nominal = new int[4];
        int acc = 0;
        for (int i = 0; i < 4; i++) {
            acc += sizes[i];
            nominal[i] = acc;
        }
        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i < k; i++) {
            if (members.get(i).composite != members.get(i - 1).composite) {
                candidates.add(i);
            }
        }
        int[] bounds = new int[4];
        int previous = 0;
        for (int j = 0; j < nominal.length; j++) {
            int remainingCuts = nominal.length - 1 - j;
            int lo = previous + 1;
            int hi = k - 1 - remainingCuts;
            int bestCut = -1;
            double bestDistance = Double.MAX_VALUE;
            for (int candidate : candidates) {
                if (candidate >= lo && candidate <= hi) {
                    double distance = Math.abs(candidate - nominal[j]);
                    if (distance < bestDistance) {
                        bestCut = candidate;
                        bestDistance = distance;
                    }
                }
            }
            if (bestCut < 0) {
                throw new IllegalStateException("no score-change cut point for band " + (j + 1)
                        + " of " + k + "-mob group");
            }
            bounds[j] = bestCut;
            previous = bestCut;
        }
        int start = 0;
        for (int segment = 0; segment < 5; segment++) {
            int end = segment < 4 ? bounds[segment] : k;
            for (int i = start; i < end; i++) {
                // Strongest segment -> pinnacle.
                tiers.put(members.get(i).mob, ApotheosisTables.TIERS.get(4 - segment));
            }
            start = end;
        }
    }

    private static JsonObject eliteJson(RosterMob roster, String band, TensuraData tensura) {
        TierProfile profile = ApotheosisTables.TIER_PROFILES.get(band);
        ApotheosisTables.Theme theme = ApotheosisTables.theme(roster.theme());
        Set<String> rosterIds = new LinkedHashSet<>();
        ApotheosisTables.ROSTER.forEach(r -> rosterIds.add("tensura:" + r.mob()));

        // Full gating: this elite only exists at world tiers at/above its band -
        // spawn weights and gear pools become a contiguous suffix of the ladder.
        JsonObject validGear = new JsonObject();
        JsonObject weights = new JsonObject();
        for (int i = 0; i < ApotheosisTables.TIERS.size(); i++) {
            String tier = ApotheosisTables.TIERS.get(i);
            if (i >= ApotheosisTables.TIERS.indexOf(band)) {
                JsonArray tags = new JsonArray();
                tags.add("#tensura_" + roster.theme() + "_" + tier);
                validGear.add(tier, tags);
                WeightSpec spec = ApotheosisTables.ELITE_SPAWN_WEIGHTS.get(tier);
                JsonObject weightJson = new JsonObject();
                weightJson.addProperty("quality", spec.quality());
                weightJson.addProperty("weight", spec.weight());
                weights.add(tier, weightJson);
            }
        }

        JsonArray modifiers = new JsonArray();
        modifiers.add(attribute("minecraft:generic.max_health", "add_value",
                rangeValue(profile.eliteHp()[0], profile.eliteHp()[1])));
        modifiers.add(attribute("minecraft:generic.attack_damage", "add_multiplied_total",
                rangeValue(profile.eliteDmg()[0], profile.eliteDmg()[1])));
        modifiers.add(attribute("minecraft:generic.knockback_resistance", "add_value",
                constantValue(profile.eliteKb())));
        modifiers.add(attribute("minecraft:generic.scale", "add_value",
                rangeValue(profile.eliteScale()[0], profile.eliteScale()[1])));
        modifiers.add(attribute("tensura:max_aura", "add_multiplied_total",
                constantValue(profile.elitePoolMult())));
        modifiers.add(attribute("tensura:max_magicule", "add_multiplied_total",
                constantValue(profile.elitePoolMult())));
        JsonArray effects = new JsonArray();
        for (String effect : theme.effects()) {
            JsonObject effectJson = new JsonObject();
            effectJson.addProperty("effect", effect);
            effectJson.addProperty("amplifier", 0);
            effects.add(effectJson);
        }
        JsonObject stats = new JsonObject();
        stats.add("attribute_modifiers", modifiers);
        stats.add("effects", effects);
        stats.addProperty("enchant_chance", profile.eliteEnch());
        JsonObject levels = new JsonObject();
        levels.addProperty("primary", profile.eliteEnchLvl()[0]);
        levels.addProperty("secondary", profile.eliteEnchLvl()[1]);
        stats.add("enchantment_levels", levels);

        JsonObject basic = new JsonObject();
        JsonObject constraints = new JsonObject();
        constraints.addProperty("biomes", "#tensura:" + roster.mob() + "_spawn");
        basic.add("constraints", constraints);
        JsonObject name = new JsonObject();
        name.addProperty("translate", "elite.tensura_apotheosis." + roster.mob());
        basic.add("name", name);
        // Apotheosis' own elites block exactly one spawn type - spawn_egg - and
        // let its native spawn pipeline (biome constraints, tier weights, the
        // success_chance roll) do the rest.
        JsonArray spawnConditions = new JsonArray();
        JsonObject notCondition = new JsonObject();
        notCondition.addProperty("type", "apotheosis:not");
        JsonObject spawnType = new JsonObject();
        spawnType.addProperty("type", "apotheosis:spawn_type");
        JsonArray spawnTypes = new JsonArray();
        spawnTypes.add("spawn_egg");
        spawnType.add("spawn_types", spawnTypes);
        notCondition.add("spawn_condition", spawnType);
        spawnConditions.add(notCondition);
        basic.add("spawn_conditions", spawnConditions);
        basic.add("valid_gear_sets", validGear);
        basic.add("weights", weights);
        // Kill-time extras rolled through Apotheosis' BonusLootTables: the
        // band's tier table and the family's theme table.
        JsonArray bonusLoot = new JsonArray();
        bonusLoot.add("tensura_apotheosis:bonus/" + band);
        bonusLoot.add("tensura_apotheosis:bonus/" + roster.theme());
        basic.add("bonus_loot", bonusLoot);
        if (ApotheosisTables.ELITE_MOUNTS.containsKey(roster.mob())) {
            String mountMob = ApotheosisTables.ELITE_MOUNTS.get(roster.mob());
            if (!rosterIds.contains(mountMob)) {
                throw new IllegalStateException("mount " + mountMob + " for elite " + roster.mob()
                        + " is not a roster mob");
            }
            JsonObject mount = new JsonObject();
            mount.addProperty("entity", mountMob);
            basic.add("mount", mount);
        }
        if (ApotheosisTables.ELITE_ESCORTS.containsKey(roster.mob())) {
            JsonArray escorts = new JsonArray();
            for (ApotheosisTables.Escort escort : ApotheosisTables.ELITE_ESCORTS.get(roster.mob())) {
                if (!rosterIds.contains(escort.entity())) {
                    throw new IllegalStateException("escort " + escort.entity() + " for elite "
                            + roster.mob() + " is not a roster mob");
                }
                JsonObject escortJson = new JsonObject();
                escortJson.addProperty("entity", escort.entity());
                escortJson.addProperty("x", escort.x());
                escortJson.addProperty("y", escort.y());
                escortJson.addProperty("z", escort.z());
                escorts.add(escortJson);
            }
            basic.add("supporting_entities", escorts);
        }

        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:elite");
        json.add("basic_data", basic);
        json.addProperty("entities", "tensura:" + roster.mob());
        json.add("stats", stats);
        json.addProperty("success_chance", theme.success());
        // Elite rarity ladder: the rarities this elite's own gear affixes roll,
        // taken from the shared tier profile.
        JsonObject affixData = new JsonObject();
        affixData.addProperty("affix_chance", profile.affixChance());
        JsonArray rarities = new JsonArray();
        profile.eliteRarities().forEach(rarities::add);
        affixData.add("rarities", rarities);
        json.add("affix_data", affixData);
        return json;
    }

    private static JsonObject attribute(String attribute, String operation, JsonElement value) {
        JsonObject json = new JsonObject();
        json.addProperty("attribute", attribute);
        json.addProperty("operation", operation);
        json.add("value", value);
        return json;
    }

    private static JsonElement rangeValue(double min, double max) {
        JsonObject json = new JsonObject();
        num(json, "min", min);
        num(json, "max", max);
        return json;
    }

    private static JsonElement constantValue(double value) {
        JsonObject json = new JsonObject();
        num(json, "v", value);
        return json.get("v");
    }

    private static void num(JsonObject json, String key, double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            json.addProperty(key, (long) value);
        } else {
            json.addProperty(key, value);
        }
    }

    /** Reads {@code tools/name-pools.json} (the hand-authored 40-50 name pools). */
    private static Map<String, List<String>> namePools(Path file) {
        JsonObject json;
        try {
            json = com.google.gson.JsonParser.parseString(
                    java.nio.file.Files.readString(file)).getAsJsonObject();
        } catch (Exception ex) {
            throw new IllegalStateException("cannot read the name pools at " + file, ex);
        }
        if (json.keySet().size() != ApotheosisTables.ROSTER.size()) {
            throw new IllegalStateException("name pools: " + json.keySet().size() + " mobs expected "
                    + ApotheosisTables.ROSTER.size());
        }
        Set<String> seen = new TreeSet<>();
        Map<String, List<String>> pools = new LinkedHashMap<>();
        for (RosterMob roster : ApotheosisTables.ROSTER) {
            JsonElement names = json.get(roster.mob());
            if (names == null || !names.isJsonArray()) {
                throw new IllegalStateException("name pools: missing entry for " + roster.mob());
            }
            List<String> list = new ArrayList<>();
            names.getAsJsonArray().forEach(n -> list.add(n.getAsString()));
            if (list.size() < 40 || list.size() > 50) {
                throw new IllegalStateException("name pools: " + roster.mob() + " has " + list.size()
                        + " names expected 40-50");
            }
            for (String name : list) {
                if (name.isBlank()) {
                    throw new IllegalStateException("name pools: " + roster.mob() + " contains a blank name");
                }
                if (!seen.add(name)) {
                    throw new IllegalStateException("name pools: duplicate name '" + name + "'");
                }
            }
            pools.put(roster.mob(), List.copyOf(list));
        }
        return pools;
    }
}
