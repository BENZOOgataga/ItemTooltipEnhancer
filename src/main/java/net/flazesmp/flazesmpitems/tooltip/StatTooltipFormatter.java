package net.flazesmp.flazesmpitems.tooltip;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats item stat tooltips in Hypixel Skyblock style
 */
public class StatTooltipFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatTooltipFormatter.class);
    
    // Patterns for recognizing different types of stat lines
    private static final Pattern VANILLA_ATTRIBUTE_PATTERN = Pattern.compile("(?:When (?:on [\\w\\s]+|in [\\w\\s]+): )?([-+]?\\d+(?:\\.\\d+)?)(?: )?(.+)");
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)%");
    private static final Pattern COMMON_STAT_PATTERN = Pattern.compile("(?:^|: )([-+]?\\d+(?:\\.\\d+)?)(?: )?([\\w\\s]+)(?:$|,)");
    private static final Pattern GENERIC_STAT_PATTERN = Pattern.compile("([\\w\\s]+)(?:: | )([-+]?\\d+(?:\\.\\d+)?)");
    
    // Section headers we want to ignore/replace
    private static final List<String> VANILLA_SECTION_HEADERS = Arrays.asList(
        "when in main hand", 
        "when on main hand",
        "when in off hand", 
        "when on body", 
        "when on head", 
        "when on legs", 
        "when on feet",
        "when worn",
        "attributes"
    );
    
    // Map of attribute names to their Hypixel display names
    private static final Map<String, String> ATTRIBUTE_DISPLAY_NAMES = new HashMap<>();
    
    // Map for special stats that need custom formatting
    private static final Map<String, StatFormatter> SPECIAL_STAT_FORMATTERS = new HashMap<>();
    
    /**
     * Sets up default attribute mappings - called during initialization
     * and doesn't rely on config values
     */
    public static void setupDefaultAttributeMappings() {
        // Clear existing mappings in case this is called multiple times
        ATTRIBUTE_DISPLAY_NAMES.clear();
        SPECIAL_STAT_FORMATTERS.clear();
        
        // Vanilla attributes to Hypixel names
        ATTRIBUTE_DISPLAY_NAMES.put("attack damage", "Damage");
        ATTRIBUTE_DISPLAY_NAMES.put("attack speed", "Attack Speed");
        ATTRIBUTE_DISPLAY_NAMES.put("armor", "Defense");
        ATTRIBUTE_DISPLAY_NAMES.put("armor toughness", "Armor Toughness");
        ATTRIBUTE_DISPLAY_NAMES.put("knockback resistance", "Knockback Resistance");
        ATTRIBUTE_DISPLAY_NAMES.put("max health", "Health");
        ATTRIBUTE_DISPLAY_NAMES.put("movement speed", "Speed");
        ATTRIBUTE_DISPLAY_NAMES.put("luck", "Luck");
        
        // Special stats with custom formatting (e.g., percentage stats)
        SPECIAL_STAT_FORMATTERS.put("attack speed", (name, value) -> {
            try {
                double numVal = Double.parseDouble(value);
                return new StatLine("Attack Speed", 
                    String.format("%+.0f%%", (numVal * 100)), 
                    numVal > 0 ? ChatFormatting.GREEN : ChatFormatting.RED);
            } catch (NumberFormatException e) {
                return new StatLine("Attack Speed", value, ChatFormatting.RED);
            }
        });
        
        SPECIAL_STAT_FORMATTERS.put("movement speed", (name, value) -> {
            try {
                double numVal = Double.parseDouble(value);
                return new StatLine("Speed", 
                    String.format("%+.0f%%", (numVal * 100)), 
                    numVal > 0 ? ChatFormatting.GREEN : ChatFormatting.RED);
            } catch (NumberFormatException e) {
                return new StatLine("Speed", value, ChatFormatting.RED);
            }
        });
    }
    
    /**
     * Process a tooltip list to replace stat lines with Hypixel style format
     * 
     * @param tooltip The tooltip list to process
     * @param stack The itemstack
     * @return True if any changes were made to the tooltip
     */
    public static boolean processTooltip(List<Component> tooltip, ItemStack stack) {
        // Check if formatting is enabled in config
        if (!TooltipConfig.CLIENT.enableStatFormatting.get()) {
            return false;
        }
        
        if (tooltip == null || tooltip.isEmpty()) {
            return false;
        }
        
        boolean foundStats = false;
        boolean removedLines = false;
        List<StatLine> extractedStats = new ArrayList<>();
        
        // Iterate through tooltip to find and remove vanilla and mod stat lines
        for (int i = tooltip.size() - 1; i >= 0; i--) {
            Component line = tooltip.get(i);
            if (line == null) continue;
            
            String plainText = line.getString().toLowerCase().trim();
            
            // Skip empty lines
            if (plainText.isEmpty()) {
                continue;
            }
            
            // Check if this is a section header we want to remove
            boolean isSectionHeader = false;
            for (String header : VANILLA_SECTION_HEADERS) {
                if (plainText.contains(header)) {
                    isSectionHeader = true;
                    break;
                }
            }
            
            // Remove section headers and nearby lines
            if (isSectionHeader) {
                tooltip.remove(i);
                removedLines = true;
                continue;
            }
            
            // Try to extract stat information
            StatLine statLine = tryExtractStat(line);
            if (statLine != null) {
                extractedStats.add(statLine);
                tooltip.remove(i);
                foundStats = true;
                removedLines = true;
                continue;
            }
        }
        
        // If we found stats, add them back in Hypixel format
        if (foundStats && !extractedStats.isEmpty()) {
            // Insert a blank line before stats if there's content and no blank line already
            if (tooltip.size() > 0 && 
                !tooltip.get(tooltip.size() - 1).getString().trim().isEmpty()) {
                tooltip.add(Component.literal(""));
            }
            
            // Add header for stats section if enabled in config
            if (TooltipConfig.CLIENT.showStatHeader.get()) {
                String headerText = TooltipConfig.CLIENT.statHeaderText.get()
                    .replace('&', '§');
                tooltip.add(Component.literal(headerText));
            }
            
            // Add each stat in Hypixel format - reverse to maintain original order (since we extracted bottom-up)
            Collections.reverse(extractedStats);
            
            // Group by stat name - some attributes might appear multiple times (e.g. damage from different sources)
            Map<String, Double> statTotals = new HashMap<>();
            Map<String, ChatFormatting> statColors = new HashMap<>();
            Map<String, String> nonNumericStats = new HashMap<>();
            
            for (StatLine stat : extractedStats) {
                try {
                    String valueStr = stat.value.replace("+", "");
                    // Check if this is a percentage value
                    boolean isPercentage = valueStr.endsWith("%");
                    if (isPercentage) {
                        valueStr = valueStr.substring(0, valueStr.length() - 1);
                    }
                    
                    double value = Double.parseDouble(valueStr);
                    String statKey = stat.name + (isPercentage ? "%" : "");
                    
                    statTotals.put(statKey, statTotals.getOrDefault(statKey, 0.0) + value);
                    statColors.put(statKey, value >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED);
                    
                } catch (NumberFormatException e) {
                    // For non-numeric stats, just keep the latest value
                    nonNumericStats.put(stat.name, stat.value);
                }
            }
            
            // Add the aggregated stats
            for (Map.Entry<String, Double> entry : statTotals.entrySet()) {
                String name = entry.getKey();
                double value = entry.getValue();
                boolean isPercentage = name.endsWith("%");
                
                if (isPercentage) {
                    name = name.substring(0, name.length() - 1);
                }
                
                String displayValue = (value > 0 ? "+" : "") + formatStatValue(name, value, isPercentage);
                ChatFormatting color = statColors.get(isPercentage ? name + "%" : name);
                
                tooltip.add(createStatComponent(new StatLine(name, displayValue, color)));
            }
            
            // Add any non-numeric stats
            for (Map.Entry<String, String> entry : nonNumericStats.entrySet()) {
                tooltip.add(createStatComponent(new StatLine(
                    entry.getKey(), entry.getValue(), ChatFormatting.GREEN)));
            }
            
            // Add another blank line after stats
            tooltip.add(Component.literal(""));
        }
        
        return removedLines || foundStats;
    }
    
    /**
     * Creates a Hypixel-styled stat component
     */
    private static Component createStatComponent(StatLine stat) {
        MutableComponent statNameComponent = Component.literal(stat.name + ": ")
            .withStyle(ChatFormatting.GRAY);
        
        MutableComponent statValueComponent = Component.literal(stat.value)
            .withStyle(stat.color);
            
        return statNameComponent.append(statValueComponent);
    }
    
    /**
     * Attempts to extract stat information from a tooltip line
     */
    private static StatLine tryExtractStat(Component line) {
        String text = line.getString();
        
        // Try vanilla attribute pattern first
        Matcher vanillaMatcher = VANILLA_ATTRIBUTE_PATTERN.matcher(text);
        if (vanillaMatcher.find()) {
            String value = vanillaMatcher.group(1);
            String attributeName = vanillaMatcher.group(2).toLowerCase();
            
            // Apply custom formatting for special stats
            if (SPECIAL_STAT_FORMATTERS.containsKey(attributeName)) {
                return SPECIAL_STAT_FORMATTERS.get(attributeName).format(attributeName, value);
            }
            
            // Map attribute name to Hypixel display name if available
            String displayName = ATTRIBUTE_DISPLAY_NAMES.getOrDefault(attributeName, 
                attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1));
            
            ChatFormatting color = value.contains("-") ? ChatFormatting.RED : ChatFormatting.GREEN;
            return new StatLine(displayName, value, color);
        }
        
        // Try common stat pattern
        Matcher commonMatcher = COMMON_STAT_PATTERN.matcher(text);
        if (commonMatcher.find()) {
            String value = commonMatcher.group(1);
            String attributeName = commonMatcher.group(2).toLowerCase().trim();
            
            String displayName = ATTRIBUTE_DISPLAY_NAMES.getOrDefault(attributeName, 
                attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1));
            
            ChatFormatting color = value.contains("-") ? ChatFormatting.RED : ChatFormatting.GREEN;
            return new StatLine(displayName, value, color);
        }
        
        // Try generic format (e.g., "Attack Damage: +5")
        Matcher genericMatcher = GENERIC_STAT_PATTERN.matcher(text);
        if (genericMatcher.find()) {
            String attributeName = genericMatcher.group(1).toLowerCase().trim();
            String value = genericMatcher.group(2);
            
            String displayName = ATTRIBUTE_DISPLAY_NAMES.getOrDefault(attributeName, 
                attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1));
            
            ChatFormatting color = value.contains("-") ? ChatFormatting.RED : ChatFormatting.GREEN;
            return new StatLine(displayName, value, color);
        }
        
        // Check for percentage patterns in the text
        Matcher percentMatcher = PERCENTAGE_PATTERN.matcher(text);
        if (percentMatcher.find()) {
            // This might be a percentage stat, but we need to determine the attribute name
            // For now, just return null since we can't reliably extract the stat name
            return null;
        }
        
        // Could not extract stat information
        return null;
    }
    
    /**
     * Format a stat value based on the stat name
     */
    private static String formatStatValue(String statName, double value, boolean isPercentage) {
        // Format percentage stats differently
        if (isPercentage || 
            statName.equalsIgnoreCase("attack speed") || 
            statName.equalsIgnoreCase("speed") || 
            statName.equalsIgnoreCase("knockback resistance")) {
            return String.format("%.0f%%", value);
        }
        
        // Default format for numeric values
        return String.format("%.1f", value).replace(".0", "");
    }
    
    /**
     * Register a custom attribute display name
     */
    public static void registerAttributeDisplayName(String attributeName, String displayName) {
        ATTRIBUTE_DISPLAY_NAMES.put(attributeName.toLowerCase(), displayName);
    }
    
    /**
     * Register a custom formatter for a specific stat
     */
    public static void registerStatFormatter(String statName, StatFormatter formatter) {
        SPECIAL_STAT_FORMATTERS.put(statName.toLowerCase(), formatter);
    }
    
    /**
     * A simple structure to hold stat information
     */
    private static class StatLine {
        final String name;
        final String value;
        final ChatFormatting color;
        
        StatLine(String name, String value, ChatFormatting color) {
            this.name = name;
            this.value = value;
            this.color = color;
        }
    }
    
    /**
     * Interface for custom stat formatters
     */
    @FunctionalInterface
    public interface StatFormatter {
        StatLine format(String name, String value);
    }
}