package com.encorecraft.encorecarts.mixin.client;

import com.encorecraft.encorecarts.client.cutscene.CameraPose;
import com.encorecraft.encorecarts.client.cutscene.CutsceneController;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    /** Detaches the camera for the cutscene; the player entity itself stays where the server keeps it. */
    @Inject(method = "setup", at = @At("TAIL"))
    private void encorecarts$cutsceneCamera(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse,
                                            float partialTick, CallbackInfo ci) {
        CameraPose pose = CutsceneController.cameraPose(partialTick);
        if (pose != null) {
            setRotation(pose.yaw(), pose.pitch());
            setPosition(pose.x(), pose.y(), pose.z());
        }
    }
}
