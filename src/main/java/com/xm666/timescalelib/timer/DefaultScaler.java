package com.xm666.timescalelib.timer;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class DefaultScaler implements Scaler {
    private final float scale;
    private final int end;
    private final int transition;
    private final Entity target;

    public DefaultScaler(float scale, int duration, int transition, Entity target, int tickCount) {
        this.scale = Scaler.sanitizeScale(scale);
        this.end = Scaler.sanitizeEnd(duration, tickCount);
        this.transition = Scaler.sanitizeTransition(transition, duration);
        this.target = target;
    }

    @Override
    public float getScale(int tickCount) {
        if (end == -1) return scale;

        var remainingTicks = end - tickCount;
        var delta = MathLib.clampedInverseLerp(remainingTicks, transition, 0);
        var smoothedDelta = MathLib.smoothstep(delta);
        return Mth.lerp(smoothedDelta, scale, 1.0F);
    }

    @Override
    public boolean isEnd(int tickCount) {
        return end != -1 && tickCount >= end;
    }

    @Override
    public boolean scalesTravelling(Entity entity) {
        return entity == target;
    }
}
