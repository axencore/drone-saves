package com.encorecraft.encorecarts.client.render;

import com.encorecraft.encorecarts.client.cutscene.CutsceneController;
import com.encorecraft.encorecarts.client.model.CartsModels;
import com.encorecraft.encorecarts.client.model.ModelDefinition;
import com.encorecraft.encorecarts.client.model.bb.BbAnimation;
import com.encorecraft.encorecarts.client.model.bb.BbModelRenderer;
import com.encorecraft.encorecarts.client.model.bb.BbPose;
import com.encorecraft.encorecarts.entity.CartEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.phys.Vec3;

/** Renders the cart from {@code encorecarts_models/cart.json}; the model itself is fully replaceable. */
public class CartRenderer extends EntityRenderer<CartEntity> {
    public CartRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.3F;
        this.shadowStrength = 0.8F;
    }

    @Override
    public boolean shouldRender(CartEntity cart, Frustum frustum, double camX, double camY, double camZ) {
        return !CutsceneController.isHidden(cart) && super.shouldRender(cart, frustum, camX, camY, camZ);
    }

    @Override
    public void render(CartEntity cart, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        ModelDefinition def = CartsModels.get(CartsModels.CART);
        if (def == null) {
            return;
        }
        float yaw = Mth.rotLerp(partialTick, cart.yRotO, cart.getYRot());
        float pitch = Mth.lerp(partialTick, cart.renderPitchO, cart.renderPitch);
        float seconds = (cart.tickCount + partialTick) / 20.0F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        // Definition offsets are [right, up, forward]; model space has +X right, +Y up, -Z forward.
        poseStack.translate(def.offset.x, def.offset.y, -def.offset.z);
        float scale = def.scale / 16.0F;
        poseStack.scale(scale, scale, scale);

        BbPose pose = def.model.newPose();
        BbAnimation animation = def.animation(cart.cutsceneActor ? "moving" : "idle");
        if (animation != null) {
            animation.apply(pose, animation.localTime(seconds), 1.0F, seconds);
        }
        float spin = Mth.lerp(partialTick, cart.wheelSpinO, cart.wheelSpin);
        for (int wheel : def.wheelBones) {
            pose.addRotation(wheel, -spin, 0, 0);
        }
        BbModelRenderer.render(def.model, pose, poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, def.hiddenBones);
        poseStack.popPose();

        if (def.horse && !cart.cutsceneActor) {
            renderStandingHorse(cart, def, yaw, partialTick, poseStack, buffers);
        }
        super.render(cart, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private void renderStandingHorse(CartEntity cart, ModelDefinition def, float yaw, float partialTick,
                                     PoseStack poseStack, MultiBufferSource buffers) {
        Horse horse = horseFor(cart);
        if (horse == null) {
            return;
        }
        Vec3 relative = localOffset(yaw, def.horseOffset);
        Vec3 world = cart.position().add(relative);
        horse.setPos(world.x, world.y, world.z);
        horse.xo = horse.xOld = world.x;
        horse.yo = horse.yOld = world.y;
        horse.zo = horse.zOld = world.z;
        horse.setYRot(yaw);
        horse.yRotO = yaw;
        horse.yBodyRot = horse.yBodyRotO = yaw;
        horse.yHeadRot = horse.yHeadRotO = yaw;
        horse.tickCount = cart.tickCount;
        int light = LevelRenderer.getLightColor(cart.level(), BlockPos.containing(world.add(0, 1.0, 0)));
        poseStack.pushPose();
        poseStack.translate(relative.x, relative.y, relative.z);
        if (def.horseScale != 1.0F) {
            poseStack.scale(def.horseScale, def.horseScale, def.horseScale);
        }
        this.entityRenderDispatcher.render(horse, 0, 0, 0, yaw, partialTick, poseStack, buffers, light);
        poseStack.popPose();
    }

    /** A purely visual horse, never added to the level. */
    public static Horse horseFor(CartEntity cart) {
        if (cart.renderHorse == null) {
            Horse horse = EntityType.HORSE.create(cart.level());
            if (horse != null) {
                Variant[] variants = {Variant.CHESTNUT, Variant.BROWN, Variant.DARK_BROWN, Variant.BLACK, Variant.GRAY, Variant.CREAMY};
                horse.setVariant(variants[Math.floorMod(cart.getUUID().hashCode(), variants.length)]);
                horse.setNoAi(true);
                horse.setSilent(true);
            }
            cart.renderHorse = horse;
        }
        return cart.renderHorse;
    }

    /** {@code [right, up, forward]} rotated into world axes for an entity yaw. */
    public static Vec3 localOffset(float yaw, Vec3 offset) {
        double r = Math.toRadians(yaw);
        double fx = -Math.sin(r);
        double fz = Math.cos(r);
        double rx = -Math.cos(r);
        double rz = -Math.sin(r);
        return new Vec3(rx * offset.x + fx * offset.z, offset.y, rz * offset.x + fz * offset.z);
    }

    @Override
    public ResourceLocation getTextureLocation(CartEntity cart) {
        ModelDefinition def = CartsModels.get(CartsModels.CART);
        return def == null || def.model.textures().isEmpty()
                ? MissingTextureAtlasSprite.getLocation() : def.model.textures().get(0).location();
    }
}
