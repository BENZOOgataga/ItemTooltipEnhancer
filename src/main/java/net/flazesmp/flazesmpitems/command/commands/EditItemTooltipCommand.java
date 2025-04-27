package net.flazesmp.flazesmpitems.command.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EditItemTooltipCommand implements IModCommand {

    /**
     * Registers this command as a subcommand of the main command
     *
     * @param parent The parent command builder to attach this subcommand to
     * @param buildContext The command build context
     */
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent, CommandBuildContext buildContext) {
        // edittooltip command setup
        parent.then(Commands.literal("edittooltip")
            .requires(source -> source.hasPermission(2)) // Admin permission level
            
            // Item argument using vanilla item argument type
            .then(Commands.argument("item", ItemArgument.item(buildContext))
                .then(Commands.argument("line", IntegerArgumentType.integer(1))
                    .suggests(EditItemTooltipCommand::suggestTooltipLines)
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(context -> executeSetTooltip(
                            context,
                            ItemArgument.getItem(context, "item").getItem(),
                            IntegerArgumentType.getInteger(context, "line"),
                            StringArgumentType.getString(context, "text"))))))
                            
            // Alternative with item ID as string for better flexibility
            .then(Commands.argument("itemId", StringArgumentType.string())
                .suggests(EditItemTooltipCommand::suggestItemIds)
                .then(Commands.argument("line", IntegerArgumentType.integer(1))
                    .suggests(EditItemTooltipCommand::suggestTooltipLines)
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(context -> executeSetTooltipById(
                            context,
                            StringArgumentType.getString(context, "itemId"),
                            IntegerArgumentType.getInteger(context, "line"),
                            StringArgumentType.getString(context, "text")))))));
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
     * Suggests tooltip line numbers (1-5 by default)
     */
    private static CompletableFuture<Suggestions> suggestTooltipLines(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        // Default to suggesting lines 1-5
        return SharedSuggestionProvider.suggest(
            List.of("1", "2", "3", "4", "5"),
            builder
        );
    }
    
    /**
     * Execute setting a tooltip line for an item
     */
    private static int executeSetTooltip(
            CommandContext<CommandSourceStack> context, Item item, int line, String text) {
        
        CommandSourceStack source = context.getSource();
        
        try {
            // Process color codes
            String formattedText = formatTextWithColorCodes(text);
            
            // Update the tooltip line
            RarityManager.setTooltipLine(item, line, formattedText);
            
            // Confirm to the user
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            source.sendSuccess(() -> Component.literal("Updated tooltip line " + line + " for " + itemId + " to: ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(formattedText)), true);
            
            return 1;
        } catch (Exception e) {
            // Handle any errors
            source.sendFailure(Component.literal("Error setting tooltip: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute setting a tooltip line using an item ID
     */
    private static int executeSetTooltipById(
            CommandContext<CommandSourceStack> context, String itemId, int line, String text) {
        
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
            return executeSetTooltip(context, item, line, text);
            
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
        return "edittooltip";
    }
    
    /**
     * Gets the description of this subcommand for the help message
     */
    public static String getDescription() {
        return "Set a tooltip line for an item";
    }
}