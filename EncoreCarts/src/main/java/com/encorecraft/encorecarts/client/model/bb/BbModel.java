package com.encorecraft.encorecarts.client.model.bb;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

/** Parsed Blockbench project: bones (parent before child), textures and animations. */
public final class BbModel {
    public final String name;
    private final List<BbBone> bones;
    private final List<BbTexture> textures;
    private final Map<String, BbAnimation> animations;
    private final Map<String, BbBone> bonesByName = new HashMap<>();
    private final Map<String, BbBone> bonesByUuid = new HashMap<>();
    private final Map<String, BbAnimation> animationsByName = new HashMap<>();

    BbModel(String name, List<BbBone> bones, List<BbTexture> textures, Map<String, BbAnimation> animations) {
        this.name = name;
        this.bones = List.copyOf(bones);
        this.textures = List.copyOf(textures);
        this.animations = Collections.unmodifiableMap(animations);
        for (BbBone bone : bones) {
            bonesByName.putIfAbsent(bone.name.toLowerCase(Locale.ROOT), bone);
            bonesByUuid.put(bone.uuid, bone);
        }
        animations.forEach((key, animation) -> {
            animationsByName.putIfAbsent(key.toLowerCase(Locale.ROOT), animation);
            animation.bind(this);
        });
    }

    public List<BbBone> bones() {
        return bones;
    }

    public List<BbTexture> textures() {
        return textures;
    }

    public Map<String, BbAnimation> animations() {
        return animations;
    }

    @Nullable
    public BbBone bone(String name) {
        return name == null ? null : bonesByName.get(name.toLowerCase(Locale.ROOT));
    }

    @Nullable
    BbBone boneByUuid(String uuid) {
        return uuid == null ? null : bonesByUuid.get(uuid);
    }

    @Nullable
    public BbAnimation animation(String name) {
        return name == null ? null : animationsByName.get(name.toLowerCase(Locale.ROOT));
    }

    public BbPose newPose() {
        return new BbPose(bones.size());
    }
}
