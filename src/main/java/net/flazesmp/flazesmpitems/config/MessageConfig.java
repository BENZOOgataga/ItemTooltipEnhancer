package net.flazesmp.flazesmpitems.config;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration class for all player-facing messages in the mod
 */
@Mod.EventBusSubscriber(modid = FlazeSMPItems.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MessageConfig {
    // Original message mapping for fallback
    private static final Map<String, String> ORIGINAL_MESSAGES = new HashMap<>();
    
    // Message cache to avoid repeated config lookups
    private static Map<String, String> messageCache = new HashMap<>();
    
    // Properties file for messages
    private static final String MESSAGES_FILE = "itemtooltipenhancer-messages.toml";
    private static Properties messagesProps = new Properties();
    
    /**
     * Register the message config file
     */
    public static void register() {
        registerDefaultMessages();
        FlazeSMPItems.LOGGER.info("Registered message configuration");
        
        // Create messages file if it doesn't exist
        createMessagesFile();
        
        // Load messages from properties file
        loadMessages();
    }

    /**
     * Create the messages file if it doesn't exist
     * Uses the embedded default file as a template
     */
    private static void createMessagesFile() {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            Files.createDirectories(configDir);
            
            File messagesFile = configDir.resolve(MESSAGES_FILE).toFile();
            if (!messagesFile.exists()) {
                // Copy the default messages file from resources
                try (InputStream input = MessageConfig.class.getClassLoader()
                        .getResourceAsStream("data/itemtooltipenhancer/messages-default.toml")) {
                    
                    if (input != null) {
                        FileUtils.copyInputStreamToFile(input, messagesFile);
                        FlazeSMPItems.LOGGER.info("Created default messages file at {}", messagesFile.getPath());
                    } else {
                        // Fallback if resource not found
                        createManualMessagesFile(messagesFile);
                    }
                }
            }
        } catch (IOException e) {
            FlazeSMPItems.LOGGER.error("Failed to create messages file", e);
        }
    }
    
    /**
     * Manually create messages file if the resource cannot be found
     */
    private static void createManualMessagesFile(File messagesFile) throws IOException {
        try (FileWriter writer = new FileWriter(messagesFile)) {
            writer.write("# ItemTooltipEnhancer Message Configuration\n");
            writer.write("# You can customize all player-facing messages in the mod here.\n");
            writer.write("#\n");
            writer.write("# Format:\n");
            writer.write("# - Use & for color codes (e.g. &a for green)\n");
            writer.write("# - Use {0}, {1}, etc. for parameters that will be replaced dynamically\n\n");
            
            writer.write("[messages]\n");
            
            // Command messages
            writer.write("    # Command messages\n");
            for (Map.Entry<String, String> entry : ORIGINAL_MESSAGES.entrySet()) {
                if (entry.getKey().startsWith("command.")) {
                    writer.write("    \"" + entry.getKey() + "\" = \"" + entry.getValue() + "\"\n");
                }
            }
            writer.write("\n");
            
            
            // Config messages
            writer.write("    # Config messages\n");
            for (Map.Entry<String, String> entry : ORIGINAL_MESSAGES.entrySet()) {
                if (entry.getKey().startsWith("config.")) {
                    writer.write("    \"" + entry.getKey() + "\" = \"" + entry.getValue() + "\"\n");
                }
            }
            writer.write("\n");
            
            // Tooltip messages
            writer.write("    # Special item tooltips\n");
            for (Map.Entry<String, String> entry : ORIGINAL_MESSAGES.entrySet()) {
                if (entry.getKey().startsWith("tooltip.")) {
                    writer.write("    \"" + entry.getKey() + "\" = \"" + entry.getValue() + "\"\n");
                }
            }
        }
        FlazeSMPItems.LOGGER.info("Created manual messages file at {}", messagesFile.getPath());
    }
    
    /**
     * Register all default messages for fallback
     */
    private static void registerDefaultMessages() {
        // Command messages
        ORIGINAL_MESSAGES.put("command.reset.held_item_requirement", "You must hold an item in your main hand");
        ORIGINAL_MESSAGES.put("command.reset.success", "Reset item: &e{0}");
        ORIGINAL_MESSAGES.put("command.reset.success.held", "Reset item in hand: &e{0}");
        ORIGINAL_MESSAGES.put("command.reset.error", "Error resetting item: &c{0}");
        ORIGINAL_MESSAGES.put("command.reset.not_found", "Item not found: &c{0}");
        ORIGINAL_MESSAGES.put("command.reset.invalid_id", "Invalid item ID format: &c{0}");
        
        
        // Item command messages
        ORIGINAL_MESSAGES.put("command.name.success", "&aUpdated display name for {0} to: {1}");
        ORIGINAL_MESSAGES.put("command.name.error", "Error setting display name: &c{0}");
        ORIGINAL_MESSAGES.put("command.tooltip.success", "&aUpdated tooltip line {0} for {1} to: {2}");
        ORIGINAL_MESSAGES.put("command.tooltip.error", "Error setting tooltip: &c{0}");
        ORIGINAL_MESSAGES.put("command.rarity.success", "Updated rarity for {0} to: {1}");
        ORIGINAL_MESSAGES.put("command.rarity.error", "Error setting rarity: &c{0}");
        ORIGINAL_MESSAGES.put("command.dump.collecting", "&eCollecting item information...");
        ORIGINAL_MESSAGES.put("command.dump.success", "&aSuccessfully dumped {0} items to file:");
        ORIGINAL_MESSAGES.put("command.dump.error", "&cFailed to dump items: {0}");
        ORIGINAL_MESSAGES.put("command.reload.checking", "&eChecking and repairing config files if needed...");
        ORIGINAL_MESSAGES.put("command.reload.success", "&aConfiguration reloaded successfully!");
        ORIGINAL_MESSAGES.put("command.reload.error", "&cFailed to reload configuration: {0}");
        
        
        // Config messages
        ORIGINAL_MESSAGES.put("config.set.success", "Set {0} to {1}");
        ORIGINAL_MESSAGES.put("config.unknown_setting", "Unknown setting: &c{0}");
        ORIGINAL_MESSAGES.put("config.invalid_value", "Invalid {0}: &c{1}");
        ORIGINAL_MESSAGES.put("config.add.warning_time.exists", "Warning time {0} seconds is already in the list");
        ORIGINAL_MESSAGES.put("config.add.warning_time.success", "&aAdded warning time: {0} seconds");
        ORIGINAL_MESSAGES.put("config.remove.warning_time.not_found", "&cWarning time {0} seconds is not in the list");
        ORIGINAL_MESSAGES.put("config.remove.warning_time.last", "&cCannot remove the last warning time");
        ORIGINAL_MESSAGES.put("config.remove.warning_time.success", "&cRemoved warning time: {0} seconds");
        ORIGINAL_MESSAGES.put("config.add.entity_type.success", "&aAdded entity type: {0}");
        ORIGINAL_MESSAGES.put("config.remove.entity_type.not_found", "&cEntity type {0} is not in the list");
        ORIGINAL_MESSAGES.put("config.remove.entity_type.last", "&cCannot remove the last entity type");
        ORIGINAL_MESSAGES.put("config.remove.entity_type.success", "&cRemoved entity type: {0}");
        
        // Special item tooltips
        ORIGINAL_MESSAGES.put("tooltip.potion.no_effects", "No Effects");
        ORIGINAL_MESSAGES.put("tooltip.potion.effects_header", "&bEFFECTS");
        ORIGINAL_MESSAGES.put("tooltip.potion.duration", "  Duration: &f{0}");
        ORIGINAL_MESSAGES.put("tooltip.potion.custom_potion", "Custom Potion");
        ORIGINAL_MESSAGES.put("tooltip.music_disc.track", "&7Track: &9{0}");
        ORIGINAL_MESSAGES.put("tooltip.music_disc.unknown", "Unknown Track");
    }
    
    /**
     * Load messages from TOML file
     */
    private static void loadMessages() {
        // Clear the cache
        messageCache.clear();
        
        File messagesFile = FMLPaths.CONFIGDIR.get().resolve(MESSAGES_FILE).toFile();
        
        FlazeSMPItems.LOGGER.info("Attempting to load messages from: {}", messagesFile.getAbsolutePath());
        
        if (!messagesFile.exists()) {
            FlazeSMPItems.LOGGER.warn("Messages file not found at: {}", messagesFile.getAbsolutePath());
            return;
        }
        
        if (!messagesFile.canRead()) {
            FlazeSMPItems.LOGGER.error("Cannot read messages file (permission issue): {}", messagesFile.getAbsolutePath());
            return;
        }
        
        try {
            String content = FileUtils.readFileToString(messagesFile, StandardCharsets.UTF_8);
            TomlMessageParser parser = new TomlMessageParser();
            Map<String, String> parsedMessages = parser.parseMessages(content);
            
            if (!parsedMessages.isEmpty()) {
                // Replace the properties with our parsed messages
                messagesProps.clear();
                for (Map.Entry<String, String> entry : parsedMessages.entrySet()) {
                    messagesProps.setProperty(entry.getKey(), entry.getValue());
                    FlazeSMPItems.LOGGER.debug("Loaded message: {} = {}", entry.getKey(), entry.getValue());
                }
                FlazeSMPItems.LOGGER.info("Loaded {} custom messages from TOML", parsedMessages.size());
            } else {
                FlazeSMPItems.LOGGER.warn("No messages found in TOML file");
            }
        } catch (Exception e) {
            FlazeSMPItems.LOGGER.error("Failed to load messages from TOML file", e);
        }
    }
    
    /**
     * Save messages to TOML file
     * Since we can't easily modify an existing TOML file, this creates a new one
     */
    public static void saveMessages() {
        // Currently we don't support saving back to TOML
        // This would require preserving comments and structure which is complex
        FlazeSMPItems.LOGGER.warn("Message saving not implemented for TOML format");
    }
    
    /**
     * Explicitly reloads messages from the TOML file
     * This is called by the reload command
     */
    public static void reloadMessages() {
        FlazeSMPItems.LOGGER.info("Explicitly reloading message config...");
        
        // Check if the file exists, and recreate if missing
        File messagesFile = FMLPaths.CONFIGDIR.get().resolve(MESSAGES_FILE).toFile();
        if (!messagesFile.exists()) {
            FlazeSMPItems.LOGGER.warn("Messages file missing. Recreating from template...");
            createMessagesFile(); // Recreate the missing file
        }
        
        // Clear both the properties and the cache
        messagesProps.clear();
        messageCache.clear();
        
        // Reload messages from file
        loadMessages();
        
        // Debug output of a few key messages to verify loading
        if (messagesProps.containsKey("command.reset.held_item_requirement")) {
            FlazeSMPItems.LOGGER.info("Sample loaded message - command.reset.held_item_requirement: '{}'", 
                messagesProps.getProperty("command.reset.held_item_requirement"));
        } else {
            FlazeSMPItems.LOGGER.warn("Sample message 'command.reset.held_item_requirement' not found in config");
        }
        
        FlazeSMPItems.LOGGER.info("Message config reloaded successfully with {} messages", messagesProps.size());
    }
    
    /**
     * Format a message with parameters and color codes
     */
    private static String formatMessage(String message, Object... params) {
        // Replace color codes
        String formatted = message.replace('&', '§');
        
        // Replace parameters
        for (int i = 0; i < params.length; i++) {
            formatted = formatted.replace("{" + i + "}", String.valueOf(params[i]));
        }
        
        return formatted;
    }
    
    /**
     * Set a custom message - not supported in TOML mode currently
     */
    public static void setMessage(String key, String value) {
        FlazeSMPItems.LOGGER.warn("Message modification not supported in TOML format");
        messageCache.remove(key); // Remove from cache just in case
    }
    
    /**
     * Clear the message cache when config is reloaded
     */
    @SubscribeEvent
    public static void onConfigReload(final ModConfigEvent.Reloading event) {
        loadMessages();
        FlazeSMPItems.LOGGER.info("Reloaded message config values");
    }
    
    /**
     * Simple parser for our TOML message format
     */
    private static class TomlMessageParser {
        public Map<String, String> parseMessages(String content) {
            Map<String, String> result = new HashMap<>();
            boolean inMessagesSection = false;
            
            // Process line by line
            String[] lines = content.split("\n");
            for (String line : lines) {
                String trimmedLine = line.trim();
                
                // Skip comments and empty lines
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#") || trimmedLine.startsWith("//")) {
                    continue;
                }
                
                // Check for section markers
                if (trimmedLine.equals("[messages]")) {
                    inMessagesSection = true;
                    continue;
                } else if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                    inMessagesSection = false;
                    continue;
                }
                
                // Only process lines in the messages section
                if (!inMessagesSection) {
                    continue;
                }
                
                // Parse key-value pairs
                int equalsPos = trimmedLine.indexOf('=');
                if (equalsPos > 0) {
                    // Extract key and value, handling quotes properly
                    String key = trimmedLine.substring(0, equalsPos).trim();
                    String value = trimmedLine.substring(equalsPos + 1).trim();
                    
                    // Remove quotes from key
                    if (key.startsWith("\"") && key.endsWith("\"")) {
                        key = key.substring(1, key.length() - 1);
                    }
                    
                    // Remove quotes from value
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    if (!key.isEmpty()) {
                        result.put(key, value);
                        FlazeSMPItems.LOGGER.debug("Parsed message: {} = {}", key, value);
                    }
                }
            }
            
            FlazeSMPItems.LOGGER.debug("Parsed {} messages from TOML", result.size());
            
            // Debug output - print a few important messages
            if (result.containsKey("command.reset.held_item_requirement")) {
                FlazeSMPItems.LOGGER.info("Parsed 'command.reset.held_item_requirement': '{}'",
                    result.get("command.reset.held_item_requirement"));
            }
            if (result.containsKey("command.reset.success")) {
                FlazeSMPItems.LOGGER.info("Parsed 'command.reset.success': '{}'",
                    result.get("command.reset.success"));
            }
            
            return result;
        }
    }
    
    /**
     * Gets a configured message, falling back to the original if not configured
     * 
     * @param key The message key
     * @param params Optional parameters to format into the message
     * @return The formatted message
     */
    public static String getMessage(String key, Object... params) {
        // Check cache first
        if (messageCache.containsKey(key)) {
            String cached = messageCache.get(key);
            FlazeSMPItems.LOGGER.debug("Using cached message for {}: {}", key, cached);
            return formatMessage(cached, params);
        }
        
        // Get from properties
        String message = messagesProps.getProperty(key);
        
        // Log what was found in the properties
        if (message != null) {
            FlazeSMPItems.LOGGER.debug("Found message in properties for {}: {}", key, message);
        } else {
            FlazeSMPItems.LOGGER.debug("No message found in properties for {}", key);
        }
        
        // Fall back to original message if not in properties
        if (message == null) {
            message = ORIGINAL_MESSAGES.getOrDefault(key, "Missing message: " + key);
            FlazeSMPItems.LOGGER.debug("Using default message for {}: {}", key, message);
        }
        
        // Cache for future use
        messageCache.put(key, message);
        
        // Format with parameters
        return formatMessage(message, params);
    }
}
