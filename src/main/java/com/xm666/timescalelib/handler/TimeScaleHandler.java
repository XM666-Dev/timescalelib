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
    public static ScalableTimer.Client clientTimer;
    public static ScalableTimer.Server serverTimer;
    public static boolean scaleRunNormally = false;
    public static boolean disableRunNormally = false;
    public static boolean scalePartialTick = false;

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
        if (clientTimer == null) return;

        clientTimer.tick();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        if (serverTimer == null) return;

        serverTimer.tick();
    }

    public static void handlePayload(final ApplyScalePayload payload, final IPayloadContext context) {
        var level = context.player().level();
        clientTimer.addScaler(payload.scale(), payload.duration(), payload.transition(), level.getEntity(payload.targetEntity()));
    }

    public static void handlePayload(final RemoveScalePayload payload, final IPayloadContext context) {
        clientTimer.clearScaler();
    }

    public static void applyScale(float scale, int scaleTicks) {
        applyScale(null, scale, scaleTicks, serverTimer.getDefaultTransition());
    }

    public static void applyScale(float scale, int scaleTicks, int transition) {
        applyScale(null, scale, scaleTicks, transition);
    }

    public static void applyScale(Entity entity, float scale, int scaleTicks) {
        applyScale(entity, scale, scaleTicks, serverTimer.getDefaultTransition());
    }

    public static void applyScale(Entity entity, float scale, int scaleTicks, int transition) {
        serverTimer.addScaler(scale, scaleTicks, transition, entity);
        PacketDistributor.sendToAllPlayers(new ApplyScalePayload(scale, scaleTicks, transition, entity != null ? entity.getId() : 0));
    }

    public static void removeScale() {
        serverTimer.clearScaler();
        PacketDistributor.sendToAllPlayers(new RemoveScalePayload());
    }

    public static ScalableTimer getTimer(boolean isClientSide) {
        return isClientSide ? clientTimer : serverTimer;
    }

    public static boolean isEntityOriginalFrozen(Entity entity) {
        var mc = Minecraft.getInstance();
        var tickRateManager = mc.level.tickRateManager();

        return tickRateManager.isEntityFrozen(entity);
    }

    public static boolean isEntityScalableFrozen(Entity entity) {
        var mc = Minecraft.getInstance();
        var tickRateManager = mc.level.tickRateManager();

        TimeScaleHandler.disableRunNormally = true;
        var frozen = tickRateManager.isEntityFrozen(entity);
        TimeScaleHandler.disableRunNormally = false;

        return frozen;
    }

    public static boolean isEntityAuthoritativeFrozen(Entity entity) {
        var mc = Minecraft.getInstance();
        var tickRateManager = mc.level.tickRateManager();

        TimeScaleHandler.disableRunNormally = true;
        var frozen = tickRateManager.isEntityFrozen(entity) || TimeScaleHandler.clientTimer.scalesTravelling(entity);
        TimeScaleHandler.disableRunNormally = false;

        return frozen;
    }

    public static float getScalablePartialTick(boolean runsNormally) {
        var mc = Minecraft.getInstance();
        var timer = mc.getTimer();

        TimeScaleHandler.scalePartialTick = true;
        var partialTick = timer.getGameTimeDeltaPartialTick(runsNormally);
        TimeScaleHandler.scalePartialTick = false;

        return partialTick;
    }
}
