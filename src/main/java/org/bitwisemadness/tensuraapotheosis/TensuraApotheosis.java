package org.bitwisemadness.tensuraapotheosis;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.data.DataGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.bitwisemadness.tensuraapotheosis.datagen.AffixContent;
import org.bitwisemadness.tensuraapotheosis.datagen.BonusLootContent;
import org.bitwisemadness.tensuraapotheosis.datagen.EliteContent;
import org.bitwisemadness.tensuraapotheosis.datagen.GatewayContent;
import org.bitwisemadness.tensuraapotheosis.datagen.GearSetContent;
import org.bitwisemadness.tensuraapotheosis.datagen.GemContent;
import org.bitwisemadness.tensuraapotheosis.datagen.JsonFilesProvider;

/**
 * Tensura Apotheosis - Tensura-themed affixes, socket gems, elites and
 * gateways for Apotheosis.
 *
 * <p>Almost all content is data: every affix ({@code data/tensura_apotheosis/affixes/...})
 * and gem ({@code data/tensura_apotheosis/gems/...}) is loaded by Apotheosis'
 * Placebo dynamic registries, gem stacks reuse Apotheosis' shared
 * {@code apotheosis:gem} item, and gem models resolve through Apotheosis'
 * hardcoded model namespace. The single piece of runtime logic is
 * {@link EliteNamePools}, which draws a themed name per elite spawn from
 * per-mob pool data - Apotheosis' name generation is global-config only and
 * cannot be themed per mob. Public APIs only; no mixins.</p>
 */
@Mod(TensuraApotheosis.MOD_ID)
public final class TensuraApotheosis {

    public static final String MOD_ID = "tensura_apotheosis";

    public TensuraApotheosis(IEventBus modBus) {
        modBus.addListener(TensuraApotheosis::gatherData);
        // The only runtime: per-elite name pools (data-driven, public API).
        EliteNamePools namePools = new EliteNamePools();
        NeoForge.EVENT_BUS.addListener(namePools::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(namePools::onServerTick);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent ev) -> ev.addListener(namePools.newReloader()));
    }

    /**
     * Feeds the shipped content - the retired
     * {@code tools/generate-content.ps1}'s output, now ported into the datagen
     * tables - into datagen: the datapack, the gem item models and the lang
     * file, all from the same tables.
     */
    private static void gatherData(GatherDataEvent event) {
        if (!event.includeServer()) {
            return;
        }
        DataGenerator generator = event.getGenerator();
        // The hand-authored name pools beside the retired generator script feed
        // the elite name pools (resolved relative to src/generated/resources).
        Path namePools = generator.getPackOutput().getOutputFolder()
                .resolve("../../../tools/name-pools.json").normalize();

        Map<String, JsonObject> files = new TreeMap<>();
        files.putAll(AffixContent.files());
        files.putAll(GemContent.files());
        files.putAll(GearSetContent.files());
        files.putAll(EliteContent.files(namePools));
        files.putAll(GatewayContent.files(EliteContent.tiersByMob()));
        files.putAll(BonusLootContent.files());

        Map<String, String> lang = new TreeMap<>();
        AffixContent.lang(lang);
        GemContent.lang(lang);
        EliteContent.lang(lang);
        GatewayContent.lang(lang);
        files.put("assets/tensura_apotheosis/lang/en_us.json", langJson(lang));

        event.addProvider(new JsonFilesProvider("apotheosis_content", generator.getPackOutput(), files));
    }

    private static JsonObject langJson(Map<String, String> lang) {
        JsonObject json = new JsonObject();
        lang.forEach(json::addProperty);
        return json;
    }
}
