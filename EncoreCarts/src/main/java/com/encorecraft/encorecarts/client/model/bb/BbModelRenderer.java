package com.encorecraft.encorecarts.client.model.bb;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.BitSet;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Draws a {@link BbModel} in Blockbench model space (Y up, pixels, front = -Z). Callers set up the entity
 * transform and the 1/16 scale; this class applies bone pivots, base rotations and the pose offsets.
 */
public final class BbModelRenderer {
    private BbModelRenderer() {
    }

    public static void render(BbModel model, BbPose pose, PoseStack poseStack, MultiBufferSource buffers,
                              int packedLight, int packedOverlay, int color, BitSet hiddenBones) {
        List<BbBone> bones = model.bones();
        int count = bones.size();
        Matrix4f[] matrices = new Matrix4f[count];
        Matrix3f[] normals = new Matrix3f[count];
        boolean[] visible = new boolean[count];
        PoseStack.Pose root = poseStack.last();
        for (BbBone bone : bones) {
            int i = bone.index;
            BbBone parent = bone.parent;
            visible[i] = bone.visible && !hiddenBones.get(i) && (parent == null || visible[parent.index]);
            if (!visible[i]) {
                continue;
            }
            Matrix4f parentMatrix = parent == null ? root.pose() : matrices[parent.index];
            Matrix3f parentNormal = parent == null ? root.normal() : normals[parent.index];
            float px = bone.pivotX - (parent == null ? 0 : parent.pivotX) + pose.pos(i, 0);
            float py = bone.pivotY - (parent == null ? 0 : parent.pivotY) + pose.pos(i, 1);
            float pz = bone.pivotZ - (parent == null ? 0 : parent.pivotZ) + pose.pos(i, 2);
            Quaternionf rotation = new Quaternionf().rotationZYX(bone.rotZ + pose.rot(i, 2), bone.rotY + pose.rot(i, 1),
                    bone.rotX + pose.rot(i, 0));
            float sx = pose.scl(i, 0);
            float sy = pose.scl(i, 1);
            float sz = pose.scl(i, 2);
            matrices[i] = new Matrix4f(parentMatrix).translate(px, py, pz).rotate(rotation).scale(sx, sy, sz);
            Matrix3f normal = new Matrix3f(parentNormal).rotate(rotation);
            if (sx != sy || sy != sz) {
                normal.scale(1 / nonZero(sx), 1 / nonZero(sy), 1 / nonZero(sz));
            }
            normals[i] = normal;
        }

        List<BbTexture> textures = model.textures();
        Vector4f position = new Vector4f();
        Vector3f normal = new Vector3f();
        for (int t = 0; t < textures.size(); t++) {
            BbTexture texture = textures.get(t);
            VertexConsumer consumer = null;
            for (BbBone bone : bones) {
                if (!visible[bone.index]) {
                    continue;
                }
                Matrix4f matrix = matrices[bone.index];
                Matrix3f normalMatrix = normals[bone.index];
                for (BbQuad quad : bone.quads) {
                    if (quad.texture() != t) {
                        continue;
                    }
                    if (consumer == null) {
                        consumer = buffers.getBuffer(texture.translucent()
                                ? RenderType.entityTranslucent(texture.location())
                                : RenderType.entityCutoutNoCull(texture.location()));
                    }
                    normalMatrix.transform(normal.set(quad.normalX(), quad.normalY(), quad.normalZ()));
                    if (normal.lengthSquared() > 0) {
                        normal.normalize();
                    }
                    float[] p = quad.positions();
                    float[] uv = quad.uvs();
                    for (int v = 0; v < 4; v++) {
                        matrix.transform(position.set(p[v * 3], p[v * 3 + 1], p[v * 3 + 2], 1.0F));
                        consumer.addVertex(position.x(), position.y(), position.z(), color, uv[v * 2], uv[v * 2 + 1],
                                packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
                    }
                }
            }
        }
    }

    private static float nonZero(float value) {
        return Math.abs(value) < 1e-6F ? 1e-6F : value;
    }
}
