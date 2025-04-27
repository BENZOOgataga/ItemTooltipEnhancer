package net.flazesmp.flazesmpitems.item;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.item.custom.CustomItem;
import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = FlazeSMPItems.MOD_ID)
public class CustomItemManager {
    private static final Map<ResourceLocation, CustomItemData> CUSTOM_ITEMS = new HashMap<>();
    private static final String SAVE_DIR = "config/" + FlazeSMPItems.MOD_ID + "/custom_items/";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public static void initialize() {
        // Create save directory if it doesn't exist
        File saveDir = new File(SAVE_DIR);
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        
        // Load custom items
        loadItems();
    }
    
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Make sure custom items are loaded
        initialize();
    }
    
    public static void createItem(String itemId, String displayName, String texturePath, 
                                ItemRarity rarity, List<String> tooltipLines) {
        ResourceLocation id = validateAndNormalizeItemId(itemId);
        CustomItemData itemData = new CustomItemData(id, displayName, texturePath, rarity, tooltipLines);
        
        CUSTOM_ITEMS.put(id, itemData);
        saveItem(itemData);
    }
    
    public static void updateItem(String itemId, String displayName, String texturePath, 
                                ItemRarity rarity, List<String> tooltipLines) {
        ResourceLocation id = new ResourceLocation(itemId);
        CustomItemData itemData = new CustomItemData(id, displayName, texturePath, rarity, tooltipLines);
        
        CUSTOM_ITEMS.put(id, itemData);
        saveItem(itemData);
        
        // Update rarity for existing item
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item != null) {
            RarityManager.forceRarity(item, rarity);
        }
    }
    
    public static void deleteItem(ResourceLocation id) {
        if (CUSTOM_ITEMS.containsKey(id)) {
            CUSTOM_ITEMS.remove(id);
            
            // Delete the item's JSON file
            File itemFile = new File(SAVE_DIR + id.getNamespace() + "/" + id.getPath() + ".json");
            if (itemFile.exists()) {
                itemFile.delete();
            }
        }
    }
    
    private static ResourceLocation validateAndNormalizeItemId(String itemId) {
        // If no namespace, add mod ID namespace
        if (!itemId.contains(":")) {
            itemId = FlazeSMPItems.MOD_ID + ":" + itemId;
        }
        
        ResourceLocation id = new ResourceLocation(itemId);
        
        // Check if this item already exists
        Item existingItem = ForgeRegistries.ITEMS.getValue(id);
        if (existingItem != null) {
            throw new IllegalArgumentException("An item with ID " + id + " already exists");
        }
        
        return id;
    }
    
    private static void loadItems() {
        File saveDir = new File(SAVE_DIR);
        
        // Recursively scan all subdirectories
        loadItemsFromDirectory(saveDir);
        
        FlazeSMPItems.LOGGER.info("Loaded " + CUSTOM_ITEMS.size() + " custom items");
    }
    
    private static void loadItemsFromDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                loadItemsFromDirectory(file);
            } else if (file.getName().endsWith(".json")) {
                try {
                    loadItemFromFile(file);
                } catch (Exception e) {
                    FlazeSMPItems.LOGGER.error("Failed to load custom item from " + file.getName(), e);
                }
            }
        }
    }
    
    private static void loadItemFromFile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            
            String itemId = json.get("id").getAsString();
            ResourceLocation id = new ResourceLocation(itemId);
            
            String displayName = json.get("displayName").getAsString();
            String texturePath = json.get("texturePath").getAsString();
            ItemRarity rarity = ItemRarity.valueOf(json.get("rarity").getAsString());
            
            List<String> tooltipLines = new ArrayList<>();
            JsonArray tooltips = json.getAsJsonArray("tooltipLines");
            for (JsonElement element : tooltips) {
                tooltipLines.add(element.getAsString());
            }
            
            CustomItemData itemData = new CustomItemData(id, displayName, texturePath, rarity, tooltipLines);
            CUSTOM_ITEMS.put(id, itemData);
        }
    }
    
    private static void saveItem(CustomItemData itemData) {
        try {
            ResourceLocation id = itemData.id;
            
            // Create namespace directory if it doesn't exist
            File namespaceDir = new File(SAVE_DIR + id.getNamespace());
            if (!namespaceDir.exists()) {
                namespaceDir.mkdirs();
            }
            
            // Create JSON file
            File itemFile = new File(SAVE_DIR + id.getNamespace() + "/" + id.getPath() + ".json");
            
            // Convert to JSON
            JsonObject json = new JsonObject();
            json.addProperty("id", id.toString());
            json.addProperty("displayName", itemData.displayName);
            json.addProperty("texturePath", itemData.texturePath);
            json.addProperty("rarity", itemData.rarity.name());
            
            JsonArray tooltips = new JsonArray();
            for (String line : itemData.tooltipLines) {
                tooltips.add(line);
            }
            json.add("tooltipLines", tooltips);
            
            // Write to file
            try (FileWriter writer = new FileWriter(itemFile)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            FlazeSMPItems.LOGGER.error("Failed to save custom item: " + itemData.id, e);
        }
    }
    
    public static class CustomItemData {
        public final ResourceLocation id;
        public final String displayName;
        public final String texturePath;
        public final ItemRarity rarity;
        public final List<String> tooltipLines;
        
        public CustomItemData(ResourceLocation id, String displayName, String texturePath, 
                             ItemRarity rarity, List<String> tooltipLines) {
            this.id = id;
            this.displayName = displayName;
            this.texturePath = texturePath;
            this.rarity = rarity;
            this.tooltipLines = tooltipLines;
        }
    }
}