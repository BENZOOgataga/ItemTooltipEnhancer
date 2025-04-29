package net.flazesmp.flazesmpitems.clearlag;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.config.MessageConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = FlazeSMPItems.MOD_ID)
public class ClearlagCommand {

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher(), event.getBuildContext());
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> clearlagCommand = Commands.literal("clearlag");
        
        // Basic clearlag command - executes with a 1-minute delay
        clearlagCommand.requires(source -> source.hasPermission(2)) // Admin permission level
            .executes(ClearlagCommand::executeClearlagWithDelay);
        
        // Notifications command - replaces toggle command
        // Available to everyone - /clearlag notifications <type>
        clearlagCommand.then(Commands.literal("notifications")
            .executes(ClearlagCommand::executeShowNotificationType)
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests(ClearlagCommand::suggestNotificationTypes)
                .executes(context -> executeSetNotificationType(
                    context,
                    StringArgumentType.getString(context, "type")))));
        
        // Time subcommand - available to everyone
        clearlagCommand.then(Commands.literal("time")
            .executes(ClearlagCommand::executeCheckTime));
        
        // Help subcommand - available to everyone
        clearlagCommand.then(Commands.literal("help")
            .executes(ClearlagCommand::executeHelp));
        
        // Config subcommand - admin only
        LiteralArgumentBuilder<CommandSourceStack> configCommand = Commands.literal("config")
            .requires(source -> source.hasPermission(2)); // Admin permission level
        
        // /clearlag config show - show current config values
        configCommand.then(Commands.literal("show")
            .executes(ClearlagCommand::executeShowConfig));
        
        // /clearlag config set - set config values
        LiteralArgumentBuilder<CommandSourceStack> setCommand = Commands.literal("set");
        
        // Toggle auto clearlag
        setCommand.then(Commands.literal("enableAutoClearlag")
            .then(Commands.argument("value", BoolArgumentType.bool())
                .executes(ctx -> executeSetConfigBool(ctx, "enableAutoClearlag", BoolArgumentType.getBool(ctx, "value")))));
        
        // Set interval
        setCommand.then(Commands.literal("clearlagIntervalMinutes")
            .then(Commands.argument("value", IntegerArgumentType.integer(5, 1440))
                .executes(ctx -> executeSetConfigInt(ctx, "clearlagIntervalMinutes", IntegerArgumentType.getInteger(ctx, "value")))));
        
        // Set notification duration
        setCommand.then(Commands.literal("notificationDisplayDurationSeconds")
            .then(Commands.argument("value", IntegerArgumentType.integer(1, 10))
                .executes(ctx -> executeSetConfigInt(ctx, "notificationDisplayDurationSeconds", IntegerArgumentType.getInteger(ctx, "value")))));
        
        // Toggle dynamic countdown
        setCommand.then(Commands.literal("useDynamicCountdown")
            .then(Commands.argument("value", BoolArgumentType.bool())
                .executes(ctx -> executeSetConfigBool(ctx, "useDynamicCountdown", BoolArgumentType.getBool(ctx, "value")))));
        
        // Set long threshold
        setCommand.then(Commands.literal("longNotificationThresholdSeconds")
            .then(Commands.argument("value", IntegerArgumentType.integer(30, 3600))
                .executes(ctx -> executeSetConfigInt(ctx, "longNotificationThresholdSeconds", IntegerArgumentType.getInteger(ctx, "value")))));
        
        // Set short threshold
        setCommand.then(Commands.literal("shortNotificationThresholdSeconds")
            .then(Commands.argument("value", IntegerArgumentType.integer(1, 30))
                .executes(ctx -> executeSetConfigInt(ctx, "shortNotificationThresholdSeconds", IntegerArgumentType.getInteger(ctx, "value")))));
        
        // Set default notification type
        setCommand.then(Commands.literal("defaultNotificationType")
            .then(Commands.argument("value", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(NotificationType.values())
                         .map(type -> type.name().toLowerCase())
                         .collect(Collectors.toList()),
                    builder))
                .executes(ctx -> executeSetConfigEnum(ctx, "defaultNotificationType", StringArgumentType.getString(ctx, "value")))));
        
        // Add/remove warning times
        setCommand.then(Commands.literal("warningTimes")
            .then(Commands.literal("add")
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                    .executes(ctx -> executeAddWarningTime(ctx, IntegerArgumentType.getInteger(ctx, "seconds")))))
            .then(Commands.literal("remove")
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                    .suggests(ClearlagCommand::suggestWarningTimes)
                    .executes(ctx -> executeRemoveWarningTime(ctx, IntegerArgumentType.getInteger(ctx, "seconds")))))
            .then(Commands.literal("list")
                .executes(ClearlagCommand::executeListWarningTimes)));
        
        // Entity types to clear
        setCommand.then(Commands.literal("entityTypes")
            .then(Commands.literal("add")
                .then(Commands.argument("type", StringArgumentType.string())
                    .executes(ctx -> executeAddEntityType(ctx, StringArgumentType.getString(ctx, "type")))))
            .then(Commands.literal("remove")
                .then(Commands.argument("type", StringArgumentType.string())
                    .suggests(ClearlagCommand::suggestEntityTypes)
                    .executes(ctx -> executeRemoveEntityType(ctx, StringArgumentType.getString(ctx, "type")))))
            .then(Commands.literal("list")
                .executes(ClearlagCommand::executeListEntityTypes)));
        
        configCommand.then(setCommand);
        
        // Add config command to main clearlag command
        clearlagCommand.then(configCommand);
        
        dispatcher.register(clearlagCommand);
    }
    
    /**
     * Execute showing the current config values
     */
    private static int executeShowConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal(MessageConfig.getMessage("config.show.header")).withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal(MessageConfig.getMessage("config.show.general")).withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("- Enable auto clearlag: ")
            .append(Component.literal(String.valueOf(ClearlagConfig.SERVER.enableAutoClearlag.get()))
                    .withStyle(ClearlagConfig.SERVER.enableAutoClearlag.get() ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        source.sendSuccess(() -> Component.literal("- Interval (minutes): ")
            .append(Component.literal(String.valueOf(ClearlagConfig.SERVER.clearlagIntervalMinutes.get()))
                    .withStyle(ChatFormatting.AQUA)), false);
        
        source.sendSuccess(() -> Component.literal("Notification settings:").withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("- Default notification type: ")
            .append(Component.literal(ClearlagConfig.SERVER.defaultNotificationType.get().name())
                    .withStyle(ChatFormatting.AQUA)), false);
        source.sendSuccess(() -> Component.literal("- Notification duration (seconds): ")
            .append(Component.literal(String.valueOf(ClearlagConfig.SERVER.notificationDisplayDurationSeconds.get()))
                    .withStyle(ChatFormatting.AQUA)), false);
        source.sendSuccess(() -> Component.literal("- Dynamic countdown: ")
            .append(Component.literal(String.valueOf(ClearlagConfig.SERVER.useDynamicCountdown.get()))
                    .withStyle(ClearlagConfig.SERVER.useDynamicCountdown.get() ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        source.sendSuccess(() -> Component.literal("- Long threshold (seconds): ")
            .append(Component.literal(String.valueOf(ClearlagConfig.SERVER.longNotificationThresholdSeconds.get()))
                    .withStyle(ChatFormatting.AQUA)), false);
        source.sendSuccess(() -> Component.literal("- Short threshold (seconds): ")
            .append(Component.literal(String.valueOf(ClearlagConfig.SERVER.shortNotificationThresholdSeconds.get()))
                    .withStyle(ChatFormatting.AQUA)), false);
        
        source.sendSuccess(() -> Component.literal(MessageConfig.getMessage("config.show.usage.set")).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(MessageConfig.getMessage("config.show.usage.warning_times")).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(MessageConfig.getMessage("config.show.usage.entity_types")).withStyle(ChatFormatting.GRAY), false);
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Execute showing the current notification type for the player
     */
    private static int executeShowNotificationType(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ServerPlayer player = source.getPlayerOrException();
            NotificationType currentType = ClearlagConfig.getPlayerNotificationType(player.getUUID());
            
            player.sendSystemMessage(Component.literal(
                    MessageConfig.getMessage("clearlag.notification.type.current", currentType.name()))
                .withStyle(ChatFormatting.YELLOW));
                
            player.sendSystemMessage(Component.literal(
                    MessageConfig.getMessage("clearlag.notification.type.change"))
                .withStyle(ChatFormatting.GRAY));
                
            return Command.SINGLE_SUCCESS;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal(
                    MessageConfig.getMessage("clearlag.notification.type.player_only")));
            return 0;
        }
    }
    
    /**
     * Execute setting notification type
     */
    private static int executeSetNotificationType(
            CommandContext<CommandSourceStack> context, String typeStr) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            NotificationType newType;
            try {
                // Convert to uppercase to match enum
                newType = NotificationType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Handle invalid notification type
                player.sendSystemMessage(Component.literal(
                        MessageConfig.getMessage("clearlag.notification.type.invalid", typeStr))
                    .withStyle(ChatFormatting.RED));
                player.sendSystemMessage(Component.literal(
                        MessageConfig.getMessage("clearlag.notification.type.valid"))
                    .withStyle(ChatFormatting.YELLOW));
                return 0;
            }
            
            // Check if none type was selected and warn about risks
            if (newType == NotificationType.NONE) {
                player.sendSystemMessage(Component.literal(
                        MessageConfig.getMessage("clearlag.notification.type.none_warning"))
                    .withStyle(ChatFormatting.RED));
            }
            
            // Update the player's preference
            ClearlagConfig.setPlayerNotificationType(player.getUUID(), newType);
            
            // Inform the player of the change
            String messageKey = switch(newType) {
                case CHAT -> "clearlag.notification.type.chat";
                case HOTBAR -> "clearlag.notification.type.hotbar";
                case NONE -> "clearlag.notification.type.none";
            };
            
            player.sendSystemMessage(Component.literal(
                    MessageConfig.getMessage(messageKey))
                .withStyle(ChatFormatting.GREEN));
            return Command.SINGLE_SUCCESS;
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal(
                    MessageConfig.getMessage("clearlag.notification.type.player_only")));
            return 0;
        }
    }
    
    /**
     * Execute setting a boolean config value
     */
    private static int executeSetConfigBool(CommandContext<CommandSourceStack> context, String setting, boolean value) {
        CommandSourceStack source = context.getSource();
        
        switch (setting) {
            case "enableAutoClearlag" -> {
                ClearlagConfig.SERVER.enableAutoClearlag.set(value);
                if (value) {
                    ClearlagManager.scheduleNextClearlag();
                }
            }
            case "useDynamicCountdown" -> ClearlagConfig.SERVER.useDynamicCountdown.set(value);
            default -> {
                source.sendFailure(Component.literal(MessageConfig.getMessage("config.unknown_setting", setting)).withStyle(ChatFormatting.RED));
                return 0;
            }
        }
        
        source.sendSuccess(() -> Component.literal(MessageConfig.getMessage("config.set.success", setting, value ? "enabled" : "disabled"))
            .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        
        saveConfigAndReschedule();
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Execute setting an integer config value
     */
    private static int executeSetConfigInt(CommandContext<CommandSourceStack> context, String setting, int value) {
        CommandSourceStack source = context.getSource();
        
        switch (setting) {
            case "clearlagIntervalMinutes" -> ClearlagConfig.SERVER.clearlagIntervalMinutes.set(value);
            case "notificationDisplayDurationSeconds" -> ClearlagConfig.SERVER.notificationDisplayDurationSeconds.set(value);
            case "longNotificationThresholdSeconds" -> ClearlagConfig.SERVER.longNotificationThresholdSeconds.set(value);
            case "shortNotificationThresholdSeconds" -> ClearlagConfig.SERVER.shortNotificationThresholdSeconds.set(value);
            default -> {
                source.sendFailure(Component.literal(MessageConfig.getMessage("config.unknown_setting", setting)).withStyle(ChatFormatting.RED));
                return 0;
            }
        }
        
        source.sendSuccess(() -> Component.literal(MessageConfig.getMessage("config.set.success", setting, value))
            .withStyle(ChatFormatting.AQUA), true);
        
        saveConfigAndReschedule();
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Execute setting an enum config value
     */
    private static int executeSetConfigEnum(CommandContext<CommandSourceStack> context, String setting, String value) {
        CommandSourceStack source = context.getSource();
        
        if ("defaultNotificationType".equals(setting)) {
            try {
                NotificationType type = NotificationType.valueOf(value.toUpperCase());
                ClearlagConfig.SERVER.defaultNotificationType.set(type);
                
                source.sendSuccess(() -> Component.literal("Set default notification type to ")
                    .append(Component.literal(type.name())
                            .withStyle(ChatFormatting.AQUA)), true);
                
                saveConfigAndReschedule();
                return Command.SINGLE_SUCCESS;
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.literal(MessageConfig.getMessage("config.invalid_value", "notification type", value))
                    .withStyle(ChatFormatting.RED));
                return 0;
            }
        } else {
            source.sendFailure(Component.literal(MessageConfig.getMessage("config.unknown_setting", setting))
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Execute adding a warning time to the config
     */
    private static int executeAddWarningTime(CommandContext<CommandSourceStack> context, int seconds) {
        CommandSourceStack source = context.getSource();
        
        List<Integer> currentWarningTimes = new ArrayList<>(ClearlagConfig.SERVER.warningTimesSeconds.get());
        
        if (currentWarningTimes.contains(seconds)) {
            source.sendFailure(Component.literal(MessageConfig.getMessage("config.add.warning_time.exists", seconds))
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        currentWarningTimes.add(seconds);
        ClearlagConfig.SERVER.warningTimesSeconds.set(currentWarningTimes);
        
        source.sendSuccess(() -> Component.literal(MessageConfig.getMessage("config.add.warning_time.success", seconds))
            .withStyle(ChatFormatting.GREEN), true);
        
        saveConfigAndReschedule();
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Execute removing a warning time from the config
     */
    private static int executeRemoveWarningTime(CommandContext<CommandSourceStack> context, int seconds) {
        CommandSourceStack source = context.getSource();
        
        List<Integer> currentWarningTimes = new ArrayList<>(ClearlagConfig.SERVER.warningTimesSeconds.get());
        
        if (!currentWarningTimes.contains(seconds)) {
            source.sendFailure(Component.literal(MessageConfig.getMessage("config.remove.warning_time.not_found", seconds))
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        if (currentWarningTimes.size() <= 1) {
            source.sendFailure(Component.literal(MessageConfig.getMessage("config.remove.warning_time.last"))
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        currentWarningTimes.remove(Integer.valueOf(seconds));
        ClearlagConfig.SERVER.warningTimesSeconds.set(currentWarningTimes);
        
        source.sendSuccess(() -> Component.literal(MessageConfig.getMessage("config.remove.warning_time.success", seconds))
            .withStyle(ChatFormatting.RED), true);
        
        saveConfigAndReschedule();
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Execute listing all configured warning times
     */
    private static int executeListWarningTimes(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        List<Integer> sortedTimes = ClearlagConfig.getSortedWarningTimes();
        
        source.sendSuccess(() -> Component.literal("=== Configured Warning Times ===")
            .withStyle(ChatFormatting.GOLD), false);
            
        StringBuilder timesList = new StringBuilder();
        for (int i = 0; i < sortedTimes.size(); i++) {
            timesList.append(sortedTimes.get(i));
            if (i < sortedTimes.size() - 1) {
                timesList.append(", ");
            }
            // Add line breaks for readability
            if ((i + 1) % 8 == 0 && i < sortedTimes.size() - 1) {
                timesList.append("\n");
            }
        }
        
        source.sendSuccess(() -> Component.literal(timesList.toString())
            .withStyle(ChatFormatting.YELLOW), false);
            
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Execute adding an entity type to the config
     */
    private static int executeAddEntityType(CommandContext<CommandSourceStack> context, String entityType) {
        CommandSourceStack source = context.getSource();
        
        // Make sure the entity type starts with a namespace
        if (!entityType.contains(":")) {
            entityType = "minecraft:" + entityType;
        }
        
        final String finalEntityType = entityType;
        List<String> currentEntityTypes = new ArrayList<>(ClearlagConfig.SERVER.entityTypesToClear.get());
        
        if (currentEntityTypes.contains(entityType)) {
            source.sendFailure(Component.literal("Entity type " + entityType + " is already in the list")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        currentEntityTypes.add(entityType);
        ClearlagConfig.SERVER.entityTypesToClear.set(currentEntityTypes);
        
        source.sendSuccess(() -> Component.literal("Added entity type: ")
            .append(Component.literal(finalEntityType)
                    .withStyle(ChatFormatting.GREEN)), true);
        
        saveConfigAndReschedule();
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Execute removing an entity type from the config
     */
    private static int executeRemoveEntityType(CommandContext<CommandSourceStack> context, String entityType) {
        CommandSourceStack source = context.getSource();
        
        // Make sure the entity type starts with a namespace
        if (!entityType.contains(":")) {
            entityType = "minecraft:" + entityType;
        }
        
        final String finalEntityType = entityType;
        List<String> currentEntityTypes = new ArrayList<>(ClearlagConfig.SERVER.entityTypesToClear.get());
        
        if (!currentEntityTypes.contains(entityType)) {
            source.sendFailure(Component.literal(MessageConfig.getMessage("config.remove.entity_type.not_found", entityType))
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        if (currentEntityTypes.size() <= 1) {
            source.sendFailure(Component.literal(MessageConfig.getMessage("config.remove.entity_type.last"))
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        currentEntityTypes.remove(entityType);
        ClearlagConfig.SERVER.entityTypesToClear.set(currentEntityTypes);
        
        source.sendSuccess(() -> Component.literal(MessageConfig.getMessage("config.remove.entity_type.success", finalEntityType))
            .withStyle(ChatFormatting.RED), true);
        
        saveConfigAndReschedule();
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Execute listing all configured entity types
     */
    private static int executeListEntityTypes(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        List<String> entityTypes = new ArrayList<>(ClearlagConfig.SERVER.entityTypesToClear.get());
        
        source.sendSuccess(() -> Component.literal("=== Entity Types to Clear ===")
            .withStyle(ChatFormatting.GOLD), false);
            
        for (String entityType : entityTypes) {
            source.sendSuccess(() -> Component.literal("- ")
                .append(Component.literal(entityType)
                        .withStyle(ChatFormatting.YELLOW)), false);
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Suggestion provider for existing warning times
     */
    private static CompletableFuture<Suggestions> suggestWarningTimes(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> warningTimesStr = ClearlagConfig.SERVER.warningTimesSeconds.get().stream()
            .map(String::valueOf)
            .collect(Collectors.toList());
        return SharedSuggestionProvider.suggest(warningTimesStr, builder);
    }
    
    /**
     * Suggestion provider for existing entity types
     */
    private static CompletableFuture<Suggestions> suggestEntityTypes(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> entityTypes = new ArrayList<>(ClearlagConfig.SERVER.entityTypesToClear.get());
        return SharedSuggestionProvider.suggest(entityTypes, builder);
    }
    
    /**
     * Save the config to file and reschedule clearlag if needed
     */
    private static void saveConfigAndReschedule() {
        // Save the config
        ClearlagConfig.SERVER_SPEC.save();
        
        // Reschedule clearlag tasks with new settings
        ClearlagManager.rescheduleWithNewSettings();
    }
    
    /**
     * Suggest notification types for tab completion
     */
    private static CompletableFuture<Suggestions> suggestNotificationTypes(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
            List.of("hotbar", "chat", "none"),
            builder
        );
    }
    
    /**
     * Execute a clearlag operation with a 1-minute delay
     */
    private static int executeClearlagWithDelay(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Check if a manual clearlag is already in progress
        if (!ClearlagManager.canExecuteManualClearlag()) {
            source.sendFailure(Component.literal(
                    MessageConfig.getMessage("clearlag.already_scheduled"))
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        // Start a manual clearlag
        ClearlagManager.startManualClearlag();
        
        source.sendSuccess(() -> Component.literal(
                MessageConfig.getMessage("clearlag.scheduled"))
            .withStyle(ChatFormatting.YELLOW), true);
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Execute the check time command
     */
    private static int executeCheckTime(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Get time until next clearlag
        int secondsRemaining = ClearlagManager.getTimeUntilNextClearlag();
        
        if (secondsRemaining <= 0) {
            source.sendSuccess(() -> Component.literal(
                    MessageConfig.getMessage("clearlag.none_scheduled"))
                .withStyle(ChatFormatting.YELLOW), false);
        } else {
            String timeRemaining = ClearlagConfig.formatTimeRemaining(secondsRemaining);
            source.sendSuccess(() -> Component.literal(
                    MessageConfig.getMessage("clearlag.next", timeRemaining))
                .withStyle(ChatFormatting.YELLOW), false);
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Execute the help command to show available commands
     */
    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean isAdmin = source.hasPermission(2);
        
        source.sendSuccess(() -> Component.literal("=== Clearlag Commands ===").withStyle(ChatFormatting.GOLD), false);
        
        if (isAdmin) {
            source.sendSuccess(() -> Component.literal("/clearlag").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Schedule a clearlag in 1 minute").withStyle(ChatFormatting.WHITE)), false);
                
            source.sendSuccess(() -> Component.literal("/clearlag config show").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Show current configuration").withStyle(ChatFormatting.WHITE)), false);
                
            source.sendSuccess(() -> Component.literal("/clearlag config set <option> <value>").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Change configuration").withStyle(ChatFormatting.WHITE)), false);
        }
        
        source.sendSuccess(() -> Component.literal("/clearlag time").withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(" - Check when the next clearlag will occur").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("/clearlag notifications [type]").withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(" - Set notification preferences (hotbar, chat, none)").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("/clearlag help").withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(" - Show this help message").withStyle(ChatFormatting.WHITE)), false);
        
        return Command.SINGLE_SUCCESS;
    }
}