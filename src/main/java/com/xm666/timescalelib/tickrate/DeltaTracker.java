package com.xm666.timescalelib.tickrate;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface DeltaTracker {
    DeltaTracker ZERO = new DefaultValue(0.0F);
    DeltaTracker ONE = new DefaultValue(1.0F);

    float getGameTimeDeltaTicks();

    float getGameTimeDeltaPartialTick(boolean var1);

    float getRealtimeDeltaTicks();

    @OnlyIn(Dist.CLIENT)
    class DefaultValue implements DeltaTracker {
        private final float value;

        DefaultValue(float value) {
            this.value = value;
        }

        public float getGameTimeDeltaTicks() {
            return this.value;
        }

        public float getGameTimeDeltaPartialTick(boolean runsNormally) {
            return this.value;
        }

        public float getRealtimeDeltaTicks() {
            return this.value;
        }
    }

    @OnlyIn(Dist.CLIENT)
    class Timer implements DeltaTracker {
        private final float msPerTick;
        private final FloatUnaryOperator targetMsptProvider;
        public boolean frozen;
        private float deltaTicks;
        private float deltaTickResidual;
        private float realtimeDeltaTicks;
        private float pausedDeltaTickResidual;
        private long lastMs;
        private long lastUiMs;
        private boolean paused;

        public Timer(float ticksPerSecond, long time, FloatUnaryOperator targetMsptProvider) {
            this.msPerTick = 1000.0F / ticksPerSecond;
            this.lastUiMs = this.lastMs = time;
            this.targetMsptProvider = targetMsptProvider;
        }

        public int advanceTime(long time, boolean advanceGameTime) {
            this.advanceRealTime(time);
            return advanceGameTime ? this.advanceGameTime(time) : 0;
        }

        private int advanceGameTime(long time) {
            this.deltaTicks = (float) (time - this.lastMs) / this.targetMsptProvider.apply(this.msPerTick);
            this.lastMs = time;
            this.deltaTickResidual += this.deltaTicks;
            int i = (int) this.deltaTickResidual;
            this.deltaTickResidual -= (float) i;
            return i;
        }

        private void advanceRealTime(long time) {
            this.realtimeDeltaTicks = (float) (time - this.lastUiMs) / this.msPerTick;
            this.lastUiMs = time;
        }

        public void updatePauseState(boolean paused) {
            if (paused) {
                this.pause();
            } else {
                this.unPause();
            }

        }

        private void pause() {
            if (!this.paused) {
                this.pausedDeltaTickResidual = this.deltaTickResidual;
            }

            this.paused = true;
        }

        private void unPause() {
            if (this.paused) {
                this.deltaTickResidual = this.pausedDeltaTickResidual;
            }

            this.paused = false;
        }

        public void updateFrozenState(boolean frozen) {
            this.frozen = frozen;
        }

        public float getGameTimeDeltaTicks() {
            return this.deltaTicks;
        }

        public float getGameTimeDeltaPartialTick(boolean runsNormally) {
            return TickRateHandler.getModifiablePartialTick(this, runsNormally);
        }

        public float getRealtimeDeltaTicks() {
            return this.realtimeDeltaTicks > 7.0F ? 0.5F : this.realtimeDeltaTicks;
        }
    }
}
