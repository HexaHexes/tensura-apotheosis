package org.bitwisemadness.tensuraapotheosis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.shadowsoffire.apotheosis.mobs.types.Elite;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Per-elite themed name pools: every Tensura elite draws a random full name
 * from its own pool on spawn ("Sovereign of the Void Zarnakzul", "Planeshifter
 * Nabrkael", ...).
 *
 * <p>This mirrors Apotheosis' name generation mechanic - its
 * {@code use_name_generation} flag hands naming to {@code NameHelper} - but
 * Apotheosis' pools are global config lists ({@code apotheosis/names.cfg}),
 * not selectable per mob. The per-mob pools ship as mod data
 * ({@code data/tensura_apotheosis/elite_names/<mob>.json}, emitted by
 * {@code tools/generate-content.ps1} from {@code tools/name-pools.json}) and
 * are applied here through public APIs only - no mixins, no Apotheosis
 * internals.
 *
 * <p>Rename flow: {@link EntityJoinLevelEvent} at {@code LOWEST} priority
 * runs after Apotheosis' own join handler has tagged and transformed the
 * elite (the {@code apoth.miniboss} tag is written at finalize-spawn time and
 * never removed), so the captured id is the elite's registry id - the pool
 * key. The actual rename happens on the next server tick, which sidesteps
 * any remaining event-ordering questions. Mobs loaded from disk keep their
 * saved name (a re-roll on every chunk load would be noise).
 */
public final class EliteNamePools {

    /** Data folder holding the per-elite pool files. */
    public static final String FOLDER = "elite_names";

    private static final Gson GSON = new GsonBuilder().create();
    private static final int MIN_POOL_SIZE = 40;

    /** Pool data from the last server data reload. */
    private static volatile Map<ResourceLocation, List<String>> pools = Map.of();

    /** Elites waiting for their one-tick-later rename. */
    private final Queue<Pending> pending = new ArrayDeque<>();

    /** The pool backing an elite, empty if none shipped. */
    public static List<String> poolFor(ResourceLocation id) {
        return pools.getOrDefault(id, List.of());
    }

    /** Number of elites with a loaded pool - the gametest checks this. */
    public static int poolCount() {
        return pools.size();
    }

    public void onEntityJoin(EntityJoinLevelEvent ev) {
        // Client level and chunk reloads: no rename - saved names must stick.
        if (ev.getLevel().isClientSide() || ev.loadedFromDisk()) {
            return;
        }
        if (!(ev.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!mob.getPersistentData().contains(Elite.MINIBOSS_KEY)) {
            return;
        }
        ResourceLocation id =
                ResourceLocation.tryParse(mob.getPersistentData().getString(Elite.MINIBOSS_KEY));
        if (id != null && TensuraApotheosis.MOD_ID.equals(id.getNamespace())) {
            pending.add(new Pending(mob, id));
        }
    }

    public void onServerTick(ServerTickEvent.Post ev) {
        if (pending.isEmpty()) {
            return;
        }
        for (Pending p = pending.poll(); p != null; p = pending.poll()) {
            Mob mob = p.mob();
            // A natural spawn can enqueue twice (spawn event + join event);
            // the second draw simply wins, both are uniform pool draws.
            if (!mob.isAlive()) {
                continue;
            }
            List<String> pool = pools.get(p.id());
            if (pool.isEmpty()) {
                continue;
            }
            mob.setCustomName(Component.literal(pool.get(mob.getRandom().nextInt(pool.size()))));
            mob.setCustomNameVisible(true);
        }
    }

    private record Pending(Mob mob, ResourceLocation id) {}

    /** Reads {@code data/tensura_apotheosis/elite_names/<mob>.json} files. */
    private static final class Reloader extends SimpleJsonResourceReloadListener {

        private Reloader() {
            super(GSON, FOLDER);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, List<String>> next = new HashMap<>();
            map.forEach((id, el) -> {
                if (!el.isJsonObject()) {
                    return;
                }
                JsonElement names = el.getAsJsonObject().get("names");
                if (names == null || !names.isJsonArray()) {
                    return;
                }
                List<String> list = new ArrayList<>();
                names.getAsJsonArray().forEach(n -> {
                    String name = n.getAsString();
                    if (!name.isBlank() && list.size() < MIN_POOL_SIZE * 2) {
                        list.add(name);
                    }
                });
                next.put(id, List.copyOf(list));
            });
            pools = Map.copyOf(next);
        }
    }

    /** Creates the listener for this instance's reload registration. */
    Reloader newReloader() {
        return new Reloader();
    }
}
