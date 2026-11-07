package com.xm666.timescalelib.mixin.ragdollified;

import com.raiiiden.ragdollified.client.ClientRagdollManager;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@OnlyIn(Dist.CLIENT)
public class RagdollMixin {
    @Mixin(value = ClientRagdollManager.class, remap = false)
    private static class ClientRagdollManagerMixin {
        @ModifyArg(method = "tickAll", at = @At(value = "INVOKE", target = "Lcom/raiiiden/ragdollified/client/ClientPhysicsWorld;step(FI)V"))
        private static float modifyDelta(float dt) {
            return dt * TimeScaleHandler.clientTimer.getScale();
        }
    }
}
