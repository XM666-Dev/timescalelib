package com.xm666.timescalelib.timer;

import net.minecraft.util.Mth;

public class Scaler {
    private final float scale;
    private final int end;
    private final int transition;

    public Scaler(float scale, int end, int transition) {
        this.scale = scale;
        this.end = end;
        this.transition = transition;
    }

    public boolean isEnd(int tickCount) {
        return tickCount >= end;
    }

    public float getScale(int tickCount) {
        var remainingTicks = getRemainingTicks(tickCount);
        var delta = MathLib.clampedInverseLerp(remainingTicks, transition, 0);
        delta = MathLib.smoothstep(delta);
        return Mth.lerp(delta, scale, 1.0F);
    }

    public int getRemainingTicks(int tickCount) {
        return end - tickCount;
    }

    public boolean scalesTravelling(net.minecraft.world.entity.Entity entity) {
        return false;
    }

    public static class Entity extends Scaler {
        private final net.minecraft.world.entity.Entity target;

        public Entity(float scale, int end, int transition, net.minecraft.world.entity.Entity target) {
            super(scale, end, transition);
            this.target = target;
        }

        @Override
        public boolean scalesTravelling(net.minecraft.world.entity.Entity entity) {
            return entity == target;
        }
    }
}
