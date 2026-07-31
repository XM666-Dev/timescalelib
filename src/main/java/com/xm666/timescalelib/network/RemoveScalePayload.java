package com.xm666.timescalelib.network;

import com.xm666.timescalelib.handler.TimeScaleHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RemoveScalePayload() {
    public static void write(RemoveScalePayload msg, FriendlyByteBuf buf) {
    }

    public static RemoveScalePayload read(FriendlyByteBuf buf) {
        return new RemoveScalePayload();
    }

    public static void handle(RemoveScalePayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TimeScaleHandler.handlePayload(msg, ctx))
        );
        ctx.get().setPacketHandled(true);
    }
}
