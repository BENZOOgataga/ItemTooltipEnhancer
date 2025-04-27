package net.flazesmp.flazesmpitems.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles configuration for custom items created or modified by the mod
 */
public class CustomItemsConfig {
    private static final String CONFIG_FILE = "config/itemtooltipenhancer/custom_items.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static Map<ResourceLocation, CustomItemData> customItems = new HashMap<>();
    private static boolean hasChanges = false;
    
    /**
     * Load the custom items configuration
     */
    public static void load() {
        File configFile = new File(CONFIG_FILE);
        File configDir = configFile.getParentFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                Type type = new TypeToken<Map<String, CustomItemData>>() {}.getType();
                Map<String, CustomItemData> loadedItems = GSON.fromJson(reader, type);
                
                customItems.clear();
                if (loadedItems != null) {
                    loadedItems.forEach((key, value) -> {
                        try {
                            ResourceLocation itemId = new ResourceLocation(key);
                            customItems.put(itemId, value);
                        } catch (Exception e) {
                            FlazeSMPItems.LOGGER.error("Failed to parse item ID: " + key, e);
                        }
                    });
                }
                
                FlazeSMPItems.LOGGER.info("Loaded {} custom items from config", customItems.size());
            } catch (JsonSyntaxException e) {
                FlazeSMPItems.LOGGER.error("Error parsing custom items config", e);
            } catch (IOException e) {
                FlazeSMPItems.LOGGER.error("Error reading custom items config", e);
            }
        }
        
        hasChanges = false;
        applyCustomItemsToRarityManager();
    }
    
    /**
     * Apply custom item configurations to the RarityManager
     */
    private static void applyCustomItemsToRarityManager() {
        for (Map.Entry<ResourceLocation, CustomItemData> entry : customItems.entrySet()) {
            Item item = ForgeRegistries.ITEMS.getValue(entry.getKey());
            if (item != null) {
                CustomItemData data = entry.getValue();
                net.flazesmp.flazesmpitems.util.RarityManager.forceRarity(item, data.rarity);
            }
        }
    }
    
    /**
     * Save the custom items configuration
     */
    public static void save() {
        if (!hasChanges) {
            return;
        }
        
        File configFile = new File(CONFIG_FILE);
        File configDir = configFile.getParentFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        Map<String, CustomItemData> saveData = new HashMap<>();
        customItems.forEach((itemId, data) -> saveData.put(itemId.toString(), data));
        
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(saveData, writer);
            FlazeSMPItems.LOGGER.info("Saved {} custom items to config", customItems.size());
            hasChanges = false;
        } catch (IOException e) {
            FlazeSMPItems.LOGGER.error("Error saving custom items config", e);
        }
    }
    
    /**
     * Get custom data for an item
     * @param item The item to get data for
     * @return The custom item data, or null if none exists
     */
    public static CustomItemData getCustomItemData(Item item) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        return customItems.get(itemId);
    }
    
    /**
     * Set custom data for an item
     * @param item The item to set data for
     * @param data The custom item data
     */
    public static void setCustomItemData(Item item, CustomItemData data) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        if (itemId != null) {
            customItems.put(itemId, data);
            hasChanges = true;
        }
    }
    
    /**
     * Remove custom data for an item
     * @param item The item to remove data for
     * @return True if data was removed, false otherwise
     */
    public static boolean removeCustomItemData(Item item) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        if (itemId != null && customItems.containsKey(itemId)) {
            customItems.remove(itemId);
            hasChanges = true;
            return true;
        }
        return false;
    }
    
    /**
     * Check if an item has custom data
     * @param item The item to check
     * @return True if the item has custom data
     */
    public static boolean hasCustomItemData(Item item) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        return itemId != null && customItems.containsKey(itemId);
    }
    
    /**
     * Get all custom items
     * @return A map of item IDs to custom item data
     */
    public static Map<ResourceLocation, CustomItemData> getAllCustomItems() {
        return new HashMap<>(customItems);
    }
    
    /**
     * Class to hold custom item data
     */
    public static class CustomItemData {
        public String displayName;
        public List<String> tooltipLines;
        public ItemRarity rarity;
        public boolean isModItem;
        
        public CustomItemData() {
            this.displayName = "";
            this.tooltipLines = new ArrayList<>();
            this.rarity = ItemRarity.COMMON;
            this.isModItem = false;
        }
        
        public CustomItemData(String displayName, List<String> tooltipLines, ItemRarity rarity, boolean isModItem) {
            this.displayName = displayName;
            this.tooltipLines = tooltipLines != null ? new ArrayList<>(tooltipLines) : new ArrayList<>();
            this.rarity = rarity;
            this.isModItem = isModItem;
        }
    }
}