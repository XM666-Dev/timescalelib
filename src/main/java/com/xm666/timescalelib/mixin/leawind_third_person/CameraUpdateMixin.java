package com.xm666.timescalelib.mixin.leawind_third_person;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import io.github.leawind.perspectiveapi.api.PerspectiveContext;
import io.github.leawind.thirdperson.internal.logic.base.ThirdPersonPerspective;
import io.github.leawind.thirdperson.internal.logic.base.camera.CameraInput;
import io.github.leawind.thirdperson.internal.logic.base.camera.CameraSmoother;
import io.github.leawind.thirdperson.internal.logic.base.camera.CameraSmoothingParameters;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

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

    @Mixin(CameraSmoother.class)
    private static class CameraSmootherMixin {
        @WrapMethod(method = "update")
        private Optional<CameraInput> wrapUpdate(CameraInput target, double deltaSeconds, CameraSmoothingParameters smoothing, Operation<Optional<CameraInput>> original) {
            var mc = Minecraft.getInstance();
            var entity = mc.getCameraEntity();
            return TimeScaleHandler.clientTimer.runsTravelling(entity) ? original.call(target, deltaSeconds, smoothing) : Optional.empty();
        }
    }
}
