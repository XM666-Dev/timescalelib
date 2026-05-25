package com.xm666.timescalelib.mixin.punchy;

import com.xm666.timescalelib.handler.TimeScaleHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import punchy.client.state.AttackStateMachine;
import punchy.client.state.MiningStateMachine;
import punchy.client.state.SpearStateMachine;
import punchy.client.state.UseItemStateMachine;

@OnlyIn(Dist.CLIENT)
public class GameTimeMixin {
    @Mixin(AttackStateMachine.class)
    private static class AttackStateMachineMixin {
        @Redirect(method = "currentTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getGameTime()J"))
        private long redirectGameTime(ClientLevel instance) {
            return TimeScaleHandler.clientTimer.getTickCount();
        }
    }

    @Mixin(MiningStateMachine.class)
    private static class MiningStateMachineMixin {
        @Redirect(method = "currentTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getGameTime()J"))
        private long redirectGameTime(ClientLevel instance) {
            return TimeScaleHandler.clientTimer.getTickCount();
        }
    }

    @Mixin(SpearStateMachine.class)
    private static class SpearStateMachineMixin {
        @Redirect(method = "currentTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getGameTime()J"))
        private long redirectGameTime(ClientLevel instance) {
            return TimeScaleHandler.clientTimer.getTickCount();
        }
    }

    @Mixin(UseItemStateMachine.class)
    private static class UseItemStateMachineMixin {
        @Redirect(method = "currentTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getGameTime()J"))
        private long redirectGameTime(ClientLevel instance) {
            return TimeScaleHandler.clientTimer.getTickCount();
        }
    }
}
