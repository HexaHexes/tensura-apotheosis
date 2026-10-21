package dev.kurok.mods.tensuraapotheosis.gametest;

import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.mobs.registries.EliteRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.placebo.systems.gear.GearSetRegistry;
import io.github.manasmods.tensura.item.weapon.TwoHandedSwordItem;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Content verification for Tensura Apotheosis. The mod is pure data, so the
 * tests assert against Apotheosis' own registries (Placebo dynamic registries,
 * reloaded during server data-pack load) rather than any code of ours:
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
 *       must resolve in {@link EliteRegistry}. These are the 38 Tensura elite
 *       variants; a biome or stats parse failure would silently drop one.</li>
 *   <li>Every gear set JSON under {@code data/tensura_apotheosis/gear_sets/} must
 *       resolve in {@link GearSetRegistry}. Track A sets join Apotheosis' vanilla
 *       tier pools; Track B sets are scoped to the Tensura elites via tags.</li>
 * </ol>
 */
@GameTestHolder("tensura_apotheosis")
@PrefixGameTestTemplate(false)
public class ApotheosisContentGameTests {

    private static final String NS = "tensura_apotheosis";
    private static final int EXPECTED_AFFIXES = 51;
    private static final int EXPECTED_GEMS = 14;
    private static final int EXPECTED_ELITES = 38;
    private static final int EXPECTED_GEARSETS = 80;

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

    /** Every shipped elite JSON must parse into Apotheosis' elite registry. */
    @GameTest(template = "empty")
    public static void allElitesLoad(GameTestHelper helper) {
        Set<ResourceLocation> expected = expectedIds(helper, "apothic_elites");
        if (expected.size() != EXPECTED_ELITES) {
            throw new GameTestAssertException(
                    "found " + expected.size() + " elite data files expected " + EXPECTED_ELITES);
        }
        for (ResourceLocation id : expected) {
            if (EliteRegistry.INSTANCE.getValue(id) == null) {
                throw new GameTestAssertException("elite " + id + " not in EliteRegistry (parse failure?)");
            }
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

    private static String ap(Purity purity) {
        return String.valueOf(purity.getSerializedName());
    }
}
