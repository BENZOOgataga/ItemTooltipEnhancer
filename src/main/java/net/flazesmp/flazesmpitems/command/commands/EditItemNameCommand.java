package net.flazesmp.flazesmpitems.command.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.flazesmp.flazesmpitems.command.IModCommand;
import net.flazesmp.flazesmpitems.config.ConfigManager;
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

public class EditItemNameCommand implements IModCommand {

    /**
     * Registers this command as a subcommand of the main command
     *
     * @param parent The parent command builder to attach this subcommand to
     * @param buildContext The command build context
     */
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent, CommandBuildContext buildContext) {
        // editdisplayname command setup
        parent.then(Commands.literal("editdisplayname")
            .requires(source -> source.hasPermission(2)) // Admin permission level
            
            // Item argument using vanilla item argument type
            .then(Commands.argument("item", ItemArgument.item(buildContext))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .executes(context -> executeSetDisplayName(
                        context,
                        ItemArgument.getItem(context, "item").getItem(),
                        StringArgumentType.getString(context, "name")))))
                        
            // Alternative with item ID as string for better flexibility
            .then(Commands.argument("itemId", StringArgumentType.string())
                .suggests(EditItemNameCommand::suggestItemIds)
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .executes(context -> executeSetDisplayNameById(
                        context,
                        StringArgumentType.getString(context, "itemId"),
                        StringArgumentType.getString(context, "name"))))));
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
     * Execute setting a new display name for an item
     */
    private static int executeSetDisplayName(
            CommandContext<CommandSourceStack> context, Item item, String name) {
        
        CommandSourceStack source = context.getSource();
        
        try {
            // Replace ampersand (&) color codes with § symbols
            String formattedName = formatTextWithColorCodes(name);
            
            // Update the item's display name in RarityManager
            RarityManager.setCustomName(item, formattedName);
            
            // Confirm to the user
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            source.sendSuccess(() -> Component.literal("Updated display name for " + itemId + " to: ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(formattedName)), true);
            
            return 1;
        } catch (Exception e) {
            // Handle any errors
            source.sendFailure(Component.literal("Error setting display name: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute setting a display name using an item ID
     */
    private static int executeSetDisplayNameById(
            CommandContext<CommandSourceStack> context, String itemId, String name) {
        
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
            return executeSetDisplayName(context, item, name);
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Invalid item ID format: " + itemId));
            return 0;
        }
    }
    
    /**
     * Replace ampersand color codes with § symbols
     */
    private static String formatTextWithColorCodes(String text) {
        return text.replace('&', '§');
    }

    /**
     * Gets the name of this subcommand
     */
    public static String getName() {
        return "editdisplayname";
    }
    
    /**
     * Gets the description of this subcommand for the help message
     */
    public static String getDescription() {
        return "Set the display name for an item";
    }
}