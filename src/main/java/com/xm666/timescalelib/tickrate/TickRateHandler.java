package com.xm666.timescalelib.tickrate;

import com.xm666.timescalelib.TimeScaleLib;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TimeScaleLib.MODID)
public class TickRateHandler {
    public static TickRateManager clientTickRateManager;
    public static ServerTickRateManager serverTickRateManager;
    public static DeltaTracker.Timer timer;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        TickRateHandler.clientTickRateManager = new TickRateManager();
        TickRateHandler.timer = new DeltaTracker.Timer(20.0F, 0L, TickRateHandler::getTickTargetMillis);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        TickRateHandler.serverTickRateManager = new ServerTickRateManager();
    }

    public static TickRateManager getTickRateManager(Level level) {
        return level.isClientSide() ? clientTickRateManager : serverTickRateManager;
    }

    public static DeltaTracker.Timer getTimer(Minecraft minecraft) {
        return timer;
    }

    public static boolean isScalableRunsNormally(TickRateManager tickRateManager) {
        TimeScaleHandler.scaleRunNormally = true;
        var runsNormally = tickRateManager.runsNormally();
        TimeScaleHandler.scaleRunNormally = false;

        return runsNormally;
    }

    public static boolean isScalableEntityFrozen(TickRateManager tickRateManager, Entity entity) {
        TimeScaleHandler.scaleRunNormally = true;
        var frozen = tickRateManager.isEntityFrozen(entity);
        TimeScaleHandler.scaleRunNormally = false;

        return frozen;
    }

    public static boolean isModifiableRunsNormally(TickRateManager tickRateManager) {
        return modifyRunsNormally(tickRateManager, true);
    }

    public static float getModifiablePartialTick(DeltaTracker.Timer timer, boolean runsNormally) {
        if (!runsNormally && timer.frozen) {
            return 1.0F;
        } else {
            return modifyPartialTick(Minecraft.getInstance().getPartialTick());
        }
    }

    public static int countPlayerPassengers(Entity entity) {
        return (int) entity.getIndirectPassengersStream().filter(passenger -> passenger instanceof Player).count();
    }

    private static float getTickTargetMillis(float defaultValue) {
        var mc = Minecraft.getInstance();
        return mc.level != null && clientTickRateManager.runsNormally()
                ? Math.max(defaultValue, clientTickRateManager.millisecondsPerTick())
                : defaultValue;
    }

    private static boolean modifyRunsNormally(TickRateManager tickRateManager, boolean original) {
        var timer = TimeScaleHandler.getTimer(!(tickRateManager instanceof ServerTickRateManager));
        return original && (!TimeScaleHandler.scaleRunNormally || timer.runsTicking()) && !TimeScaleHandler.disableRunNormally;
    }

    private static float modifyPartialTick(float original) {
        if (!TimeScaleHandler.scalePartialTick || TimeScaleHandler.clientTimer == null) return original;

        var sequentialTick = TimeScaleHandler.clientTimer.getDeltaTickSequential();
        var scale = Math.min(TimeScaleHandler.clientTimer.getScale(), 1.0F - sequentialTick);
        return sequentialTick + original * scale;
    }
}
