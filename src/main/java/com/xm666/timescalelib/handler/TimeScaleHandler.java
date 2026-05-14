package com.xm666.timescalelib.handler;

import com.xm666.timescalelib.TimeScaleLib;
import com.xm666.timescalelib.network.ApplyScalePayload;
import com.xm666.timescalelib.network.RemoveScalePayload;
import com.xm666.timescalelib.timer.ScalableTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = TimeScaleLib.MODID)
public class TimeScaleHandler {
    public static ScalableTimer clientTimer;
    public static ScalableTimer serverTimer;
    public static boolean scaleRunNormally = true;
    public static boolean disableRunNormally = false;
    public static boolean scalePartialTick = true;
    public static float deltaTickRunning;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        clientTimer = new ScalableTimer.Client();
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        serverTimer = new ScalableTimer.Server(event.getServer());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        clientTimer.tick();
        deltaTickRunning = clientTimer.runsTicking() ? 0.0F : deltaTickRunning + clientTimer.getScale();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        serverTimer.tick();
    }

    public static void handlePayload(final ApplyScalePayload payload, final IPayloadContext context) {
        var level = context.player().level();
        var target = level.getEntity(payload.targetEntity());
        if (target != null) {
            clientTimer.addScaler(payload.scale(), payload.duration(), payload.transition(), target);
            return;
        }
        clientTimer.addScaler(payload.scale(), payload.duration(), payload.transition());
    }

    public static void handlePayload(final RemoveScalePayload payload, final IPayloadContext context) {
        clientTimer.clearScaler();
    }

    public static void applyScale(float scale, int scaleTicks) {
        applyScale(scale, scaleTicks, serverTimer.getDefaultTransition());
    }

    public static void applyScale(float scale, int scaleTicks, int transition) {
        serverTimer.addScaler(scale, scaleTicks, transition);
        PacketDistributor.sendToAllPlayers(new ApplyScalePayload(scale, scaleTicks, transition, 0));
    }

    public static void applyScale(Entity entity, float scale, int scaleTicks) {
        applyScale(entity, scale, scaleTicks, serverTimer.getDefaultTransition());
    }

    public static void applyScale(Entity entity, float scale, int scaleTicks, int transition) {
        serverTimer.addScaler(scale, scaleTicks, transition, entity);
        PacketDistributor.sendToAllPlayers(new ApplyScalePayload(scale, scaleTicks, transition, entity.getId()));
    }

    public static void removeScale() {
        serverTimer.clearScaler();
        PacketDistributor.sendToAllPlayers(new RemoveScalePayload());
    }

    public static ScalableTimer getTimer(boolean isClientSide) {
        return isClientSide ? clientTimer : serverTimer;
    }

    public static float getScale(boolean isClientSide) {
        return getScale(getTimer(isClientSide));
    }

    private static float getScale(ScalableTimer timer) {
        return timer != null ? timer.getScale() : 1.0F;
    }

    @SuppressWarnings("DataFlowIssue")
    public static boolean isEntityOriginalFrozen(Entity entity) {
        var mc = Minecraft.getInstance();

        TimeScaleHandler.scaleRunNormally = false;
        var frozen = mc.level.tickRateManager().isEntityFrozen(entity);
        TimeScaleHandler.scaleRunNormally = true;

        return frozen;
    }

    @SuppressWarnings("DataFlowIssue")
    public static boolean isEntityScalableFrozen(Entity entity) {
        var mc = Minecraft.getInstance();

        TimeScaleHandler.disableRunNormally = true;
        var frozen = mc.level.tickRateManager().isEntityFrozen(entity);
        TimeScaleHandler.disableRunNormally = false;

        return frozen;
    }

    @SuppressWarnings("DataFlowIssue")
    public static boolean isEntityAuthoritativeFrozen(Entity entity) {
        var mc = Minecraft.getInstance();

        TimeScaleHandler.disableRunNormally = true;
        var frozen = mc.level.tickRateManager().isEntityFrozen(entity) || TimeScaleHandler.clientTimer.scalesTravelling(entity);
        TimeScaleHandler.disableRunNormally = false;

        return frozen;
    }

    public static float getOriginalPartialTick(boolean runsNormally) {
        var mc = Minecraft.getInstance();

        TimeScaleHandler.scalePartialTick = false;
        var partialTick = mc.getTimer().getGameTimeDeltaPartialTick(runsNormally);
        TimeScaleHandler.scalePartialTick = true;

        return partialTick;
    }

    public static float getScalablePartialTick(boolean runsNormally) {
        var mc = Minecraft.getInstance();

        return mc.getTimer().getGameTimeDeltaPartialTick(runsNormally);
    }
}
