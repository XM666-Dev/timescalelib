package com.xm666.timescalelib.mixin.timescalelib;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import com.xm666.timescalelib.handler.WrapHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

public class TickMixin {
    @Mixin(TickRateManager.class)
    private static class TickRateManagerMixin {
        @ModifyReturnValue(method = "runsNormally", at = @At("RETURN"))
        private boolean modifyRunsNormally(boolean original) {
            var timer = TimeScaleHandler.getTimer(!((Object) this instanceof ServerTickRateManager));
            return original && (!TimeScaleHandler.scaleRunNormally || timer.runsTicking()) && !TimeScaleHandler.disableRunNormally;
        }
    }

    @Mixin(LivingEntity.class)
    private static class LivingEntityMixin {
        @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V"))
        private boolean wrapTravel(LivingEntity instance, Vec3 travelVector) {
            var timer = TimeScaleHandler.getTimer(instance.level().isClientSide());
            return timer.runsTravelling(instance);
        }
    }

    @Mixin(ClientLevel.class)
    private static class ClientLevelMixin {
        @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;runsNormally()Z"))
        private boolean wrapClientRunsNormally(TickRateManager instance, Operation<Boolean> original) {
            return WrapHandler.callWithScale(original, instance);
        }

        @WrapOperation(method = "lambda$tickEntities$7", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;isEntityFrozen(Lnet/minecraft/world/entity/Entity;)Z"))
        private boolean wrapClientEntityFrozen(TickRateManager instance, Entity entity, Operation<Boolean> original) {
            return WrapHandler.callWithScale(original, instance, entity);
        }
    }

    @Mixin(ServerLevel.class)
    private static class ServerLevelMixin {
        @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;runsNormally()Z"))
        private boolean wrapServerRunsNormally(TickRateManager instance, Operation<Boolean> original) {
            return WrapHandler.callWithScale(original, instance);
        }

        @WrapOperation(method = "lambda$tick$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;isEntityFrozen(Lnet/minecraft/world/entity/Entity;)Z"))
        private boolean wrapServerEntityFrozen(TickRateManager instance, Entity entity, Operation<Boolean> original) {
            return WrapHandler.callWithScale(original, instance, entity);
        }
    }

    @Mixin(ServerFunctionManager.class)
    private static class ServerFunctionManagerMixin {
        @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickRateManager;runsNormally()Z"))
        private boolean wrapFunctionRunsNormally(ServerTickRateManager instance, Operation<Boolean> original) {
            return WrapHandler.callWithScale(original, instance);
        }
    }

    @Mixin(ServerChunkCache.class)
    private static class ServerChunkCacheMixin {
        @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;runsNormally()Z"))
        private boolean wrapCacheRunsNormally(TickRateManager instance, Operation<Boolean> original) {
            return WrapHandler.callWithScale(original, instance);
        }

        @WrapOperation(method = "tickChunks()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;runsNormally()Z"))
        private boolean wrapChunkRunsNormally(TickRateManager instance, Operation<Boolean> original) {
            return WrapHandler.callWithScale(original, instance);
        }
    }

    @Mixin(ServerPlayer.class)
    private static class ServerPlayerMixin {
        @WrapOperation(method = "pushEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;runsNormally()Z"))
        private boolean wrapPlayerRunsNormally(TickRateManager instance, Operation<Boolean> original) {
            return WrapHandler.callWithScale(original, instance);
        }
    }

    @Mixin(Level.class)
    private static class LevelMixin {
        @WrapOperation(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;runsNormally()Z"))
        private boolean wrapBlockEntityRunsNormally(TickRateManager instance, Operation<Boolean> original) {
            return WrapHandler.callWithScale(original, instance);
        }
    }

    @Mixin(Minecraft.class)
    private static class MinecraftMixin {
        @WrapOperation(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLevelRunningNormally()Z", ordinal = 0))
        private boolean wrapTextureRunsNormally(Minecraft instance, Operation<Boolean> original) {
            return WrapHandler.callWithScale(original, instance);
        }

        @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLevelRunningNormally()Z"))
        private boolean wrapVisualRunsNormally(Minecraft instance, Operation<Boolean> original) {
            return WrapHandler.callWithScale(original, instance);
        }
    }

    @Mixin(GameRenderer.class)
    private static class GameRendererMixin {
        @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;runsNormally()Z"))
        private boolean wrapGameRunsNormally(TickRateManager instance, Operation<Boolean> original) {
            return WrapHandler.callWithScale(original, instance);
        }
    }

    @Mixin(LevelRenderer.class)
    private static class LevelRendererMixin {
        @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;runsNormally()Z"))
        private boolean wrapLevelRunsNormally(TickRateManager instance, Operation<Boolean> original) {
            return WrapHandler.callWithScale(original, instance);
        }
    }
}
