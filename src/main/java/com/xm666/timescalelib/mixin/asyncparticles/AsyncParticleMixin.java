package com.xm666.timescalelib.mixin.asyncparticles;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import forge.fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;

@OnlyIn(Dist.CLIENT)
public class AsyncParticleMixin {
    @Mixin(value = AsyncTickBehavior.class, remap = false)
    private static class AsyncTickBehaviorMixin {
        @WrapMethod(method = "preTick")
        private void wrapPreTick(boolean isTailTick, Operation<Void> original) {
            if (TimeScaleHandler.clientTimer == null || !TimeScaleHandler.clientTimer.runsTicking()) return;

            original.call(isTailTick);
        }

        @WrapMethod(method = "postTick")
        private void wrapPostTick(Operation<Void> original) {
            if (TimeScaleHandler.clientTimer == null || !TimeScaleHandler.clientTimer.runsTicking()) return;

            original.call();
        }
    }
}
