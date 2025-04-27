package net.flazesmp.flazesmpitems.event;

import net.flazesmp.flazesmpitems.tooltip.SpecialItemTooltipHandler;
import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.tooltip.StatTooltipFormatter;
import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = FlazeSMPItems.MOD_ID)
public class ItemTooltipEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemTooltipEventHandler.class);
    
    // Common vanilla category names
    private static final Set<String> VANILLA_CATEGORIES = new HashSet<>(Arrays.asList(
        "tools & utilities", 
        "combat",
        "building blocks", 
        "decoration blocks", 
        "redstone & logic",
        "food & drinks", 
        "ingredients",
        "spawn eggs", 
        "ores & resources", 
        "miscellaneous",
        "redstone",
        "transportation"
    ));

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        List<Component> tooltip = event.getToolTip();
        
        if (tooltip == null || tooltip.isEmpty()) {
            return;
        }
        
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        if (itemId == null) return;
        
        // Apply custom data to the stack
        RarityManager.applyCustomDataToItemStack(stack);
        
        // Process the tooltip with our stat formatter (do this first)
        boolean statsChanged = StatTooltipFormatter.processTooltip(tooltip, stack);
        
        // Remove vanilla categories and mod names
        removeUnwantedTooltipLines(tooltip);

        // Get custom tooltips
        Map<Integer, String> customTooltips = RarityManager.getTooltipLines(item);
        
        // Add custom tooltips with spacing if they exist
        if (customTooltips != null && !customTooltips.isEmpty()) {
            // Add a blank line before custom tooltips section
            tooltip.add(Component.literal(""));
            
            // Add all custom tooltips in order
            for (int i = 1; i <= customTooltips.size(); i++) {
                String tooltipText = customTooltips.get(i);
                if (tooltipText != null && !tooltipText.isEmpty()) {
                    tooltip.add(Component.literal(tooltipText));
                }
            }
            
            // Add a blank line after custom tooltips section
            tooltip.add(Component.literal(""));
        }
        
        // Handle special items like potions and music discs
        if (SpecialItemTooltipHandler.handleSpecialItem(tooltip, stack)) {
            // If it was a special item, still apply rarity and category at the bottom
            ItemRarity rarity = RarityManager.getRarity(item);
            String category = RarityManager.getItemCategory(item);
            String itemTypeSuffix = RarityManager.getItemTypeSuffix(item);
            
            // Create rarity component with suffix if available
            Component rarityLine;
            if (itemTypeSuffix != null && !itemTypeSuffix.isEmpty()) {
                rarityLine = Component.literal(rarity.getName().toUpperCase() + " " + itemTypeSuffix)
                    .withStyle(Style.EMPTY.withColor(rarity.getColor().getColor()).withBold(true));
            } else {
                rarityLine = Component.literal(rarity.getName().toUpperCase())
                    .withStyle(Style.EMPTY.withColor(rarity.getColor().getColor()).withBold(true));
            }
            
            // If category exists, show it above rarity at the bottom
            if (category != null && !category.isEmpty()) {
                Component categoryLine = Component.literal(category)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY));
                tooltip.add(categoryLine);
            }
            
            // Add rarity as the last element
            tooltip.add(rarityLine);
            
            return; // Skip regular tooltip processing
        }
        
        // Get rarity from RarityManager
        ItemRarity rarity = RarityManager.getRarity(item);
        
        // Get item category
        String category = RarityManager.getItemCategory(item);
        
        // Get the item type suffix (use getItemTypeSuffix instead of determineItemTypeSuffix)
        String itemTypeSuffix = RarityManager.getItemTypeSuffix(item);
        
        // Create rarity component with suffix if available
        Component rarityLine;
        if (itemTypeSuffix != null && !itemTypeSuffix.isEmpty()) {
            rarityLine = Component.literal(rarity.getName().toUpperCase() + " " + itemTypeSuffix)
                .withStyle(Style.EMPTY.withColor(rarity.getColor().getColor()).withBold(true));
        } else {
            rarityLine = Component.literal(rarity.getName().toUpperCase())
                .withStyle(Style.EMPTY.withColor(rarity.getColor().getColor()).withBold(true));
        }
        
        // Check if rarity is already shown (from previous runs)
        // We need to update this check to account for the suffix
        boolean hasRarityLine = tooltip.stream().anyMatch(line -> {
            if (line.getStyle().getColor() != null && 
                line.getStyle().getColor().getValue() == rarity.getColor().getColor() &&
                line.getStyle().isBold()) {
                
                String lineText = line.getString();
                String rarityPrefix = rarity.getName().toUpperCase();
                // Check if line starts with the rarity name
                return lineText.startsWith(rarityPrefix);
            }
            return false;
        });
        
        // If rarity not already in tooltip, add to the bottom
        if (!hasRarityLine) {
            // If category exists, show it above rarity at the bottom
            if (category != null && !category.isEmpty()) {
                Component categoryLine = Component.literal(category)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY));
                tooltip.add(categoryLine);
            }
            
            // Add rarity as the last element
            tooltip.add(rarityLine);
        }
    }
    
    /**
     * Simple method to remove unwanted tooltip lines like mod names and vanilla categories
     */
    private static void removeUnwantedTooltipLines(List<Component> tooltip) {
        if (tooltip == null || tooltip.isEmpty()) {
            return;
        }
        
        // Process the tooltip from the end since mod names are often at the end
        Iterator<Component> iterator = tooltip.iterator();
        while (iterator.hasNext()) {
            Component line = iterator.next();
            if (line == null) continue;
            
            String text = line.getString().trim().toLowerCase();
            if (text.isEmpty()) continue;
            
            // Remove vanilla categories
            if (VANILLA_CATEGORIES.contains(text)) {
                iterator.remove();
                continue;
            }
            
            // Remove mod names - they're typically in blue or gray color
            Style style = line.getStyle();
            if (style != null && style.getColor() != null) {
                int color = style.getColor().getValue();
                
                // Blue, aqua, or gray colors are typically used for mod names
                if ((color == ChatFormatting.BLUE.getColor() || 
                     color == ChatFormatting.AQUA.getColor() ||
                     color == ChatFormatting.GRAY.getColor()) && 
                    text.length() < 30) {
                    
                    // Additional check for common mod name content
                    if (text.contains("minecraft") || 
                        text.contains("forge") || 
                        text.contains("mod")) {
                        iterator.remove();
                    }
                }
            }
        }
    }
}