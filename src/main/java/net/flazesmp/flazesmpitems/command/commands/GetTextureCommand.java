package net.flazesmp.flazesmpitems.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.flazesmp.flazesmpitems.command.IModCommand;
import net.flazesmp.flazesmpitems.util.TextureUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.flazesmp.flazesmpitems.config.MessageConfig;

public class GetTextureCommand implements IModCommand {
    
    /**
     * Registers this command as a standalone command (no longer used)
     */
    @Deprecated
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("gettexture")
            .requires(source -> source.hasPermission(0))
            .executes(context -> getTextureForHeldItem(context.getSource()))
            .then(Commands.argument("item", ItemArgument.item(buildContext))
                .executes(context -> getTextureForSpecifiedItem(
                    context.getSource(), 
                    ItemArgument.getItem(context, "item").getItem())))
            .then(Commands.argument("itemId", StringArgumentType.string())
                .executes(context -> getTextureForItemId(
                    context.getSource(), 
                    StringArgumentType.getString(context, "itemId")))));
    }
    
    /**
     * Registers this command as a subcommand of a main command
     * 
     * @param parent The parent command builder to attach this subcommand to
     * @param buildContext The command build context
     */
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent, CommandBuildContext buildContext) {
        parent.then(Commands.literal("gettexture")
            .executes(context -> getTextureForHeldItem(context.getSource()))
            .then(Commands.argument("item", ItemArgument.item(buildContext))
                .executes(context -> getTextureForSpecifiedItem(
                    context.getSource(), 
                    ItemArgument.getItem(context, "item").getItem())))
            .then(Commands.argument("itemId", StringArgumentType.string())
                .executes(context -> getTextureForItemId(
                    context.getSource(), 
                    StringArgumentType.getString(context, "itemId")))));
    }
    
    private static int getTextureForHeldItem(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack heldItem = player.getMainHandItem();
        
        if (heldItem.isEmpty()) {
            source.sendFailure(Component.literal(MessageConfig.getMessage("command.reset.held_item_requirement")));
            return 0;
        }
        
        String texturePath = TextureUtil.getItemTexturePath(heldItem.getItem());
        sendTextureInfo(source, heldItem.getItem(), texturePath);
        return 1;
    }
    
    private static int getTextureForSpecifiedItem(CommandSourceStack source, Item item) {
        String texturePath = TextureUtil.getItemTexturePath(item);
        sendTextureInfo(source, item, texturePath);
        return 1;
    }
    
    private static int getTextureForItemId(CommandSourceStack source, String itemId) {
        try {
            ResourceLocation resourceLocation = new ResourceLocation(itemId);
            Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
            
            if (item == null) {
                source.sendFailure(Component.literal(MessageConfig.getMessage("command.reset.not_found", itemId)));
                return 0;
            }
            
            String texturePath = TextureUtil.getItemTexturePath(item);
            sendTextureInfo(source, item, texturePath);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal(MessageConfig.getMessage("command.reset.invalid_id", itemId)));
            return 0;
        }
    }
    
    private static void sendTextureInfo(CommandSourceStack source, Item item, String texturePath) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        
        Component message = Component.literal(MessageConfig.getMessage("command.gettexture.item"))
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(itemId.toString())
                .withStyle(ChatFormatting.GREEN));
        
        source.sendSuccess(() -> message, false);
        
        Component textureMessage = Component.literal(MessageConfig.getMessage("command.gettexture.path"))
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(texturePath)
                .withStyle(ChatFormatting.AQUA));
        
        source.sendSuccess(() -> textureMessage, false);
        
        // Create a clickable message to copy the texture path
        Component copyMessage = Component.literal(MessageConfig.getMessage("command.gettexture.copy"))
            .withStyle(style -> style
                .withColor(ChatFormatting.GOLD)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, texturePath))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Component.literal(MessageConfig.getMessage("command.gettexture.copy_tooltip"))
                )));
        
        source.sendSuccess(() -> copyMessage, false);
    }
}