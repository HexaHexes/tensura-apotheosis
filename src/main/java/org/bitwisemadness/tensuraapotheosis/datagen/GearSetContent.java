package org.bitwisemadness.tensuraapotheosis.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.ArmorSpec;
import org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.Theme;

/**
 * The 80 Placebo gear sets: Track A injects Tensura gear into Apotheosis'
 * vanilla tier pools (the four built-in elites roll it alongside vanilla
 * sets), Track B emits the fourteen identity-themed sets referenced only by
 * the 38 Tensura elites so themed gear never leaks onto vanilla bosses.
 */
public final class GearSetContent {

    private GearSetContent() {
    }

    /** One weighted stack entry in a gear set slot. */
    private record Stack(String id, int weight) {
    }

    /** One assembled gear set: registry path plus the JSON body. */
    private record GearSet(String rel, JsonObject json) {
    }

    private static final List<GearSet> GEAR_SETS = new ArrayList<>();

    static {
        // ---- Track A: Tensura gear in Apotheosis' vanilla tier pools.
        // The four built-in elites reference these tags; weights mirror
        // Apotheosis' own ladder (haven 100 to pinnacle 40).
        List<ArmorSpec> trackArmor = List.of(
                new ArmorSpec("leather", "d"), new ArmorSpec("leather", "c"),
                new ArmorSpec("leather", "b"), new ArmorSpec("leather", "a"),
                new ArmorSpec("orichalcum", null));
        List<ArmorSpec> trackAArmor = trackArmor;
        List<List<String>> trackAMelee = List.of(
                List.of("tensura:wooden_katana", "tensura:wooden_great_sword"),
                List.of("tensura:iron_katana", "tensura:iron_great_sword"),
                List.of("tensura:low_magisteel_katana", "tensura:low_magisteel_spear"),
                List.of("tensura:high_magisteel_katana", "tensura:mithril_great_sword"),
                List.of("tensura:orichalcum_katana", "tensura:hihiirokane_great_sword"));
        List<List<String>> trackARanged = List.of(
                List.of("tensura:short_bow"),
                List.of("tensura:short_bow", "tensura:long_bow"),
                List.of("tensura:long_bow"),
                List.of("tensura:war_bow", "tensura:spider_bow"),
                List.of("tensura:war_bow", "tensura:war_spider_bow", "tensura:ant_crossbow"));
        int[] trackAWeight = { 100, 90, 70, 50, 40 };
        for (int i = 0; i < ApotheosisTables.TIERS.size(); i++) {
            String tier = ApotheosisTables.TIERS.get(i);
            GEAR_SETS.add(new GearSet(tier + "/melee/tensura", gearSetJson(
                    List.of(tier + "_melee"), stacks(trackAMelee.get(i)), trackAArmor.get(i), null,
                    trackAWeight[i])));
            GEAR_SETS.add(new GearSet(tier + "/ranged/tensura", gearSetJson(
                    List.of(tier + "_ranged"), stacks(trackARanged.get(i)), trackAArmor.get(i), null,
                    trackAWeight[i])));
        }

        // ---- Track B: identity-themed sets for the 38 Tensura elites ----
        // Each themed tag resolves to exactly one set, so weight is cosmetic.
        for (Theme theme : ApotheosisTables.THEMES) {
            for (int i = 0; i < ApotheosisTables.TIERS.size(); i++) {
                String tier = ApotheosisTables.TIERS.get(i);
                String barding = theme.barding().isEmpty()
                        ? null
                        : ApotheosisTables.BARDING_BY_TIER.get(i);
                ArmorSpec armor = barding == null && i < theme.armor().size() ? theme.armor().get(i) : null;
                GEAR_SETS.add(new GearSet("themes/" + theme.key() + "/" + tier, gearSetJson(
                        List.of("tensura_" + theme.key() + "_" + tier), stacks(theme.weapons().get(i)),
                        armor, barding, 100)));
            }
        }
    }

    private static List<Stack> stacks(List<String> ids) {
        List<Stack> out = new ArrayList<>();
        for (String id : ids) {
            out.add(new Stack(id, 10));
        }
        return out;
    }

    private static JsonObject gearSetJson(List<String> tags, List<Stack> mainhands,
            ArmorSpec armor, String barding, int weight) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "placebo:gear_set");
        if (mainhands != null && !mainhands.isEmpty()) {
            json.add("mainhands", stackArray(mainhands));
        }
        if (barding != null) {
            // AnimalArmorItem horse barding occupies the chest slot; only mobs whose
            // renderer has an *ArmorLayer (pegasus/unicorn/hover_lizard) show it.
            json.add("chestplates", stackArray(List.of(
                    new Stack("minecraft:" + barding + "_horse_armor", 10))));
        } else if (armor != null) {
            json.add("helmets", stackArray(List.of(new Stack(armorId(armor, "helmet"), 10))));
            json.add("chestplates", stackArray(List.of(new Stack(armorId(armor, "chestplate"), 10))));
            json.add("leggings", stackArray(List.of(new Stack(armorId(armor, "leggings"), 10))));
            json.add("boots", stackArray(List.of(new Stack(armorId(armor, "boots"), 10))));
        }
        JsonArray tagArray = new JsonArray();
        tags.forEach(tagArray::add);
        json.add("tags", tagArray);
        json.addProperty("weight", weight);
        return json;
    }

    /** Armor ids for a material: monster leather carries the grade, others do not. */
    private static String armorId(ArmorSpec armor, String slot) {
        return armor.grade() != null
                ? "tensura:monster_leather_" + slot + "_" + armor.grade()
                : "tensura:" + armor.mat() + "_" + slot;
    }

    private static JsonArray stackArray(List<Stack> stacks) {
        JsonArray array = new JsonArray();
        for (Stack stack : stacks) {
            JsonObject entry = new JsonObject();
            JsonObject stackJson = new JsonObject();
            stackJson.addProperty("count", 1);
            stackJson.addProperty("id", stack.id());
            entry.add("stack", stackJson);
            entry.addProperty("weight", stack.weight());
            array.add(entry);
        }
        return array;
    }

    /** Every shipped gear set, keyed by its path inside the jar. */
    public static Map<String, JsonObject> files() {
        if (GEAR_SETS.size() != 80) {
            throw new IllegalStateException("gear set table is " + GEAR_SETS.size() + " expected 80 "
                    + "(10 track-A + 14 themes x 5 tiers; the GameTest asserts exactly 80)");
        }
        Map<String, JsonObject> files = new LinkedHashMap<>();
        for (GearSet set : GEAR_SETS) {
            files.put("data/tensura_apotheosis/gear_sets/" + set.rel() + ".json", set.json());
        }
        return files;
    }
}
