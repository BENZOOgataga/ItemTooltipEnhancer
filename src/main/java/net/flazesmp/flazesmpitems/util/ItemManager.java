package net.flazesmp.flazesmpitems.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager class for handling custom item properties like display names, tooltips, and rarities.
 */
public class ItemManager {
    // Maps to store custom item data
    private static final Map<ResourceLocation, CustomItemData> ITEM_DATA = new HashMap<>();
    
    /**
     * Gets the custom data for an item, creating it if it doesn't exist
     */
    private static CustomItemData getOrCreateData(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return ITEM_DATA.computeIfAbsent(id, key -> new CustomItemData());
    }
    
    /**
     * Sets the display name for an item
     */
    public static void setDisplayName(Item item, String displayName) {
        CustomItemData data = getOrCreateData(item);
        data.displayName = displayName;
        
        // Update the display name color based on rarity if rarity is set
        if (data.rarity != null) {
            data.displayName = applyRarityColor(displayName, data.rarity);
        }
    }
    
    /**
     * Gets the custom display name for an item, or null if not set
     */
    public static String getDisplayName(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        CustomItemData data = ITEM_DATA.get(id);
        return data != null ? data.displayName : null;
    }
    
    /**
     * Sets a tooltip line for an item
     */
    public static void setTooltipLine(Item item, int line, String text) {
        CustomItemData data = getOrCreateData(item);
        
        // Ensure tooltip list is large enough
        while (data.tooltipLines.size() < line) {
            data.tooltipLines.add("");
        }
        
        // Set the tooltip line (line - 1 because we store zero-indexed but user provides 1-indexed)
        data.tooltipLines.set(line - 1, text);
    }
    
    /**
     * Gets all tooltip lines for an item
     */
    public static List<String> getTooltipLines(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        CustomItemData data = ITEM_DATA.get(id);
        return data != null ? new ArrayList<>(data.tooltipLines) : List.of();
    }
    
    /**
     * Gets the number of tooltip lines for an item
     */
    public static int getTooltipLineCount(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        CustomItemData data = ITEM_DATA.get(id);
        return data != null ? data.tooltipLines.size() : 0;
    }
    
    /**
     * Sets the rarity for an item
     */
    public static void setRarity(Item item, ItemRarity rarity) {
        CustomItemData data = getOrCreateData(item);
        data.rarity = rarity;
        
        // Update display name color if a custom name is set
        if (data.displayName != null && !data.displayName.isEmpty()) {
            // Strip any existing color codes from the start of the name
            String plainName = stripLeadingColorCodes(data.displayName);
            data.displayName = applyRarityColor(plainName, rarity);
        }
    }
    
    /**
     * Gets the rarity for an item
     */
    public static ItemRarity getRarity(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        CustomItemData data = ITEM_DATA.get(id);
        return data != null ? data.rarity : ItemRarity.COMMON; // Default to COMMON
    }
    
    /**
     * Applies the appropriate color code for a rarity to the given text
     */
    private static String applyRarityColor(String text, ItemRarity rarity) {
        // Strip any existing color codes at the beginning
        String plainText = stripLeadingColorCodes(text);
        
        // Apply the rarity color code
        return getRarityColor(rarity) + plainText;
    }
    
    /**
     * Gets the color code for a rarity
     */
    private static String getRarityColor(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> "§7"; // Gray
            case UNCOMMON -> "§a"; // Green
            case RARE -> "§9"; // Blue
            case EPIC -> "§5"; // Purple
            case LEGENDARY -> "§6"; // Gold
            case SPECIAL -> "§c"; // Red
            case MYTHIC -> "§d"; // Light Purple
            case ADMIN -> "§c"; // Red
        };
    }
    
    /**
     * Strips color codes from the beginning of a string
     */
    private static String stripLeadingColorCodes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        int start = 0;
        while (start < text.length() - 1) {
            if (text.charAt(start) == '§') {
                start += 2; // Skip the § and the color character
            } else {
                break;
            }
        }
        return text.substring(start);
    }
    
    /**
     * Applies custom item data to an item stack for display
     */
    public static void applyDataToItemStack(ItemStack stack) {
        Item item = stack.getItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        CustomItemData data = ITEM_DATA.get(id);
        
        if (data == null) {
            return;
        }
        
        // Apply custom display name if set
        if (data.displayName != null && !data.displayName.isEmpty()) {
            stack.setHoverName(Component.literal(data.displayName));
        }
        
        // Apply tooltip lines if any exist
        if (!data.tooltipLines.isEmpty()) {
            // Get or create the display tag
            CompoundTag tag = stack.getOrCreateTag();
            CompoundTag display = tag.contains("display") ? 
                tag.getCompound("display") : new CompoundTag();
                
            // Create lore list
            ListTag lore = new ListTag();
            for (String line : data.tooltipLines) {
                if (line != null && !line.isEmpty()) {
                    // Use directly StringTag without trying to get JSON
                    // This creates a text component in the lore format Minecraft expects
                    lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
                }
            }
            
            // Set lore and add display tag to the item
            if (!lore.isEmpty()) {
                display.put("Lore", lore);
                tag.put("display", display);
            }
        }
    }
    
    /**
     * Data class to hold custom properties for an item
     */
    private static class CustomItemData {
        String displayName;
        List<String> tooltipLines = new ArrayList<>();
        ItemRarity rarity = ItemRarity.COMMON;
    }
}