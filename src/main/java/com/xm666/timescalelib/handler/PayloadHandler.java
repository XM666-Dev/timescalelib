package com.xm666.timescalelib.handler;

import com.xm666.timescalelib.TimeScaleLib;
import com.xm666.timescalelib.network.ApplyScalePayload;
import com.xm666.timescalelib.network.RemoveScalePayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = TimeScaleLib.MODID)
public class PayloadHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TimeScaleLib.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init() {
        var index = 0;
        INSTANCE.registerMessage(index++, ApplyScalePayload.class, ApplyScalePayload::write, ApplyScalePayload::read, ApplyScalePayload::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(index++, RemoveScalePayload.class, RemoveScalePayload::write, RemoveScalePayload::read, RemoveScalePayload::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
