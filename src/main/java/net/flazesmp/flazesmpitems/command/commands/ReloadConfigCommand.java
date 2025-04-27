package net.flazesmp.flazesmpitems.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.flazesmp.flazesmpitems.command.IModCommand;
import net.flazesmp.flazesmpitems.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Command to reload the config files
 */
public class ReloadConfigCommand implements IModCommand {

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
            // Reload all configs
            ConfigManager.loadAllConfigs();
            
            // Success message
            source.sendSuccess(() -> Component.literal("ItemTooltipEnhancer configs reloaded successfully!")
                    .withStyle(ChatFormatting.GREEN), true);
            
            return 1;
        } catch (Exception e) {
            // Error message
            source.sendFailure(Component.literal("Error reloading configs: " + e.getMessage())
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