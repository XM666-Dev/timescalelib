package com.xm666.timescalelib.network;

import com.xm666.timescalelib.TimeScaleLib;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RemoveScalePayload() implements CustomPacketPayload {
    public static final Type<RemoveScalePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimeScaleLib.MODID, "remove_scale"));
    public static final StreamCodec<ByteBuf, RemoveScalePayload> STREAM_CODEC = StreamCodec.unit(new RemoveScalePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
