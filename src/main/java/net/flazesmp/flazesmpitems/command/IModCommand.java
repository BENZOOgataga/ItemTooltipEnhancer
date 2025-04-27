package net.flazesmp.flazesmpitems.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

/**
 * Interface for mod commands to implement
 */
public interface IModCommand {
    /**
     * Register this command as a subcommand of a main command
     * 
     * @param parent The parent command builder to attach this subcommand to
     * @param buildContext The command build context
     */
    static void register(LiteralArgumentBuilder<CommandSourceStack> parent, CommandBuildContext buildContext) {
        throw new UnsupportedOperationException("Command registration not implemented");
    }
    
    /**
     * Gets the name of this subcommand
     * 
     * @return The subcommand name
     */
    static String getName() {
        return "unnamed";
    }
    
    /**
     * Gets the description of this subcommand for the help message
     * 
     * @return The subcommand description
     */
    static String getDescription() {
        return "No description available";
    }
}