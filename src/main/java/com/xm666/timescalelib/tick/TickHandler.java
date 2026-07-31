package com.xm666.timescalelib.tick;

import com.xm666.timescalelib.TimeScaleLib;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TimeScaleLib.MODID)
public class TickHandler {
    private static TickRateManager clientTickRateManager;
    private static ServerTickRateManager serverTickRateManager;
    private static DeltaTracker.Timer timer;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        TickHandler.clientTickRateManager = new TickRateManager();
        TickHandler.timer = new DeltaTracker.Timer(20.0F, 0L, TickHandler::getTickTargetMillis);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        TickHandler.serverTickRateManager = new ServerTickRateManager();
    }

    public static TickRateManager getTickRateManager(Level level) {
        return level.isClientSide() ? clientTickRateManager : serverTickRateManager;
    }

    public static DeltaTracker.Timer getTimer() {
        return timer;
    }

    private static float getTickTargetMillis(float defaultValue) {
        var mc = Minecraft.getInstance();
        return mc.level != null && clientTickRateManager.runsNormally()
                ? Math.max(defaultValue, clientTickRateManager.millisecondsPerTick())
                : defaultValue;
    }
}
