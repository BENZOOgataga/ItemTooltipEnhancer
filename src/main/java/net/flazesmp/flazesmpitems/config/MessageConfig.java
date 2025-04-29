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
            
            // Clearlag messages
            writer.write("    # Clearlag messages\n");
            for (Map.Entry<String, String> entry : ORIGINAL_MESSAGES.entrySet()) {
                if (entry.getKey().startsWith("clearlag.") && !entry.getKey().startsWith("clearlag.notification.")) {
                    writer.write("    \"" + entry.getKey() + "\" = \"" + entry.getValue() + "\"\n");
                }
            }
            writer.write("\n");
            
            // Clearlag notification messages
            writer.write("    # Clearlag notification messages\n");
            for (Map.Entry<String, String> entry : ORIGINAL_MESSAGES.entrySet()) {
                if (entry.getKey().startsWith("clearlag.notification.")) {
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
        
        // Clearlag messages
        ORIGINAL_MESSAGES.put("clearlag.scheduled", "&eManual clearlag scheduled to occur in 1 minute.");
        ORIGINAL_MESSAGES.put("clearlag.already_scheduled", "&cA manual clearlag operation is already scheduled!");
        ORIGINAL_MESSAGES.put("clearlag.none_scheduled", "&eThere is no clearlag currently scheduled.");
        ORIGINAL_MESSAGES.put("clearlag.next", "&eNext clearlag will occur in {0}");
        ORIGINAL_MESSAGES.put("clearlag.warning", "Items will be cleared in {0}");
        ORIGINAL_MESSAGES.put("clearlag.manual_warning", "Manual clearlag: Items will be cleared in {0}");
        ORIGINAL_MESSAGES.put("clearlag.cleared", "Cleared {0} item{1}");
        ORIGINAL_MESSAGES.put("clearlag.notification", "Next clearlag in {0}");
        ORIGINAL_MESSAGES.put("clearlag.manual_notification", "Manual clearlag in {0}");
        
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
        
        // Clearlag notification messages
        ORIGINAL_MESSAGES.put("clearlag.notification.type.current", "&eYour current clearlag notification type: &a{0}");
        ORIGINAL_MESSAGES.put("clearlag.notification.type.change", "&7You can change it with: /clearlag notifications <type>");
        ORIGINAL_MESSAGES.put("clearlag.notification.type.invalid", "&cInvalid notification type: {0}");
        ORIGINAL_MESSAGES.put("clearlag.notification.type.valid", "&eValid types: hotbar, chat, none");
        ORIGINAL_MESSAGES.put("clearlag.notification.type.none_warning", "&cWARNING: You will not receive any clearlag notifications!");
        ORIGINAL_MESSAGES.put("clearlag.notification.type.chat", "&aClearlag notifications will now appear in chat");
        ORIGINAL_MESSAGES.put("clearlag.notification.type.hotbar", "&aClearlag notifications will now appear on your hotbar");
        ORIGINAL_MESSAGES.put("clearlag.notification.type.none", "&aClearlag notifications have been disabled (risky!)");
        ORIGINAL_MESSAGES.put("clearlag.notification.type.player_only", "This command must be used by a player");
        
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
        if (!messagesFile.exists()) {
            FlazeSMPItems.LOGGER.info("No custom messages file found, using defaults");
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
                }
                FlazeSMPItems.LOGGER.info("Loaded {} custom messages from TOML", parsedMessages.size());
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
     * Gets a configured message, falling back to the original if not configured
     * 
     * @param key The message key
     * @param params Optional parameters to format into the message
     * @return The formatted message
     */
    public static String getMessage(String key, Object... params) {
        // Check cache first
        if (messageCache.containsKey(key)) {
            return formatMessage(messageCache.get(key), params);
        }
        
        // Get from properties
        String message = messagesProps.getProperty(key);
        
        // Fall back to original message if not in properties
        if (message == null) {
            message = ORIGINAL_MESSAGES.getOrDefault(key, "Missing message: " + key);
        }
        
        // Cache for future use
        messageCache.put(key, message);
        
        // Format with parameters
        return formatMessage(message, params);
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
                line = line.trim();
                
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Check for section markers
                if (line.equals("[messages]")) {
                    inMessagesSection = true;
                    continue;
                } else if (line.startsWith("[") && line.endsWith("]")) {
                    inMessagesSection = false;
                    continue;
                }
                
                // Only process lines in the messages section
                if (!inMessagesSection) {
                    continue;
                }
                
                // Parse key-value pairs
                int equalsPos = line.indexOf('=');
                if (equalsPos > 0) {
                    String key = line.substring(0, equalsPos).trim();
                    String value = line.substring(equalsPos + 1).trim();
                    
                    // Clean up key and value
                    key = key.replaceAll("^\"|\"$", "").trim();
                    value = value.replaceAll("^\"|\"$", "").trim();
                    
                    if (!key.isEmpty() && !value.isEmpty()) {
                        result.put(key, value);
                    }
                }
            }
            
            return result;
        }
    }
}