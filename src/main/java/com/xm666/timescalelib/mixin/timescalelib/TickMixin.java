package com.xm666.timescalelib.mixin.timescalelib;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import com.xm666.timescalelib.tickrate.TickRateHandler;
import com.xm666.timescalelib.tickrate.TickRateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTicks;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

public class TickMixin {
    @Mixin(TickRateManager.class)
    private static class TickRateManagerMixin {
    }

    @Mixin(LivingEntity.class)
    private static class LivingEntityMixin {
        @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V"))
        private boolean wrapTravel(LivingEntity instance, Vec3 travelVector) {
            var timer = TimeScaleHandler.getTimer(instance.level().isClientSide());
            return timer.runsTravelling(instance);
        }

        @WrapMethod(method = "pushEntities")
        private void wrapPlayerRunsNormally(Operation<Void> original) {
            var living = (LivingEntity) (Object) this;
            var level = living.level();
            var tickRateManager = TickRateHandler.getTickRateManager(level);
            if (!tickRateManager.runsNormally()) return;

            original.call();
        }
    }

    @Mixin(ClientLevel.class)
    private static class ClientLevelMixin {
        @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;tickTime()V"))
        private boolean wrapClientRunsNormally(ClientLevel instance) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.clientTickRateManager);
        }

        @WrapMethod(method = "tickNonPassenger")
        private void wrapClientEntityFrozen(Entity entity, Operation<Void> original) {
            if (TickRateHandler.isScalableEntityFrozen(TickRateHandler.clientTickRateManager, entity)) return;

            original.call(entity);
        }
    }

    @Mixin(ServerLevel.class)
    private static class ServerLevelMixin {
        @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder;tick()V"))
        private boolean wrapBorderRunsNormally(WorldBorder instance) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.serverTickRateManager);
        }

        @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;advanceWeatherCycle()V"))
        private boolean wrapWeatherRunsNormally(ServerLevel instance) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.serverTickRateManager);
        }

        @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickTime()V"))
        private boolean wrapTimeRunsNormally(ServerLevel instance) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.serverTickRateManager);
        }

        @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ticks/LevelTicks;tick(JILjava/util/function/BiConsumer;)V"))
        private boolean wrapTicksRunsNormally(LevelTicks<?> instance, long p_193226_, int p_193227_, BiConsumer<BlockPos, ?> p_193228_) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.serverTickRateManager);
        }

        @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raids;tick()V"))
        private boolean wrapRaidsRunsNormally(Raids instance) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.serverTickRateManager);
        }

        @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;runBlockEvents()V"))
        private boolean wrapEventsRunsNormally(ServerLevel instance) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.serverTickRateManager);
        }

        @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/end/EndDragonFight;tick()V"))
        private boolean wrapFightRunsNormally(EndDragonFight instance) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.serverTickRateManager);
        }

        @WrapMethod(method = "tickNonPassenger")
        private void wrapServerEntityFrozen(Entity entity, Operation<Void> original) {
            if (TickRateHandler.isScalableEntityFrozen(TickRateHandler.serverTickRateManager, entity)) return;

            original.call(entity);
        }
    }

    @Mixin(ServerFunctionManager.class)
    private static class ServerFunctionManagerMixin {
        @WrapMethod(method = "tick")
        private void wrapFunctionRunsNormally(Operation<Void> original) {
            if (!TickRateHandler.isScalableRunsNormally(TickRateHandler.serverTickRateManager)) return;

            original.call();
        }
    }

    @Mixin(ServerChunkCache.class)
    private static class ServerChunkCacheMixin {
        @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/DistanceManager;purgeStaleTickets()V"))
        private boolean wrapCacheRunsNormally(DistanceManager instance, @Local(argsOnly = true) boolean tickChunks) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.serverTickRateManager) || !tickChunks;
        }

        @WrapWithCondition(method = "tickChunks", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerChunkCache;lastSpawnState:Lnet/minecraft/world/level/NaturalSpawner$SpawnState;", opcode = Opcodes.PUTFIELD))
        private boolean wrapChunkRunsNormally(ServerChunkCache instance, NaturalSpawner.SpawnState value) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.serverTickRateManager);
        }

        @WrapWithCondition(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;incrementInhabitedTime(J)V"))
        private boolean wrapChunkRunsNormally(LevelChunk instance, long amount) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.serverTickRateManager);
        }

        @WrapWithCondition(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;spawnForChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZZ)V"))
        private boolean wrapChunkRunsNormally(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState spawnState, boolean spawnFriendlies, boolean spawnMonsters, boolean forcedDespawn) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.serverTickRateManager);
        }

        @WrapWithCondition(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V"))
        private boolean wrapChunkRunsNormally(ServerLevel instance, LevelChunk chunk, int randomTickSpeed) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.serverTickRateManager);
        }

        @WrapWithCondition(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickCustomSpawners(ZZ)V"))
        private boolean wrapChunkRunsNormally(ServerLevel instance, boolean spawnEnemies, boolean spawnFriendlies) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.serverTickRateManager);
        }
    }

    @Mixin(ServerPlayer.class)
    private static class ServerPlayerMixin {
    }

    @Mixin(Level.class)
    private static class LevelMixin {
        @WrapMethod(method = "tickBlockEntities")
        private void wrapBlockEntityRunsNormally(Operation<Void> original) {
            var level = (Level) (Object) this;
            var tickRateManager = TickRateHandler.getTickRateManager(level);
            if (!TickRateHandler.isScalableRunsNormally(tickRateManager)) return;

            original.call();
        }
    }

    @Mixin(Minecraft.class)
    private static class MinecraftMixin {
        @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;tick()V"))
        private boolean wrapTextureRunsNormally(TextureManager instance) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.clientTickRateManager);
        }

        @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;animateTick(III)V"))
        private boolean wrapAnimateRunsNormally(ClientLevel instance, int x, int y, int z) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.clientTickRateManager);
        }

        @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;tick()V"))
        private boolean wrapParticleRunsNormally(ParticleEngine instance) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.clientTickRateManager);
        }
    }

    @Mixin(GameRenderer.class)
    private static class GameRendererMixin {
        @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;tickRain(Lnet/minecraft/client/Camera;)V"), cancellable = true)
        private void wrapGameRunsNormally(CallbackInfo ci) {
            if (TickRateHandler.isScalableRunsNormally(TickRateHandler.clientTickRateManager)) return;

            ci.cancel();
        }
    }

    @Mixin(LevelRenderer.class)
    private static class LevelRendererMixin {
        @WrapWithCondition(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;ticks:I", opcode = Opcodes.PUTFIELD))
        private boolean wrapLevelRunsNormally(LevelRenderer instance, int value) {
            return TickRateHandler.isScalableRunsNormally(TickRateHandler.clientTickRateManager);
        }
    }
}
