package com.xm666.timescalelib.mixin.timescalelib;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.xm666.timescalelib.handler.MixinHandler;
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

@OnlyIn(Dist.CLIENT)
public class PartialTickMixin {
    private static class GameTimerMixin {
        @Mixin(DeltaTracker.Timer.class)
        private static class TimerMixin {
            @ModifyReturnValue(method = "getGameTimeDeltaPartialTick", at = @At(value = "RETURN", ordinal = 1))
            private float modifyPartialTick(float original) {
                if (!TimeScaleHandler.scalePartialTick || TimeScaleHandler.clientTimer == null) return original;

                var sequentialTick = TimeScaleHandler.clientTimer.getDeltaTickSequential();
                var scale = Math.min(TimeScaleHandler.clientTimer.getScale(), 1.0F - sequentialTick);
                return sequentialTick + original * scale;
            }
        }
    }

    private static class GameBobMixin {
        @Mixin(AbstractClientPlayer.class)
        private static class AbstractClientPlayerMixin {
            @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/ClientAvatarState;tick(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V"))
            private boolean wrapWalkDist(ClientAvatarState instance, Vec3 position, Vec3 deltaMovement) {
                return TimeScaleHandler.clientTimer.runsTravelling((Entity) (Object) this);
            }

            @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;updateBob()V"))
            private boolean wrapBob(AbstractClientPlayer instance) {
                return TimeScaleHandler.clientTimer.runsTravelling(instance);
            }
        }
    }

    private static class GameViewMixin {
        @Mixin(GameRenderer.class)
        private static class GameRendererMixin {
            @Shadow
            @Final
            private Minecraft minecraft;

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
            private float modifyHandPartialTick(float partialTick, @Share("scalablePartialTick") LocalFloatRef scalablePartialTickRef) {
                return scalablePartialTickRef.get();
            }

            @ModifyArg(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
            private float modifyHandBobPartialTick(float partialTick) {
                var entity = minecraft.getCameraEntity();
                return TimeScaleHandler.isEntityAuthoritativeFrozen(entity)
                        ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                        : partialTick;
            }
        }
    }

    private static class GameCameraMixin {
        @Mixin(GameRenderer.class)
        private static class GameRendererMixin {
            @Shadow
            @Final
            private Minecraft minecraft;

            @ModifyArg(method = "updateCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V"))
            private float modifyCameraPartialTick(float partialTick) {
                var entity = minecraft.getCameraEntity();
                return TimeScaleHandler.isEntityScalableFrozen(entity)
                        ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                        : partialTick;
            }

            @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
            private float modifyCameraBobPartialTick(float partialTick) {
                var entity = minecraft.getCameraEntity();
                return TimeScaleHandler.isEntityAuthoritativeFrozen(entity)
                        ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                        : partialTick;
            }
        }

        @Mixin(Camera.class)
        private static class CameraMixin {
            @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 0))
            private void onCameraPositionLerp(Level level, Entity entity, boolean detached, boolean mirror, float partialTick, CallbackInfo ci, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
                var authoritativePartialTick = TimeScaleHandler.isEntityAuthoritativeFrozen(entity)
                        ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                        : partialTick;
                authoritativePartialTickRef.set(authoritativePartialTick);
            }

            @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D"), index = 0)
            private double modifyCameraPositionLerpDelta(double delta, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
                return authoritativePartialTickRef.get();
            }
        }
    }

    private static class GameLevelMixin {
        @Mixin(LevelRenderer.class)
        private static class LevelRendererMixin {
            @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DeltaTracker;getGameTimeDeltaPartialTick(Z)F", ordinal = 0))
            private float wrapLevelPartialTick(DeltaTracker instance, boolean runsNormally, Operation<Float> original) {
                return MixinHandler.callWithScale(original, instance, runsNormally);
            }
        }
    }

    private static class GameEntityMixin {
        @Mixin(LevelRenderer.class)
        private static class LevelRendererMixin {
            @WrapOperation(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DeltaTracker;getGameTimeDeltaPartialTick(Z)F"))
            private float wrapEntityPartialTick(DeltaTracker instance, boolean runsNormally, Operation<Float> original, @Local Entity entity) {
                return TimeScaleHandler.isEntityScalableFrozen(entity)
                        ? MixinHandler.callWithScale(original, instance, runsNormally)
                        : original.call(instance, runsNormally);
            }
        }

        @Mixin(Entity.class)
        private static class EntityMixin {
            @WrapWithCondition(method = "setOldPosAndRot()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setOldPos()V"))
            private boolean wrapOldPos(Entity instance) {
                return TimeScaleHandler.clientTimer.runsTravelling(instance);
            }
        }

        @Mixin(EntityRenderer.class)
        private static class EntityRendererMixin {
            @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 0))
            private void onEntityPositionLerp(Entity entity, EntityRenderState reusedState, float partialTick, CallbackInfo ci, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
                var authoritativePartialTick = TimeScaleHandler.isEntityAuthoritativeFrozen(entity)
                        ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                        : partialTick;
                authoritativePartialTickRef.set(authoritativePartialTick);
            }

            @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D"), index = 0)
            private double modifyEntityPositionLerpDelta(double delta, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
                return authoritativePartialTickRef.get();
            }
        }

        @Mixin(LivingEntity.class)
        private static class LivingEntityMixin {
            @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;calculateEntityAnimation(Z)V"))
            private boolean wrapEntityAnimation(LivingEntity instance, boolean includeHeight) {
                return TimeScaleHandler.clientTimer.runsTravelling(instance);
            }
        }

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
}
