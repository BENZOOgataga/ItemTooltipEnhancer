package net.flazesmp.flazesmpitems.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.command.IModCommand;
import net.flazesmp.flazesmpitems.config.ConfigManager;
import net.flazesmp.flazesmpitems.config.MessageConfig;
import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Command to dump all registered items to a text file
 */
public class DumpItemsCommand implements IModCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DumpItemsCommand.class);
    private static final String DUMPS_DIR = "itemtooltipenhancer/dumps";

    /**
     * Registers this command as a subcommand of the main command
     */
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent, CommandBuildContext buildContext) {
        // dumpitems command setup
        parent.then(Commands.literal("dumpitems")
            .requires(source -> source.hasPermission(2)) // Admin permission level
            .executes(DumpItemsCommand::executeDump));
    }
    
    /**
     * Execute dumping all items to a file
     */
    private static int executeDump(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            source.sendSuccess(() -> Component.literal(
                    MessageConfig.getMessage("command.dump.collecting"))
                    .withStyle(ChatFormatting.YELLOW), true);
            
            // Ensure dumps directory exists
            Path dumpsDir = FMLPaths.GAMEDIR.get().resolve(DUMPS_DIR);
            Files.createDirectories(dumpsDir);
            
            // Create timestamp for the file name
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String fileName = "items_dump_" + timestamp + ".txt";
            Path filePath = dumpsDir.resolve(fileName);
            
            int totalItems = 0;
            final int[] processedItemsArray = new int[1];
            
            // Count total items first (for progress reporting)
            for (ResourceLocation ignored : ForgeRegistries.ITEMS.getKeys()) {
                totalItems++;
            }
            
            // Write to file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
                // Write header
                writer.write("ItemTooltipEnhancer Item Dump");
                writer.newLine();
                writer.write("Generated: " + new Date().toString());
                writer.newLine();
                writer.write("Total items: " + totalItems);
                writer.newLine();
                writer.newLine();
                writer.write(String.format("%-50s | %-15s | %s", "Item ID", "Rarity", "Has Explicit Customizations"));
                writer.newLine();
                writer.write("-".repeat(100));
                writer.newLine();
                
                String currentNamespace = null;
                
                // Process items in batches sorted by namespace
                List<ResourceLocation> itemIds = new ArrayList<>(ForgeRegistries.ITEMS.getKeys());
                itemIds.sort(Comparator.comparing(ResourceLocation::toString));
                
                for (ResourceLocation id : itemIds) {
                    try {
                        Item item = ForgeRegistries.ITEMS.getValue(id);
                        if (item == null) continue;
                        
                        // Get namespace for grouping
                        String namespace = id.getNamespace();
                        if (!namespace.equals(currentNamespace)) {
                            writer.newLine();
                            writer.write("=== " + namespace.toUpperCase() + " ===");
                            writer.newLine();
                            currentNamespace = namespace;
                        }
                        
                        // Try to get rarity safely
                        ItemRarity rarity;
                        try {
                            rarity = RarityManager.getRarity(item);
                        } catch (Exception e) {
                            // If rarity determination fails, use COMMON as default
                            rarity = ItemRarity.COMMON;
                            LOGGER.debug("Failed to determine rarity for item {}: {}", id, e.getMessage());
                        }
                        
                        // Check if item has EXPLICIT customizations (from config file, not automatic)
                        boolean hasExplicitCustomizations = ConfigManager.hasCustomItemData(id);
                        
                        writer.write(String.format("%-50s | %-15s | %s", 
                                id.toString(),
                                rarity.getName(),
                                hasExplicitCustomizations ? "Yes" : "No"));
                        writer.newLine();
                        
                        processedItemsArray[0]++;
                    } catch (Exception e) {
                        // Handle individual item errors gracefully
                        LOGGER.error("Error processing item {}: {}", id, e.getMessage());
                        writer.write(String.format("%-50s | %-15s | %s", 
                                id.toString(), 
                                "ERROR", 
                                "Failed to process"));
                        writer.newLine();
                    }
                }
                
                // Write summary at the end
                writer.newLine();
                writer.write("-".repeat(100));
                writer.newLine();
                writer.write("Successfully processed " + processedItemsArray[0] + " of " + totalItems + " items");
                if (processedItemsArray[0] < totalItems) {
                    writer.write(" (Some items may have been skipped due to errors)");
                }
            }
            
            // Provide feedback
            source.sendSuccess(() -> Component.literal(
                    MessageConfig.getMessage("command.dump.success", processedItemsArray[0]))
                    .withStyle(ChatFormatting.GREEN), true);
            source.sendSuccess(() -> Component.literal(filePath.toString())
                    .withStyle(ChatFormatting.YELLOW), false);
            
            return 1;
        } catch (IOException e) {
            LOGGER.error("Failed to dump items to file", e);
            source.sendFailure(Component.literal(
                    MessageConfig.getMessage("command.dump.error", e.getMessage()))
                    .withStyle(ChatFormatting.RED));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected error during item dump", e);
            source.sendFailure(Component.literal(
                    MessageConfig.getMessage("command.dump.error", e.getMessage()))
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    /**
     * Gets the name of this subcommand
     */
    public static String getName() {
        return "dumpitems";
    }
    
    /**
     * Gets the description of this subcommand for the help message
     */
    public static String getDescription() {
        return "Dump all registered items to a text file";
    }
}