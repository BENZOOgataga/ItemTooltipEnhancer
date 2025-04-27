package net.flazesmp.flazesmpitems.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.command.commands.GetTextureCommand;
import net.flazesmp.flazesmpitems.command.commands.GuiCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registry for all mod commands
 */
@Mod.EventBusSubscriber(modid = FlazeSMPItems.MOD_ID)
public class ModCommands {
    
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommandBuildContext buildContext = event.getBuildContext();
        
        // Register the main commands with aliases
        registerMainCommand(dispatcher, buildContext, "itemtooltipenhancer");
        registerMainCommand(dispatcher, buildContext, "ite");
        
        FlazeSMPItems.LOGGER.info("ItemTooltipEnhancer: Registered commands");
    }
    
    /**
     * Registers a main command with the given name and all subcommands
     * 
     * @param dispatcher The command dispatcher
     * @param buildContext The command build context
     * @param commandName The main command name
     */
    private static void registerMainCommand(
            CommandDispatcher<CommandSourceStack> dispatcher, 
            CommandBuildContext buildContext,
            String commandName) {
        
        LiteralArgumentBuilder<CommandSourceStack> mainCommand = Commands.literal(commandName)
            .requires(source -> source.hasPermission(0));
        
        // Add all subcommands to the main command
        // Each subcommand is responsible for attaching itself to the provided builder
        GetTextureCommand.register(mainCommand, buildContext);
        GuiCommand.register(mainCommand, buildContext); // Added the GUI command
        
        // Add help command
        mainCommand.then(Commands.literal("help")
            .executes(context -> {
                context.getSource().sendSuccess(() -> 
                    getHelpComponent(commandName), false);
                return 1;
            }));
            
        // Add the base command which shows help
        mainCommand.executes(context -> {
            context.getSource().sendSuccess(() -> 
                getHelpComponent(commandName), false);
            return 1;
        });
        
        // Register the complete command
        dispatcher.register(mainCommand);
    }
    
    /**
     * Creates the help text component
     */
    private static Component getHelpComponent(String commandName) {
        return Component.literal("=== ItemTooltipEnhancer Commands ===")
            .withStyle(ChatFormatting.GOLD)
            .append(Component.literal("\n/" + commandName + " gettexture [item] - Get texture path for an item")
                .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("\n/" + commandName + " gui - Open item manager GUI (operators only)")
                .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("\n/" + commandName + " help - Show this help message")
                .withStyle(ChatFormatting.YELLOW));
    }
}