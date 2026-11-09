package com.encorecraft.encorecarts.client.model.bb;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** A Blockbench group (outliner node): pivot, base rotation and the geometry it owns, in model pixels. */
public final class BbBone {
    public final int index;
    public final String uuid;
    public final String name;
    @Nullable
    public final BbBone parent;
    public final float pivotX;
    public final float pivotY;
    public final float pivotZ;
    /** Base rotation in radians, Euler ZYX as in Blockbench. */
    public final float rotX;
    public final float rotY;
    public final float rotZ;
    public final boolean visible;
    final List<BbQuad> quads = new ArrayList<>();
    final List<BbBone> children = new ArrayList<>();

    BbBone(int index, String uuid, String name, @Nullable BbBone parent, float pivotX, float pivotY, float pivotZ,
           float rotX, float rotY, float rotZ, boolean visible) {
        this.index = index;
        this.uuid = uuid;
        this.name = name;
        this.parent = parent;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        this.pivotZ = pivotZ;
        this.rotX = rotX;
        this.rotY = rotY;
        this.rotZ = rotZ;
        this.visible = visible;
    }

    public List<BbQuad> quads() {
        return quads;
    }

    public List<BbBone> children() {
        return children;
    }

    @Override
    public String toString() {
        return "BbBone[" + name + "]";
    }
}
