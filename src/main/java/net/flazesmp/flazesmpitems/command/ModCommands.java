package net.flazesmp.flazesmpitems.command;

import com.mojang.brigadier.CommandDispatcher;
import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.command.commands.GetTextureCommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
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
        
        // Register commands with the build context
        GetTextureCommand.register(dispatcher, buildContext);
        
        FlazeSMPItems.LOGGER.info("ItemTooltipEnhancer: Registered commands");
    }
}