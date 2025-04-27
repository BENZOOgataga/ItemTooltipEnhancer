package net.flazesmp.flazesmpitems.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.flazesmp.flazesmpitems.command.IModCommand;
import net.flazesmp.flazesmpitems.network.NetworkHandler;
import net.flazesmp.flazesmpitems.network.OpenGuiPacket;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GuiCommand implements IModCommand {
    
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent, CommandBuildContext buildContext) {
        parent.then(net.minecraft.commands.Commands.literal("gui")
            .requires(source -> source.hasPermission(2)) // Require level 2 permission (op)
            .executes(context -> {
                // Check if command was executed by a player
                try {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Send packet to open GUI on client side
                    NetworkHandler.sendToPlayer(new OpenGuiPacket(), player);
                    
                    return 1;
                } catch (Exception e) {
                    // Command was not executed by a player
                    context.getSource().sendFailure(Component.translatable("itemtooltipenhancer.command.playerOnly"));
                    return 0;
                }
            })
        );
    }
    
    public static String getName() {
        return "gui";
    }
    
    public static String getDescription() {
        return "itemtooltipenhancer.commands.gui.description";
    }
}