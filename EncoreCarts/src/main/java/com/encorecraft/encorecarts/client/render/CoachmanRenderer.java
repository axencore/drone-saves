package com.encorecraft.encorecarts.client.render;

import com.encorecraft.encorecarts.client.cutscene.CutsceneController;
import com.encorecraft.encorecarts.client.model.CartsModels;
import com.encorecraft.encorecarts.client.model.ModelDefinition;
import com.encorecraft.encorecarts.client.model.bb.BbAnimation;
import com.encorecraft.encorecarts.client.model.bb.BbModelRenderer;
import com.encorecraft.encorecarts.client.model.bb.BbPose;
import com.encorecraft.encorecarts.entity.CoachmanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Coachman driven by the classic/slim Blockbench models and their animations:
 * "idle" while standing, "greet" (hello) when someone talks to him, "blink" now and then, "sit" on the cart.
 */
public class CoachmanRenderer extends EntityRenderer<CoachmanEntity> {
    private static final float GREET_FADE_SECONDS = 0.25F;
    private static final int BLINK_PERIOD_TICKS = 87;
    private static final int BLINK_TICKS = 3;

    public CoachmanRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Nullable
    public static ModelDefinition definition(CoachmanEntity coachman) {
        ModelDefinition def = CartsModels.get(coachman.isSlim() ? CartsModels.COACHMAN_SLIM : CartsModels.COACHMAN_CLASSIC);
        return def != null ? def : CartsModels.get(CartsModels.COACHMAN_CLASSIC);
    }

    @Override
    public boolean shouldRender(CoachmanEntity coachman, Frustum frustum, double camX, double camY, double camZ) {
        return !CutsceneController.isHidden(coachman) && super.shouldRender(coachman, frustum, camX, camY, camZ);
    }

    @Override
    public void render(CoachmanEntity coachman, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        ModelDefinition def = definition(coachman);
        if (def == null) {
            return;
        }
        float bodyYaw = Mth.rotLerp(partialTick, coachman.yBodyRotO, coachman.yBodyRot);
        float headYaw = Mth.clamp(Mth.wrapDegrees(Mth.rotLerp(partialTick, coachman.yHeadRotO, coachman.yHeadRot) - bodyYaw), -70.0F, 70.0F);
        float headPitch = Mth.lerp(partialTick, coachman.xRotO, coachman.getXRot());

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.translate(def.offset.x, def.offset.y, -def.offset.z);
        float scale = def.scale / 16.0F;
        poseStack.scale(scale, scale, scale);

        BbPose pose = def.model.newPose();
        animate(def, coachman, partialTick, headYaw, headPitch, pose);
        BbModelRenderer.render(def.model, pose, poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, def.hiddenBones);
        poseStack.popPose();
        super.render(coachman, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    static void animate(ModelDefinition def, CoachmanEntity coachman, float partialTick, float headYaw, float headPitch, BbPose pose) {
        float age = coachman.tickCount + partialTick;
        float seconds = age / 20.0F;
        BbAnimation base = def.animation(coachman.cutsceneSeated ? "sit" : "idle");
        if (base != null) {
            base.apply(pose, base.localTime(seconds), 1.0F, seconds);
        }
        BbAnimation greet = coachman.cutsceneSeated ? null : def.animation("greet");
        if (greet != null) {
            float since = (age - coachman.greetStartTick) / 20.0F;
            if (since >= 0 && since <= greet.length) {
                float weight = Math.min(1.0F, Math.min(since, greet.length - since) / GREET_FADE_SECONDS);
                greet.apply(pose, greet.localTime(since), Mth.clamp(weight, 0.0F, 1.0F), seconds);
            }
        }
        BbAnimation blink = def.animation("blink");
        if (blink != null && Math.floorMod((int) age + coachman.getId() * 31, BLINK_PERIOD_TICKS) < BLINK_TICKS) {
            blink.apply(pose, 0.0F, 1.0F, seconds);
        }
        if (def.headBone >= 0) {
            pose.addRotation(def.headBone, (float) Math.toRadians(-headPitch), (float) Math.toRadians(-headYaw), 0.0F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CoachmanEntity coachman) {
        ModelDefinition def = definition(coachman);
        return def == null || def.model.textures().isEmpty()
                ? MissingTextureAtlasSprite.getLocation() : def.model.textures().get(0).location();
    }
}
