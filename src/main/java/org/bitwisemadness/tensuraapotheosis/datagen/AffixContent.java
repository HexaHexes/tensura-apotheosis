package org.bitwisemadness.tensuraapotheosis.datagen;

import static org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.ALL;
import static org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.ARMO;
import static org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.BODY;
import static org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.BOW;
import static org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.BRK;
import static org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.LB;
import static org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.MEA;
import static org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.SHLD;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.RangeVal;

/**
 * The 55 Tensura affixes for Apotheosis' affix registry, as tables.
 *
 * <p>Attribute affixes roll stat ranges per rarity; proc affixes apply a
 * Tensura mob effect on a trigger with per-rarity duration/amplifier/cooldown
 * specs. Scales mirror Apotheosis' own affixes. Values keyed by rarity id;
 * the writer emits whole numbers as integers, matching the retired
 * generator's output.</p>
 */
public final class AffixContent {

    /** The affix registry's rarity ids, in ladder order. */
    public enum Rarity {
        UNCOMMON("apotheosis:uncommon"),
        RARE("apotheosis:rare"),
        EPIC("apotheosis:epic"),
        MYTHIC("apotheosis:mythic");

        private final String id;

        Rarity(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    /** One shipped affix: its registry path, display name/suffix, and the JSON body. */
    public record AffixDef(String rel, String name, String suffix, JsonObject body) {
    }

    private static final List<AffixDef> AFFIXES = new ArrayList<>();

    private AffixContent() {
    }

    private static RangeVal range(double min, double max) {
        return new RangeVal(min, max, null);
    }

    private static RangeVal range(double min, double max, double step) {
        return new RangeVal(min, max, step);
    }

    private static Map<Rarity, RangeVal> ranges(Rarity[] keys, RangeVal[] values) {
        Map<Rarity, RangeVal> out = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) {
            out.put(keys[i], values[i]);
        }
        return out;
    }

    private static Map<Rarity, ApotheosisTables.ProcSpec> procs(Rarity[] keys,
            ApotheosisTables.ProcSpec[] values) {
        Map<Rarity, ApotheosisTables.ProcSpec> out = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) {
            out.put(keys[i], values[i]);
        }
        return out;
    }

    private static void attr(String rel, String name, String suffix, String attribute, List<String> cats,
            String op, Map<Rarity, RangeVal> values) {
        attr(rel, name, suffix, attribute, cats, op, values, 25);
    }

    private static void attr(String rel, String name, String suffix, String attribute, List<String> cats,
            String op, Map<Rarity, RangeVal> values, int weight) {
        attr(rel, name, suffix, attribute, cats, op, values, weight, List.of());
    }

    private static void attr(String rel, String name, String suffix, String attribute, List<String> cats,
            String op, Map<Rarity, RangeVal> values, int weight, List<String> exclusive) {
        JsonObject def = new JsonObject();
        def.addProperty("affix_type", "stat");
        JsonArray excl = new JsonArray();
        exclusive.forEach(excl::add);
        def.add("exclusive_set", excl);
        JsonObject weights = new JsonObject();
        weights.addProperty("quality", 0.1);
        weights.addProperty("weight", weight);
        def.add("weights", weights);
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:attribute");
        json.addProperty("attribute", attribute);
        JsonArray catArray = new JsonArray();
        cats.forEach(catArray::add);
        json.add("categories", catArray);
        json.add("definition", def);
        json.addProperty("operation", op);
        json.add("values", valuesJson(values));
        AFFIXES.add(new AffixDef(rel, name, suffix, json));
    }

    private static void proc(String rel, String name, String suffix, String effect, String target,
            List<String> cats, Map<Rarity, ApotheosisTables.ProcSpec> values) {
        proc(rel, name, suffix, effect, target, cats, values, 25, true, List.of());
    }

    private static void proc(String rel, String name, String suffix, String effect, String target,
            List<String> cats, Map<Rarity, ApotheosisTables.ProcSpec> values, int weight) {
        proc(rel, name, suffix, effect, target, cats, values, weight, true, List.of());
    }

    private static void proc(String rel, String name, String suffix, String effect, String target,
            List<String> cats, Map<Rarity, ApotheosisTables.ProcSpec> values, int weight, boolean stack,
            List<String> exclusive) {
        JsonObject def = new JsonObject();
        def.addProperty("affix_type", "basic_effect");
        JsonArray excl = new JsonArray();
        exclusive.forEach(excl::add);
        def.add("exclusive_set", excl);
        JsonObject weights = new JsonObject();
        weights.addProperty("quality", 0.1);
        weights.addProperty("weight", weight);
        def.add("weights", weights);
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:mob_effect");
        json.add("definition", def);
        json.addProperty("mob_effect", effect);
        if (stack) {
            // Stack fields sit directly after mob_effect, matching Apotheosis' layout.
            json.addProperty("stack_on_reapply", true);
            json.addProperty("stacking_limit", 3);
        }
        json.addProperty("target", target);
        JsonArray catArray = new JsonArray();
        cats.forEach(catArray::add);
        json.add("types", catArray);
        json.add("values", procValuesJson(values));
        AFFIXES.add(new AffixDef(rel, name, suffix, json));
    }

    private static final Rarity[] RE = { Rarity.RARE, Rarity.EPIC, Rarity.MYTHIC };
    private static final Rarity[] EM = { Rarity.EPIC, Rarity.MYTHIC };
    private static final Rarity[] URE = { Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC, Rarity.MYTHIC };

    static {
        // ---- melee: attribute affixes (scales mirror Apotheosis' own affixes) ----
        attr("melee/attribute/flame_blade", "Flame Blade", "of the Salamander",
                "apothic_attributes:fire_damage", MEA, "add_value",
                ranges(URE, new RangeVal[] { range(2, 4), range(2, 5), range(4, 6), range(4, 10) }));
        attr("melee/attribute/frost_blade", "Frost Blade", "of the Yeti",
                "apothic_attributes:cold_damage", MEA, "add_value",
                ranges(URE, new RangeVal[] { range(2, 4), range(2, 5), range(4, 6), range(4, 10) }));
        attr("melee/attribute/armor_piercing", "Armor Piercing", "of the Breaker",
                "apothic_attributes:armor_pierce", MEA, "add_value",
                ranges(RE, new RangeVal[] { range(4, 8), range(5, 10), range(5, 12) }));
        attr("melee/attribute/lethal", "Lethal", "of the Assassin",
                "apothic_attributes:crit_chance", MEA, "add_value",
                ranges(RE, new RangeVal[] { range(0.1, 0.25), range(0.15, 0.35), range(0.25, 0.55) }));
        attr("melee/attribute/keen", "Keen", "of the Edge",
                "apothic_attributes:crit_damage", MEA, "add_value",
                ranges(RE, new RangeVal[] { range(0.15, 0.25), range(0.15, 0.3), range(0.25, 0.4) }));
        attr("melee/attribute/soul_thief", "Soul Thief", "of Predation",
                "apothic_attributes:life_steal", MEA, "add_value",
                ranges(RE, new RangeVal[] { range(0.05, 0.1), range(0.08, 0.13), range(0.1, 0.15) }));
        attr("melee/attribute/predators_edge", "Predator's Edge", "of the Hunt",
                "apothic_attributes:current_hp_damage", MEA, "add_value",
                ranges(EM, new RangeVal[] { range(0.1, 0.2), range(0.1, 0.25) }));

        // ---- melee: DoT family (uncommon+), shared per-rarity proc spec ----
        Map<Rarity, ApotheosisTables.ProcSpec> dot = procs(URE, new ApotheosisTables.ProcSpec[] {
                new ApotheosisTables.ProcSpec(range(80, 100, 20), 0, 80),
                new ApotheosisTables.ProcSpec(range(100, 120, 20), 0, 80),
                new ApotheosisTables.ProcSpec(range(120, 160, 20), range(0, 1, 0.5), 70),
                new ApotheosisTables.ProcSpec(range(140, 200, 20), range(0, 1, 0.5), 60) });
        proc("melee/mob_effect/frostbite", "Frostbite", "of the Frost Queen",
                "tensura:frost", "attack_target", MEA, dot);
        proc("melee/mob_effect/infective", "Infective", "of the Contagion",
                "tensura:infection", "attack_target", MEA, dot);
        proc("melee/mob_effect/venomous", "Venomous", "of the Poison Breath",
                "tensura:magicule_poison", "attack_target", MEA, dot);
        proc("melee/mob_effect/corrosive", "Corrosive", "of Dissolution",
                "tensura:corrosion", "attack_target", MEA, dot);

        // ---- melee: hard-CC family (rare+, lower weight, mutually exclusive) ----
        Map<Rarity, ApotheosisTables.ProcSpec> cc = procs(RE, new ApotheosisTables.ProcSpec[] {
                new ApotheosisTables.ProcSpec(range(60, 80, 20), 0, 120),
                new ApotheosisTables.ProcSpec(range(80, 100, 20), 0, 110),
                new ApotheosisTables.ProcSpec(range(100, 120, 20), range(0, 1, 0.5), 100) });
        List<String> ccParalyzing = List.of(
                "tensura_apotheosis:melee/mob_effect/silencing",
                "tensura_apotheosis:melee/mob_effect/terrifying",
                "tensura_apotheosis:melee/mob_effect/stone_gaze");
        List<String> ccSilencing = List.of(
                "tensura_apotheosis:melee/mob_effect/paralyzing",
                "tensura_apotheosis:melee/mob_effect/terrifying",
                "tensura_apotheosis:melee/mob_effect/stone_gaze");
        List<String> ccTerrifying = List.of(
                "tensura_apotheosis:melee/mob_effect/paralyzing",
                "tensura_apotheosis:melee/mob_effect/silencing",
                "tensura_apotheosis:melee/mob_effect/stone_gaze");
        List<String> ccGaze = List.of(
                "tensura_apotheosis:melee/mob_effect/paralyzing",
                "tensura_apotheosis:melee/mob_effect/silencing",
                "tensura_apotheosis:melee/mob_effect/terrifying");
        proc("melee/mob_effect/paralyzing", "Paralyzing", "of the Bound",
                "tensura:paralysis", "attack_target", MEA, cc, 20, true, ccParalyzing);
        proc("melee/mob_effect/silencing", "Silencing", "of Severance",
                "tensura:silence", "attack_target", MEA, cc, 20, true, ccSilencing);
        proc("melee/mob_effect/terrifying", "Terrifying", "of Dread",
                "tensura:fear", "attack_target", MEA, cc, 20, true, ccTerrifying);
        Map<Rarity, ApotheosisTables.ProcSpec> gaze = procs(EM, new ApotheosisTables.ProcSpec[] {
                new ApotheosisTables.ProcSpec(range(40, 60, 20), 0, 160),
                new ApotheosisTables.ProcSpec(range(60, 80, 20), 0, 140) });
        proc("melee/mob_effect/stone_gaze", "Stone Gaze", "of Petrification",
                "tensura:petrification", "attack_target", MEA, gaze, 10, true, ccGaze);

        // ---- ranged ----
        attr("ranged/attribute/hawk_eye", "Hawk Eye", "of the Sentinel",
                "apothic_attributes:arrow_damage", BOW, "add_multiplied_total",
                ranges(RE, new RangeVal[] { range(0.2, 0.3), range(0.25, 0.35), range(0.25, 0.4) }));
        attr("ranged/attribute/quick_draw", "Quick Draw", "of the Swift",
                "apothic_attributes:draw_speed", BOW, "add_multiplied_total",
                ranges(RE, new RangeVal[] { range(0.3, 0.5), range(0.4, 0.6), range(0.5, 0.65) }));
        attr("ranged/attribute/warped", "Warped", "of Spatial Rending",
                "tensura:warp_shot", BOW, "add_value",
                ranges(RE, new RangeVal[] { range(1, 2), range(2, 3), range(3, 5) }));
        Map<Rarity, ApotheosisTables.ProcSpec> rangedDot = procs(RE, new ApotheosisTables.ProcSpec[] {
                new ApotheosisTables.ProcSpec(range(100, 120, 20), 0, 80),
                new ApotheosisTables.ProcSpec(range(120, 160, 20), range(0, 1, 0.5), 70),
                new ApotheosisTables.ProcSpec(range(140, 200, 20), range(0, 1, 0.5), 60) });
        proc("ranged/mob_effect/frostbite", "Frostbite", "of the Frost Queen",
                "tensura:frost", "arrow_target", BOW, rangedDot);
        proc("ranged/mob_effect/infective", "Infective", "of the Contagion",
                "tensura:infection", "arrow_target", BOW, rangedDot);
        proc("ranged/mob_effect/venomous", "Venomous", "of the Poison Breath",
                "tensura:magicule_poison", "arrow_target", BOW, rangedDot);

        // ---- armor: attribute affixes ----
        attr("armor/attribute/magicule_fortified", "Magicule Fortified", "of the Magicule",
                "tensura:max_magicule", BODY, "add_value",
                ranges(RE, new RangeVal[] { range(30, 60), range(50, 100), range(80, 160) }));
        attr("armor/attribute/magicule_attuned", "Magicule Attuned", "of Accumulation",
                "tensura:magicule_gain", BODY, "add_value",
                ranges(RE, new RangeVal[] { range(0.05, 0.25), range(0.1, 0.35), range(0.15, 0.5) }));
        attr("armor/attribute/aura_attuned", "Aura Attuned", "of the Aura",
                "tensura:max_aura", BODY, "add_value",
                ranges(RE, new RangeVal[] { range(20, 40), range(35, 70), range(60, 120) }));
        attr("armor/attribute/aura_blessed", "Aura Blessed", "of the Spirit",
                "tensura:aura_gain", BODY, "add_value",
                ranges(RE, new RangeVal[] { range(0.05, 0.25), range(0.1, 0.35), range(0.15, 0.5) }));
        attr("armor/attribute/spiritually_fortified", "Spiritually Fortified", "of the Soul",
                "tensura:max_spiritual_health", BODY, "add_value",
                ranges(RE, new RangeVal[] { range(10, 25), range(15, 40), range(25, 60) }));
        attr("armor/attribute/mending_soul", "Mending Soul", "of Rejuvenation",
                "tensura:spiritual_health_regeneration", BODY, "add_value",
                ranges(RE, new RangeVal[] { range(0.5, 1), range(0.75, 1.5), range(1, 2) }));
        attr("armor/attribute/arcane_barrier", "Arcane Barrier", "of Warding",
                "tensura:magic_barrier", BODY, "add_value",
                ranges(RE, new RangeVal[] { range(3, 6), range(4, 8), range(6, 12) }));
        attr("armor/attribute/iron_will", "Iron Will", "of the Bulwark",
                "tensura:physical_barrier", BODY, "add_value",
                ranges(RE, new RangeVal[] { range(3, 6), range(4, 8), range(6, 12) }));
        attr("armor/attribute/warded", "Warded", "of Null Magic",
                "tensura:magic_resistance", ARMO, "add_value",
                ranges(RE, new RangeVal[] { range(3, 6), range(4, 9), range(6, 12) }));
        attr("armor/attribute/sanctified", "Sanctified", "of the Holy",
                "tensura:light_resistance", BODY, "add_value",
                ranges(RE, new RangeVal[] { range(3, 6), range(4, 9), range(6, 12) }));
        attr("armor/attribute/indomitable", "Indomitable", "of Glory",
                "apothic_attributes:overheal", BODY, "add_value",
                ranges(RE, new RangeVal[] { range(0.1, 0.2), range(0.1, 0.25), range(0.15, 0.25) }));
        attr("armor/attribute/regenerative", "Regenerative", "of Vigor",
                "apothic_attributes:healing_received", BODY, "add_multiplied_base",
                ranges(RE, new RangeVal[] { range(0.1, 0.25), range(0.15, 0.3), range(0.2, 0.4) }));
        attr("armor/attribute/untouchable", "Untouchable", "of Evasion",
                "tensura:melee_dodge_chance", LB, "add_value",
                ranges(RE, new RangeVal[] { range(2, 4), range(3, 6), range(5, 9) }));
        attr("armor/attribute/ghost_step", "Ghost Step", "of Phantoms",
                "tensura:projectile_dodge_chance", LB, "add_value",
                ranges(RE, new RangeVal[] { range(2, 4), range(3, 6), range(5, 9) }));
        attr("armor/attribute/concealed", "Concealed", "of the Shadow",
                "tensura:presence_concealment", ARMO, "add_value",
                ranges(RE, new RangeVal[] { range(5, 12), range(8, 18), range(12, 25) }));

        // ---- armor: hurt_self procs (no stacking) ----
        Map<Rarity, ApotheosisTables.ProcSpec> surge = procs(RE, new ApotheosisTables.ProcSpec[] {
                new ApotheosisTables.ProcSpec(100, 0, 200),
                new ApotheosisTables.ProcSpec(140, 0, 180),
                new ApotheosisTables.ProcSpec(180, range(0, 1, 0.5), 160) });
        proc("armor/mob_effect/magicule_surge", "Magicule Surge", "of Recovery",
                "tensura:magicule_regeneration", "hurt_self", BODY, surge, 25, false, List.of());
        Map<Rarity, ApotheosisTables.ProcSpec> resto = procs(RE, new ApotheosisTables.ProcSpec[] {
                new ApotheosisTables.ProcSpec(80, 0, 220),
                new ApotheosisTables.ProcSpec(110, 0, 200),
                new ApotheosisTables.ProcSpec(140, range(0, 1, 0.5), 180) });
        proc("armor/mob_effect/restorative", "Restorative", "of Renewal",
                "tensura:instant_regeneration", "hurt_self", BODY, resto, 25, false, List.of());

        // ---- shield ----
        attr("shield/attribute/bulwark", "Bulwark", "of the Guardian",
                "tensura:magic_barrier", SHLD, "add_value",
                ranges(RE, new RangeVal[] { range(3, 6), range(4, 8), range(6, 12) }));
        attr("shield/attribute/aegis", "Aegis", "of Protection",
                "tensura:physical_barrier", SHLD, "add_value",
                ranges(RE, new RangeVal[] { range(3, 6), range(4, 8), range(6, 12) }));
        attr("shield/attribute/wardstone", "Wardstone", "of Nullification",
                "tensura:magic_resistance", SHLD, "add_value",
                ranges(RE, new RangeVal[] { range(3, 6), range(4, 9), range(6, 12) }));
        Map<Rarity, ApotheosisTables.ProcSpec> reinforce = procs(RE, new ApotheosisTables.ProcSpec[] {
                new ApotheosisTables.ProcSpec(100, 0, 200),
                new ApotheosisTables.ProcSpec(140, 0, 180),
                new ApotheosisTables.ProcSpec(180, range(0, 1, 0.5), 160) });
        proc("shield/mob_effect/reinforced", "Reinforced", "of Endurance",
                "tensura:reinforcement", "hurt_self", SHLD, reinforce, 25, false, List.of());

        // ---- breaker (tools) ----
        attr("breaker/attribute/deep_delver", "Deep Delver", "of the Depths",
                "apothic_attributes:mining_speed", BRK, "add_multiplied_total",
                ranges(RE, new RangeVal[] { range(0.25, 0.5), range(0.25, 0.7), range(0.55, 0.85) }));
        attr("breaker/attribute/learned", "Learned", "of Wisdom",
                "apothic_attributes:experience_gained", BRK, "add_multiplied_total",
                ranges(RE, new RangeVal[] { range(0.35, 0.5), range(0.35, 0.6), range(0.55, 0.65) }));
        attr("breaker/attribute/earth_affinity", "Earth Affinity", "of the Earth Dragon",
                "tensura:earth_boost", BRK, "add_value",
                ranges(RE, new RangeVal[] { range(2, 5), range(3, 7), range(5, 10) }));

        // ---- generic (all equipment) ----
        attr("generic/attribute/swift_chant", "Swift Chant", "of Quickening",
                "tensura:chant_speed", ALL, "add_multiplied_total",
                ranges(RE, new RangeVal[] { range(0.05, 0.1), range(0.1, 0.15), range(0.15, 0.25) }));
        attr("generic/attribute/efficient_casting", "Efficient Casting", "of Conservation",
                "tensura:magic_cost_multiplier", ALL, "add_value",
                ranges(RE, new RangeVal[] { range(-0.1, -0.05), range(-0.15, -0.08), range(-0.2, -0.1) }));
        attr("generic/attribute/prodigy", "Prodigy", "of Talents",
                "tensura:ability_learning_gain", ALL, "add_value",
                ranges(RE, new RangeVal[] { range(0.1, 0.3), range(0.2, 0.5), range(0.3, 0.8) }));
        attr("generic/attribute/mastered", "Mastered", "of the Savant",
                "tensura:ability_mastery_gain", ALL, "add_value",
                ranges(RE, new RangeVal[] { range(0.1, 0.3), range(0.2, 0.5), range(0.3, 0.8) }));
        // NOTE: min/max ranges become StepFunctions with an implicit step of 0.01 -
        // (max - min) must be a multiple of that step or the affix fails to parse.
        attr("generic/attribute/quickened", "Quickened", "of Haste",
                "apothic_attributes:cooldown_reduction", ALL, "add_value",
                ranges(RE, new RangeVal[] { range(0.01, 0.05), range(0.05, 0.08), range(0.05, 0.1) }));
        attr("generic/attribute/danger_sense", "Danger Sense", "of Awareness",
                "tensura:presence_sense_radius", ALL, "add_value",
                ranges(RE, new RangeVal[] { range(3, 8), range(5, 12), range(8, 16) }));
        attr("generic/attribute/analyst", "Analyst", "of Scrutiny",
                "tensura:analysis_level", ALL, "add_value",
                ranges(RE, new RangeVal[] { range(1, 2), range(2, 3), range(3, 5) }));
        attr("generic/attribute/far_sight", "Far Sight", "of Foresight",
                "tensura:analysis_distance", ALL, "add_value",
                ranges(RE, new RangeVal[] { range(5, 10), range(8, 16), range(12, 24) }));
        attr("generic/attribute/heatseeker", "Heatseeker", "of Detection",
                "tensura:heat_sense_radius", ALL, "add_value",
                ranges(RE, new RangeVal[] { range(5, 10), range(8, 16), range(12, 25) }));
        Map<Rarity, ApotheosisTables.ProcSpec> web = procs(RE, new ApotheosisTables.ProcSpec[] {
                new ApotheosisTables.ProcSpec(range(60, 80, 20), 0, 160),
                new ApotheosisTables.ProcSpec(range(80, 100, 20), 0, 150),
                new ApotheosisTables.ProcSpec(range(100, 140, 20), range(0, 1, 0.5), 140) });
        proc("generic/mob_effect/entangling", "Entangling", "of the Spider",
                "tensura:webbed", "attack_target", ALL, web, 20);
    }

    /** Every shipped affix, keyed by its path inside the jar. */
    public static Map<String, JsonObject> files() {
        if (AFFIXES.size() != 55) {
            throw new IllegalStateException("affix table is " + AFFIXES.size() + " expected 55 "
                    + "(the GameTest asserts exactly 55)");
        }
        Map<String, JsonObject> files = new LinkedHashMap<>();
        for (AffixDef affix : AFFIXES) {
            files.put("data/tensura_apotheosis/affixes/" + affix.rel() + ".json", affix.body());
        }
        return files;
    }

    /** Lang keys: the affix name and its suffix. */
    public static void lang(Map<String, String> lang) {
        for (AffixDef affix : AFFIXES) {
            lang.put("affix.tensura_apotheosis:" + affix.rel(), affix.name());
            lang.put("affix.tensura_apotheosis:" + affix.rel() + ".suffix", affix.suffix());
        }
    }

    private static JsonObject valuesJson(Map<Rarity, RangeVal> values) {
        JsonObject json = new JsonObject();
        values.forEach((rarity, range) -> {
            JsonObject rangeJson = new JsonObject();
            num(rangeJson, "min", range.min());
            num(rangeJson, "max", range.max());
            if (range.step() != null) {
                num(rangeJson, "step", range.step());
            }
            json.add(rarity.id(), rangeJson);
        });
        return json;
    }

    private static JsonObject procValuesJson(Map<Rarity, ApotheosisTables.ProcSpec> values) {
        JsonObject json = new JsonObject();
        values.forEach((rarity, spec) -> {
            JsonObject specJson = new JsonObject();
            specJson.add("duration", valueJson(spec.duration()));
            specJson.add("amplifier", valueJson(spec.amplifier()));
            num(specJson, "cooldown", spec.cooldown());
            json.add(rarity.id(), specJson);
        });
        return json;
    }

    /** A proc component: a range object or a plain constant. */
    private static JsonElement valueJson(Object value) {
        if (value instanceof RangeVal range) {
            JsonObject rangeJson = new JsonObject();
            num(rangeJson, "min", range.min());
            num(rangeJson, "max", range.max());
            if (range.step() != null) {
                num(rangeJson, "step", range.step());
            }
            return rangeJson;
        }
        Number number = (Number) value;
        if (number.doubleValue() == Math.rint(number.doubleValue())) {
            return new JsonPrimitive(number.longValue());
        }
        return new JsonPrimitive(number.doubleValue());
    }

    /** Whole numbers emit as integers, matching the retired generator's output. */
    private static void num(JsonObject json, String key, double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            json.addProperty(key, (long) value);
        } else {
            json.addProperty(key, value);
        }
    }

    /** Count guard mirroring the GameTest's EXPECTED_AFFIXES. */
    static int count() {
        return AFFIXES.size();
    }
}
