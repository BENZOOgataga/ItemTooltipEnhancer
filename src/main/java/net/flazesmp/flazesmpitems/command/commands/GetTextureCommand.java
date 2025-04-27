package net.flazesmp.flazesmpitems.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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

public class GetTextureCommand implements IModCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // We'll use the simpler version without ItemArgument since it requires CommandBuildContext
        dispatcher.register(Commands.literal("gettexture")
            .requires(source -> source.hasPermission(0)) // Permission level 0 means anyone can use it
            .executes(context -> getTextureForHeldItem(context.getSource()))
            .then(Commands.argument("itemId", StringArgumentType.string())
                .executes(context -> getTextureForItemId(
                    context.getSource(), 
                    StringArgumentType.getString(context, "itemId")))));
    }
    
    // Updated version that requires CommandBuildContext
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
    
    private static int getTextureForHeldItem(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack heldItem = player.getMainHandItem();
        
        if (heldItem.isEmpty()) {
            source.sendFailure(Component.literal("You must hold an item in your main hand"));
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
                source.sendFailure(Component.literal("Item not found: " + itemId));
                return 0;
            }
            
            String texturePath = TextureUtil.getItemTexturePath(item);
            sendTextureInfo(source, item, texturePath);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Invalid item ID format: " + itemId));
            return 0;
        }
    }
    
    private static void sendTextureInfo(CommandSourceStack source, Item item, String texturePath) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        
        // Fixed sendSuccess calls to match the expected function signature
        Component message = Component.literal("Item: ")
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(itemId.toString())
                .withStyle(ChatFormatting.GREEN));
        
        source.sendSuccess(() -> message, false);
        
        Component textureMessage = Component.literal("Texture Path: ")
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(texturePath)
                .withStyle(ChatFormatting.AQUA));
        
        source.sendSuccess(() -> textureMessage, false);
        
        // Create a clickable message to copy the texture path
        Component copyMessage = Component.literal("[Click to Copy]")
            .withStyle(style -> style
                .withColor(ChatFormatting.GOLD)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, texturePath))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Copy texture path to clipboard")
                )));
        
        source.sendSuccess(() -> copyMessage, false);
    }
}