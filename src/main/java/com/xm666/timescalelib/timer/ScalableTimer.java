package com.xm666.timescalelib.timer;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;

public abstract class ScalableTimer {
    private final ArrayList<Scaler> scalers = new ArrayList<>();
    private float scale;
    private boolean runTick;
    private float deltaTickResidual;

    private float getDefaultScale() {
        return 1.0F;
    }

    public int getDefaultTransition() {
        return 20;
    }

    private float calculateScale() {
        var tickCount = getTickCount();
        scalers.removeIf((scaler -> scaler.isEnd(tickCount)));
        return getDefaultScale() * scalers.stream()
                .min(Comparator.comparing((scaler -> scaler.getScale(tickCount))))
                .map(scaler -> scaler.getScale(tickCount))
                .orElse(1.0F);
    }

    public void tick() {
        var nextScale = calculateScale();
        var nextDeltaTickResidual = deltaTickResidual + nextScale;
        scale = nextScale;
        runTick = nextDeltaTickResidual >= 1.0F;
        deltaTickResidual = Mth.frac(nextDeltaTickResidual);
    }

    public void addScaler(float scale, int duration, int transition, Entity target) {
        scalers.add(new Scaler(scale, getScalerEnd(duration), getScalerTransition(duration, transition), target));
    }

    private int getScalerEnd(int duration) {
        return duration != -1 ? getTickCount() + duration + 1 : -1;
    }

    private int getScalerTransition(int duration, int transition) {
        return transition != -1 ? Math.min(transition, duration) : duration;
    }

    public void clearScaler() {
        scalers.clear();
    }

    public boolean runsTicking() {
        return runTick;
    }

    public float getScale() {
        return scale;
    }

    public boolean runsTraveling(Entity entity) {
        return !scalesTravelling(entity) || runTick;
    }

    public boolean scalesTravelling(Entity entity) {
        return scalers.stream().anyMatch(scaler -> scaler.scalesTravelling(entity));
    }

    public abstract int getTickCount();

    public static class Client extends ScalableTimer {
        private int tickCount;
        private float deltaTickBase;

        @Override
        public void tick() {
            var mc = Minecraft.getInstance();
            if (mc.level == null || mc.isPaused()) return;

            ++tickCount;
            super.tick();
            deltaTickBase = runsTicking() ? 0.0F : deltaTickBase + getScale();
        }

        @Override
        public int getTickCount() {
            return tickCount;
        }

        public float getDeltaTickBase() {
            return deltaTickBase;
        }
    }

    public static class Server extends ScalableTimer {
        private final MinecraftServer server;

        public Server(MinecraftServer server) {
            this.server = server;
        }

        @Override
        public int getTickCount() {
            return server.getTickCount();
        }
    }
}
