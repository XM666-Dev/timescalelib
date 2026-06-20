package com.xm666.timescalelib.timer;

import net.minecraft.util.Mth;

public class MathLib {
    public static float clampedInverseLerp(float delta, float start, float end) {
        var inverseDelta = Mth.inverseLerp(delta, start, end);
        return Mth.clamp(inverseDelta, 0.0F, 1.0F);
    }

    public static float smoothstep(float input) {
        return input * input * input * (input * (input * 6.0F - 15.0F) + 10.0F);
    }
}
