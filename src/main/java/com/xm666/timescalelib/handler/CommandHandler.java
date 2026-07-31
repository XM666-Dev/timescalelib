package com.xm666.timescalelib.handler;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.xm666.timescalelib.TimeScaleLib;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TimeScaleLib.MODID)
public class CommandHandler {
    @SubscribeEvent
    public static void command(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("timescale")
                        .requires(p_308941_ -> p_308941_.hasPermission(3))
                        .then(
                                Commands.literal("apply")
                                        .then(
                                                Commands.argument("scale", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"0.0", "0.25", "0.5"}, builder))
                                                        .executes(context -> {
                                                            var scale = FloatArgumentType.getFloat(context, "scale");
                                                            TimeScaleHandler.applyScale(scale, -1);
                                                            return 1;
                                                        })
                                                        .then(
                                                                Commands.argument("duration", IntegerArgumentType.integer(-1))
                                                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"-1", "80", "1200"}, builder))
                                                                        .executes(context -> {
                                                                            var scale = FloatArgumentType.getFloat(context, "scale");
                                                                            var duration = IntegerArgumentType.getInteger(context, "duration");
                                                                            TimeScaleHandler.applyScale(scale, duration);
                                                                            return 1;
                                                                        })
                                                                        .then(
                                                                                Commands.argument("transition", IntegerArgumentType.integer(-1))
                                                                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"-1", "20", "60"}, builder))
                                                                                        .executes(context -> {
                                                                                            var scale = FloatArgumentType.getFloat(context, "scale");
                                                                                            var duration = IntegerArgumentType.getInteger(context, "duration");
                                                                                            var transition = IntegerArgumentType.getInteger(context, "transition");
                                                                                            TimeScaleHandler.applyScale(scale, duration, transition);
                                                                                            return 1;
                                                                                        })
                                                                                        .then(
                                                                                                Commands.literal("include")
                                                                                                        .then(
                                                                                                                Commands.argument("target", EntityArgument.entity())
                                                                                                                        .executes(context -> {
                                                                                                                            var target = EntityArgument.getEntity(context, "target");
                                                                                                                            var scale = FloatArgumentType.getFloat(context, "scale");
                                                                                                                            var duration = IntegerArgumentType.getInteger(context, "duration");
                                                                                                                            var transition = IntegerArgumentType.getInteger(context, "transition");
                                                                                                                            TimeScaleHandler.applyScale(target, scale, duration, transition);
                                                                                                                            return 1;
                                                                                                                        })
                                                                                                        )
                                                                                        )
                                                                        )
                                                                        .then(
                                                                                Commands.literal("include")
                                                                                        .then(
                                                                                                Commands.argument("target", EntityArgument.entity())
                                                                                                        .executes(context -> {
                                                                                                            var target = EntityArgument.getEntity(context, "target");
                                                                                                            var scale = FloatArgumentType.getFloat(context, "scale");
                                                                                                            var duration = IntegerArgumentType.getInteger(context, "duration");
                                                                                                            TimeScaleHandler.applyScale(target, scale, duration);
                                                                                                            return 1;
                                                                                                        })
                                                                                        )
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.literal("include")
                                                                        .then(
                                                                                Commands.argument("target", EntityArgument.entity())
                                                                                        .executes(context -> {
                                                                                            var target = EntityArgument.getEntity(context, "target");
                                                                                            var scale = FloatArgumentType.getFloat(context, "scale");
                                                                                            TimeScaleHandler.applyScale(target, scale, -1);
                                                                                            return 1;
                                                                                        })
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("remove")
                                        .executes(context -> {
                                            TimeScaleHandler.removeScale();
                                            return 1;
                                        })
                        )
        );
    }
}
