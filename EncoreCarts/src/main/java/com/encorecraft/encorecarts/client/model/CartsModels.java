package com.encorecraft.encorecarts.client.model;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.client.model.bb.BbModel;
import com.encorecraft.encorecarts.client.model.bb.BbModelParser;
import com.encorecraft.encorecarts.client.model.bb.BbTexture;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads every {@code encorecarts_models/*.json} definition and its {@code .bbmodel} on resource reload (F3+T or
 * resource pack change), so models can be swapped by a resource pack without touching code.
 */
public final class CartsModels extends SimplePreparableReloadListener<Map<ResourceLocation, ModelDefinition>> {
    public static final ResourceLocation CART = EncoreCarts.id("cart");
    public static final ResourceLocation COACHMAN_CLASSIC = EncoreCarts.id("coachman_classic");
    public static final ResourceLocation COACHMAN_SLIM = EncoreCarts.id("coachman_slim");
    public static final CartsModels INSTANCE = new CartsModels();

    private static final String DIRECTORY = "encorecarts_models";
    private volatile Map<ResourceLocation, ModelDefinition> definitions = Map.of();
    private final List<ResourceLocation> uploaded = new ArrayList<>();

    private CartsModels() {
    }

    @Nullable
    public static ModelDefinition get(ResourceLocation id) {
        return INSTANCE.definitions.get(id);
    }

    @Override
    protected Map<ResourceLocation, ModelDefinition> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, ModelDefinition> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry
                : resourceManager.listResources(DIRECTORY, loc -> loc.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation file = entry.getKey();
            String path = file.getPath().substring(DIRECTORY.length() + 1, file.getPath().length() - ".json".length());
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(file.getNamespace(), path);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                ResourceLocation modelLocation = ResourceLocation.parse(json.get("model").getAsString());
                BbModel model;
                try (Reader modelReader = resourceManager.openAsReader(modelLocation)) {
                    model = BbModelParser.parse(JsonParser.parseReader(modelReader).getAsJsonObject(), modelLocation.toString());
                }
                if (json.has("textures")) {
                    for (Map.Entry<String, com.google.gson.JsonElement> override : json.getAsJsonObject("textures").entrySet()) {
                        int index = Integer.parseInt(override.getKey());
                        if (index < 0 || index >= model.textures().size()) {
                            continue;
                        }
                        ResourceLocation textureLocation = ResourceLocation.parse(override.getValue().getAsString());
                        try (InputStream in = resourceManager.open(textureLocation)) {
                            model.textures().get(index).replaceImage(NativeImage.read(in));
                        }
                    }
                }
                result.put(id, ModelDefinition.create(id, model, json));
            } catch (Exception e) {
                EncoreCarts.LOGGER.error("Failed to load EncoreCarts model definition {}", file, e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<ResourceLocation, ModelDefinition> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        uploaded.forEach(textures::release);
        uploaded.clear();
        prepared.forEach((id, definition) -> {
            List<BbTexture> list = definition.model.textures();
            for (int i = 0; i < list.size(); i++) {
                BbTexture texture = list.get(i);
                if (texture.image() == null) {
                    continue;
                }
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "bbmodel/" + id.getPath() + "/" + i);
                textures.register(location, new DynamicTexture(texture.image()));
                texture.setLocation(location);
                uploaded.add(location);
            }
        });
        definitions = Map.copyOf(prepared);
        EncoreCarts.LOGGER.info("Loaded {} EncoreCarts model definitions: {}", prepared.size(), prepared.keySet());
    }
}
