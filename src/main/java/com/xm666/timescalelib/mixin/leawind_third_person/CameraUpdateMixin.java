package com.xm666.timescalelib.mixin.leawind_third_person;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import io.github.leawind.perspectiveapi.api.PerspectiveContext;
import io.github.leawind.thirdperson.internal.logic.base.MinecraftClientIntegration;
import io.github.leawind.thirdperson.internal.logic.base.ThirdPersonPerspective;
import io.github.leawind.thirdperson.internal.logic.base.camera.CameraPivotSmoother;
import io.github.leawind.thirdperson.internal.logic.base.camera.CameraSmoothingParameters;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

public class CameraUpdateMixin {
    @Mixin(ThirdPersonPerspective.class)
    private static class ThirdPersonPerspectiveMixin {
        @WrapOperation(method = "applyCameraState", at = @At(value = "INVOKE", target = "Lio/github/leawind/perspectiveapi/api/PerspectiveContext;partialTicks()F", ordinal = 0))
        private static float wrapPartialTick(PerspectiveContext instance, Operation<Float> original, @Local(name = "entity") Entity entity) {
            return TimeScaleHandler.isEntityEnforceableFrozen(entity)
                    ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                    : original.call(instance);
        }
    }

    @Mixin(MinecraftClientIntegration.class)
    private static class MinecraftClientIntegrationMixin {
        @WrapWithCondition(method = "onClientTick", at = @At(value = "INVOKE", target = "Lio/github/leawind/thirdperson/internal/logic/base/camera/CameraPivotSmoother;updateTick(Lorg/joml/Vector3dc;DLio/github/leawind/thirdperson/internal/logic/base/camera/CameraSmoothingParameters;)Ljava/util/Optional;"))
        private static boolean wrapUpdateTick(CameraPivotSmoother instance, Vector3dc target, double deltaSeconds, CameraSmoothingParameters smoothing, @Local(name = "cameraEntity") Entity cameraEntity) {
            return TimeScaleHandler.clientTimer.runsTravelling(cameraEntity);
        }
    }
}
