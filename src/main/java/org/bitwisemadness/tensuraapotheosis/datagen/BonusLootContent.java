package org.bitwisemadness.tensuraapotheosis.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.Extra;
import org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.TierProfile;

/**
 * The 19 elite bonus-loot tables (Apotheosis' BonusLootTables - extra kill
 * drops rolled on top of the base mob's own table): five world-tier tables
 * scaling a gem-dust pile, a random-gem shot and a rare diamond with the
 * tier, plus fourteen family tables carrying the theme's flavor stack and
 * verified Tensura extras.
 */
public final class BonusLootContent {

    private BonusLootContent() {
    }

    /** Every bonus loot table, keyed by its path inside the jar. */
    public static Map<String, JsonObject> files() {
        Map<String, JsonObject> files = new LinkedHashMap<>();
        for (String tier : ApotheosisTables.TIERS) {
            TierProfile profile = ApotheosisTables.TIER_PROFILES.get(tier);
            files.put("data/tensura_apotheosis/loot_table/bonus/" + tier + ".json",
                    tierTable(tier, profile));
        }
        for (Map.Entry<String, ApotheosisTables.ThemeIdentity> identity
                : ApotheosisTables.THEME_IDENTITY.entrySet()) {
            List<Extra> extras = ApotheosisTables.FAMILY_EXTRAS.get(identity.getKey());
            files.put("data/tensura_apotheosis/loot_table/bonus/" + identity.getKey() + ".json",
                    familyTable(identity.getKey(), identity.getValue(),
                            extras == null ? List.of() : extras));
        }
        if (files.size() != 19) {
            throw new IllegalStateException("bonus loot tables = " + files.size() + " expected 19 "
                    + "(5 tier + 14 family; the GameTest asserts exactly 19)");
        }
        return files;
    }

    /**
     * Tier core table: pool 1 is dust-or-gem (dust weight 70, a random gem from
     * the profile, empty the rest), pool 2 is the rare diamond (skipped at
     * haven, whose diamond weight is zero).
     */
    private static JsonObject tierTable(String tier, TierProfile profile) {
        JsonArray pool1Entries = new JsonArray();
        JsonObject dust = new JsonObject();
        dust.addProperty("type", "minecraft:item");
        dust.addProperty("name", "apotheosis:gem_dust");
        dust.addProperty("weight", 70);
        JsonArray dustFunctions = new JsonArray();
        JsonObject setCount = new JsonObject();
        setCount.addProperty("function", "minecraft:set_count");
        JsonObject count = new JsonObject();
        count.addProperty("min", profile.bonusDust()[0]);
        count.addProperty("max", profile.bonusDust()[1]);
        setCount.add("count", count);
        dustFunctions.add(setCount);
        dust.add("functions", dustFunctions);
        pool1Entries.add(dust);
        JsonObject randomGem = new JsonObject();
        randomGem.addProperty("type", "apotheosis:random_gem");
        randomGem.addProperty("quality", 1);
        randomGem.addProperty("weight", profile.bonusGemWeight());
        pool1Entries.add(randomGem);
        JsonObject empty = new JsonObject();
        empty.addProperty("type", "minecraft:empty");
        empty.addProperty("weight", Math.max(10, 30 - profile.bonusGemWeight()));
        pool1Entries.add(empty);

        JsonArray pools = new JsonArray();
        pools.add(pool(1.0, pool1Entries));
        if (profile.bonusDiamondWeight() > 0) {
            JsonArray pool2Entries = new JsonArray();
            JsonObject diamond = new JsonObject();
            diamond.addProperty("type", "minecraft:item");
            diamond.addProperty("name", "minecraft:diamond");
            diamond.addProperty("weight", profile.bonusDiamondWeight());
            JsonArray diamondFunctions = new JsonArray();
            JsonObject diamondCount = new JsonObject();
            diamondCount.addProperty("function", "minecraft:set_count");
            JsonObject diamondRange = new JsonObject();
            diamondRange.addProperty("min", 1);
            diamondRange.addProperty("max", Math.max(1, profile.bonusDiamondWeight() / 6));
            diamondCount.add("count", diamondRange);
            diamondFunctions.add(diamondCount);
            diamond.add("functions", diamondFunctions);
            pool2Entries.add(diamond);
            JsonObject pool2Empty = new JsonObject();
            pool2Empty.addProperty("type", "minecraft:empty");
            pool2Empty.addProperty("weight", 100 - profile.bonusDiamondWeight());
            pool2Entries.add(pool2Empty);
            pools.add(pool(1.0, pool2Entries));
        }

        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:entity");
        json.add("pools", pools);
        json.addProperty("random_sequence", "tensura_apotheosis:bonus/" + tier);
        return json;
    }

    /** Family theme table: the flavor stack (1-3) plus optional Tensura extras. */
    private static JsonObject familyTable(String themeKey,
            ApotheosisTables.ThemeIdentity identity, List<Extra> extras) {
        JsonArray entries = new JsonArray();
        JsonObject stack = new JsonObject();
        stack.addProperty("type", "minecraft:item");
        stack.addProperty("name", identity.stack());
        stack.addProperty("weight", 60);
        JsonArray stackFunctions = new JsonArray();
        JsonObject setCount = new JsonObject();
        setCount.addProperty("function", "minecraft:set_count");
        JsonObject count = new JsonObject();
        count.addProperty("min", 1);
        count.addProperty("max", 3);
        setCount.add("count", count);
        stackFunctions.add(setCount);
        stack.add("functions", stackFunctions);
        entries.add(stack);
        for (Extra extra : extras) {
            JsonObject extraJson = new JsonObject();
            extraJson.addProperty("type", "minecraft:item");
            extraJson.addProperty("name", extra.name());
            extraJson.addProperty("weight", extra.weight());
            entries.add(extraJson);
        }
        JsonObject empty = new JsonObject();
        empty.addProperty("type", "minecraft:empty");
        empty.addProperty("weight", 40);
        entries.add(empty);

        JsonArray pools = new JsonArray();
        pools.add(pool(1.0, entries));
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:entity");
        json.add("pools", pools);
        json.addProperty("random_sequence", "tensura_apotheosis:bonus/" + themeKey);
        return json;
    }

    private static JsonObject pool(double rolls, JsonArray entries) {
        JsonObject json = new JsonObject();
        json.addProperty("rolls", rolls);
        json.addProperty("bonus_rolls", 0.0);
        json.add("entries", entries);
        return json;
    }
}
