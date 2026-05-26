package com.xm666.timescalelib.mixin.timescalelib;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class TimeScaleMixin {
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

        @OnlyIn(Dist.CLIENT)
        @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;calculateEntityAnimation(Z)V"))
        private boolean wrapEntityAnimation(LivingEntity instance, boolean includeHeight) {
            return TimeScaleHandler.clientTimer.runsTraveling(instance);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(DeltaTracker.Timer.class)
    private static class DeltaTrackerTimerMixin {
        @ModifyReturnValue(method = "getGameTimeDeltaPartialTick", at = @At(value = "RETURN", ordinal = 1))
        private float modifyPartialTick(float original) {
            if (!TimeScaleHandler.scalePartialTick || TimeScaleHandler.clientTimer == null) return original;

            var base = TimeScaleHandler.clientTimer.getDeltaTickBase();
            var scale = Math.min(TimeScaleHandler.clientTimer.getScale(), 1.0F - base);
            return base + original * scale;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(Minecraft.class)
    private static class MinecraftMixin {
        @WrapOperation(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLevelRunningNormally()Z", ordinal = 1))
        private boolean wrapRunNormally(Minecraft instance, Operation<Boolean> original) {
            TimeScaleHandler.scaleRunNormally = false;
            var normally = original.call(instance);
            TimeScaleHandler.scaleRunNormally = true;

            return normally;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(GameRenderer.class)
    private static class GameRendererMixin {
        @Shadow
        @Final
        private Minecraft minecraft;

        @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DeltaTracker;getGameTimeDeltaPartialTick(Z)F"))
        private float wrapOriginalPartialTick(DeltaTracker instance, boolean runsNormally, Operation<Float> original) {
            return TimeScaleHandler.getOriginalPartialTick(runsNormally);
        }

        @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;pick(F)V"))
        private float modifyPickPartialTick(float partialTick, @Share("scalablePartialTick") LocalFloatRef scalablePartialTickRef) {
            var entity = minecraft.getCameraEntity();
            var scalablePartialTick = TimeScaleHandler.isEntityScalableFrozen(entity)
                    ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                    : partialTick;
            scalablePartialTickRef.set(scalablePartialTick);
            return scalablePartialTick;
        }

        @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(FZLorg/joml/Matrix4f;)V"))
        private float modifyItemPartialTick(float partialTick, @Share("scalablePartialTick") LocalFloatRef scalablePartialTickRef) {
            return scalablePartialTickRef.get();
        }

        @ModifyArg(method = "updateCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V"))
        private float modifyCameraPartialTick(float partialTick) {
            var entity = minecraft.getCameraEntity();
            return TimeScaleHandler.isEntityScalableFrozen(entity)
                    ? partialTick
                    : TimeScaleHandler.getOriginalPartialTick(true);
        }

        @ModifyArg(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
        private float modifyHandBobTick(float partialTick) {
            var entity = minecraft.getCameraEntity();
            return TimeScaleHandler.isEntityAuthoritativeFrozen(entity)
                    ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                    : partialTick;
        }

        @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
        private float modifyCameraBobTick(float partialTick) {
            var entity = minecraft.getCameraEntity();
            return TimeScaleHandler.isEntityAuthoritativeFrozen(entity)
                    ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                    : partialTick;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(Camera.class)
    private static class CameraMixin {
        @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 0))
        private void onPositionLerp(Level level, Entity entity, boolean detached, boolean mirror, float partialTick, CallbackInfo ci, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
            var authoritativePartialTick = TimeScaleHandler.isEntityAuthoritativeFrozen(entity)
                    ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                    : partialTick;
            authoritativePartialTickRef.set(authoritativePartialTick);
        }

        @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D"), index = 0)
        private double modifyPositionLerpDelta(double delta, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
            return authoritativePartialTickRef.get();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(LevelRenderer.class)
    private static class LevelRendererMixin {
        @WrapOperation(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;isEntityFrozen(Lnet/minecraft/world/entity/Entity;)Z"))
        private boolean wrapEntityFrozen(TickRateManager instance, Entity entity, Operation<Boolean> original) {
            return TimeScaleHandler.isEntityOriginalFrozen(entity);
        }

        @WrapOperation(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DeltaTracker;getGameTimeDeltaPartialTick(Z)F"))
        private float wrapPartialTick(DeltaTracker instance, boolean runsNormally, Operation<Float> original, @Local Entity entity) {
            return TimeScaleHandler.isEntityScalableFrozen(entity)
                    ? original.call(instance, runsNormally)
                    : TimeScaleHandler.getOriginalPartialTick(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(EntityRenderer.class)
    private static class EntityRendererMixin {
        @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 0))
        private void onPositionLerp(Entity entity, EntityRenderState reusedState, float partialTick, CallbackInfo ci, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
            var authoritativePartialTick = TimeScaleHandler.isEntityAuthoritativeFrozen(entity)
                    ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                    : partialTick;
            authoritativePartialTickRef.set(authoritativePartialTick);
        }

        @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D"), index = 0)
        private double modifyPositionLerpDelta(double delta, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
            return authoritativePartialTickRef.get();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(Entity.class)
    private static class EntityMixin {
        @WrapWithCondition(method = "setOldPosAndRot()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setOldPos()V"))
        private boolean wrapOldPos(Entity instance) {
            return TimeScaleHandler.clientTimer.runsTraveling(instance);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(AbstractClientPlayer.class)
    private static class AbstractClientPlayerMixin {
        @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/ClientAvatarState;tick(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V"))
        private boolean wrapTick(ClientAvatarState instance, Vec3 position, Vec3 deltaMovement) {
            return TimeScaleHandler.clientTimer.runsTraveling((Entity) (Object) this);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(LivingEntityRenderer.class)
    private static class LivingEntityRendererMixin {
        @ModifyArg(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;position(F)F"))
        private float modifyPositionPartialTick(float partialTick, @Local(argsOnly = true) LivingEntity entity, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
            var authoritativePartialTick = TimeScaleHandler.isEntityAuthoritativeFrozen(entity)
                    ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                    : partialTick;
            authoritativePartialTickRef.set(authoritativePartialTick);
            return authoritativePartialTick;
        }

        @ModifyArg(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;speed(F)F"))
        private float modifySpeedPartialTick(float partialTick, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
            return authoritativePartialTickRef.get();
        }
    }
}
