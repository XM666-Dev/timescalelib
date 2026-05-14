package com.xm666.timescalelib.handler;

import com.xm666.timescalelib.TimeScaleLib;
import com.xm666.timescalelib.network.ApplyScalePayload;
import com.xm666.timescalelib.network.RemoveScalePayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = TimeScaleLib.MODID)
public class PayloadHandler {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(
                ApplyScalePayload.TYPE,
                ApplyScalePayload.STREAM_CODEC,
                TimeScaleHandler::handlePayload
        );
        registrar.playToClient(
                RemoveScalePayload.TYPE,
                RemoveScalePayload.STREAM_CODEC,
                TimeScaleHandler::handlePayload
        );
    }
}
