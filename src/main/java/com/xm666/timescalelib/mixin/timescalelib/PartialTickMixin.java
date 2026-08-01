package com.xm666.timescalelib.mixin.timescalelib;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xm666.timescalelib.TimeScaleLib;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import com.xm666.timescalelib.tickrate.DeltaTracker;
import com.xm666.timescalelib.tickrate.TickRateHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
    @Mixin(DeltaTracker.Timer.class)
    private static class TimerMixin {
    }

    private static class GameBobMixin {
        @Mixin(Entity.class)
        private static class EntityMixin {
            @WrapWithCondition(method = "baseTick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;walkDistO:F", opcode = Opcodes.PUTFIELD))
            private boolean wrapWalkDistO(Entity instance, float value) {
                return TimeScaleHandler.clientTimer.runsTravelling(instance);
            }
        }

        @Mixin(Player.class)
        private static class PlayerMixin {
            @WrapWithCondition(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;oBob:F", opcode = Opcodes.PUTFIELD))
            private boolean wrapOBob(Player instance, float value, @Share("set") LocalBooleanRef setRef) {
                var set = TimeScaleHandler.clientTimer.runsTravelling(instance);
                setRef.set(set);
                return set;
            }

            @WrapWithCondition(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;bob:F", opcode = Opcodes.PUTFIELD))
            private boolean wrapBob(Player instance, float value, @Share("set") LocalBooleanRef setRef) {
                return setRef.get();
            }
        }
    }

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

        @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V"))
        private float modifyHandPartialTick(float original, @Share(value = "scalablePartialTick", namespace = TimeScaleLib.MODID) LocalFloatRef scalablePartialTickRef) {
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
            private void onCameraPositionLerp(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
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
        @Mixin(GameRenderer.class)
        private static class GameRendererMixin {
            @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V"))
            private float wrapLevelPartialTick(float partialTick) {
                return TimeScaleHandler.getScalablePartialTick(true);
            }
        }
    }

    private static class GameEntityMixin {
        @Mixin(LevelRenderer.class)
        private static class LevelRendererMixin {
            @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
            private float wrapEntityPartialTick(float partialTick, @Local Entity entity) {
                return TimeScaleHandler.isEntityScalableFrozen(entity)
                        ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                        : TickRateHandler.timer.getGameTimeDeltaPartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity));
            }

            @Inject(method = "renderEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 0))
            private void onEntityPositionLerp(Entity entity, double camX, double camY, double camZ, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
                var authoritativePartialTick = TimeScaleHandler.isEntityAuthoritativeFrozen(entity)
                        ? TimeScaleHandler.getScalablePartialTick(!TimeScaleHandler.isEntityOriginalFrozen(entity))
                        : partialTick;
                authoritativePartialTickRef.set(authoritativePartialTick);
            }

            @ModifyArg(method = "renderEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(DDD)D"), index = 0)
            private double modifyEntityPositionLerpDelta(double delta, @Share("authoritativePartialTick") LocalFloatRef authoritativePartialTickRef) {
                return authoritativePartialTickRef.get();
            }
        }

        @Mixin(Entity.class)
        private static class EntityMixin {
            @WrapWithCondition(method = "setOldPosAndRot", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;xo:D", opcode = Opcodes.PUTFIELD))
            private boolean wrapXO(Entity instance, double value, @Share("set") LocalBooleanRef setRef) {
                var set = TimeScaleHandler.clientTimer.runsTravelling((Entity) (Object) this);
                setRef.set(set);
                return set;
            }

            @WrapWithCondition(method = "setOldPosAndRot", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;yo:D", opcode = Opcodes.PUTFIELD))
            private boolean wrapYO(Entity instance, double value, @Share("set") LocalBooleanRef setRef) {
                return setRef.get();
            }

            @WrapWithCondition(method = "setOldPosAndRot", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;zo:D", opcode = Opcodes.PUTFIELD))
            private boolean wrapZO(Entity instance, double value, @Share("set") LocalBooleanRef setRef) {
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
