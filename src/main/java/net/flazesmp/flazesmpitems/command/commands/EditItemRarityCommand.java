package net.flazesmp.flazesmpitems.command.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.flazesmp.flazesmpitems.command.IModCommand;
import net.flazesmp.flazesmpitems.config.ConfigManager;
import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class EditItemRarityCommand implements IModCommand {

    /**
     * Registers this command as a subcommand of the main command
     *
     * @param parent The parent command builder to attach this subcommand to
     * @param buildContext The command build context
     */
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent, CommandBuildContext buildContext) {
        // editrarity command setup
        parent.then(Commands.literal("editrarity")
            .requires(source -> source.hasPermission(2)) // Admin permission level
            
            // Item argument using vanilla item argument type
            .then(Commands.argument("item", ItemArgument.item(buildContext))
                .then(Commands.argument("rarity", StringArgumentType.word())
                    .suggests(EditItemRarityCommand::suggestRarityValues)
                    .executes(context -> executeSetRarity(
                        context,
                        ItemArgument.getItem(context, "item").getItem(),
                        StringArgumentType.getString(context, "rarity")))))
                        
            // Alternative with item ID as string for better flexibility
            .then(Commands.argument("itemId", StringArgumentType.string())
                .suggests(EditItemRarityCommand::suggestItemIds)
                .then(Commands.argument("rarity", StringArgumentType.word())
                    .suggests(EditItemRarityCommand::suggestRarityValues)
                    .executes(context -> executeSetRarityById(
                        context,
                        StringArgumentType.getString(context, "itemId"),
                        StringArgumentType.getString(context, "rarity"))))));
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
     * Suggests available rarity values for tab completion
     */
    private static CompletableFuture<Suggestions> suggestRarityValues(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
            Stream.of(ItemRarity.values())
                .map(rarity -> rarity.name().toLowerCase()),
            builder
        );
    }
    
    /**
     * Execute setting a new rarity for an item
     */
    private static int executeSetRarity(
            CommandContext<CommandSourceStack> context, Item item, String rarityName) {
        
        CommandSourceStack source = context.getSource();
        
        try {
            // Try to parse the rarity
            ItemRarity rarity = parseRarity(rarityName);
            
            if (rarity == null) {
                source.sendFailure(Component.literal("Invalid rarity: " + rarityName));
                source.sendFailure(Component.literal("Valid rarities: " + 
                        Stream.of(ItemRarity.values())
                            .map(r -> r.name().toLowerCase())
                            .toList()));
                return 0;
            }
            
            // Update the item's rarity
            RarityManager.setRarity(item, rarity);
            
            // Confirm to the user
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            source.sendSuccess(() -> Component.literal("Updated rarity for " + itemId + " to: " + rarity.getName())
                    .withStyle(style -> style.withColor(rarity.getColor().getColor())), true);
            
            return 1;
        } catch (Exception e) {
            // Handle any errors
            source.sendFailure(Component.literal("Error setting rarity: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute setting a rarity using an item ID
     */
    private static int executeSetRarityById(
            CommandContext<CommandSourceStack> context, String itemId, String rarityName) {
        
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
            return executeSetRarity(context, item, rarityName);
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Invalid item ID format: " + itemId));
            return 0;
        }
    }
    
    /**
     * Parse a rarity string into an ItemRarity enum
     */
    private static ItemRarity parseRarity(String rarityName) {
        try {
            return ItemRarity.valueOf(rarityName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Gets the name of this subcommand
     */
    public static String getName() {
        return "editrarity";
    }
    
    /**
     * Gets the description of this subcommand for the help message
     */
    public static String getDescription() {
        return "Set the rarity for an item";
    }
}