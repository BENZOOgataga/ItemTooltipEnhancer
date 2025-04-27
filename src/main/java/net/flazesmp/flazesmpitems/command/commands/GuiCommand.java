package net.flazesmp.flazesmpitems.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.command.IModCommand;
import net.flazesmp.flazesmpitems.gui.ItemManagerScreen;
import net.flazesmp.flazesmpitems.networking.ModMessages;
import net.flazesmp.flazesmpitems.networking.packet.OpenItemManagerGuiPacket;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GuiCommand implements IModCommand {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent, CommandBuildContext buildContext) {
        parent.then(Commands.literal("gui")
            .requires(source -> source.hasPermission(2)) // Operator permission level
            .executes(context -> openGui(context.getSource())));
    }
    
    private static int openGui(CommandSourceStack source) {
        try {
            // Only operators can use this command
            ServerPlayer player = source.getPlayerOrException();
            
            // Send packet to open GUI on client side
            ModMessages.sendToPlayer(new OpenItemManagerGuiPacket(), player);
            
            source.sendSuccess(() -> Component.literal("Opening Item Manager GUI..."), false);
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("You must be an in-game player to use this command"));
            return 0;
        }
    }
}