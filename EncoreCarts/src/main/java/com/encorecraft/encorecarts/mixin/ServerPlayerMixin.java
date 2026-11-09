package com.encorecraft.encorecarts.mixin;

import com.encorecraft.encorecarts.travel.TravelVisibility;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    /** Other players simply stop seeing a traveller until they arrive. */
    @Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
    private void encorecarts$hideTraveller(ServerPlayer viewer, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (viewer != self && TravelVisibility.isHidden(self.getUUID())) {
            cir.setReturnValue(false);
        }
    }
}
