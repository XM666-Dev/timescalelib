package com.xm666.timescalelib.mixin.epicfight;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import net.minecraft.client.Camera;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.client.event.types.BuildCameraTransform;

@OnlyIn(Dist.CLIENT)
public class CameraMixin {
    @Mixin(EpicFightCameraAPI.class)
    private static class EpicFightCameraAPIMixin {
        @Inject(method = "setupCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 0))
        private void onCameraPositionLerp(Camera camera, float partialTick, CallbackInfoReturnable<BuildCameraTransform.Pre> cir, @Share("enforceablePartialTick") LocalFloatRef enforceablePartialTickRef) {
            var entity = camera.getEntity();
            var enforceablePartialTick = TimeScaleHandler.isEntityEnforceableFrozen(entity)
                    ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                    : partialTick;
            enforceablePartialTickRef.set(enforceablePartialTick);
        }

        @ModifyArg(method = "setupCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 0), index = 0)
        private double modifyCameraPositionLerpDelta0(double delta, @Share("enforceablePartialTick") LocalFloatRef enforceablePartialTickRef) {
            return enforceablePartialTickRef.get();
        }

        @ModifyArg(method = "setupCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 1), index = 0)
        private double modifyCameraPositionLerpDelta1(double delta, @Share("enforceablePartialTick") LocalFloatRef enforceablePartialTickRef) {
            return enforceablePartialTickRef.get();
        }

        @ModifyArg(method = "setupCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 2), index = 0)
        private double modifyCameraPositionLerpDelta2(double delta, @Share("enforceablePartialTick") LocalFloatRef enforceablePartialTickRef) {
            return enforceablePartialTickRef.get();
        }

        @ModifyArg(method = "setupCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 3), index = 0)
        private double modifyCameraPositionLerpDelta3(double delta, @Share("enforceablePartialTick") LocalFloatRef enforceablePartialTickRef) {
            return enforceablePartialTickRef.get();
        }

        @ModifyArg(method = "setupCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 4), index = 0)
        private double modifyCameraPositionLerpDelta4(double delta, @Share("enforceablePartialTick") LocalFloatRef enforceablePartialTickRef) {
            return enforceablePartialTickRef.get();
        }

        @ModifyArg(method = "setupCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 5), index = 0)
        private double modifyCameraPositionLerpDelta5(double delta, @Share("enforceablePartialTick") LocalFloatRef enforceablePartialTickRef) {
            return enforceablePartialTickRef.get();
        }

        @ModifyArg(method = "setupCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 6), index = 0)
        private double modifyCameraPositionLerpDelta6(double delta, @Share("enforceablePartialTick") LocalFloatRef enforceablePartialTickRef) {
            return enforceablePartialTickRef.get();
        }

        @ModifyArg(method = "setupCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 7), index = 0)
        private double modifyCameraPositionLerpDelta7(double delta, @Share("enforceablePartialTick") LocalFloatRef enforceablePartialTickRef) {
            return enforceablePartialTickRef.get();
        }

        @ModifyArg(method = "setupCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 8), index = 0)
        private double modifyCameraPositionLerpDelta8(double delta, @Share("enforceablePartialTick") LocalFloatRef enforceablePartialTickRef) {
            return enforceablePartialTickRef.get();
        }

        @ModifyArg(method = "setupCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 9), index = 0)
        private double modifyCameraPositionLerpDelta9(double delta, @Share("enforceablePartialTick") LocalFloatRef enforceablePartialTickRef) {
            return enforceablePartialTickRef.get();
        }

        @ModifyArg(method = "setupCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F", ordinal = 0), index = 0)
        private float modifyCameraPositionLerpDelta10(float delta, @Share("enforceablePartialTick") LocalFloatRef enforceablePartialTickRef) {
            return enforceablePartialTickRef.get();
        }

        @ModifyArg(method = "setupCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F", ordinal = 1), index = 0)
        private float modifyCameraPositionLerpDelta11(float delta, @Share("enforceablePartialTick") LocalFloatRef enforceablePartialTickRef) {
            return enforceablePartialTickRef.get();
        }
    }
}
