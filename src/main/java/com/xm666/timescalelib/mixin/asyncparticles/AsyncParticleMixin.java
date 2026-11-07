package com.xm666.timescalelib.mixin.asyncparticles;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import neoforge.fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraft.client.particle.ParticleEngine;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@OnlyIn(Dist.CLIENT)
public class AsyncParticleMixin {
    @Mixin(AsyncTickBehavior.class)
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

    @Mixin(value = ParticleEngine.class, priority = 2000)
    private static class ParticleEngineMixin {
        @TargetHandler(mixin = "neoforge.fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.core.particle.render.MixinParticleEngine_Render", name = "render")
        @ModifyArg(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
        private float modifyPartialTick(float original) {
            return TimeScaleHandler.getScalablePartialTick(false);
        }
    }
}
