package com.xm666.timescalelib.mixin.timescalelib;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

public class TickMixin {
    @Mixin(TickRateManager.class)
    private static class TickRateManagerMixin {
        @SuppressWarnings("ConstantValue")
        @ModifyReturnValue(method = "runsNormally", at = @At("RETURN"))
        private boolean modifyRunNormally(boolean original) {
            var timer = TimeScaleHandler.getTimer(!((Object) this instanceof ServerTickRateManager));
            return original && (!TimeScaleHandler.scaleRunNormally || timer.runsTicking()) && !TimeScaleHandler.disableRunNormally;
        }
    }

    @Mixin(LivingEntity.class)
    private static class LivingEntityMixin {
        @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V"))
        private boolean wrapTravel(LivingEntity instance, Vec3 travelVector) {
            var timer = TimeScaleHandler.getTimer(instance.level().isClientSide());
            return timer.runsTraveling(instance);
        }
    }
}
