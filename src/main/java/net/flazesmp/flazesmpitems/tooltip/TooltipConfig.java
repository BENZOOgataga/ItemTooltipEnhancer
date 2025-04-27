package net.flazesmp.flazesmpitems.tooltip;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for tooltip formatting
 */
@Mod.EventBusSubscriber(modid = FlazeSMPItems.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TooltipConfig {
    // Config specification
    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
    public static final ClientConfig CLIENT = new ClientConfig(CLIENT_BUILDER);
    public static final ForgeConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();
    
    // Register configs
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, "itemtooltipenhancer-tooltips.toml");
        FlazeSMPItems.LOGGER.info("Registered tooltip formatting config");
    }
    
    /**
     * Event handler for config loading and reloading
     */
    @SubscribeEvent
    public static void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == CLIENT_SPEC) {
            loadConfigValues();
            FlazeSMPItems.LOGGER.info("Loaded tooltip config values");
        }
    }
    
    @SubscribeEvent
    public static void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == CLIENT_SPEC) {
            loadConfigValues();
            FlazeSMPItems.LOGGER.info("Reloaded tooltip config values");
        }
    }
    
    /**
     * Loads config values into the formatter
     */
    private static void loadConfigValues() {
        CLIENT.loadStatDisplayNames();
    }
    
    // Client config
    public static class ClientConfig {
        // Main toggle for the tooltip stat formatting feature
        public final ForgeConfigSpec.BooleanValue enableStatFormatting;
        
        // Display options
        public final ForgeConfigSpec.BooleanValue showStatHeader;
        public final ForgeConfigSpec.ConfigValue<String> statHeaderText;
        
        // Stat display names mappings
        private final Map<String, ForgeConfigSpec.ConfigValue<String>> statDisplayNames = new HashMap<>();
        private final Map<String, ForgeConfigSpec.ConfigValue<String>> internalNames = new HashMap<>();
        
        public ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("ItemTooltipEnhancer Tooltip Formatting Configuration")
                   .push("general");
            
            enableStatFormatting = builder
                    .comment("Enable Hypixel Skyblock-style stat formatting")
                    .define("enableStatFormatting", true);
            
            builder.pop().push("display");
            
            showStatHeader = builder
                    .comment("Show a header above the stats section")
                    .define("showStatHeader", false);
                    
            statHeaderText = builder
                    .comment("Text to show as the stats header (supports & color codes)")
                    .define("statHeaderText", "&9&lSTATS");
            
            builder.pop().push("statNames");
            
            // Add configurable attribute display names
            addStatDisplayName(builder, "attack_damage", "Damage", "attack damage");
            addStatDisplayName(builder, "attack_speed", "Attack Speed", "attack speed");
            addStatDisplayName(builder, "armor", "Defense", "armor");
            addStatDisplayName(builder, "armor_toughness", "Armor Toughness", "armor toughness");
            addStatDisplayName(builder, "knockback_resistance", "Knockback Resistance", "knockback resistance");
            addStatDisplayName(builder, "max_health", "Health", "max health");
            addStatDisplayName(builder, "movement_speed", "Speed", "movement speed");
            addStatDisplayName(builder, "luck", "Luck", "luck");
            
            // Add common modded attributes
            addStatDisplayName(builder, "reach_distance", "Reach", "reach distance");
            addStatDisplayName(builder, "mining_speed", "Mining Speed", "mining speed");
            addStatDisplayName(builder, "magic_damage", "Magic Damage", "magic damage");
            addStatDisplayName(builder, "crit_chance", "Crit Chance", "critical chance");
            addStatDisplayName(builder, "crit_damage", "Crit Damage", "critical damage");
            
            builder.pop();
        }
        
        private void addStatDisplayName(ForgeConfigSpec.Builder builder, 
                                      String configKey, 
                                      String defaultDisplayName,
                                      String defaultInternalName) {
            // Define the display name for the stat
            ForgeConfigSpec.ConfigValue<String> displayNameConfig = builder
                    .comment("Display name for " + configKey + " stat")
                    .define(configKey, defaultDisplayName);
            
            // Define the internal name that we'll look for in tooltips
            ForgeConfigSpec.ConfigValue<String> internalNameConfig = builder
                    .comment("Internal name for " + configKey + " stat (what appears in tooltips)")
                    .define(configKey + "_internal", defaultInternalName);
                    
            statDisplayNames.put(configKey, displayNameConfig);
            internalNames.put(configKey, internalNameConfig);
        }
        
        /**
         * Loads the stat display names from config into the StatTooltipFormatter
         * Only called after configs are properly loaded
         */
        public void loadStatDisplayNames() {
            // Add default mappings first
            StatTooltipFormatter.setupDefaultAttributeMappings();
            
            try {
                // Then override with config values
                for (Map.Entry<String, ForgeConfigSpec.ConfigValue<String>> entry : statDisplayNames.entrySet()) {
                    String attributeKey = entry.getKey();
                    String displayName = entry.getValue().get();
                    
                    // Safely get the internal name
                    ForgeConfigSpec.ConfigValue<String> internalNameConfig = internalNames.get(attributeKey);
                    if (internalNameConfig != null) {
                        String internalName = internalNameConfig.get();
                        
                        if (internalName != null && !internalName.isEmpty() && 
                            displayName != null && !displayName.isEmpty()) {
                            StatTooltipFormatter.registerAttributeDisplayName(internalName, displayName);
                            FlazeSMPItems.LOGGER.debug("Registered stat mapping: {} -> {}", internalName, displayName);
                        }
                    }
                }
            } catch (Exception e) {
                FlazeSMPItems.LOGGER.error("Error loading stat display names from config", e);
            }
        }
    }
    
    private boolean enableRarityColors = true;

    public boolean enableRarityColors() {
        return enableRarityColors;
    }

    public void setEnableRarityColors(boolean enableRarityColors) {
        this.enableRarityColors = enableRarityColors;
    }
}