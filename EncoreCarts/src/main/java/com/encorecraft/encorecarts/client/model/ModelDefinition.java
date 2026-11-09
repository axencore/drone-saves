package com.encorecraft.encorecarts.client.model;

import com.encorecraft.encorecarts.client.model.bb.BbAnimation;
import com.encorecraft.encorecarts.client.model.bb.BbBone;
import com.encorecraft.encorecarts.client.model.bb.BbModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Rig description loaded from {@code assets/<ns>/encorecarts_models/<id>.json}. Everything that ties a model
 * to gameplay (which animation plays when, wheels, seats, the draught horse) lives here, so replacing a model
 * only needs a new {@code .bbmodel} and, if its proportions differ, adjusted numbers in this file.
 * Vectors are {@code [right, up, forward]} in blocks relative to the entity position.
 */
public final class ModelDefinition {
    public final ResourceLocation id;
    public final BbModel model;
    public final float scale;
    public final Vec3 offset;
    public final BitSet hiddenBones = new BitSet();
    public final int headBone;
    public final int[] wheelBones;
    public final float wheelRadius;
    public final float seatHeight;
    public final boolean horse;
    public final Vec3 horseOffset;
    public final float horseScale;
    private final Map<String, String> animations = new HashMap<>();
    private final Map<String, Vec3> seats = new HashMap<>();

    private ModelDefinition(ResourceLocation id, BbModel model, JsonObject json) {
        this.id = id;
        this.model = model;
        this.scale = json.has("scale") ? json.get("scale").getAsFloat() : 1.0F;
        this.offset = vec(json.get("offset"), Vec3.ZERO);
        if (json.has("hidden_bones")) {
            for (JsonElement name : json.getAsJsonArray("hidden_bones")) {
                BbBone bone = model.bone(name.getAsString());
                if (bone != null) {
                    hiddenBones.set(bone.index);
                }
            }
        }
        BbBone head = json.has("head_bone") ? model.bone(json.get("head_bone").getAsString()) : null;
        this.headBone = head == null ? -1 : head.index;
        List<Integer> wheels = new ArrayList<>();
        if (json.has("wheel_bones")) {
            for (JsonElement name : json.getAsJsonArray("wheel_bones")) {
                BbBone bone = model.bone(name.getAsString());
                if (bone != null) {
                    wheels.add(bone.index);
                }
            }
        }
        this.wheelBones = wheels.stream().mapToInt(Integer::intValue).toArray();
        this.wheelRadius = json.has("wheel_radius") ? json.get("wheel_radius").getAsFloat() : 0.5F;
        this.seatHeight = json.has("seat_height") ? json.get("seat_height").getAsFloat() : 0.0F;
        if (json.has("animations")) {
            json.getAsJsonObject("animations").entrySet()
                    .forEach(entry -> animations.put(entry.getKey(), entry.getValue().getAsString()));
        }
        if (json.has("seats")) {
            json.getAsJsonObject("seats").entrySet()
                    .forEach(entry -> seats.put(entry.getKey(), vec(entry.getValue(), Vec3.ZERO)));
        }
        JsonObject horseJson = json.has("horse") ? json.getAsJsonObject("horse") : new JsonObject();
        this.horse = horseJson.has("enabled") && horseJson.get("enabled").getAsBoolean();
        this.horseOffset = vec(horseJson.get("offset"), new Vec3(0, 0, 2.8));
        this.horseScale = horseJson.has("scale") ? horseJson.get("scale").getAsFloat() : 1.0F;
    }

    static ModelDefinition create(ResourceLocation id, BbModel model, JsonObject json) {
        return new ModelDefinition(id, model, json);
    }

    /** Animation bound to a gameplay role ("idle", "moving", "sit", "greet", "blink"), if the model has it. */
    @Nullable
    public BbAnimation animation(String role) {
        return model.animation(animations.getOrDefault(role, role));
    }

    public Vec3 seat(String role, Vec3 fallback) {
        return seats.getOrDefault(role, fallback);
    }

    private static Vec3 vec(@Nullable JsonElement element, Vec3 fallback) {
        if (element == null || !element.isJsonArray()) {
            return fallback;
        }
        JsonArray a = element.getAsJsonArray();
        return a.size() < 3 ? fallback : new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
    }
}
