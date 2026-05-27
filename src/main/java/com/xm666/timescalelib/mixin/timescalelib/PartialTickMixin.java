package com.xm666.timescalelib.mixin.timescalelib;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xm666.timescalelib.TimeScaleLib;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.objectweb.asm.Opcodes;
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

        @Mixin(Minecraft.class)
        private static class MinecraftMixin {
            @WrapOperation(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLevelRunningNormally()Z"))
            private boolean wrapRunNormally(Minecraft instance, Operation<Boolean> original) {
                TimeScaleHandler.scaleRunNormally = false;
                var normally = original.call(instance);
                TimeScaleHandler.scaleRunNormally = true;

                return normally;
            }
        }
    }

    @Mixin(GameRenderer.class)
    private static class GameRendererMixin {
        @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DeltaTracker;getGameTimeDeltaPartialTick(Z)F"))
        private float wrapOriginalPartialTick(DeltaTracker instance, boolean runsNormally, Operation<Float> original) {
            return TimeScaleHandler.getOriginalPartialTick(runsNormally);
        }

        @ModifyArg(method = "bobView", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F"), index = 0)
        private float modifyBobLerpDelta(float delta, @Local Player player) {
            return TimeScaleHandler.isEntityScalableFrozen(player)
                    ? delta
                    : TimeScaleHandler.getOriginalPartialTick(!TimeScaleHandler.isEntityOriginalFrozen(player));
        }
    }

    @Mixin(Entity.class)
    private static class EntityMixin {
        @WrapWithCondition(method = "baseTick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;walkDistO:F", opcode = Opcodes.PUTFIELD))
        private boolean wrapWalkDistO(Entity instance, float value) {
            return TimeScaleHandler.clientTimer.runsTraveling((Entity) (Object) this);
        }
    }

    private static class GameViewMixin {
        @Mixin(GameRenderer.class)
        private static class GameRendererMixin {
            @Shadow
            @Final
            Minecraft minecraft;

            @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;pick(F)V"))
            private float modifyPickPartialTick(float partialTick, @Share(value = "scalablePartialTick", namespace = TimeScaleLib.MODID) LocalFloatRef scalablePartialTickRef) {
                var entity = minecraft.getCameraEntity();
                var scalablePartialTick = TimeScaleHandler.isEntityScalableFrozen(entity)
                        ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                        : partialTick;
                scalablePartialTickRef.set(scalablePartialTick);
                return scalablePartialTick;
            }

            @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/Camera;FLorg/joml/Matrix4f;)V"))
            private float modifyHandPartialTick(float original, @Share(value = "scalablePartialTick", namespace = TimeScaleLib.MODID) LocalFloatRef scalablePartialTickRef) {
                return scalablePartialTickRef.get();
            }

            @ModifyArg(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
            private float modifyHandBobTick(float partialTick) {
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
            Minecraft minecraft;

            @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"))
            private float modifyCameraPartialTick(float original, @Share(value = "scalablePartialTick", namespace = TimeScaleLib.MODID) LocalFloatRef scalablePartialTickRef) {
                return scalablePartialTickRef.get();
            }

            @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
            private float modifyCameraBobTick(float partialTick) {
                var entity = minecraft.getCameraEntity();
                return TimeScaleHandler.isEntityAuthoritativeFrozen(entity)
                        ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                        : partialTick;
            }
        }

        @Mixin(Camera.class)
        private static class CameraMixin {
            @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 0))
            private void onPositionLerp(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
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
    }

    private static class GameEntityMixin {
        @Mixin(LevelRenderer.class)
        private static class LevelRendererMixin {
            @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;isEntityFrozen(Lnet/minecraft/world/entity/Entity;)Z"))
            private boolean wrapEntityFrozen(TickRateManager instance, Entity entity, Operation<Boolean> original) {
                return TimeScaleHandler.isEntityOriginalFrozen(entity);
            }

            @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DeltaTracker;getGameTimeDeltaPartialTick(Z)F", ordinal = 1))
            private float wrapEntityPartialTick(DeltaTracker instance, boolean runsNormally, Operation<Float> original, @Local Entity entity) {
                return TimeScaleHandler.isEntityScalableFrozen(entity)
                        ? original.call(instance, runsNormally)
                        : TimeScaleHandler.getOriginalPartialTick(true);
            }

            @Inject(method = "renderEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 0))
            private void onPositionLerp(Entity entity, double camX, double camY, double camZ, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
                var authoritativePartialTick = TimeScaleHandler.isEntityAuthoritativeFrozen(entity)
                        ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                        : partialTick;
                authoritativePartialTickRef.set(authoritativePartialTick);
            }

            @ModifyArg(method = "renderEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D"), index = 0)
            private double modifyPositionLerpDelta(double delta, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
                return authoritativePartialTickRef.get();
            }
        }

        @Mixin(Entity.class)
        private static class EntityMixin {
            @WrapWithCondition(method = "setOldPosAndRot", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;xo:D", opcode = Opcodes.PUTFIELD))
            private boolean wrapXo(Entity instance, double value, @Share("set") LocalBooleanRef setRef) {
                var set = TimeScaleHandler.clientTimer.runsTraveling((Entity) (Object) this);
                setRef.set(set);
                return set;
            }

            @WrapWithCondition(method = "setOldPosAndRot", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;yo:D", opcode = Opcodes.PUTFIELD))
            private boolean wrapYo(Entity instance, double value, @Share("set") LocalBooleanRef setRef) {
                return setRef.get();
            }

            @WrapWithCondition(method = "setOldPosAndRot", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;zo:D", opcode = Opcodes.PUTFIELD))
            private boolean wrapZo(Entity instance, double value, @Share("set") LocalBooleanRef setRef) {
                return setRef.get();
            }

            @WrapWithCondition(method = "setOldPosAndRot", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;xOld:D", opcode = Opcodes.PUTFIELD))
            private boolean wrapXOld(Entity instance, double value, @Share("set") LocalBooleanRef setRef) {
                return setRef.get();
            }

            @WrapWithCondition(method = "setOldPosAndRot", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;yOld:D", opcode = Opcodes.PUTFIELD))
            private boolean wrapYOld(Entity instance, double value, @Share("set") LocalBooleanRef setRef) {
                return setRef.get();
            }

            @WrapWithCondition(method = "setOldPosAndRot", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;zOld:D", opcode = Opcodes.PUTFIELD))
            private boolean wrapZOld(Entity instance, double value, @Share("set") LocalBooleanRef setRef) {
                return setRef.get();
            }
        }

        @Mixin(LivingEntityRenderer.class)
        private static class LivingEntityRendererMixin {
            @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;speed(F)F"))
            private float modifySpeedPartialTick(float partialTick, @Local(argsOnly = true) LivingEntity entity, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
                var authoritativePartialTick = TimeScaleHandler.isEntityAuthoritativeFrozen(entity)
                        ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                        : partialTick;
                authoritativePartialTickRef.set(authoritativePartialTick);
                return authoritativePartialTick;
            }

            @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;position(F)F"))
            private float modifyPositionPartialTick(float partialTick, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
                return authoritativePartialTickRef.get();
            }
        }

        @Mixin(EntityRenderDispatcher.class)
        private static class EntityRenderDispatcherMixin {
            @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V"), index = 4)
            private float modifyShadowPartialTick(float partialTick, @Local(argsOnly = true) Entity entity) {
                return TimeScaleHandler.isEntityAuthoritativeFrozen(entity)
                        ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                        : partialTick;
            }
        }
    }
}
