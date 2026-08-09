package com.xm666.timescalelib.mixin.leawind_third_person;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import io.github.leawind.perspectiveapi.api.PerspectiveContext;
import io.github.leawind.thirdperson.internal.logic.base.ThirdPersonPerspective;
import io.github.leawind.thirdperson.internal.logic.base.pivot.CameraPivotSmoothing;
import io.github.leawind.thirdperson.internal.logic.base.pivot.CameraPivotTracker;
import io.github.leawind.thirdperson.internal.logic.base.pivot.MinecraftCameraPivotIntegration;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

public class CameraUpdateMixin {
    @Mixin(ThirdPersonPerspective.class)
    private static class ThirdPersonPerspectiveMixin {
        @WrapOperation(method = "computeCameraState", at = @At(value = "INVOKE", target = "Lio/github/leawind/perspectiveapi/api/PerspectiveContext;partialTicks()F"))
        private static float wrapPartialTick(PerspectiveContext instance, Operation<Float> original, @Local(name = "entity") Entity entity) {
            return TimeScaleHandler.isEntityEnforceableFrozen(entity)
                    ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                    : original.call(instance);
        }
    }

    @Mixin(MinecraftCameraPivotIntegration.class)
    private static class MinecraftCameraPivotIntegrationMixin {
        @WrapWithCondition(method = "onClientTick", at = @At(value = "INVOKE", target = "Lio/github/leawind/thirdperson/internal/logic/base/pivot/CameraPivotTracker;updateTick(Lorg/joml/Vector3dc;DLio/github/leawind/thirdperson/internal/logic/base/pivot/CameraPivotSmoothing;)Ljava/util/Optional;"))
        private static boolean wrapUpdateTick(CameraPivotTracker instance, Vector3dc target, double deltaSeconds, CameraPivotSmoothing smoothing, @Local(name = "entity") Entity entity) {
            return TimeScaleHandler.clientTimer.runsTravelling(entity);
        }
    }
}
