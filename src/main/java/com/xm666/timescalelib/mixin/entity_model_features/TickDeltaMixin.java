package com.xm666.timescalelib.mixin.entity_model_features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import com.xm666.timescalelib.handler.WrapHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import traben.entity_model_features.models.animation.EMFAnimationEntityContext;
import traben.entity_model_features.utils.EMFEntity;

@OnlyIn(Dist.CLIENT)
@Mixin(EMFAnimationEntityContext.class)
public class TickDeltaMixin {
    @Shadow
    @Deprecated
    private static EMFEntity emfEntity() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @WrapOperation(method = "getTickDelta", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DeltaTracker$Timer;getGameTimeDeltaPartialTick(Z)F"))
    private static float wrapPartialTick(DeltaTracker.Timer instance, boolean runsNormally, Operation<Float> original) {
        var entity = (Entity) emfEntity();
        if (entity == null) return original.call(instance, runsNormally);

        var mc = Minecraft.getInstance();
        var tickRateManager = mc.level.tickRateManager();
        var entityRunsNormally = !tickRateManager.isEntityFrozen(entity);
        return TimeScaleHandler.isEntityScalableFrozen(entity)
                ? WrapHandler.callScaled(original, instance, entityRunsNormally)
                : original.call(instance, entityRunsNormally);
    }
}
