package net.flazesmp.flazesmpitems.clearlag;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = FlazeSMPItems.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClearlagConfig {
    private static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
    public static final ServerConfig SERVER = new ServerConfig(SERVER_BUILDER);
    public static final ForgeConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

    // Player preferences storage (memory cache)
    private static final Map<UUID, NotificationType> playerPreferences = new ConcurrentHashMap<>();
    
    // Gson for JSON operations
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Player preferences directory
    private static final String PLAYER_PREFS_DIR = "playerpreferencesdata";
    
    // Register configs
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "itemtooltipenhancer-clearlag.toml");
        
        // Create the player preferences directory if it doesn't exist
        createPlayerPreferencesDirectory();
        
        FlazeSMPItems.LOGGER.info("Registered clearlag configuration");
    }
    
    /**
     * Create the player preferences directory if it doesn't exist
     */
    private static void createPlayerPreferencesDirectory() {
        try {
            Path prefsDir = FMLPaths.CONFIGDIR.get()
                .resolve("itemtooltipenhancer")
                .resolve(PLAYER_PREFS_DIR);
            
            Files.createDirectories(prefsDir);
            FlazeSMPItems.LOGGER.info("Player preferences directory created at: {}", prefsDir);
        } catch (IOException e) {
            FlazeSMPItems.LOGGER.error("Failed to create player preferences directory", e);
        }
    }
    
    /**
     * Event handler for config loading and reloading
     */
    @SubscribeEvent
    public static void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            FlazeSMPItems.LOGGER.info("Loaded clearlag config values");
            loadAllPlayerPreferences();
        }
    }
    
    @SubscribeEvent
    public static void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            FlazeSMPItems.LOGGER.info("Reloaded clearlag config values");
            loadAllPlayerPreferences();
        }
    }
    
    public static class ServerConfig {
        // Automatic clearlag settings
        public final ForgeConfigSpec.BooleanValue enableAutoClearlag;
        public final ForgeConfigSpec.IntValue clearlagIntervalMinutes;
        
        // Clearlag notification settings
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> warningTimesSeconds;
        
        // Entity types to clear
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> entityTypesToClear;
        
        // Default notification type
        public final ForgeConfigSpec.EnumValue<NotificationType> defaultNotificationType;
        
        // Notification display settings - NEW
        public final ForgeConfigSpec.IntValue notificationDisplayDurationSeconds;
        
        // Dynamic countdown - NEW
        public final ForgeConfigSpec.BooleanValue useDynamicCountdown;
        public final ForgeConfigSpec.IntValue longNotificationThresholdSeconds;
        public final ForgeConfigSpec.IntValue shortNotificationThresholdSeconds;
        
        public ServerConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("ItemTooltipEnhancer ClearLag Configuration")
                   .push("general");
            
            enableAutoClearlag = builder
                    .comment("Enable automatic clearlag")
                    .define("enableAutoClearlag", true);
            
            clearlagIntervalMinutes = builder
                    .comment("Interval between automatic clearlag operations (in minutes)")
                    .defineInRange("clearlagIntervalMinutes", 60, 5, 1440);
            
            builder.pop().push("notifications");
            
            warningTimesSeconds = builder
                    .comment("Notification times before clearlag (in seconds)")
                    .defineList("warningTimesSeconds", 
                                Lists.newArrayList(1800, 900, 600, 300, 60, 30, 10, 5, 3, 2, 1),
                                e -> e instanceof Integer && (Integer) e > 0);
            
            defaultNotificationType = builder
                    .comment("Default notification type for players (HOTBAR, CHAT, NONE)")
                    .defineEnum("defaultNotificationType", NotificationType.HOTBAR);
            
            // New configuration options for notification display
            notificationDisplayDurationSeconds = builder
                    .comment("How long notifications should stay visible on the hotbar (in seconds)")
                    .defineInRange("notificationDisplayDurationSeconds", 3, 1, 10);
                    
            useDynamicCountdown = builder
                    .comment("Whether to use dynamic countdown times based on the time remaining")
                    .define("useDynamicCountdown", true);
                    
            longNotificationThresholdSeconds = builder
                    .comment("Threshold (in seconds) above which notifications will show minutes:seconds")
                    .defineInRange("longNotificationThresholdSeconds", 60, 30, 3600);
                    
            shortNotificationThresholdSeconds = builder
                    .comment("Threshold (in seconds) below which notifications will show exact seconds")
                    .defineInRange("shortNotificationThresholdSeconds", 10, 1, 30);
            
            builder.pop().push("entities");
            
            entityTypesToClear = builder
                    .comment("Entity types to clear during clearlag operation (reference: https://minecraft.wiki/w/Java_Edition_data_values#Entities)")
                    .defineList("entityTypesToClear", 
                                Lists.newArrayList("minecraft:item"),
                                e -> e instanceof String);
            
            builder.pop();
        }
    }
    
    // Get player notification preference or default
    public static NotificationType getPlayerNotificationType(UUID playerUUID) {
        return playerPreferences.getOrDefault(playerUUID, SERVER.defaultNotificationType.get());
    }
    
    // Set player notification preference and save to disk
    public static void setPlayerNotificationType(UUID playerUUID, NotificationType type) {
        // If it's the default value, remove it to save memory
        if (type == SERVER.defaultNotificationType.get()) {
            playerPreferences.remove(playerUUID);
            // Also remove the preference file if it exists
            deletePlayerPreferenceFile(playerUUID);
        } else {
            // Set in memory
            playerPreferences.put(playerUUID, type);
            // Save to disk
            savePlayerPreference(playerUUID, type);
        }
    }
    
    /**
     * Get entity selectors for clearlag command
     */
    public static String getEntitySelector() {
        List<? extends String> entities = SERVER.entityTypesToClear.get();
        if (entities.isEmpty()) {
            return "@e[type=minecraft:item]"; // Default
        }
        
        if (entities.size() == 1) {
            return "@e[type=" + entities.get(0) + "]";
        }
        
        StringBuilder selector = new StringBuilder("@e[type=");
        selector.append(entities.get(0));
        for (int i = 1; i < entities.size(); i++) {
            selector.append(",type=").append(entities.get(i));
        }
        selector.append("]");
        return selector.toString();
    }
    
    /**
     * Get warning times sorted in descending order
     */
    public static List<Integer> getSortedWarningTimes() {
        List<Integer> times = new ArrayList<>(SERVER.warningTimesSeconds.get());
        Collections.sort(times, Comparator.reverseOrder());
        return times;
    }
    
    /**
     * Format a time remaining message for notifications based on settings
     * @param secondsRemaining Seconds until clearlag
     * @return Formatted time string
     */
    public static String formatTimeRemaining(int secondsRemaining) {
        if (!SERVER.useDynamicCountdown.get()) {
            // If dynamic countdown is disabled, just show the exact seconds
            return secondsRemaining + " second" + (secondsRemaining == 1 ? "" : "s");
        }
        
        // Otherwise use dynamic formatting
        if (secondsRemaining >= SERVER.longNotificationThresholdSeconds.get()) {
            // Format as minutes and seconds for longer times
            int minutes = secondsRemaining / 60;
            int seconds = secondsRemaining % 60;
            if (seconds == 0) {
                return minutes + " minute" + (minutes == 1 ? "" : "s");
            } else {
                return String.format("%d minute%s and %d second%s", 
                    minutes, (minutes == 1 ? "" : "s"), 
                    seconds, (seconds == 1 ? "" : "s"));
            }
        } else if (secondsRemaining <= SERVER.shortNotificationThresholdSeconds.get()) {
            // Just show exact seconds for very short times
            return secondsRemaining + " second" + (secondsRemaining == 1 ? "" : "s");
        } else {
            // For medium times, just show the seconds
            return secondsRemaining + " seconds";
        }
    }
    
    /**
     * Save player preference to a file
     */
    private static void savePlayerPreference(UUID playerUUID, NotificationType type) {
        Path playerPrefsPath = getPlayerPreferencePath(playerUUID);
        try {
            Files.createDirectories(playerPrefsPath.getParent());
            
            JsonObject json = new JsonObject();
            json.addProperty("notificationType", type.name());
            
            try (FileWriter writer = new FileWriter(playerPrefsPath.toFile())) {
                GSON.toJson(json, writer);
            }
            
            FlazeSMPItems.LOGGER.debug("Saved preference for player {}: {}", playerUUID, type);
        } catch (IOException e) {
            FlazeSMPItems.LOGGER.error("Failed to save player preference for {}", playerUUID, e);
        }
    }
    
    /**
     * Delete player preference file
     */
    private static void deletePlayerPreferenceFile(UUID playerUUID) {
        Path playerPrefsPath = getPlayerPreferencePath(playerUUID);
        try {
            Files.deleteIfExists(playerPrefsPath);
            FlazeSMPItems.LOGGER.debug("Deleted preference file for player {}", playerUUID);
        } catch (IOException e) {
            FlazeSMPItems.LOGGER.error("Failed to delete player preference file for {}", playerUUID, e);
        }
    }
    
    /**
     * Load a player's preference from file
     */
    private static void loadPlayerPreference(UUID playerUUID) {
        Path playerPrefsPath = getPlayerPreferencePath(playerUUID);
        File file = playerPrefsPath.toFile();
        
        if (!file.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("notificationType")) {
                String typeStr = json.get("notificationType").getAsString();
                try {
                    NotificationType type = NotificationType.valueOf(typeStr);
                    playerPreferences.put(playerUUID, type);
                    FlazeSMPItems.LOGGER.debug("Loaded preference for player {}: {}", playerUUID, type);
                } catch (IllegalArgumentException e) {
                    FlazeSMPItems.LOGGER.warn("Invalid notification type in preference file for {}: {}", 
                                             playerUUID, typeStr);
                }
            }
        } catch (IOException e) {
            FlazeSMPItems.LOGGER.error("Failed to load player preference for {}", playerUUID, e);
        }
    }
    
    /**
     * Load all player preferences from disk
     */
    public static void loadAllPlayerPreferences() {
        // Clear the in-memory map first
        playerPreferences.clear();
        
        Path prefsDir = FMLPaths.CONFIGDIR.get()
            .resolve("itemtooltipenhancer")
            .resolve(PLAYER_PREFS_DIR);
        
        File[] files = prefsDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            FlazeSMPItems.LOGGER.debug("No player preferences found to load");
            return;
        }
        
        for (File file : files) {
            String fileName = file.getName();
            try {
                // Remove .json extension to get the UUID
                String uuidStr = fileName.substring(0, fileName.length() - 5);
                UUID playerUUID = UUID.fromString(uuidStr);
                loadPlayerPreference(playerUUID);
            } catch (IllegalArgumentException e) {
                FlazeSMPItems.LOGGER.warn("Invalid preference file name: {}", fileName);
            }
        }
        
        FlazeSMPItems.LOGGER.info("Loaded {} player preferences", playerPreferences.size());
    }
    
    /**
     * Get the file path for a player's preference file
     */
    private static Path getPlayerPreferencePath(UUID playerUUID) {
        return FMLPaths.CONFIGDIR.get()
            .resolve("itemtooltipenhancer")
            .resolve(PLAYER_PREFS_DIR)
            .resolve(playerUUID.toString() + ".json");
    }
}