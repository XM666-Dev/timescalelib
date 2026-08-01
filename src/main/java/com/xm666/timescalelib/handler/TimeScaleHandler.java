package com.xm666.timescalelib.handler;

import com.xm666.timescalelib.TimeScaleLib;
import com.xm666.timescalelib.network.ApplyScalePayload;
import com.xm666.timescalelib.network.RemoveScalePayload;
import com.xm666.timescalelib.tickrate.TickRateHandler;
import com.xm666.timescalelib.timer.ScalableTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = TimeScaleLib.MODID)
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
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || clientTimer == null) return;

        clientTimer.tick();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || serverTimer == null) return;

        serverTimer.tick();
    }

    public static void handlePayload(final ApplyScalePayload payload, final Supplier<NetworkEvent.Context> context) {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        clientTimer.addScaler(payload.scale(), payload.duration(), payload.transition(), level.getEntity(payload.target()));
    }

    public static void handlePayload(final RemoveScalePayload payload, final Supplier<NetworkEvent.Context> context) {
        clientTimer.clearScaler();
    }

    public static void applyScale(float scale, int duration) {
        applyScale(null, scale, duration, ScalableTimer.getDefaultTransition());
    }

    public static void applyScale(float scale, int duration, int transition) {
        applyScale(null, scale, duration, transition);
    }

    public static void applyScale(Entity target, float scale, int duration) {
        applyScale(target, scale, duration, ScalableTimer.getDefaultTransition());
    }

    public static void applyScale(Entity target, float scale, int duration, int transition) {
        var targetId = target != null ? target.getId() : 0;
        serverTimer.addScaler(scale, duration, transition, target);
        PayloadHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new ApplyScalePayload(scale, duration, transition, targetId));
    }

    public static void removeScale() {
        serverTimer.clearScaler();
        PayloadHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new RemoveScalePayload());
    }

    public static ScalableTimer getTimer(boolean isClientSide) {
        return isClientSide ? clientTimer : serverTimer;
    }

    public static boolean isEntityOriginalFrozen(Entity entity) {
        var mc = Minecraft.getInstance();
        var tickRateManager = TickRateHandler.getTickRateManager(mc.level);

        return tickRateManager.isEntityFrozen(entity);
    }

    public static boolean isEntityScalableFrozen(Entity entity) {
        var mc = Minecraft.getInstance();
        var tickRateManager = TickRateHandler.getTickRateManager(mc.level);

        TimeScaleHandler.disableRunNormally = true;
        var frozen = tickRateManager.isEntityFrozen(entity);
        TimeScaleHandler.disableRunNormally = false;

        return frozen;
    }

    public static boolean isEntityAuthoritativeFrozen(Entity entity) {
        var mc = Minecraft.getInstance();
        var tickRateManager = TickRateHandler.getTickRateManager(mc.level);

        TimeScaleHandler.disableRunNormally = true;
        var frozen = tickRateManager.isEntityFrozen(entity) || TimeScaleHandler.clientTimer.scalesTravelling(entity);
        TimeScaleHandler.disableRunNormally = false;

        return frozen;
    }

    public static float getScalablePartialTick(boolean runsNormally) {
        var mc = Minecraft.getInstance();
        var timer = TickRateHandler.getTimer(mc);

        TimeScaleHandler.scalePartialTick = true;
        var partialTick = timer.getGameTimeDeltaPartialTick(runsNormally);
        TimeScaleHandler.scalePartialTick = false;

        return partialTick;
    }
}
