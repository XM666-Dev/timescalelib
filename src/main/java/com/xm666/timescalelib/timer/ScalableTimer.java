package com.xm666.timescalelib.timer;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;

public abstract class ScalableTimer {
    private final ArrayList<Scaler> scalers = new ArrayList<>();
    private float scale;
    private boolean runTick;
    private float deltaTickResidual;

    public static float getDefaultScale() {
        return 1.0F;
    }

    public static int getDefaultTransition() {
        return 20;
    }

    public void tick() {
        var nextScale = nextScale();
        var nextDeltaTickResidual = deltaTickResidual + nextScale;
        scale = nextScale;
        runTick = nextDeltaTickResidual >= 1.0F;
        deltaTickResidual = Mth.frac(nextDeltaTickResidual);
    }

    private float nextScale() {
        var tickCount = getTickCount();
        scalers.removeIf((scaler -> scaler.isEnd(tickCount)));
        return getDefaultScale() * scalers.stream()
                .map(scaler -> scaler.getScale(tickCount))
                .min(Float::compareTo)
                .orElse(1.0F);
    }

    public void addScaler(float scale, int duration, int transition, Entity target) {
        addScaler(new DefaultScaler(scale, duration, transition, target, getTickCount()));
    }

    public void addScaler(Scaler scaler) {
        scalers.add(scaler);
    }

    public void removeScaler(Scaler scaler) {
        scalers.remove(scaler);
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

    public boolean runsTravelling(Entity entity) {
        return !scalesTravelling(entity) || runTick;
    }

    public boolean scalesTravelling(Entity entity) {
        return scalers.stream().anyMatch(scaler -> scaler.scalesTravelling(entity));
    }

    public abstract int getTickCount();

    public static class Client extends ScalableTimer {
        private int tickCount;
        private float deltaTickSequential;

        @Override
        public void tick() {
            var mc = Minecraft.getInstance();
            if (mc.level == null || mc.isPaused()) return;

            ++tickCount;
            super.tick();
            deltaTickSequential = runsTicking() ? 0.0F : deltaTickSequential + getScale();
        }

        @Override
        public int getTickCount() {
            return tickCount;
        }

        public float getDeltaTickSequential() {
            return deltaTickSequential;
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
