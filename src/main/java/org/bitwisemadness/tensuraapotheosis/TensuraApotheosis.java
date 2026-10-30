package org.bitwisemadness.tensuraapotheosis;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

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
 * cannot be themed per mob. Public APIs only; no mixins.
 */
@Mod(TensuraApotheosis.MOD_ID)
public final class TensuraApotheosis {

    public static final String MOD_ID = "tensura_apotheosis";

    public TensuraApotheosis(IEventBus modBus) {
        // The only runtime: per-elite name pools (data-driven, public API).
        EliteNamePools namePools = new EliteNamePools();
        NeoForge.EVENT_BUS.addListener(namePools::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(namePools::onServerTick);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent ev) -> ev.addListener(namePools.newReloader()));
    }
}
