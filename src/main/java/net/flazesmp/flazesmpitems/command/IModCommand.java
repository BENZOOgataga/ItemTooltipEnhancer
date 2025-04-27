package net.flazesmp.flazesmpitems.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * Interface for mod commands to implement
 */
public interface IModCommand {
    /**
     * Register this command with the command dispatcher
     * @param dispatcher The command dispatcher
     */
    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        throw new UnsupportedOperationException("Command registration not implemented");
    }
}