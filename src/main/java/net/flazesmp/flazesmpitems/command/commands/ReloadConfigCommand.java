package net.flazesmp.flazesmpitems.command.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.flazesmp.flazesmpitems.clearlag.ClearlagManager;
import net.flazesmp.flazesmpitems.command.IModCommand;
import net.flazesmp.flazesmpitems.config.ConfigManager;
import net.flazesmp.flazesmpitems.config.MessageConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command to reload the config files
 */
public class ReloadConfigCommand implements IModCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReloadConfigCommand.class);
    
    /**
     * Registers this command as a subcommand of the main command
     */
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent, CommandBuildContext buildContext) {
        // reload command setup
        parent.then(Commands.literal("reload")
            .requires(source -> source.hasPermission(2)) // Admin permission level
            .executes(ReloadConfigCommand::executeReload));
    }
    
    /**
     * Execute reloading configs
     */
    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            source.sendSuccess(() -> Component.literal(MessageConfig.getMessage("command.reload.checking"))
                    .withStyle(ChatFormatting.YELLOW), true);
                    
            // Check and repair configurations if necessary
            ConfigManager.checkAndRepairConfig();
            
            // Reload the message configuration first
            MessageConfig.reloadMessages();
            
            // Load all configurations
            ConfigManager.loadAllConfigs();
            
            // Reschedule clearlag task to apply new settings
            ClearlagManager.rescheduleWithNewSettings();
            
            source.sendSuccess(() -> Component.literal(MessageConfig.getMessage("command.reload.success"))
                    .withStyle(ChatFormatting.GREEN), true);
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            LOGGER.error("Failed to reload configuration", e);
            source.sendFailure(Component.literal(
                    MessageConfig.getMessage("command.reload.error", e.getMessage()))
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    /**
     * Gets the name of this subcommand
     */
    public static String getName() {
        return "reload";
    }
    
    /**
     * Gets the description of this subcommand for the help message
     */
    public static String getDescription() {
        return "Reload item configs from files";
    }
}