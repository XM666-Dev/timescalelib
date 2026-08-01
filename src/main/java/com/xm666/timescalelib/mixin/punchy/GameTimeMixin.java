package com.xm666.timescalelib.mixin.punchy;

import com.bawnorton.mixinsquared.TargetHandler;
import com.xm666.timescalelib.handler.TimeScaleHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.state.AttackStateMachine;
import punchy.client.state.MiningStateMachine;
import punchy.client.state.SpearStateMachine;
import punchy.client.state.UseItemStateMachine;

@OnlyIn(Dist.CLIENT)
public class GameTimeMixin {
    @Mixin(value = AttackStateMachine.class, remap = false)
    private static class AttackStateMachineMixin {
        @Redirect(method = "currentTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getGameTime()J"))
        private long redirectGameTime(ClientLevel instance) {
            return TimeScaleHandler.clientTimer.getTickCount();
        }
    }

    @Mixin(value = MiningStateMachine.class, remap = false)
    private static class MiningStateMachineMixin {
        @Redirect(method = "currentTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getGameTime()J"))
        private long redirectGameTime(ClientLevel instance) {
            return TimeScaleHandler.clientTimer.getTickCount();
        }
    }

    @Mixin(value = SpearStateMachine.class, remap = false)
    private static class SpearStateMachineMixin {
        @Redirect(method = "currentTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getGameTime()J"))
        private long redirectGameTime(ClientLevel instance) {
            return TimeScaleHandler.clientTimer.getTickCount();
        }
    }

    @Mixin(value = UseItemStateMachine.class, remap = false)
    private static class UseItemStateMachineMixin {
        @Redirect(method = "currentTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getGameTime()J"))
        private long redirectGameTime(ClientLevel instance) {
            return TimeScaleHandler.clientTimer.getTickCount();
        }
    }

    @Mixin(value = PunchyAnimationManager.class, remap = false)
    private static class PunchyAnimationManagerMixin {
        @Redirect(method = "updateUseMeshSuppression", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getGameTime()J"))
        private static long redirectGameTime(ClientLevel instance) {
            return TimeScaleHandler.clientTimer.getTickCount();
        }
    }

    @Mixin(value = ItemProperties.class, priority = 2000, remap = false)
    private static class ItemPropertiesMixin {
        @TargetHandler(mixin = "punchy.mixin.client.ItemPropertiesMixin", name = "resolveRemainingTicks")
        @Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getGameTime()J"))
        private static long redirectGameTime(ClientLevel instance) {
            return TimeScaleHandler.clientTimer.getTickCount();
        }
    }
}
