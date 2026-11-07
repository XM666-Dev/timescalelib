package com.xm666.timescalelib.mixin.geckolib;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import com.xm666.timescalelib.tickrate.TickRateHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

@OnlyIn(Dist.CLIENT)
public class ModelMixin {
    @Mixin(GeoModel.class)
    private static class GeoModelMixin {
        @WrapOperation(method = "handleAnimations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getFrameTime()F"))
        private float wrapFrameTime(Minecraft instance, Operation<Float> original, GeoAnimatable animatable) {
            if (!(animatable instanceof Entity entity)) return original.call(instance);

            return TimeScaleHandler.isEntityScalableFrozen(entity)
                    ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                    : TickRateHandler.timer.getGameTimeDeltaPartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity));
        }
    }

    @Mixin(GeoArmorRenderer.class)
    private static class GeoArmorRendererMixin {
        @Shadow(remap = false)
        protected Entity currentEntity;

        @WrapOperation(method = "renderToBuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getFrameTime()F"))
        private float wrapFrameTime(Minecraft instance, Operation<Float> original) {
            if (currentEntity == null) return original.call(instance);

            return TimeScaleHandler.isEntityScalableFrozen(currentEntity)
                    ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(currentEntity))
                    : TickRateHandler.timer.getGameTimeDeltaPartialTick(!TimeScaleHandler.isEntityOriginalFrozen(currentEntity));
        }
    }
}
