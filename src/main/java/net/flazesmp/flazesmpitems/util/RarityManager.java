package net.flazesmp.flazesmpitems.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.flazesmp.flazesmpitems.FlazeSMPItems;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages item rarities, custom names, tooltips and categories
 */
@Mod.EventBusSubscriber(modid = FlazeSMPItems.MOD_ID)
public class RarityManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RarityManager.class);
    
    // Maps to track item data
    private static final Map<ResourceLocation, ItemRarity> ITEM_RARITIES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, String> ITEM_CATEGORIES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, String> CUSTOM_NAMES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Map<Integer, String>> TOOLTIPS = new ConcurrentHashMap<>();

    // Default rarity
    private static final ItemRarity DEFAULT_RARITY = ItemRarity.COMMON;
    
    /**
     * Initialize the RarityManager - called during mod startup
     */
    public static void initialize() {
        LOGGER.info("Initializing RarityManager");
        // Load data from config/storage if needed
        loadData();
    }
    
    /**
     * Sets the rarity for an item
     * 
     * @param item The item
     * @param rarity The rarity to set
     */
    public static void setRarity(Item item, ItemRarity rarity) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        ITEM_RARITIES.put(id, rarity);
        
        // Update custom name color if needed
        String customName = getCustomName(item);
        if (customName != null && !customName.isEmpty()) {
            // Strip any existing color codes from the name
            customName = stripLeadingColorCode(customName);
            // Apply new rarity color
            setCustomName(item, applyRarityColor(customName, rarity));
        }
    }
    
    /**
     * Gets the rarity for an item
     * 
     * @param item The item
     * @return The item's rarity, or the default rarity if not set
     */
    public static ItemRarity getRarity(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return ITEM_RARITIES.getOrDefault(id, DEFAULT_RARITY);
    }
    
    /**
     * Sets the category for an item
     * 
     * @param item The item
     * @param category The category to set
     */
    public static void setItemCategory(Item item, String category) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        ITEM_CATEGORIES.put(id, category);
    }
    
    /**
     * Gets the category for an item
     * 
     * @param item The item
     * @return The item's category, or null if not set
     */
    public static String getItemCategory(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return ITEM_CATEGORIES.get(id);
    }
    
    /**
     * Sets a custom display name for an item
     * 
     * @param item The item
     * @param name The custom name to set
     */
    public static void setCustomName(Item item, String name) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        
        // Apply color based on rarity if name doesn't already have a color code
        if (name != null && !name.isEmpty() && !name.startsWith("§")) {
            ItemRarity rarity = getRarity(item);
            name = applyRarityColor(name, rarity);
        }
        
        CUSTOM_NAMES.put(id, name);
    }
    
    /**
     * Gets the custom display name for an item
     * 
     * @param item The item
     * @return The custom name, or null if not set
     */
    public static String getCustomName(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return CUSTOM_NAMES.get(id);
    }
    
    /**
     * Sets a tooltip line for an item
     * 
     * @param item The item
     * @param line The line number (1-based)
     * @param text The tooltip text
     */
    public static void setTooltipLine(Item item, int line, String text) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        Map<Integer, String> itemTooltips = TOOLTIPS.computeIfAbsent(id, k -> new HashMap<>());
        itemTooltips.put(line, text);
    }
    
    /**
     * Gets all tooltip lines for an item
     * 
     * @param item The item
     * @return Map of line numbers to tooltip text
     */
    public static Map<Integer, String> getTooltipLines(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return TOOLTIPS.getOrDefault(id, Collections.emptyMap());
    }
    
    /**
     * Applies the appropriate rarity color to text
     */
    private static String applyRarityColor(String text, ItemRarity rarity) {
        // Strip any existing color codes
        text = stripLeadingColorCode(text);
        
        // Apply the rarity color code (section symbol + color code)
        return "§" + rarity.getColor().getChar() + text;
    }
    
    /**
     * Removes color codes from the beginning of a string
     */
    private static String stripLeadingColorCode(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // Remove section symbol + color char if present at the start
        if (text.length() >= 2 && text.charAt(0) == '§') {
            return text.substring(2);
        }
        
        return text;
    }
    
    /**
     * Removes a tooltip line
     * 
     * @param item The item
     * @param line The line number to remove
     */
    public static void removeTooltipLine(Item item, int line) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        Map<Integer, String> itemTooltips = TOOLTIPS.get(id);
        if (itemTooltips != null) {
            itemTooltips.remove(line);
            if (itemTooltips.isEmpty()) {
                TOOLTIPS.remove(id);
            }
        }
    }
    
    /**
     * Clear all data for an item
     * 
     * @param item The item to clear data for
     */
    public static void clearItemData(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        ITEM_RARITIES.remove(id);
        ITEM_CATEGORIES.remove(id);
        CUSTOM_NAMES.remove(id);
        TOOLTIPS.remove(id);
        
        LOGGER.info("Cleared all custom data for item: {}", id);
    }
    
    /**
     * Apply custom data to an ItemStack
     * 
     * @param stack The ItemStack to modify
     */
    public static void applyCustomDataToItemStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        
        Item item = stack.getItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        
        // Apply custom name if set
        String customName = CUSTOM_NAMES.get(id);
        if (customName != null && !customName.isEmpty()) {
            stack.setHoverName(Component.literal(customName));
        }
        
        // Apply tooltips if set
        Map<Integer, String> tooltipLines = TOOLTIPS.get(id);
        if (tooltipLines != null && !tooltipLines.isEmpty()) {
            CompoundTag tag = stack.getOrCreateTag();
            CompoundTag display = tag.contains("display") ? 
                tag.getCompound("display") : new CompoundTag();
                
            // Create lore list
            ListTag lore = new ListTag();
            TreeMap<Integer, String> sortedTooltips = new TreeMap<>(tooltipLines);
            
            for (Map.Entry<Integer, String> entry : sortedTooltips.entrySet()) {
                String line = entry.getValue();
                if (line != null && !line.isEmpty()) {
                    lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
                }
            }
            
            if (!lore.isEmpty()) {
                display.put("Lore", lore);
                tag.put("display", display);
            }
        }
    }

    /**
     * Reset an ItemStack to its default state
     * 
     * @param stack The ItemStack to reset
     */
    public static void resetItemStack(ItemStack stack) {
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
     * Event handler to apply custom display names to items
     */
    @SubscribeEvent
    public static void onItemNameDisplay(PlayerEvent.ItemPickupEvent event) {
        applyCustomDataToItemStack(event.getStack());
    }
    
    /**
     * Event handler to apply custom display names to items when crafting
     */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        applyCustomDataToItemStack(event.getCrafting());
    }
    
    /**
     * Save all data to a config file
     */
    public static void saveData() {
        LOGGER.info("Saving RarityManager data");
        // To be implemented based on your storage preferences
    }
    
    /**
     * Load data from a config file
     */
    public static void loadData() {
        LOGGER.info("Loading RarityManager data");
        // To be implemented based on your storage preferences
    }
}