package org.bitwisemadness.tensuraapotheosis.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.RosterMob;
import org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.ThemeIdentity;
import org.bitwisemadness.tensuraapotheosis.datagen.ApotheosisTables.TierProfile;

/**
 * The 39 Gateways-to-Eternity gates: one summonable boss gateway per Tensura
 * elite (a herald wave of the mob's lesser kin, then the elite itself via
 * Apotheosis' elite wave entity), plus the endless horde chase gateway granted
 * as a bonus pearl from every pinnacle gateway. Recipes ramp with the tier:
 * 2 ender pearls plus tier-scaled diamonds and a pinch of gem dust.
 */
public final class GatewayContent {

    private GatewayContent() {
    }

    /**
     * NOTE on the herald affix modifier: no rarities list. Its AffixData codec
     * resolves rarity values at DECODE time and Gateways' registry reloads
     * before Apotheosis' rarity registry binds (all 38 files fail with
     * "unbound value: apotheosis:common"). The omitted field falls back to
     * LootRarity.random with default weights - tier-aware for free. The static
     * band only belongs in elite affix_data and the affix_item reward.
     */
    public static Map<String, JsonObject> files(Map<String, String> tiersByMob) {
        Map<String, JsonObject> files = new LinkedHashMap<>();
        for (RosterMob roster : ApotheosisTables.ROSTER) {
            String band = tiersByMob.get(roster.mob());
            TierProfile profile = ApotheosisTables.TIER_PROFILES.get(band);
            files.put("data/tensura_apotheosis/gateways/elite/" + roster.mob() + ".json",
                    gatewayJson(roster, band, profile));
            files.put("data/tensura_apotheosis/recipe/gateways/elite/" + roster.mob() + ".json",
                    recipeJson(roster, profile));
        }
        files.put("data/tensura_apotheosis/gateways/endless/horde.json", endlessJson());
        return files;
    }

    /** Lang keys: the gateway display names (Gateways translates ':' -> '.'). */
    public static void lang(Map<String, String> lang) {
        for (RosterMob roster : ApotheosisTables.ROSTER) {
            lang.put("tensura_apotheosis.elite/" + roster.mob(), roster.display() + " Gateway");
        }
        lang.put("tensura_apotheosis.endless/horde", "Endless Tensura Horde Gateway");
    }

    private static JsonObject gatewayJson(RosterMob roster, String band, TierProfile profile) {
        ThemeIdentity identity = ApotheosisTables.THEME_IDENTITY.get(roster.theme());
        JsonObject settings = new JsonObject();
        settings.addProperty("soundtrack", profile.soundtrack());
        settings.addProperty("tier", band);
        settings.addProperty("size", profile.size());
        settings.addProperty("color", identity.color());

        JsonArray waves = new JsonArray();
        waves.add(wave(2400, 150, heraldEntities(roster), List.of(
                experienceReward(profile.heraldXp(), 60))));
        waves.add(bossWave(roster, profile));

        JsonArray rewards = new JsonArray();
        rewards.add(countedReward(profile.itemCount(), affixItemReward(profile.rewardRarities())));
        rewards.add(countedReward(profile.gemCount(), gemReward(profile, identity)));
        rewards.add(stackReward(profile.dust(), "apotheosis:gem_dust", null));
        rewards.add(stackReward(profile.stackCount(), identity.stack(), null));
        rewards.add(experienceReward(profile.finalXp(), 100));
        // Pinnacle gateways carry the chase pearl: a gate pearl summoning the
        // endless horde gateway (mirrors Apotheosis granting endless_invader
        // from their pinnacle tier).
        if (profile.chaseGateway() != null) {
            rewards.add(stackReward(1, "gateways:gate_pearl", "tensura_apotheosis:" + profile.chaseGateway()));
        }

        JsonObject rules = new JsonObject();
        rules.addProperty("leash_range", 48.0);
        rules.addProperty("lives", 3);
        rules.addProperty("requires_nearby_player", true);
        rules.addProperty("spawn_range", 24.0);

        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:tiered");
        json.add("settings", settings);
        json.add("waves", waves);
        json.add("rewards", rewards);
        json.add("rules", rules);
        return json;
    }

    private static JsonArray heraldEntities(RosterMob roster) {
        JsonArray entities = new JsonArray();
        JsonObject entity = new JsonObject();
        entity.addProperty("type", "gateways:standard");
        entity.addProperty("count", 3);
        entity.addProperty("entity", "tensura:" + roster.mob());
        JsonArray modifiers = new JsonArray();
        JsonObject health = new JsonObject();
        health.addProperty("type", "gateways:attribute");
        health.addProperty("attribute", "minecraft:generic.max_health");
        health.addProperty("operation", "add_multiplied_total");
        health.addProperty("value", 0.5);
        modifiers.add(health);
        JsonObject affix = new JsonObject();
        affix.addProperty("type", "apotheosis:affix");
        affix.addProperty("affix_chance", 0.5);
        modifiers.add(affix);
        entity.add("modifiers", modifiers);
        entities.add(entity);
        return entities;
    }

    private static JsonObject bossWave(RosterMob roster, TierProfile profile) {
        JsonObject base = new JsonObject();
        base.addProperty("type", "gateways:standard");
        base.addProperty("entity", "tensura:" + roster.mob());
        base.addProperty("finalize_spawn", false);
        JsonArray modifiers = new JsonArray();
        JsonObject health = new JsonObject();
        health.addProperty("type", "gateways:attribute");
        health.addProperty("attribute", "minecraft:generic.max_health");
        health.addProperty("operation", "add_multiplied_total");
        health.addProperty("value", 1.0);
        modifiers.add(health);
        JsonObject damage = new JsonObject();
        damage.addProperty("type", "gateways:attribute");
        damage.addProperty("attribute", "minecraft:generic.attack_damage");
        damage.addProperty("operation", "add_multiplied_total");
        damage.addProperty("value", 0.25);
        modifiers.add(damage);
        base.add("modifiers", modifiers);

        JsonObject entity = new JsonObject();
        entity.addProperty("type", "apotheosis:elite");
        entity.add("base_entity", base);
        entity.addProperty("desc", "elite.tensura_apotheosis." + roster.mob());
        entity.addProperty("elite", "tensura_apotheosis:" + roster.mob());

        JsonObject wave = new JsonObject();
        wave.addProperty("max_wave_time", 4800);
        wave.addProperty("setup_time", 200);
        JsonArray entities = new JsonArray();
        entities.add(entity);
        wave.add("entities", entities);
        JsonArray rewards = new JsonArray();
        rewards.add(experienceReward(profile.bossXp(), 100));
        wave.add("rewards", rewards);
        return wave;
    }

    private static JsonObject wave(int maxWaveTime, int setupTime, JsonArray entities,
            List<JsonObject> rewards) {
        JsonObject wave = new JsonObject();
        wave.addProperty("max_wave_time", maxWaveTime);
        wave.addProperty("setup_time", setupTime);
        wave.add("entities", entities);
        wave.add("rewards", jsonArray(rewards));
        return wave;
    }

    private static JsonArray jsonArray(List<JsonObject> items) {
        JsonArray array = new JsonArray();
        items.forEach(array::add);
        return array;
    }

    private static JsonObject countedReward(int count, JsonObject reward) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "gateways:counted");
        json.addProperty("count", count);
        json.add("reward", reward);
        return json;
    }

    private static JsonObject affixItemReward(List<String> rarities) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:affix_item");
        JsonArray array = new JsonArray();
        rarities.forEach(array::add);
        json.add("rarities", array);
        return json;
    }

    private static JsonObject gemReward(TierProfile profile, ThemeIdentity identity) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:gem");
        JsonArray purities = new JsonArray();
        profile.purities().forEach(purities::add);
        json.add("purities", purities);
        JsonArray gems = new JsonArray();
        identity.gems().forEach(gem -> gems.add("tensura_apotheosis:" + gem));
        json.add("gems", gems);
        return json;
    }

    private static JsonObject stackReward(int count, String id, String gatewayComponent) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "gateways:stack");
        JsonObject stack = new JsonObject();
        stack.addProperty("count", count);
        stack.addProperty("id", id);
        if (gatewayComponent != null) {
            JsonObject components = new JsonObject();
            components.addProperty("gateways:gateway", gatewayComponent);
            stack.add("components", components);
        }
        json.add("stack", stack);
        return json;
    }

    private static JsonObject experienceReward(int experience, int orbSize) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "gateways:experience");
        json.addProperty("experience", experience);
        json.addProperty("orb_size", orbSize);
        return json;
    }

    private static JsonObject endlessJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "gateways:endless");
        json.addProperty("size", "large");
        // The gold color is deliberately outside the mob-family palette so it
        // reads as the chase activity.
        json.addProperty("color", "#FFD700");
        JsonArray baseEntities = new JsonArray();
        baseEntities.add(entityEntry(4, "tensura:hover_lizard"));
        baseEntities.add(entityEntry(3, "tensura:leech_lizard"));
        baseEntities.add(entityEntry(3, "tensura:army_wasp"));
        json.add("base_wave", wave(1600, 100, baseEntities, List.of(
                stackReward(4, "apotheosis:gem_dust", null),
                experienceReward(100, 100))));
        json.add("modifiers", hordeModifiers());
        json.addProperty("spawn_algorithm", "gateways:inward_spiral");
        JsonObject bossEvent = new JsonObject();
        bossEvent.addProperty("fog", false);
        bossEvent.addProperty("mode", "name_plate");
        json.add("boss_event", bossEvent);
        return json;
    }

    private static JsonArray hordeModifiers() {
        JsonArray modifiers = new JsonArray();
        // Every 2nd wave (up to 5 times): frontier-tier bruisers join the horde.
        modifiers.add(hordeModifier(2, 5,
                List.of(entityEntry(2, "tensura:direwolf"), entityEntry(2, "tensura:black_spider"),
                        entityEntry(2, "tensura:orc")),
                List.of(stackReward(2, "apotheosis:gem_dust", null),
                        chancedReward(0.2, affixItemReward(
                                List.of("apotheosis:uncommon", "apotheosis:rare")))),
                null));
        // Every 4th wave (up to 4 times): a greater daemon arrives, epic payout.
        modifiers.add(hordeModifier(4, 4,
                List.of(entityEntry(1, "tensura:greater_daemon")),
                List.of(stackReward(8, "apotheosis:gem_dust", null),
                        chancedReward(0.5, affixItemReward(
                                List.of("apotheosis:epic", "apotheosis:mythic")))),
                null));
        // Every 6th wave (up to 3 times): the apex - arch daemon, mythic item.
        modifiers.add(hordeModifier(6, 3,
                List.of(entityEntry(1, "tensura:arch_daemon")),
                List.of(stackReward(16, "apotheosis:gem_dust", null),
                        affixItemReward(List.of("apotheosis:mythic"))),
                null));
        // The horde levels up: waves keep stacking max-health modifiers.
        JsonArray healthModifiers = new JsonArray();
        JsonObject health = new JsonObject();
        health.addProperty("type", "gateways:attribute");
        health.addProperty("attribute", "minecraft:generic.max_health");
        health.addProperty("operation", "add_multiplied_total");
        health.addProperty("value", 0.2);
        healthModifiers.add(health);
        modifiers.add(hordeModifier(3, 5, List.of(), List.of(), healthModifiers));
        return modifiers;
    }

    private static JsonObject entityEntry(int count, String entity) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "gateways:standard");
        json.addProperty("count", count);
        json.addProperty("entity", entity);
        return json;
    }

    private static JsonObject hordeModifier(int waves, int max, List<JsonObject> entities,
            List<JsonObject> rewards, JsonArray statModifiers) {
        JsonObject json = new JsonObject();
        JsonObject mode = new JsonObject();
        mode.addProperty("type", "gateways:after_every_n_waves");
        mode.addProperty("waves", waves);
        mode.addProperty("max", max);
        json.add("application_mode", mode);
        if (entities != null && !entities.isEmpty()) {
            json.add("entities", jsonArray(entities));
        }
        if (rewards != null && !rewards.isEmpty()) {
            json.add("rewards", jsonArray(rewards));
        }
        if (statModifiers != null && !statModifiers.isEmpty()) {
            json.add("modifiers", statModifiers);
        }
        return json;
    }

    private static JsonObject chancedReward(double chance, JsonObject reward) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "gateways:chanced");
        json.addProperty("chance", chance);
        json.add("reward", reward);
        return json;
    }

    private static JsonObject recipeJson(RosterMob roster, TierProfile profile) {
        // One shape per cost tier (E=ender pearl, D=diamond, G=gem dust): pearls
        // stay fixed at 2 while diamonds and dust ramp with the gateway's world
        // tier. An unknown count pair means the profile outgrew the 3x3 grid.
        String patternKey = profile.recipeDiamonds() + "/" + profile.recipeDust();
        List<String> pattern = switch (patternKey) {
            case "1/1" -> List.of("E E", " G ", " D ");
            case "2/2" -> List.of("E E", "D G", "G D");
            case "2/3" -> List.of("E E", "D G", "GGD");
            case "3/3" -> List.of("EDE", "D G", "GGD");
            case "4/3" -> List.of("EDE", "DGD", "DGG");
            default -> throw new IllegalStateException("gateway recipe: no 3x3 shape for diamonds="
                    + profile.recipeDiamonds() + " dust=" + profile.recipeDust());
        };
        JsonObject json = new JsonObject();
        JsonArray conditions = new JsonArray();
        JsonObject modLoaded = new JsonObject();
        modLoaded.addProperty("type", "neoforge:mod_loaded");
        modLoaded.addProperty("modid", "gateways");
        conditions.add(modLoaded);
        json.add("neoforge:conditions", conditions);
        json.addProperty("type", "minecraft:crafting_shaped");
        json.addProperty("category", "misc");
        json.addProperty("group", "tensura_apotheosis");
        JsonObject key = new JsonObject();
        JsonObject pearl = new JsonObject();
        pearl.addProperty("tag", "c:ender_pearls");
        key.add("E", pearl);
        JsonObject diamond = new JsonObject();
        diamond.addProperty("item", "minecraft:diamond");
        key.add("D", diamond);
        JsonObject dust = new JsonObject();
        dust.addProperty("item", "apotheosis:gem_dust");
        key.add("G", dust);
        json.add("key", key);
        JsonArray patternArray = new JsonArray();
        pattern.forEach(patternArray::add);
        json.add("pattern", patternArray);
        JsonObject result = new JsonObject();
        JsonObject components = new JsonObject();
        components.addProperty("gateways:gateway",
                "tensura_apotheosis:elite/" + roster.mob());
        result.add("components", components);
        result.addProperty("count", 1);
        result.addProperty("id", "gateways:gate_pearl");
        json.add("result", result);
        return json;
    }
}
