package net.flazesmp.flazesmpitems.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.command.commands.*;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers all commands for the mod
 */
@Mod.EventBusSubscriber(modid = FlazeSMPItems.MOD_ID)
public class ModCommands {
    
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommandBuildContext buildContext = event.getBuildContext();
        
        // Register the main command with aliases
        registerMainCommand(dispatcher, buildContext, "itemtooltipenhancer");
        registerMainCommand(dispatcher, buildContext, "ite");
        
        FlazeSMPItems.LOGGER.info("ItemTooltipEnhancer commands registered");
    }
    
    private static void registerMainCommand(
            CommandDispatcher<CommandSourceStack> dispatcher, 
            CommandBuildContext buildContext,
            String commandName) {
        
        LiteralArgumentBuilder<CommandSourceStack> mainCommand = Commands.literal(commandName)
            .requires(source -> source.hasPermission(0));
        
        // Register subcommands
        GetTextureCommand.register(mainCommand, buildContext);
        EditItemNameCommand.register(mainCommand, buildContext);
        EditItemTooltipCommand.register(mainCommand, buildContext);
        EditItemRarityCommand.register(mainCommand, buildContext);
        ResetItemCommand.register(mainCommand, buildContext); // Register the new reset command
        ReloadConfigCommand.register(mainCommand, buildContext);
        
        // Add help command
        mainCommand.then(Commands.literal("help")
            .executes(context -> {
                showHelpMessage(context.getSource());
                return 1;
            }));
        
        // Base command shows help
        mainCommand.executes(context -> {
            showHelpMessage(context.getSource());
            return 1;
        });
        
        dispatcher.register(mainCommand);
    }
    
    private static void showHelpMessage(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== ItemTooltipEnhancer Commands ==="), false);
        source.sendSuccess(() -> Component.literal("/ite editdisplayname <item> <name> - Set custom display name"), false);
        source.sendSuccess(() -> Component.literal("/ite edittooltip <item> <line> <text> - Set tooltip line"), false);
        source.sendSuccess(() -> Component.literal("/ite editrarity <item> <rarity> - Set item rarity"), false);
        source.sendSuccess(() -> Component.literal("/ite reset [item] - Reset item to default state"), false); // Add help for reset command
        source.sendSuccess(() -> Component.literal("/ite gettexture [item] - Get texture path for an item"), false);
        source.sendSuccess(() -> Component.literal("/ite reload - Reload item configs from files"), false);
        source.sendSuccess(() -> Component.literal("/ite help - Show this help message"), false);
    }
}