package org.bitwisemadness.tensuraapotheosis.datagen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.neoforged.fml.ModList;

/**
 * Reads the datapack facts the elite banding needs out of the staged Tensura
 * jar: per-mob entity existence stats (aura/magicule/spiritual pools), the
 * {@code #tensura:<mob>_spawn} biome tags and the dimension biome sources.
 *
 * <p>The retired PowerShell generator read the same three datasets out of
 * {@code libs/tensura-neoforge-2.0.1.3.jar}; this port reads the jar the build
 * stages from CurseMaven - the same pinned file id (8793360, Tensura
 * 2.0.1.3), so the banding reproduces byte-for-byte.</p>
 */
public final class TensuraData {

    /** Per-mob existence stats: aura pool, magicule pool, spiritual health. */
    public record Existence(double aura, double mp, double spiritual) {
    }

    private final Map<String, Existence> existence = new LinkedHashMap<>();
    private final Map<String, List<String>> biomeTags = new HashMap<>();
    private final Map<String, String> biomeDimension = new HashMap<>();

    private TensuraData() {
    }

    public Map<String, Existence> existence() {
        return existence;
    }

    public Map<String, List<String>> biomeTags() {
        return biomeTags;
    }

    public Map<String, String> biomeDimension() {
        return biomeDimension;
    }

    /** Locates the staged Tensura jar and parses its datapack facts. */
    public static TensuraData read() {
        Path jar = tensuraJar();
        TensuraData data = new TensuraData();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            zip.stream().forEach(entry -> {
                String name = entry.getName();
                if (name.startsWith("data/tensura/entity_existence/") && name.endsWith(".json")) {
                    String mob = baseName(name);
                    JsonObject json = readJson(zip, entry);
                    // max_aura is absent on aura-less mobs and reads as 0, exactly
                    // like the retired generator's [double]($j.max_aura) cast.
                    data.existence.put(mob, new Existence(
                            field(json, "max_aura"),
                            field(json, "max_magicule"),
                            field(json, "spiritualHealth")));
                } else if (name.startsWith("data/tensura/tags/worldgen/biome/") && name.endsWith(".json")) {
                    String tag = name.substring("data/tensura/tags/worldgen/biome/".length(),
                            name.length() - ".json".length());
                    JsonObject json = readJson(zip, entry);
                    List<String> values = new java.util.ArrayList<>();
                    for (JsonElement v : json.getAsJsonArray("values")) {
                        // Tag values are plain biome ids or { "id": ... } objects.
                        values.add(v.isJsonObject() ? v.getAsJsonObject().get("id").getAsString()
                                : v.getAsString());
                    }
                    data.biomeTags.put(tag, values);
                } else if (name.startsWith("data/tensura/dimension/") && name.endsWith(".json")) {
                    String dim = baseName(name);
                    JsonObject json = readJson(zip, entry);
                    JsonElement sourceElement =
                            json.getAsJsonObject("generator").get("biome_source");
                    if (sourceElement != null && sourceElement.isJsonObject()
                            && sourceElement.getAsJsonObject().has("biomes")) {
                        for (JsonElement b : sourceElement.getAsJsonObject().getAsJsonArray("biomes")) {
                            data.biomeDimension.put(
                                    b.getAsJsonObject().get("biome").getAsString(), dim);
                        }
                    }
                }
            });
        } catch (IOException ex) {
            throw new IllegalStateException("cannot read the staged Tensura jar at " + jar, ex);
        }
        return data;
    }

    private static Path tensuraJar() {
        return ModList.get().getModContainerById("tensura")
                .orElseThrow(() -> new IllegalStateException(
                        "tensura is not loaded - the data run must stage the dependency stack "
                                + "(the elite banding reads its entity-existence data)"))
                .getModInfo().getOwningFile().getFile().getFilePath();
    }

    private static String baseName(String entryName) {
        String file = entryName.substring(entryName.lastIndexOf('/') + 1);
        return file.substring(0, file.length() - ".json".length());
    }

    private static JsonObject readJson(ZipFile zip, ZipEntry entry) {
        try (InputStream in = zip.getInputStream(entry);
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException ex) {
            throw new IllegalStateException("cannot read " + entry.getName() + " from the Tensura jar", ex);
        }
    }

    /** Null-safe field read: absent keys read as 0, like the generator's cast. */
    private static double field(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value == null || value.isJsonNull() ? 0.0 : value.getAsDouble();
    }
}
