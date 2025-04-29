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
        // Main config values
        public final ForgeConfigSpec.BooleanValue enableStatFormatting;
        public final ForgeConfigSpec.BooleanValue showStatHeader;
        public final ForgeConfigSpec.ConfigValue<String> statHeaderText;
        
        // Keep only the direct config values
        public final ForgeConfigSpec.ConfigValue<String> attackDamageDisplay;
        public final ForgeConfigSpec.ConfigValue<String> attackDamageInternal;
        public final ForgeConfigSpec.ConfigValue<String> attackSpeedDisplay;
        public final ForgeConfigSpec.ConfigValue<String> attackSpeedInternal;
        public final ForgeConfigSpec.ConfigValue<String> armorDisplay;
        public final ForgeConfigSpec.ConfigValue<String> armorInternal;
        public final ForgeConfigSpec.ConfigValue<String> armorToughnessDisplay;
        public final ForgeConfigSpec.ConfigValue<String> armorToughnessInternal;
        public final ForgeConfigSpec.ConfigValue<String> knockbackResistanceDisplay;
        public final ForgeConfigSpec.ConfigValue<String> knockbackResistanceInternal;
        public final ForgeConfigSpec.ConfigValue<String> maxHealthDisplay;
        public final ForgeConfigSpec.ConfigValue<String> maxHealthInternal;
        public final ForgeConfigSpec.ConfigValue<String> movementSpeedDisplay;
        public final ForgeConfigSpec.ConfigValue<String> movementSpeedInternal;
        public final ForgeConfigSpec.ConfigValue<String> luckDisplay;
        public final ForgeConfigSpec.ConfigValue<String> luckInternal;
        public final ForgeConfigSpec.ConfigValue<String> reachDistanceDisplay;
        public final ForgeConfigSpec.ConfigValue<String> reachDistanceInternal;
        public final ForgeConfigSpec.ConfigValue<String> miningSpeedDisplay;
        public final ForgeConfigSpec.ConfigValue<String> miningSpeedInternal;
        public final ForgeConfigSpec.ConfigValue<String> magicDamageDisplay;
        public final ForgeConfigSpec.ConfigValue<String> magicDamageInternal;
        public final ForgeConfigSpec.ConfigValue<String> critChanceDisplay;
        public final ForgeConfigSpec.ConfigValue<String> critChanceInternal;
        public final ForgeConfigSpec.ConfigValue<String> critDamageDisplay;
        public final ForgeConfigSpec.ConfigValue<String> critDamageInternal;
        
        // IMPORTANT: These maps must be transient and static to avoid serialization issues
        private static final transient Map<String, String> statDisplayNamesMap = new HashMap<>();
        private static final transient Map<String, String> internalNamesMap = new HashMap<>();
        
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
            
            // IMPORTANT: Only store the config objects in the constructor, 
            // DON'T call .get() on them here!
            attackDamageDisplay = builder.define("attack_damage", "Damage");
            attackDamageInternal = builder.define("attack_damage_internal", "attack damage");
            
            attackSpeedDisplay = builder.define("attack_speed", "Attack Speed");
            attackSpeedInternal = builder.define("attack_speed_internal", "attack speed");
            
            armorDisplay = builder.define("armor", "Defense");
            armorInternal = builder.define("armor_internal", "armor");
            
            armorToughnessDisplay = builder.define("armor_toughness", "Armor Toughness");
            armorToughnessInternal = builder.define("armor_toughness_internal", "armor toughness");
            
            knockbackResistanceDisplay = builder.define("knockback_resistance", "Knockback Resistance");
            knockbackResistanceInternal = builder.define("knockback_resistance_internal", "knockback resistance");
            
            maxHealthDisplay = builder.define("max_health", "Health");
            maxHealthInternal = builder.define("max_health_internal", "max health");
            
            movementSpeedDisplay = builder.define("movement_speed", "Speed");
            movementSpeedInternal = builder.define("movement_speed_internal", "movement speed");
            
            luckDisplay = builder.define("luck", "Luck");
            luckInternal = builder.define("luck_internal", "luck");
            
            reachDistanceDisplay = builder.define("reach_distance", "Reach");
            reachDistanceInternal = builder.define("reach_distance_internal", "reach distance");
            
            miningSpeedDisplay = builder.define("mining_speed", "Mining Speed");
            miningSpeedInternal = builder.define("mining_speed_internal", "mining speed");
            
            magicDamageDisplay = builder.define("magic_damage", "Magic Damage");
            magicDamageInternal = builder.define("magic_damage_internal", "magic damage");
            
            critChanceDisplay = builder.define("crit_chance", "Crit Chance");
            critChanceInternal = builder.define("crit_chance_internal", "critical chance");
            
            critDamageDisplay = builder.define("crit_damage", "Crit Damage");
            critDamageInternal = builder.define("crit_damage_internal", "critical damage");
            
            builder.pop();
        }
        
        /**
         * Loads the stat display names from config into the StatTooltipFormatter
         * Only called after configs are properly loaded
         */
        public void loadStatDisplayNames() {
            // Add default mappings first
            StatTooltipFormatter.setupDefaultAttributeMappings();
            
            try {
                // Only populate the maps AFTER configs are loaded, in this method
                statDisplayNamesMap.clear();
                internalNamesMap.clear();
                
                // Now it's safe to call .get()
                statDisplayNamesMap.put("attack_damage", attackDamageDisplay.get());
                internalNamesMap.put("attack_damage", attackDamageInternal.get());
                statDisplayNamesMap.put("attack_speed", attackSpeedDisplay.get());
                internalNamesMap.put("attack_speed", attackSpeedInternal.get());
                statDisplayNamesMap.put("armor", armorDisplay.get());
                internalNamesMap.put("armor", armorInternal.get());
                statDisplayNamesMap.put("armor_toughness", armorToughnessDisplay.get());
                internalNamesMap.put("armor_toughness", armorToughnessInternal.get());
                statDisplayNamesMap.put("knockback_resistance", knockbackResistanceDisplay.get());
                internalNamesMap.put("knockback_resistance", knockbackResistanceInternal.get());
                statDisplayNamesMap.put("max_health", maxHealthDisplay.get());
                internalNamesMap.put("max_health", maxHealthInternal.get());
                statDisplayNamesMap.put("movement_speed", movementSpeedDisplay.get());
                internalNamesMap.put("movement_speed", movementSpeedInternal.get());
                statDisplayNamesMap.put("luck", luckDisplay.get());
                internalNamesMap.put("luck", luckInternal.get());
                statDisplayNamesMap.put("reach_distance", reachDistanceDisplay.get());
                internalNamesMap.put("reach_distance", reachDistanceInternal.get());
                statDisplayNamesMap.put("mining_speed", miningSpeedDisplay.get());
                internalNamesMap.put("mining_speed", miningSpeedInternal.get());
                statDisplayNamesMap.put("magic_damage", magicDamageDisplay.get());
                internalNamesMap.put("magic_damage", magicDamageInternal.get());
                statDisplayNamesMap.put("crit_chance", critChanceDisplay.get());
                internalNamesMap.put("crit_chance", critChanceInternal.get());
                statDisplayNamesMap.put("crit_damage", critDamageDisplay.get());
                internalNamesMap.put("crit_damage", critDamageInternal.get());
                
                // Then use these maps for your logic
                for (Map.Entry<String, String> entry : statDisplayNamesMap.entrySet()) {
                    String attributeKey = entry.getKey();
                    String displayName = entry.getValue();
                    String internalName = internalNamesMap.get(attributeKey);
                    
                    if (internalName != null && !internalName.isEmpty() && 
                        displayName != null && !displayName.isEmpty()) {
                        StatTooltipFormatter.registerAttributeDisplayName(internalName, displayName);
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