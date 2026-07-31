package com.xm666.timescalelib.network;

import com.xm666.timescalelib.handler.TimeScaleHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ApplyScalePayload(
        float scale,
        int duration,
        int transition,
        int target
) {
    public static void write(ApplyScalePayload msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.scale);
        buf.writeInt(msg.duration);
        buf.writeInt(msg.transition);
        buf.writeInt(msg.target);
    }

    public static ApplyScalePayload read(FriendlyByteBuf buf) {
        return new ApplyScalePayload(
                buf.readFloat(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(ApplyScalePayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TimeScaleHandler.handlePayload(msg, ctx))
        );
        ctx.get().setPacketHandled(true);
    }
}
