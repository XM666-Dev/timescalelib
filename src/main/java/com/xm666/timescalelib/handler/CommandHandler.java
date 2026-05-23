package com.xm666.timescalelib.handler;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.xm666.timescalelib.TimeScaleLib;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = TimeScaleLib.MODID)
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
                                                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"600", "1200", "-1"}, builder))
                                                                        .executes(context -> {
                                                                            var scale = FloatArgumentType.getFloat(context, "scale");
                                                                            var duration = IntegerArgumentType.getInteger(context, "duration");
                                                                            TimeScaleHandler.applyScale(scale, duration);
                                                                            return 1;
                                                                        })
                                                                        .then(
                                                                                Commands.argument("transition", IntegerArgumentType.integer(-1))
                                                                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"0", "20", "-1"}, builder))
                                                                                        .executes(context -> {
                                                                                            var scale = FloatArgumentType.getFloat(context, "scale");
                                                                                            var duration = IntegerArgumentType.getInteger(context, "duration");
                                                                                            var transition = IntegerArgumentType.getInteger(context, "transition");
                                                                                            TimeScaleHandler.applyScale(scale, duration, transition != -1 ? transition : duration);
                                                                                            return 1;
                                                                                        })
                                                                                        .then(
                                                                                                Commands.literal("include")
                                                                                                        .then(
                                                                                                                Commands.argument("include", EntityArgument.player())
                                                                                                                        .executes(context -> {
                                                                                                                            var include = EntityArgument.getEntity(context, "include");
                                                                                                                            var scale = FloatArgumentType.getFloat(context, "scale");
                                                                                                                            var duration = IntegerArgumentType.getInteger(context, "duration");
                                                                                                                            var transition = IntegerArgumentType.getInteger(context, "transition");
                                                                                                                            TimeScaleHandler.applyScale(include, scale, duration, transition != -1 ? transition : duration);
                                                                                                                            return 1;
                                                                                                                        })
                                                                                                        )
                                                                                        )
                                                                        )
                                                                        .then(
                                                                                Commands.literal("include")
                                                                                        .then(
                                                                                                Commands.argument("include", EntityArgument.player())
                                                                                                        .executes(context -> {
                                                                                                            var include = EntityArgument.getEntity(context, "include");
                                                                                                            var scale = FloatArgumentType.getFloat(context, "scale");
                                                                                                            var duration = IntegerArgumentType.getInteger(context, "duration");
                                                                                                            TimeScaleHandler.applyScale(include, scale, duration);
                                                                                                            return 1;
                                                                                                        })
                                                                                        )
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.literal("include")
                                                                        .then(
                                                                                Commands.argument("include", EntityArgument.player())
                                                                                        .executes(context -> {
                                                                                            var include = EntityArgument.getEntity(context, "include");
                                                                                            var scale = FloatArgumentType.getFloat(context, "scale");
                                                                                            TimeScaleHandler.applyScale(include, scale, -1);
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
