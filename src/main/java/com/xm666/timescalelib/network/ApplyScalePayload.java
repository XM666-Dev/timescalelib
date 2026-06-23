package com.xm666.timescalelib.network;

import com.xm666.timescalelib.TimeScaleLib;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ApplyScalePayload(
        float scale,
        int duration,
        int transition,
        int target
) implements CustomPacketPayload {
    public static final Type<ApplyScalePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TimeScaleLib.MODID, "apply_scale")
    );
    public static final StreamCodec<ByteBuf, ApplyScalePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            ApplyScalePayload::scale,
            ByteBufCodecs.VAR_INT,
            ApplyScalePayload::duration,
            ByteBufCodecs.VAR_INT,
            ApplyScalePayload::transition,
            ByteBufCodecs.VAR_INT,
            ApplyScalePayload::target,
            ApplyScalePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
