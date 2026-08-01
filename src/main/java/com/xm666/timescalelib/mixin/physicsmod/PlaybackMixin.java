package com.xm666.timescalelib.mixin.physicsmod;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import net.diebuddies.physics.PhysicsMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@OnlyIn(Dist.CLIENT)
@Mixin(value = PhysicsMod.class, remap = false)
public class PlaybackMixin {
    @ModifyReturnValue(method = "getPlaybackSpeed", at = @At("RETURN"))
    private static double modifyPlaybackSpeed(double original) {
        return original * TimeScaleHandler.clientTimer.getScale();
    }
}
