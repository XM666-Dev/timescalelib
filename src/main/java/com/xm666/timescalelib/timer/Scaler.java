package com.xm666.timescalelib.timer;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class Scaler {
    private final float scale;
    private final int end;
    private final int transition;
    private final Entity target;

    public Scaler(float scale, int end, int transition, Entity target) {
        this.scale = scale;
        this.end = end;
        this.transition = transition;
        this.target = target;
    }

    public boolean isEnd(int tickCount) {
        return end != -1 && tickCount >= end;
    }

    public float getScale(int tickCount) {
        if (end == -1) return scale;

        var remainingTicks = end - tickCount;
        var delta = MathLib.clampedInverseLerp(remainingTicks, transition, 0);
        var smoothedDelta = MathLib.smoothstep(delta);
        return Mth.lerp(smoothedDelta, scale, 1.0F);
    }

    public boolean scalesTravelling(Entity entity) {
        return entity == target;
    }
}
