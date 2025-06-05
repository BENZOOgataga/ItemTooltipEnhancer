// ConfigManager.java - New file to handle config loading/saving
package net.flazesmp.flazesmpitems.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, String> CUSTOM_TYPE_SUFFIXES = new HashMap<>();
    
    // Fix the config directory path - directly in config/itemtooltipenhancer/
    private static final String CONFIG_DIR = "itemtooltipenhancer";
    private static final String EXAMPLE_FILE = "example_item.json";
    
    /**
     * Initialize the config system, creating necessary directories and example files
     */
    public static void initialize() {
        LOGGER.info("Initializing ItemTooltipEnhancer config system");
        
        // Vérifier et réparer la configuration si nécessaire
        checkAndRepairConfig();
        
        // Charger toutes les configurations
        loadAllConfigs();
    }
    
    /**
     * Create an example config file with extensive comments
     */
    private static void createExampleConfigFile(Path path) throws IOException {
        String exampleConfig = 
            "{\n" +
            "  // This is an example config file for ItemTooltipEnhancer\n" +
            "  // You can duplicate this file and rename it to match your item's ID\n" +
            "  // For example: 'minecraft_diamond_sword.json' for 'minecraft:diamond_sword'\n" +
            "  // Replace colons ':' with underscores '_' in the filename\n" +
            "\n" +
            "  // The item ID this config applies to (required)\n" +
            "  \"item\": \"minecraft:apple\",\n" +
            "\n" +
            "  // Custom display name with optional color codes using '&' (optional)\n" +
            "  // Common color codes: &0-&9, &a-&f for colors, &l for bold, &o for italic, etc.\n" +
            "  \"displayName\": \"&cMagical Apple\",\n" +
            "\n" +
            "  // Rarity affects text color if no color codes are used (optional)\n" +
            "  // Valid values: COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC, SPECIAL, ADMIN\n" +
            "  \"rarity\": \"RARE\",\n" +
            "\n" +
            "  // Custom type suffix to appear after rarity (optional, defaults to automatic detection)\n" +
            "  // Example: SWORD, PICKAXE, HELMET, CHESTPLATE, etc. (will be displayed in uppercase)\n" +
            "  \"typeSuffix\": \"APPLE\",\n" +
            "\n" +
            "  // Category to display at the bottom of the tooltip (optional)\n" +
            "  \"category\": \"Magic Food\",\n" +
            "\n" +
            "  // Custom tooltip lines (optional)\n" +
            "  // Each line can have color codes using '&' symbols\n" +
            "  \"tooltips\": {\n" +
            "    // The key is the line number, and the value is the tooltip text\n" +
            "    \"1\": \"&7This apple has magical properties\",\n" +
            "    \"2\": \"&bEat to gain +2 &9Mana Regeneration\",\n" +
            "    \"3\": \"&eEffect lasts for 30 seconds\"\n" +
            "  }\n" +
            "}\n";
        
        // Write the example config file
        Files.writeString(path, exampleConfig);
    }
    
    /**
     * Load all item configs from the config directory
     */
    public static void loadAllConfigs() {
        LOGGER.info("Loading all configurations...");
        
        // Ensure config directory exists before trying to read files
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR);
        File directory = configDir.toFile();
        
        if (!directory.exists() || !directory.isDirectory()) {
            LOGGER.warn("Config directory doesn't exist: {}", configDir);
            try {
                Files.createDirectories(configDir);
                LOGGER.info("Created config directory at {}", configDir);
            } catch (IOException e) {
                LOGGER.error("Failed to create config directory: {}", e.getMessage());
                return;
            }
        }
        
        // Process all JSON files in the directory
        AtomicInteger loadedCount = new AtomicInteger(0);
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        
        if (files == null || files.length == 0) {
            LOGGER.info("No item config files found in {}", configDir);
            return;
        }
        
        for (File file : files) {
            if (file.getName().equals(EXAMPLE_FILE)) {
                // Skip example file
                continue;
            }
            
            try {
                if (loadItemConfig(file)) {
                    loadedCount.incrementAndGet();
                }
            } catch (Exception e) {
                LOGGER.error("Error loading item config from file: {}", file.getName(), e);
            }
        }
        
        LOGGER.info("Loaded {} item configs from {}", loadedCount.get(), configDir);
    }
    
    /**
     * Load a single item config from a file
     * 
     * @param file The file to load from
     * @return true if the config was loaded successfully, false otherwise
     */
    private static boolean loadItemConfig(File file) {
        try (FileReader reader = new FileReader(file)) {
            // Parse the JSON
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            
            // Get the item ID
            String itemId = json.get("item").getAsString();
            ResourceLocation resourceLocation = new ResourceLocation(itemId);
            Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
            
            if (item == null) {
                LOGGER.warn("Unknown item ID in config file {}: {}", file.getName(), itemId);
                return false;
            }
            
            // Apply the config to the item
            
            // Set rarity if specified
            if (json.has("rarity")) {
                String rarityStr = json.get("rarity").getAsString();
                try {
                    ItemRarity rarity = ItemRarity.valueOf(rarityStr.toUpperCase());
                    RarityManager.setRarity(item, rarity);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid rarity in config file {}: {}", file.getName(), rarityStr);
                }
            }
            
            // Set display name if specified
            if (json.has("displayName")) {
                String displayName = json.get("displayName").getAsString();
                RarityManager.setCustomName(item, displayName.replace('&', '§'));
            }
            
            // Set category if specified
            if (json.has("category")) {
                String category = json.get("category").getAsString();
                RarityManager.setItemCategory(item, category);
            }
            
            // Set tooltips if specified
            if (json.has("tooltips") && json.get("tooltips").isJsonObject()) {
                JsonObject tooltips = json.getAsJsonObject("tooltips");
                tooltips.entrySet().forEach(entry -> {
                    try {
                        int line = Integer.parseInt(entry.getKey());
                        String text = entry.getValue().getAsString().replace('&', '§');
                        RarityManager.setTooltipLine(item, line, text);
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Invalid tooltip line number in config file {}: {}", 
                                file.getName(), entry.getKey());
                    }
                });
            }
            
            // Load custom type suffix if specified
            if (json.has("typeSuffix")) {
                String typeSuffix = json.get("typeSuffix").getAsString().toUpperCase();
                CUSTOM_TYPE_SUFFIXES.put(resourceLocation, typeSuffix);
            }
            
            LOGGER.debug("Loaded config for item {} from file {}", itemId, file.getName());
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to load item config from file: {}", file.getName(), e);
            return false;
        }
    }
    
    /**
     * Save an item's configuration to a file
     * 
     * @param item The item to save config for
     */
    public static void saveItemConfig(Item item) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        if (itemId == null) {
            LOGGER.warn("Cannot save config for item without registry name");
            return;
        }
        
        // Create the file name from the item ID (replacing : with _)
        String fileName = itemId.toString().replace(':', '_') + ".json";
        Path filePath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR).resolve(fileName);
        
        try {
            // Create a JSON object for the item config
            JsonObject json = new JsonObject();
            
            // Add the item ID
            json.addProperty("item", itemId.toString());
            
            // Add rarity if not default
            ItemRarity rarity = RarityManager.getRarity(item);
            if (rarity != ItemRarity.COMMON) {
                json.addProperty("rarity", rarity.name());
            }
            
            // Add display name if set
            String displayName = RarityManager.getCustomName(item);
            if (displayName != null && !displayName.isEmpty()) {
                // Convert § to & for readability
                json.addProperty("displayName", displayName.replace('§', '&'));
            }
            
            // Add category if set
            String category = RarityManager.getItemCategory(item);
            if (category != null && !category.isEmpty()) {
                json.addProperty("category", category);
            }
            
            // Add tooltips if any
            Map<Integer, String> tooltips = RarityManager.getTooltipLines(item);
            if (tooltips != null && !tooltips.isEmpty()) {
                JsonObject tooltipsJson = new JsonObject();
                tooltips.forEach((line, text) -> {
                    // Convert § back to & for readability in config files
                    tooltipsJson.addProperty(line.toString(), text.replace('§', '&'));
                });
                json.add("tooltips", tooltipsJson);
            }
            
            // Add custom type suffix if set
            String typeSuffix = getCustomTypeSuffix(item);
            if (typeSuffix != null && !typeSuffix.isEmpty()) {
                json.addProperty("typeSuffix", typeSuffix);
            }
            
            // Ensure the parent directory exists
            Files.createDirectories(filePath.getParent());
            
            // Write to file
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                GSON.toJson(json, writer);
                LOGGER.info("Saved config for item {} to file {}", itemId, fileName);
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to save item config to file: {}", fileName, e);
        }
    }
    
    /**
     * Delete the config file for an item
     * 
     * @param item The item to delete config for
     */
    public static void deleteItemConfig(Item item) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        if (itemId == null) {
            return;
        }
        
        String fileName = itemId.toString().replace(':', '_') + ".json";
        Path filePath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR).resolve(fileName);
        
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                LOGGER.info("Deleted config file for item {}", itemId);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to delete config file for item {}", itemId, e);
        }
    }
    
    public static void setCustomTypeSuffix(Item item, String suffix) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (suffix != null && !suffix.isEmpty()) {
            CUSTOM_TYPE_SUFFIXES.put(id, suffix.toUpperCase());
        } else {
            CUSTOM_TYPE_SUFFIXES.remove(id);
        }
    }
    
    public static String getCustomTypeSuffix(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return CUSTOM_TYPE_SUFFIXES.get(id);
    }
    
    /**
     * Check configuration integrity and repair if needed
     */
    public static void checkAndRepairConfig() {
        LOGGER.info("Checking configuration integrity...");
        boolean needsRepair = false;
        
        // Check main config directory
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR);
        if (Files.exists(configDir)) {
            LOGGER.warn("Existing config files detected in {}. Please review them carefully.", configDir);
        } else {
            LOGGER.info("Creating config directory: {}", configDir);
            try {
                Files.createDirectories(configDir);
                
                // Create example item config file
                Path examplePath = configDir.resolve(EXAMPLE_FILE);
                createExampleConfigFile(examplePath);
                
                needsRepair = true;
            } catch (IOException e) {
                LOGGER.error("Failed to create config directory: {}", e.getMessage());
            }
        }
        
        

        // If issues were found, repair the configuration
        if (needsRepair) {
            LOGGER.info("Configuration repair completed");
        } else {
            LOGGER.info("Configuration integrity check passed");
        }
    }
    
    /**
     * Check if an item has explicit customizations set in config
     * @param itemId The item's resource location
     * @return True if the item has custom data in config
     */
    public static boolean hasCustomItemData(ResourceLocation itemId) {
        if (itemId == null) return false;
        
        try {
            // Check if we have a config file for this item
            Path configPath = FMLPaths.GAMEDIR.get().resolve("config")
                    .resolve(CONFIG_DIR)
                    .resolve("items")
                    .resolve(itemId.getNamespace())
                    .resolve(itemId.getPath() + ".json");
            
            return Files.exists(configPath);
        } catch (Exception e) {
            LOGGER.error("Error checking custom item data for {}: {}", itemId, e.getMessage());
            return false;
        }
    }
}
