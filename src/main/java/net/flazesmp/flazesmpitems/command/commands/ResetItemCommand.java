package net.flazesmp.flazesmpitems.command.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.flazesmp.flazesmpitems.command.IModCommand;
import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.concurrent.CompletableFuture;

public class ResetItemCommand implements IModCommand {

    /**
     * Registers this command as a subcommand of the main command
     */
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent, CommandBuildContext buildContext) {
        // reset command setup
        parent.then(Commands.literal("reset")
            .requires(source -> source.hasPermission(2)) // Admin permission level
            
            // Reset held item (no arguments)
            .executes(context -> executeResetHeldItem(context.getSource()))
            
            // Item argument using vanilla item argument type
            .then(Commands.argument("item", ItemArgument.item(buildContext))
                .executes(context -> executeResetItem(
                    context,
                    ItemArgument.getItem(context, "item").getItem())))
                        
            // Alternative with item ID as string for better flexibility
            .then(Commands.argument("itemId", StringArgumentType.string())
                .suggests(ResetItemCommand::suggestItemIds)
                .executes(context -> executeResetItemById(
                    context,
                    StringArgumentType.getString(context, "itemId")))));
    }

    /**
     * Suggests available item IDs for tab completion
     */
    private static CompletableFuture<Suggestions> suggestItemIds(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();
        
        // Get all item IDs from the Forge registry and filter based on input
        return SharedSuggestionProvider.suggest(
            ForgeRegistries.ITEMS.getKeys().stream()
                .map(ResourceLocation::toString)
                .filter(id -> id.toLowerCase().contains(input)),
            builder
        );
    }
    
    /**
     * Execute resetting for the item in the player's main hand
     */
    private static int executeResetHeldItem(CommandSourceStack source) {
        try {
            // Get the player's held item
            ServerPlayer player = source.getPlayerOrException();
            ItemStack heldItem = player.getMainHandItem();
            
            if (heldItem.isEmpty()) {
                source.sendFailure(Component.literal("You must hold an item in your main hand"));
                return 0;
            }
            
            Item item = heldItem.getItem();
            
            // Clear all custom data from the item maps
            RarityManager.clearItemData(item);
            
            // Reset the actual held item
            resetItemStack(heldItem);
            
            // Update the player's inventory to refresh the client
            player.getInventory().setChanged();
            
            // Provide feedback
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            source.sendSuccess(() -> Component.literal("Reset item in hand: ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(itemId.toString())
                    .withStyle(ChatFormatting.YELLOW)), true);
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error resetting held item: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute resetting an item
     */
    private static int executeResetItem(
            CommandContext<CommandSourceStack> context, Item item) {
        
        CommandSourceStack source = context.getSource();
        
        try {
            // Clear all custom data from the item maps
            RarityManager.clearItemData(item);
            
            // Also reset all instances of this item in the player's inventory
            resetAllInstancesInInventory(source, item);
            
            // Provide feedback
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            source.sendSuccess(() -> Component.literal("Reset item: ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(itemId.toString())
                    .withStyle(ChatFormatting.YELLOW)), true);
            
            source.sendSuccess(() -> Component.literal("• Display name reset to default")
                    .withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.literal("• Custom tooltips removed")
                    .withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.literal("• Rarity reset to COMMON")
                    .withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.literal("• Config file deleted")
                    .withStyle(ChatFormatting.GRAY), false);
            
            return 1;
        } catch (Exception e) {
            // Handle any errors
            source.sendFailure(Component.literal("Error resetting item: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute resetting an item using an item ID
     */
    private static int executeResetItemById(
            CommandContext<CommandSourceStack> context, String itemId) {
        
        CommandSourceStack source = context.getSource();
        
        try {
            // Try to get the item from the ID
            ResourceLocation resourceLocation = new ResourceLocation(itemId);
            Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
            
            if (item == null) {
                source.sendFailure(Component.literal("Item not found: " + itemId));
                return 0;
            }
            
            // Delegate to the main handler
            return executeResetItem(context, item);
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Invalid item ID format: " + itemId));
            return 0;
        }
    }
    
    /**
     * Reset all instances of an item in the player's inventory
     */
    private static void resetAllInstancesInInventory(CommandSourceStack source, Item item) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            Inventory inventory = player.getInventory();
            
            // Check main inventory slots
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && stack.getItem() == item) {
                    resetItemStack(stack);
                }
            }
            
            // Also check armor slots and offhand
            for (ItemStack stack : player.getArmorSlots()) {
                if (!stack.isEmpty() && stack.getItem() == item) {
                    resetItemStack(stack);
                }
            }
            
            ItemStack offhandItem = player.getOffhandItem();
            if (!offhandItem.isEmpty() && offhandItem.getItem() == item) {
                resetItemStack(offhandItem);
            }
            
            // Update inventory
            player.getInventory().setChanged();
            
        } catch (Exception e) {
            // Player is not available (command run from console) - just ignore this part
        }
    }
    
    /**
     * Reset a specific ItemStack by removing all custom display properties
     */
    private static void resetItemStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        
        // Reset the hover name to default
        stack.resetHoverName();
        
        // Remove display NBT tags
        if (stack.hasTag()) {
            if (stack.getTag().contains("display")) {
                // Remove custom name and lore specifically
                stack.getTag().getCompound("display").remove("Name");
                stack.getTag().getCompound("display").remove("Lore");
                
                // If display tag is now empty, remove it
                if (stack.getTag().getCompound("display").isEmpty()) {
                    stack.getTag().remove("display");
                }
                
                // If the entire tag is now empty, remove it completely
                if (stack.getTag().isEmpty()) {
                    stack.setTag(null);
                }
            }
        }
    }

    /**
     * Gets the name of this subcommand
     */
    public String getName() {
        return "reset";
    }
    
    /**
     * Gets the description of this subcommand for the help message
     */
    public String getDescription() {
        return "Reset an item to its default state (removes custom name, tooltips, and rarity)";
    }
}