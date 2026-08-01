package com.xm666.timescalelib;

import com.mojang.logging.LogUtils;
import com.xm666.timescalelib.handler.PayloadHandler;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import com.xm666.timescalelib.tickrate.TickRateHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TimeScaleLib.MODID)
public class TimeScaleLib {
    public static final String MODID = "timescalelib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TimeScaleLib(FMLJavaModLoadingContext context) {
        this(context.getModEventBus());
    }

    public TimeScaleLib(IEventBus eventBus) {
        eventBus.addListener(TimeScaleHandler::onClientSetup);
        eventBus.addListener(TickRateHandler::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(TimeScaleHandler::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(TickRateHandler::onServerStarting);
        PayloadHandler.init();
    }
}
