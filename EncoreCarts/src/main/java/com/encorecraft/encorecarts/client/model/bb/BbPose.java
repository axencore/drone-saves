package com.encorecraft.encorecarts.client.model.bb;

import java.util.Arrays;

/** Additive per-bone animation offsets for one frame: rotation (radians), position (pixels), scale (factor). */
public final class BbPose {
    private final float[] rotation;
    private final float[] position;
    private final float[] scale;

    public BbPose(int boneCount) {
        this.rotation = new float[boneCount * 3];
        this.position = new float[boneCount * 3];
        this.scale = new float[boneCount * 3];
        reset();
    }

    public int boneCount() {
        return rotation.length / 3;
    }

    public void reset() {
        Arrays.fill(rotation, 0);
        Arrays.fill(position, 0);
        Arrays.fill(scale, 1);
    }

    public void addRotation(int bone, float x, float y, float z) {
        rotation[bone * 3] += x;
        rotation[bone * 3 + 1] += y;
        rotation[bone * 3 + 2] += z;
    }

    public void addPosition(int bone, float x, float y, float z) {
        position[bone * 3] += x;
        position[bone * 3 + 1] += y;
        position[bone * 3 + 2] += z;
    }

    public void multiplyScale(int bone, float x, float y, float z) {
        scale[bone * 3] *= x;
        scale[bone * 3 + 1] *= y;
        scale[bone * 3 + 2] *= z;
    }

    float rot(int bone, int axis) {
        return rotation[bone * 3 + axis];
    }

    float pos(int bone, int axis) {
        return position[bone * 3 + axis];
    }

    float scl(int bone, int axis) {
        return scale[bone * 3 + axis];
    }
}
