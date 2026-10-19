package dev.kurok.mods.tensuraapotheosis;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Tensura Apotheosis - Tensura-themed affixes and socket gems for Apotheosis.
 *
 * <p>The mod is pure data: every affix ({@code data/tensura_apotheosis/affixes/...})
 * and gem ({@code data/tensura_apotheosis/gems/...}) is loaded by Apotheosis'
 * Placebo dynamic registries, gem stacks reuse Apotheosis' shared
 * {@code apotheosis:gem} item, and gem models resolve through Apotheosis'
 * hardcoded model namespace. There is intentionally no runtime logic here.
 */
@Mod(TensuraApotheosis.MOD_ID)
public class TensuraApotheosis {

    public static final String MOD_ID = "tensura_apotheosis";

    public TensuraApotheosis(IEventBus modBus) {
        // Content is data-only; nothing to initialize.
    }
}
