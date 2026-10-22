package org.bitwisemadness.tensuraapotheosis.gametest;

import org.bitwisemadness.tensuraapotheosis.EliteNamePools;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.compat.gateways.tiered_gate.TieredGateway;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.mobs.registries.EliteRegistry;
import dev.shadowsoffire.apotheosis.mobs.types.Elite;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.placebo.json.RandomAttributeModifier;
import dev.shadowsoffire.placebo.systems.gear.GearSetRegistry;
import io.github.manasmods.tensura.item.weapon.TwoHandedSwordItem;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Content verification for Tensura Apotheosis. The mod is almost entirely
 * data, so most tests assert against Apotheosis' own registries (Placebo
 * dynamic registries, reloaded during server data-pack load); the one piece
 * of our own runtime, the elite name pools, is checked directly:
 *
 * <ol>
 *   <li>Two-handed Tensura weapons must fall into Apotheosis' {@code melee_weapon}
 *       loot category - the predicate is class-independent (SWORD_DIG ability or
 *       base attack damage &gt; 1), so this pins that every TwoHandedSwordItem
 *       (greatsword, katana, odachi, spear, scythe, tachi, ...) receives melee
 *       affixes and melee gem lines.</li>
 *   <li>Every affix JSON shipped under {@code data/tensura_apotheosis/affixes/}
 *       must resolve in {@link AffixRegistry} - a malformed file fails to parse,
 *       never enters the registry, and fails the test.</li>
 *   <li>Every gem JSON under {@code data/tensura_apotheosis/gems/} must resolve
 *       in {@link GemRegistry} and produce a stack of Apotheosis' shared gem item.</li>
 *   <li>Every elite boss JSON under {@code data/tensura_apotheosis/apothic_elites/}
 *       must resolve in {@link EliteRegistry}, carry the tier band's affix
 *       rarities (the rising elite rarity ladder), keep its spawn weights
 *       a contiguous suffix of the haven..pinnacle ladder - full gating means
 *       a band's elite never weighs in below its tier - and follow the
 *       tier-scaled stat ladder (HP, damage and AP/MP pool bonuses strictly
 *       rise between adjacent tiers, never overlapping).</li>
 *   <li>Every gear set JSON under {@code data/tensura_apotheosis/gear_sets/} must
 *       resolve in {@link GearSetRegistry}. Track A sets join Apotheosis' vanilla
 *       tier pools; Track B sets are scoped to the Tensura elites via tags.</li>
 *   <li>Every gateway JSON under {@code data/tensura_apotheosis/gateways/} must
 *       resolve in Gateways' registry: the 38 tiered gateways must span all
 *       five world tiers, the endless chase gateway (granted as a pearl from
 *       the pinnacle rewards) must load, and boss-bar colors must be
 *       theme-varied (the uniform single-color design is what this guards
 *       against).</li>
 *   <li>Every elite must have a name pool file under
 *       {@code data/tensura_apotheosis/elite_names/} loaded into
 *       {@link EliteNamePools} with at least 40 names - the runtime draws
 *       one per spawn, so a missing or shrunk pool silently regresses every
 *       elite's identity.</li>
 * </ol>
 */
@GameTestHolder("tensura_apotheosis")
@PrefixGameTestTemplate(false)
public class ApotheosisContentGameTests {

    private static final String NS = "tensura_apotheosis";
    private static final int EXPECTED_AFFIXES = 55;
    private static final int EXPECTED_GEMS = 37;
    private static final int EXPECTED_ELITES = 38;
    private static final int EXPECTED_BONUS_TABLES = 19;
    private static final int EXPECTED_GEARSETS = 80;
    private static final int EXPECTED_GATEWAYS = 39; // 38 tiered + 1 endless chase
    private static final int MIN_NAME_POOL = 40;

    /**
     * Enumerates this mod's data files for a Placebo registry folder and derives
     * the registry ids Placebo will use (namespace + path below the folder).
     */
    private static Set<ResourceLocation> expectedIds(GameTestHelper helper, String folder) {
        ResourceManager rm = helper.getLevel().getServer().getResourceManager();
        Map<ResourceLocation, Resource> resources =
                rm.listResources(folder, id -> NS.equals(id.getNamespace()));
        Set<ResourceLocation> out = new HashSet<>();
        String prefix = folder + "/";
        for (ResourceLocation rid : resources.keySet()) {
            String path = rid.getPath();
            if (!path.startsWith(prefix) || !path.endsWith(".json")) {
                continue;
            }
            out.add(ResourceLocation.fromNamespaceAndPath(
                    NS, path.substring(prefix.length(), path.length() - ".json".length())));
        }
        return out;
    }

    /** Two-handed Tensura weapons must be melee-affixable in Apotheosis. */
    @GameTest(template = "empty")
    public static void twoHandedWeaponsCategorizeAsMelee(GameTestHelper helper) {
        int found = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof TwoHandedSwordItem)) {
                continue;
            }
            found++;
            ItemStack stack = new ItemStack(item);
            LootCategory category = LootCategory.forItem(stack);
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (category == null || !"apotheosis:melee_weapon".equals(category.getKey().toString())) {
                throw new GameTestAssertException(id
                        + " (TwoHandedSwordItem) has loot category "
                        + (category == null ? "null" : category.getKey())
                        + " expected apotheosis:melee_weapon");
            }
        }
        if (found == 0) {
            throw new GameTestAssertException("no TwoHandedSwordItem instances registered - test is vacuous");
        }
        helper.succeed();
    }

    /** Every shipped affix JSON must parse into Apotheosis' affix registry. */
    @GameTest(template = "empty")
    public static void allAffixesLoad(GameTestHelper helper) {
        Set<ResourceLocation> expected = expectedIds(helper, "affixes");
        if (expected.size() != EXPECTED_AFFIXES) {
            throw new GameTestAssertException(
                    "found " + expected.size() + " affix data files expected " + EXPECTED_AFFIXES);
        }
        Map<ResourceLocation, String> missing = new HashMap<>();
        for (ResourceLocation id : expected) {
            if (AffixRegistry.INSTANCE.getValue(id) == null) {
                missing.put(id, "not in AffixRegistry (parse failure?)");
            }
        }
        if (!missing.isEmpty()) {
            throw new GameTestAssertException("affixes failed to load: " + missing.keySet());
        }
        helper.succeed();
    }

    /** Every shipped gem JSON must parse and stack onto the shared gem item. */
    @GameTest(template = "empty")
    public static void allGemsLoad(GameTestHelper helper) {
        Set<ResourceLocation> expected = expectedIds(helper, "gems");
        if (expected.size() != EXPECTED_GEMS) {
            throw new GameTestAssertException(
                    "found " + expected.size() + " gem data files expected " + EXPECTED_GEMS);
        }
        for (ResourceLocation id : expected) {
            Gem gem = GemRegistry.INSTANCE.getValue(id);
            if (gem == null) {
                throw new GameTestAssertException("gem " + id + " not in GemRegistry (parse failure?)");
            }
            ItemStack stack = GemItem.createStack(gem, Purity.NORMAL);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!"apotheosis:gem".equals(itemId.toString())) {
                throw new GameTestAssertException(
                        "gem " + id + " stacks onto " + itemId + " expected apotheosis:gem");
            }
            // min_purity must clamp (e.g. ultima: flawless) instead of failing.
            Purity applied = GemItem.getPurity(stack);
            if (applied.ordinal() < gem.getMinPurity().ordinal()) {
                throw new GameTestAssertException("gem " + id + " purity " + ap(applied)
                        + " is below its min_purity " + ap(gem.getMinPurity()));
            }
        }
        helper.succeed();
    }

    /** Every shipped elite JSON must parse, expose a tier band and gate its spawns. */
    @GameTest(template = "empty")
    public static void allElitesLoad(GameTestHelper helper) {
        Set<ResourceLocation> expected = expectedIds(helper, "apothic_elites");
        if (expected.size() != EXPECTED_ELITES) {
            throw new GameTestAssertException(
                    "found " + expected.size() + " elite data files expected " + EXPECTED_ELITES);
        }
        WorldTier[] ladder = WorldTier.values();
        Set<Set<LootRarity>> distinctBands = new HashSet<>();
        // Stat ladder bounds per tier index: adjacent tiers must never overlap
        // (the generator's elite ladder emits haven HP 30-45, frontier 50-80,
        // ... so tier N's minimum always beats tier N-1's maximum).
        int tiers = ladder.length;
        float[] hpMin = new float[tiers], hpMax = new float[tiers];
        float[] dmgMin = new float[tiers], dmgMax = new float[tiers];
        float[] poolMin = new float[tiers], poolMax = new float[tiers];
        Arrays.fill(hpMin, Float.MAX_VALUE);
        Arrays.fill(dmgMin, Float.MAX_VALUE);
        Arrays.fill(poolMin, Float.MAX_VALUE);
        Arrays.fill(hpMax, -Float.MAX_VALUE);
        Arrays.fill(dmgMax, -Float.MAX_VALUE);
        Arrays.fill(poolMax, -Float.MAX_VALUE);
        for (ResourceLocation id : expected) {
            Elite elite = EliteRegistry.INSTANCE.getValue(id);
            if (elite == null) {
                throw new GameTestAssertException("elite " + id + " not in EliteRegistry (parse failure?)");
            }
            // Rising rarity ladder: every elite must carry affix rarities from
            // the tier profile, and the roster must actually differ across bands.
            if (elite.afxData().rarities().isEmpty()) {
                throw new GameTestAssertException("elite " + id + " has no affix rarities (affix_data missing?)");
            }
            distinctBands.add(elite.afxData().rarities());
            // Full gating: spawn weights must be a contiguous suffix of the
            // haven..pinnacle ladder - a band's elite never weighs in below
            // its tier (that would spawn mythic-affix elites in haven worlds).
            Set<WorldTier> present = elite.weights().weights().keySet();
            if (present.isEmpty()) {
                throw new GameTestAssertException("elite " + id + " has no spawn weights at any world tier");
            }
            int first = Integer.MAX_VALUE;
            for (int i = 0; i < ladder.length; i++) {
                if (present.contains(ladder[i])) {
                    first = Math.min(first, i);
                }
            }
            for (int i = 0; i < ladder.length; i++) {
                if (present.contains(ladder[i]) != (i >= first)) {
                    throw new GameTestAssertException(
                            "elite " + id + " spawn weights are not a contiguous tier suffix: " + present);
                }
            }
            // Fold this elite's stat bounds into its tier's ladder entries.
            for (RandomAttributeModifier mod : elite.stats().modifiers()) {
                ResourceLocation attr = mod.attribute().unwrapKey().map(k -> k.location()).orElse(null);
                if (attr == null) {
                    continue;
                }
                float min = mod.value().min(), max = mod.value().max();
                if (ResourceLocation.withDefaultNamespace("generic.max_health").equals(attr)
                        && mod.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    hpMin[first] = Math.min(hpMin[first], min);
                    hpMax[first] = Math.max(hpMax[first], max);
                } else if (ResourceLocation.withDefaultNamespace("generic.attack_damage").equals(attr)
                        && mod.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                    dmgMin[first] = Math.min(dmgMin[first], min);
                    dmgMax[first] = Math.max(dmgMax[first], max);
                } else if ((ResourceLocation.fromNamespaceAndPath(NS, "max_aura").equals(attr)
                        || ResourceLocation.fromNamespaceAndPath(NS, "max_magicule").equals(attr))
                        && mod.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                    poolMin[first] = Math.min(poolMin[first], min);
                    poolMax[first] = Math.max(poolMax[first], max);
                }
            }
        }
        if (distinctBands.size() < 3) {
            throw new GameTestAssertException(
                    "elite rarity ladder collapsed: only " + distinctBands.size()
                            + " distinct rarity bands across the roster");
        }
        // Monotone stat ladder: every tier must be strictly stronger than the
        // one below it, and every tier must be populated by some elite.
        for (int t = 0; t < tiers; t++) {
            if (hpMin[t] == Float.MAX_VALUE) {
                throw new GameTestAssertException("no elite carries stat bounds for tier " + ladder[t]);
            }
            if (t == 0) {
                continue;
            }
            if (hpMin[t] <= hpMax[t - 1]) {
                throw new GameTestAssertException("elite HP ladder overlaps between " + ladder[t - 1]
                        + " (" + hpMax[t - 1] + ") and " + ladder[t] + " (" + hpMin[t] + ")");
            }
            if (dmgMin[t] <= dmgMax[t - 1]) {
                throw new GameTestAssertException("elite damage ladder overlaps between " + ladder[t - 1]
                        + " (" + dmgMax[t - 1] + ") and " + ladder[t] + " (" + dmgMin[t] + ")");
            }
            if (poolMin[t] <= poolMax[t - 1]) {
                throw new GameTestAssertException("elite AP/MP pool ladder overlaps between " + ladder[t - 1]
                        + " (" + poolMax[t - 1] + ") and " + ladder[t] + " (" + poolMin[t] + ")");
            }
        }
        helper.succeed();
    }

    /**
     * Every elite must roll bonus loot on death: a world-tier table that
     * scales its dust and gem rewards with the world tier, plus a family
     * table carrying the theme's flavor drops. A dangling id or an empty
     * table silently strips the kill rewards the rest of the kit is built
     * around.
     */
    @GameTest(template = "empty")
    public static void eliteBonusLootLoads(GameTestHelper helper) {
        Set<ResourceLocation> expected = expectedIds(helper, "apothic_elites");
        if (expected.size() != EXPECTED_ELITES) {
            throw new GameTestAssertException(
                    "found " + expected.size() + " elite data files expected " + EXPECTED_ELITES);
        }
        Set<ResourceLocation> referenced = new HashSet<>();
        ResourceManager rm = helper.getLevel().getServer().getResourceManager();
        for (ResourceLocation id : expected) {
            Elite elite = EliteRegistry.INSTANCE.getValue(id);
            if (elite == null) {
                throw new GameTestAssertException("elite " + id + " not in EliteRegistry (parse failure?)");
            }
            List<ResourceKey<LootTable>> tables = elite.basicData().bonusLoot().tables();
            if (tables.size() != 2) {
                throw new GameTestAssertException("elite " + id + " has " + tables.size()
                        + " bonus_loot tables expected 2 (world-tier + family)");
            }
            for (ResourceKey<LootTable> key : tables) {
                referenced.add(key.location());
                LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
                if (table == LootTable.EMPTY) {
                    throw new GameTestAssertException("elite " + id + " references missing loot table " + key.location());
                }
                // LootTable exposes no public pool accessor in 1.21.1, so the
                // emptiness check reads the source JSON's pools array instead.
                ResourceLocation fileId = ResourceLocation.fromNamespaceAndPath(
                        key.location().getNamespace(), "loot_table/" + key.location().getPath() + ".json");
                Resource res = rm.getResource(fileId).orElse(null);
                if (res == null) {
                    throw new GameTestAssertException("bonus loot table file missing: " + fileId);
                }
                try (var reader = res.openAsReader()) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (!root.has("pools") || root.getAsJsonArray("pools").isEmpty()) {
                        throw new GameTestAssertException("bonus loot table " + key.location() + " has no pools");
                    }
                } catch (IOException ex) {
                    throw new GameTestAssertException("cannot read bonus loot table " + fileId + ": " + ex.getMessage());
                }
            }
        }
        if (referenced.size() != EXPECTED_BONUS_TABLES) {
            throw new GameTestAssertException("elites reference " + referenced.size()
                    + " distinct bonus loot tables expected " + EXPECTED_BONUS_TABLES);
        }
        helper.succeed();
    }

    /**
     * Every elite must have its name pool loaded: the runtime draws one of
     * 40+ themed names per spawn, so a missing or shrunk pool file silently
     * regresses elite identities in-game.
     */
    @GameTest(template = "empty")
    public static void eliteNamePoolsLoad(GameTestHelper helper) {
        Set<ResourceLocation> expected = expectedIds(helper, EliteNamePools.FOLDER);
        if (expected.size() != EXPECTED_ELITES) {
            throw new GameTestAssertException("found " + expected.size()
                    + " elite name pool files expected " + EXPECTED_ELITES);
        }
        for (ResourceLocation id : expected) {
            List<String> pool = EliteNamePools.poolFor(id);
            if (pool.size() < MIN_NAME_POOL) {
                throw new GameTestAssertException("elite " + id + " has " + pool.size()
                        + " pool names expected at least " + MIN_NAME_POOL);
            }
            for (String name : pool) {
                if (name.isBlank()) {
                    throw new GameTestAssertException("elite " + id + " contains a blank pool name");
                }
            }
        }
        if (EliteNamePools.poolCount() != EXPECTED_ELITES) {
            throw new GameTestAssertException("loaded " + EliteNamePools.poolCount()
                    + " name pools expected " + EXPECTED_ELITES + " (reload listener not registered?)");
        }
        helper.succeed();
    }

    /** Every shipped gear set JSON must parse into Placebo's gear set registry. */
    @GameTest(template = "empty")
    public static void allGearSetsLoad(GameTestHelper helper) {
        Set<ResourceLocation> expected = expectedIds(helper, "gear_sets");
        if (expected.size() != EXPECTED_GEARSETS) {
            throw new GameTestAssertException(
                    "found " + expected.size() + " gear set data files expected " + EXPECTED_GEARSETS);
        }
        for (ResourceLocation id : expected) {
            if (GearSetRegistry.INSTANCE.getValue(id) == null) {
                throw new GameTestAssertException("gear set " + id + " not in GearSetRegistry (parse failure?)");
            }
        }
        helper.succeed();
    }

    /** Every gateway JSON must parse; tiered ones span tiers, the endless one loads. */
    @GameTest(template = "empty")
    public static void allGatewaysLoad(GameTestHelper helper) {
        Set<ResourceLocation> expected = expectedIds(helper, "gateways");
        if (expected.size() != EXPECTED_GATEWAYS) {
            throw new GameTestAssertException(
                    "found " + expected.size() + " gateway data files expected " + EXPECTED_GATEWAYS);
        }
        ResourceLocation hordeId = ResourceLocation.fromNamespaceAndPath(NS, "endless/horde");
        Set<WorldTier> tiers = EnumSet.noneOf(WorldTier.class);
        Set<String> colors = new HashSet<>();
        int tiered = 0;
        int endless = 0;
        for (ResourceLocation id : expected) {
            Gateway gateway = GatewayRegistry.INSTANCE.getValue(id);
            if (gateway == null) {
                throw new GameTestAssertException("gateway " + id + " not in GatewayRegistry (parse failure?)");
            }
            colors.add(gateway.color().serialize());
            if (gateway instanceof TieredGateway tieredGate) {
                tiered++;
                tiers.add(tieredGate.settings().tier());
            } else if (hordeId.equals(id)) {
                // The endless chase gateway granted from the pinnacle rewards.
                endless++;
            } else {
                throw new GameTestAssertException(
                        "gateway " + id + " is neither apotheosis:tiered nor the endless horde");
            }
        }
        if (tiered != EXPECTED_GATEWAYS - 1) {
            throw new GameTestAssertException(
                    "gateways: " + tiered + " tiered expected " + (EXPECTED_GATEWAYS - 1)
                            + " - an elite gateway failed to load or a stray gateway appeared");
        }
        if (endless != 1) {
            throw new GameTestAssertException(
                    "endless chase gateway missing - pinnacle rewards grant a pearl for " + hordeId);
        }
        if (tiers.size() != WorldTier.values().length) {
            throw new GameTestAssertException(
                    "gateways cover " + tiers.size() + " world tiers expected all " + WorldTier.values().length
                            + " - tier distribution regressed (" + tiers + ")");
        }
        if (colors.size() < 10) {
            throw new GameTestAssertException(
                    "only " + colors.size() + " distinct gateway colors - mob theming lost?");
        }
        helper.succeed();
    }

    private static String ap(Purity purity) {
        return String.valueOf(purity.getSerializedName());
    }
}
