package org.bitwisemadness.tensuraapotheosis.datagen;

import static org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.PURITIES;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.GemLine;
import org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.TierWeights;
import org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.WeightSpec;

/**
 * The 37 socket gems for Apotheosis' gem registry, as tables, plus the gem
 * item models (in Apotheosis' model namespace) and the gem lang keys.
 *
 * <p>Textures are never generated: the original gems keep their own PNGs and
 * later gems pass an existing Apotheosis gem texture as a placeholder until
 * hand-drawn art arrives. The gem classes reuse Apotheosis' canonical keys so
 * tooltips resolve against its lang.</p>
 */
public final class GemContent {

    private GemContent() {
    }

    /**
     * One shipped gem: family/name (the registry path), display name, weight
     * block, bonus lines, minimum purity, uniqueness and the model texture.
     */
    public record GemDef(String family, String name, String display, Object weights,
            List<GemLine> bonuses, String minPurity, boolean unique, String tex) {
    }

    private static final List<GemDef> GEMS = new ArrayList<>();

    private static void gem(String family, String name, String display, Object weights,
            List<GemLine> bonuses) {
        gem(family, name, display, weights, bonuses, "cracked", false, null);
    }

    private static void gem(String family, String name, String display, Object weights,
            List<GemLine> bonuses, String minPurity, boolean unique) {
        gem(family, name, display, weights, bonuses, minPurity, unique, null);
    }

    private static void gem(String family, String name, String display, Object weights,
            List<GemLine> bonuses, String minPurity, boolean unique, String tex) {
        String texture = tex == null ? "tensura_apotheosis:items/gems/" + name : tex;
        GEMS.add(new GemDef(family, name, display, weights, bonuses, minPurity, unique, texture));
    }

    private static GemLine line(String gemClass, String attribute, String operation, double... purity) {
        return new GemLine(gemClass, attribute, operation, purity);
    }

    static {
        // ---- core gems: flat weights, cracked floor ----
        gem("core", "magicule", "Magicule Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("body", "tensura:max_magicule", "add_value", 20, 35, 55, 90, 140, 210),
                line("weapon", "tensura:magicule_gain", "add_value", 0.05, 0.08, 0.12, 0.18, 0.25, 0.35)));
        gem("core", "aura", "Aura Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("body", "tensura:max_aura", "add_value", 15, 30, 50, 85, 130, 200),
                line("boots", "tensura:aura_regeneration_multiplier", "add_multiplied_total",
                        0.02, 0.04, 0.07, 0.1, 0.14, 0.2)));
        gem("core", "soul", "Soul Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("body", "tensura:max_spiritual_health", "add_value", 8, 14, 22, 35, 55, 80),
                line("helmet", "tensura:spiritual_health_regeneration", "add_value", 0.2, 0.4, 0.6, 1, 1.4, 2)));
        gem("core", "predator", "Predator Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("weapon", "apothic_attributes:life_steal", "add_value", 0.02, 0.03, 0.05, 0.07, 0.09, 0.12),
                line("bow", "apothic_attributes:current_hp_damage", "add_value", 0.03, 0.05, 0.08, 0.12, 0.16, 0.22)));
        gem("core", "doppelganger", "Doppelganger Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("body", "tensura:melee_dodge_chance", "add_value", 1, 2, 3, 5, 7, 10),
                line("boots", "tensura:projectile_dodge_chance", "add_value", 1, 2, 3, 5, 7, 10)));
        gem("core", "analyst", "Analyst Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("helmet", "tensura:analysis_level", "add_value", 1, 1, 1, 2, 2, 3),
                line("boots", "tensura:analysis_distance", "add_value", 3, 5, 8, 12, 16, 22),
                line("body", "tensura:presence_sense_radius", "add_value", 2, 4, 6, 9, 12, 16)));
        gem("core", "greedy", "Greedy Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("tool", "apothic_attributes:mining_speed", "add_multiplied_total",
                        0.05, 0.1, 0.15, 0.22, 0.3, 0.4),
                line("helmet", "apothic_attributes:experience_gained", "add_multiplied_total",
                        0.05, 0.08, 0.12, 0.18, 0.25, 0.35),
                line("body", "apothic_attributes:cooldown_reduction", "add_value", 0.01, 0.015, 0.02, 0.03, 0.04, 0.05)));
        gem("core", "swift", "Swift Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("body", "tensura:chant_speed", "add_multiplied_total", 0.03, 0.05, 0.08, 0.12, 0.16, 0.22),
                line("weapon", "tensura:magic_cost_multiplier", "add_value", -0.05, -0.07, -0.09, -0.12, -0.15, -0.18),
                line("bow", "apothic_attributes:draw_speed", "add_multiplied_total", 0.05, 0.08, 0.12, 0.16, 0.22, 0.3)));

        // ---- dragonscales: per-world-tier weights, flawed floor, unique ----
        gem("dragonscale", "flame", "Flame Dragonscale", ApotheosisTables.TIER_WEIGHTS, List.of(
                line("weapon", "apothic_attributes:fire_damage", "add_value", 2.5, 3.5, 5, 6.5, 8, 10),
                line("body", "tensura:flame_boost", "add_value", 3, 5, 8, 12, 16, 22),
                line("boots", "tensura:flame_resistance", "add_value", 2, 4, 6, 9, 12, 16)), "flawed", true);
        gem("dragonscale", "frost", "Frost Dragonscale", ApotheosisTables.TIER_WEIGHTS, List.of(
                line("weapon", "apothic_attributes:cold_damage", "add_value", 2.5, 3.5, 5, 6.5, 8, 10),
                line("body", "tensura:water_boost", "add_value", 3, 5, 8, 12, 16, 22),
                line("boots", "tensura:water_resistance", "add_value", 2, 4, 6, 9, 12, 16)), "flawed", true);
        gem("dragonscale", "storm", "Storm Dragonscale", ApotheosisTables.TIER_WEIGHTS, List.of(
                line("weapon", "tensura:lightning_boost", "add_value", 3, 5, 8, 12, 16, 22),
                // Tensura has no lightning_resistance attribute - space resistance stands
                // in for the storm's dimensional gales.
                line("body", "tensura:space_resistance", "add_value", 2, 4, 6, 9, 12, 16),
                line("boots", "tensura:wind_resistance", "add_value", 2, 4, 6, 9, 12, 16)), "flawed", true);

        // ---- the three unique cores ----
        gem("holy", "core", "Holy Core", ApotheosisTables.TIER_WEIGHTS, List.of(
                line("weapon", "tensura:light_boost", "add_value", 3, 5, 8, 12, 16, 22),
                line("body", "apothic_attributes:healing_received", "add_multiplied_total",
                        0.05, 0.08, 0.12, 0.16, 0.2, 0.26),
                line("helmet", "tensura:light_resistance", "add_value", 3, 5, 8, 12, 16, 22)),
                "flawed", true, "tensura_apotheosis:items/gems/holy");
        gem("spirit", "core", "Spirit Core", ApotheosisTables.TIER_WEIGHTS, List.of(
                line("body", "tensura:magic_barrier", "add_value", 3, 5, 7, 10, 13, 17),
                line("shield", "tensura:physical_barrier", "add_value", 3, 5, 7, 10, 13, 17),
                line("helmet", "tensura:magic_resistance", "add_value", 2, 4, 6, 9, 12, 16)),
                "flawed", true, "tensura_apotheosis:items/gems/spirit");
        gem("ultima", "core", "Ultima Gem", ApotheosisTables.TIER_WEIGHTS, List.of(
                line("weapon", "apothic_attributes:crit_chance", "add_value", 0.1, 0.14, 0.18, 0.24, 0.3, 0.38),
                line("body", "tensura:magic_barrier", "add_value", 5, 7, 9, 12, 15, 19),
                line("helmet", "tensura:ability_mastery_gain", "add_value", 0.1, 0.15, 0.2, 0.3, 0.4, 0.55),
                line("shield", "apothic_attributes:cooldown_reduction", "add_value",
                        0.02, 0.025, 0.03, 0.04, 0.05, 0.06)),
                "flawless", true, "tensura_apotheosis:items/gems/ultima");

        // ---- additional gems: flat weights, Apotheosis placeholder textures ----
        gem("core", "barrier", "Barrier Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("body", "tensura:multilayer_barrier", "add_value", 1, 1, 2, 2, 3, 4),
                line("shield", "tensura:magic_barrier", "add_value", 2, 4, 6, 9, 12, 16)),
                "cracked", false, "apotheosis:items/gems/guardian");
        gem("core", "shadow", "Shadow Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("helmet", "tensura:presence_concealment", "add_value", 3, 5, 8, 11, 15, 20),
                line("boots", "tensura:dodge_strength", "add_value", 1, 2, 3, 5, 7, 10)),
                "cracked", false, "apotheosis:items/gems/endersurge");
        gem("core", "vigil", "Vigil Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("helmet", "tensura:heat_sense_radius", "add_value", 3, 5, 8, 12, 16, 22),
                line("body", "tensura:presence_sense_radius", "add_value", 2, 4, 6, 9, 12, 16)),
                "cracked", false, "apotheosis:items/gems/inferno");
        gem("core", "stalwart", "Stalwart Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("body", "minecraft:generic.armor", "add_value", 2, 3, 4, 5, 6, 8),
                line("boots", "minecraft:generic.armor_toughness", "add_value", 1, 2, 3, 4, 5, 7)),
                "cracked", false, "apotheosis:items/gems/ballast");
        gem("core", "gale", "Gale Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("boots", "minecraft:generic.movement_speed", "add_multiplied_total",
                        0.02, 0.03, 0.04, 0.06, 0.08, 0.1),
                line("body", "tensura:wind_resistance", "add_value", 1, 2, 3, 4, 6, 8)),
                "cracked", false, "apotheosis:items/gems/slipstream");
        gem("core", "earthen", "Earthen Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("body", "minecraft:generic.knockback_resistance", "add_value", 0.05, 0.1, 0.15, 0.2, 0.3, 0.4),
                line("boots", "tensura:earth_resistance", "add_value", 2, 4, 6, 9, 12, 16)),
                "cracked", false, "apotheosis:items/gems/verdant_ruin");
        gem("core", "fury", "Fury Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("weapon", "minecraft:generic.attack_damage", "add_multiplied_total",
                        0.03, 0.05, 0.07, 0.1, 0.13, 0.17),
                line("bow", "apothic_attributes:arrow_damage", "add_multiplied_total",
                        0.04, 0.06, 0.09, 0.12, 0.16, 0.2)),
                "cracked", false, "apotheosis:items/gems/tyrannical");
        gem("core", "vigor", "Vigor Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("body", "minecraft:generic.max_health", "add_value", 3, 5, 8, 12, 16, 22),
                line("helmet", "tensura:spiritual_health_regeneration", "add_value", 0.1, 0.2, 0.3, 0.5, 0.7, 1)),
                "cracked", false, "apotheosis:items/gems/solar");
        gem("core", "juggernaut", "Juggernaut Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("body", "minecraft:generic.knockback_resistance", "add_value", 0.1, 0.15, 0.2, 0.3, 0.4, 0.5),
                line("boots", "minecraft:generic.armor", "add_value", 1, 2, 3, 4, 5, 7)),
                "cracked", false, "apotheosis:items/gems/warlord");
        gem("core", "striker", "Striker Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("weapon", "minecraft:generic.attack_damage", "add_multiplied_total",
                        0.04, 0.06, 0.08, 0.11, 0.14, 0.18),
                line("bow", "apothic_attributes:arrow_damage", "add_multiplied_total",
                        0.05, 0.07, 0.1, 0.13, 0.17, 0.21)),
                "cracked", false, "apotheosis:items/gems/combatant");
        gem("core", "precision", "Precision Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("bow", "apothic_attributes:crit_chance", "add_value", 0.05, 0.07, 0.1, 0.13, 0.16, 0.2),
                line("weapon", "apothic_attributes:armor_pierce", "add_value", 2, 3, 4, 6, 8, 10)),
                "cracked", false, "apotheosis:items/gems/breach");
        gem("core", "sprinter", "Sprinter Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("boots", "minecraft:generic.movement_speed", "add_multiplied_total",
                        0.01, 0.02, 0.03, 0.04, 0.06, 0.08),
                line("body", "minecraft:generic.safe_fall_distance", "add_value", 2, 3, 4, 6, 8, 11)),
                "cracked", false, "apotheosis:items/gems/samurai");
        gem("core", "brawler", "Brawler Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("weapon", "minecraft:generic.attack_speed", "add_value", 0.05, 0.08, 0.1, 0.14, 0.18, 0.22),
                line("helmet", "minecraft:generic.armor_toughness", "add_value", 1, 2, 3, 4, 5, 7)),
                "cracked", false, "apotheosis:items/gems/brawlers");
        gem("core", "mystic", "Mystic Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("helmet", "tensura:magic_resistance", "add_value", 2, 3, 5, 7, 9, 12),
                line("body", "tensura:magic_barrier", "add_value", 2, 3, 5, 7, 9, 12)),
                "cracked", false, "apotheosis:items/gems/mageslayer");
        gem("core", "executioner", "Executioner Gem", ApotheosisTables.FLAT_WEIGHTS, List.of(
                line("weapon", "apothic_attributes:current_hp_damage", "add_value", 0.04, 0.06, 0.09, 0.12, 0.15, 0.19),
                line("bow", "apothic_attributes:crit_damage", "add_value", 0.08, 0.11, 0.14, 0.18, 0.22, 0.27)),
                "cracked", false, "apotheosis:items/gems/blood_lord");

        // ---- the remaining unique-tier gems ----
        gem("dragonscale", "earth", "Earth Dragonscale", ApotheosisTables.TIER_WEIGHTS, List.of(
                line("weapon", "tensura:earth_boost", "add_value", 3, 5, 8, 12, 16, 22),
                line("body", "tensura:earth_resistance", "add_value", 2, 4, 6, 9, 12, 16),
                line("helmet", "minecraft:generic.armor", "add_value", 2, 3, 4, 5, 6, 8)),
                "flawed", true, "apotheosis:items/gems/earth");
        gem("dragonscale", "darkness", "Darkness Dragonscale", ApotheosisTables.TIER_WEIGHTS, List.of(
                line("weapon", "tensura:darkness_boost", "add_value", 3, 5, 8, 12, 16, 22),
                line("body", "tensura:darkness_resistance", "add_value", 2, 4, 6, 9, 12, 16),
                line("helmet", "tensura:dark_vision", "add_value", 1, 1, 2, 2, 3, 4)),
                "flawed", true, "apotheosis:items/gems/queen");
        gem("dragonscale", "wind", "Wind Dragonscale", ApotheosisTables.TIER_WEIGHTS, List.of(
                line("weapon", "tensura:wind_boost", "add_value", 3, 5, 8, 12, 16, 22),
                line("body", "tensura:wind_resistance", "add_value", 2, 4, 6, 9, 12, 16),
                line("boots", "minecraft:generic.movement_speed", "add_multiplied_total",
                        0.02, 0.03, 0.05, 0.07, 0.09, 0.12)),
                "flawed", true, "apotheosis:items/gems/lightning");
        gem("dragonscale", "gravity", "Gravity Dragonscale", ApotheosisTables.TIER_WEIGHTS, List.of(
                line("weapon", "tensura:gravity_boost", "add_value", 3, 5, 8, 12, 16, 22),
                line("body", "minecraft:generic.max_health", "add_value", 5, 8, 12, 16, 22, 30),
                line("boots", "minecraft:generic.knockback_resistance", "add_value", 0.05, 0.1, 0.15, 0.2, 0.3, 0.4)),
                "flawed", true, "apotheosis:items/gems/lunar");
        gem("holy", "judgment", "Judgment Core", ApotheosisTables.TIER_WEIGHTS, List.of(
                line("bow", "tensura:light_boost", "add_value", 3, 5, 8, 12, 16, 22),
                line("body", "tensura:light_resistance", "add_value", 2, 4, 6, 9, 12, 16)),
                "flawed", true, "apotheosis:items/gems/splendor");
        gem("spirit", "guardian", "Guardian Core", ApotheosisTables.TIER_WEIGHTS, List.of(
                line("shield", "minecraft:generic.armor", "add_value", 2, 3, 4, 6, 8, 10),
                line("body", "minecraft:generic.max_health", "add_value", 5, 8, 12, 16, 22, 30)),
                "flawed", true, "apotheosis:items/gems/molten_breach");
        gem("spirit", "mender", "Mender Core", ApotheosisTables.TIER_WEIGHTS, List.of(
                line("body", "apothic_attributes:healing_received", "add_multiplied_total",
                        0.04, 0.06, 0.09, 0.12, 0.16, 0.2),
                line("helmet", "tensura:spiritual_health_regeneration", "add_value", 0.15, 0.3, 0.45, 0.75, 1, 1.5)),
                "flawed", true, "apotheosis:items/gems/forest");
        gem("ultima", "sovereign", "Sovereign Gem", ApotheosisTables.TIER_WEIGHTS, List.of(
                line("weapon", "apothic_attributes:crit_damage", "add_value", 0.12, 0.16, 0.2, 0.26, 0.32, 0.4),
                line("body", "tensura:max_magicule", "add_value", 15, 25, 40, 60, 90, 130),
                line("shield", "apothic_attributes:cooldown_reduction", "add_value",
                        0.02, 0.025, 0.03, 0.04, 0.05, 0.06)),
                "flawless", true, "apotheosis:items/gems/royalty");
    }

    /** The gem data files plus the gem item models, keyed by path inside the jar. */
    public static Map<String, JsonObject> files() {
        if (GEMS.size() != 37) {
            throw new IllegalStateException("gem table is " + GEMS.size() + " expected 37 "
                    + "(the GameTest asserts exactly 37)");
        }
        Map<String, JsonObject> files = new LinkedHashMap<>();
        for (GemDef gem : GEMS) {
            String rel = gem.family() + "/" + gem.name();
            files.put("data/tensura_apotheosis/gems/" + rel + ".json", gemJson(gem));
            files.put("assets/apotheosis/models/item/gems/" + rel + ".json", modelJson(gem));
        }
        return files;
    }

    /** Lang keys: the gem display names. */
    public static void lang(Map<String, String> lang) {
        for (GemDef gem : GEMS) {
            lang.put("item.apotheosis.gem.tensura_apotheosis:" + gem.family() + "/" + gem.name(),
                    gem.display());
        }
    }

    private static JsonObject gemJson(GemDef gem) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:gem");
        json.add("weights", weightsJson(gem.weights()));
        if (!"cracked".equals(gem.minPurity())) {
            json.addProperty("min_purity", gem.minPurity());
        }
        if (gem.unique()) {
            json.addProperty("unique", true);
        }
        JsonArray bonuses = new JsonArray();
        for (GemLine bonus : gem.bonuses()) {
            bonuses.add(bonusJson(bonus));
        }
        json.add("bonuses", bonuses);
        return json;
    }

    private static JsonObject weightsJson(Object weights) {
        JsonObject json = new JsonObject();
        if (weights instanceof WeightSpec flat) {
            json.addProperty("quality", flat.quality());
            json.addProperty("weight", flat.weight());
        } else if (weights instanceof TierWeights tiered) {
            weightBlock(json, "frontier", tiered.frontier());
            weightBlock(json, "ascent", tiered.ascent());
            weightBlock(json, "summit", tiered.summit());
            weightBlock(json, "pinnacle", tiered.pinnacle());
        }
        return json;
    }

    private static void weightBlock(JsonObject json, String tier, WeightSpec spec) {
        JsonObject block = new JsonObject();
        block.addProperty("quality", spec.quality());
        block.addProperty("weight", spec.weight());
        json.add(tier, block);
    }

    private static JsonObject bonusJson(GemLine bonus) {
        Map.Entry<String, List<String>> gemClass = ApotheosisTables.GEM_CLASSES.get(bonus.gemClass());
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:attribute");
        json.addProperty("attribute", bonus.attribute());
        JsonObject classJson = new JsonObject();
        classJson.addProperty("key", gemClass.getKey());
        JsonArray types = new JsonArray();
        gemClass.getValue().forEach(types::add);
        classJson.add("types", types);
        json.add("gem_class", classJson);
        json.addProperty("operation", bonus.operation());
        JsonObject values = new JsonObject();
        for (int i = 0; i < PURITIES.size(); i++) {
            double v = bonus.purity()[i];
            if (v == Math.rint(v)) {
                values.addProperty(PURITIES.get(i), (long) v);
            } else {
                values.addProperty(PURITIES.get(i), v);
            }
        }
        json.add("values", values);
        return json;
    }

    private static JsonObject modelJson(GemDef gem) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", gem.tex());
        json.add("textures", textures);
        return json;
    }
}
