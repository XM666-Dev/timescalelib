package com.xm666.timescalelib.handler;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;

public class WrapHandler {
    public static boolean callScaled(Operation<Boolean> original, TickRateManager instance) {
        TimeScaleHandler.scaleRunNormally = true;
        var runsNormally = original.call(instance);
        TimeScaleHandler.scaleRunNormally = false;

        return runsNormally;
    }

    public static boolean callScaled(Operation<Boolean> original, TickRateManager instance, Entity entity) {
        TimeScaleHandler.scaleRunNormally = true;
        var runsNormally = original.call(instance, entity);
        TimeScaleHandler.scaleRunNormally = false;

        return runsNormally;
    }

    public static float callScaled(Operation<Float> original, DeltaTracker instance, boolean runsNormally) {
        TimeScaleHandler.scalePartialTick = true;
        var partialTick = original.call(instance, runsNormally);
        TimeScaleHandler.scalePartialTick = false;

        return partialTick;
    }

    public static boolean callScaled(Operation<Boolean> original, Minecraft instance) {
        TimeScaleHandler.scaleRunNormally = true;
        var runsNormally = original.call(instance);
        TimeScaleHandler.scaleRunNormally = false;

        return runsNormally;
    }
}
