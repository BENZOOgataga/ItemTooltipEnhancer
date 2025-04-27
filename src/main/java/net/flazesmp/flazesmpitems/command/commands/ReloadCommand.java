package net.flazesmp.flazesmpitems.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.flazesmp.flazesmpitems.command.IModCommand;
import net.flazesmp.flazesmpitems.config.CustomItemsConfig;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class ReloadCommand implements IModCommand {
    
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent, CommandBuildContext buildContext) {
        parent.then(net.minecraft.commands.Commands.literal("reload")
            .requires(source -> source.hasPermission(2)) // Require level 2 permission (op)
            .executes(context -> {
                // Reload config
                CustomItemsConfig.load();
                
                context.getSource().sendSuccess(
                    () -> Component.translatable("itemtooltipenhancer.command.reload.success"), 
                    true
                );
                
                return 1;
            })
        );
    }
    
    public static String getName() {
        return "reload";
    }
    
    public static String getDescription() {
        return "itemtooltipenhancer.commands.reload.description";
    }
}