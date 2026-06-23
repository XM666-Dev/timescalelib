package com.xm666.timescalelib.timer;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public interface Scaler {
    static float sanitizeScale(float scale) {
        return Mth.clamp(scale, 0.0F, 1.0F);
    }

    static int sanitizeEnd(int duration, int tickCount) {
        return duration >= 0 ? tickCount + duration + 1 : -1;
    }

    static int sanitizeTransition(int transition, int duration) {
        return transition >= 0 ? Math.min(transition, duration) : duration;
    }

    float getScale(int tickCount);

    boolean isEnd(int tickCount);

    boolean scalesTravelling(Entity entity);
}
