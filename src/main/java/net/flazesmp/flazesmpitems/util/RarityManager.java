package net.flazesmp.flazesmpitems.util;

import net.flazesmp.flazesmpitems.config.ConfigManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraftforge.registries.ForgeRegistries;
import net.flazesmp.flazesmpitems.FlazeSMPItems;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Manages item rarities, custom names, tooltips and categories
 */
public class RarityManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RarityManager.class);
    
    // Maps to track item data
    private static final Map<ResourceLocation, ItemRarity> ITEM_RARITIES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, String> ITEM_CATEGORIES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, String> CUSTOM_NAMES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Map<Integer, String>> TOOLTIPS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ItemRarity> DEFAULT_RARITIES = new ConcurrentHashMap<>();

    // Default rarity
    private static final ItemRarity DEFAULT_RARITY = ItemRarity.COMMON;
    
    // Cache for automatic rarity calculations
    private static final Map<ResourceLocation, ItemRarity> AUTO_RARITY_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Initialize the RarityManager - called during mod startup
     */
    public static void initialize() {
        LOGGER.info("Initializing RarityManager");
        
        // Initialize config system first
        ConfigManager.initialize();
        
        // Load any custom default rarities from config file
        loadDefaultRaritiesFromConfig();
        
        // Apply manual rarities to specific items
        setupManualRarities();
        
        // Store default rarities for all items
        saveDefaultRarities();
    }
    
    /**
     * Store default rarity for an item during initialization
     */
    private static void storeDefaultRarity(Item item, ItemRarity rarity) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id != null) {
            DEFAULT_RARITIES.put(id, rarity);
        }
    }
    
    /**
     * Sets up manual rarity assignments for special items
     */
    private static void setupManualRarities() {
        // Admin tier items - operator/creative only items
        storeAndSetRarity(Items.COMMAND_BLOCK, ItemRarity.ADMIN);
        storeAndSetRarity(Items.CHAIN_COMMAND_BLOCK, ItemRarity.ADMIN);
        storeAndSetRarity(Items.REPEATING_COMMAND_BLOCK, ItemRarity.ADMIN);
        storeAndSetRarity(Items.COMMAND_BLOCK_MINECART, ItemRarity.ADMIN);
        storeAndSetRarity(Items.STRUCTURE_BLOCK, ItemRarity.ADMIN);
        storeAndSetRarity(Items.STRUCTURE_VOID, ItemRarity.ADMIN);
        storeAndSetRarity(Items.JIGSAW, ItemRarity.ADMIN);
        storeAndSetRarity(Items.BARRIER, ItemRarity.ADMIN);
        storeAndSetRarity(Items.LIGHT, ItemRarity.ADMIN);
        storeAndSetRarity(Items.DEBUG_STICK, ItemRarity.ADMIN);
        storeAndSetRarity(Items.KNOWLEDGE_BOOK, ItemRarity.ADMIN);
        storeAndSetRarity(Items.BEDROCK, ItemRarity.ADMIN);
        
        // Legendary tier - unique or extremely rare items
        storeAndSetRarity(Items.DRAGON_EGG, ItemRarity.LEGENDARY);
        storeAndSetRarity(Items.DRAGON_HEAD, ItemRarity.LEGENDARY);
        storeAndSetRarity(Items.ELYTRA, ItemRarity.LEGENDARY);
        storeAndSetRarity(Items.END_PORTAL_FRAME, ItemRarity.LEGENDARY);
        storeAndSetRarity(Items.NETHER_STAR, ItemRarity.LEGENDARY);
        
        // Mythic tier - very powerful or sought-after items
        storeAndSetRarity(Items.BEACON, ItemRarity.MYTHIC);
        storeAndSetRarity(Items.ENCHANTED_GOLDEN_APPLE, ItemRarity.MYTHIC);
        storeAndSetRarity(Items.NETHERITE_BLOCK, ItemRarity.MYTHIC);
        
        // Special tier - unique items that are special but not necessarily legendary
        storeAndSetRarity(Items.HEART_OF_THE_SEA, ItemRarity.SPECIAL);
        storeAndSetRarity(Items.MUSIC_DISC_PIGSTEP, ItemRarity.SPECIAL); // Rarest music disc
        storeAndSetRarity(Items.CONDUIT, ItemRarity.SPECIAL);
        storeAndSetRarity(Items.TOTEM_OF_UNDYING, ItemRarity.SPECIAL);
        
        // Epic tier adjustments
        storeAndSetRarity(Items.ANCIENT_DEBRIS, ItemRarity.EPIC);
        storeAndSetRarity(Items.NETHERITE_INGOT, ItemRarity.EPIC);
        storeAndSetRarity(Items.NETHERITE_SCRAP, ItemRarity.EPIC);
        
        // Apply to all other music discs
        ForgeRegistries.ITEMS.getValues().stream()
            .filter(item -> item.getDescriptionId().contains("music_disc"))
            .filter(item -> item != Items.MUSIC_DISC_PIGSTEP) // Already set to SPECIAL
            .forEach(item -> storeAndSetRarity(item, ItemRarity.RARE));
        
        LOGGER.info("Manual rarities configured for special items");
    }
    
    /**
     * Helper method to both store default rarity and set current rarity
     */
    private static void storeAndSetRarity(Item item, ItemRarity rarity) {
        storeDefaultRarity(item, rarity);
        setRarityForItem(item, rarity);
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
        
        // Save config file
        ConfigManager.saveItemConfig(item);
    }
    
    /**
     * Gets the rarity for an item - with automatic determination if not set manually
     * 
     * @param item The item
     * @return The item's rarity
     */
    public static ItemRarity getRarity(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null) return DEFAULT_RARITY;
        
        // If manually set, return that rarity
        if (ITEM_RARITIES.containsKey(id)) {
            return ITEM_RARITIES.get(id);
        }
        
        // Check cache first
        if (AUTO_RARITY_CACHE.containsKey(id)) {
            return AUTO_RARITY_CACHE.get(id);
        }
        
        // Determine rarity automatically based on item properties
        ItemRarity determinedRarity = determineItemRarity(item);
        AUTO_RARITY_CACHE.put(id, determinedRarity);
        
        return determinedRarity;
    }
    
    /**
     * Determine item rarity automatically based on various criteria
     */
    private static ItemRarity determineItemRarity(Item item) {
        // Check vanilla rarity first
        Rarity vanillaRarity = item.getRarity(new ItemStack(item));
        
        // Map vanilla rarities to our rarities
        if (vanillaRarity == Rarity.UNCOMMON) {
            return ItemRarity.UNCOMMON;
        } else if (vanillaRarity == Rarity.RARE) {
            return ItemRarity.RARE;
        } else if (vanillaRarity == Rarity.EPIC) {
            return ItemRarity.EPIC;
        }
        
        // Check for special items
        if (isSpecialItem(item)) {
            return ItemRarity.SPECIAL;
        }
        
        // Check for legendary items
        if (isLegendaryItem(item)) {
            return ItemRarity.LEGENDARY;
        }
        
        // Check for mythic items (very rare or powerful)
        if (isMythicItem(item)) {
            return ItemRarity.MYTHIC;
        }
        
        // Check for epic items based on material and tier
        if (isEpicItem(item)) {
            return ItemRarity.EPIC;
        }
        
        // Check for rare items based on material and tier
        if (isRareItem(item)) {
            return ItemRarity.RARE;
        }
        
        // Check for uncommon items based on material and tier
        if (isUncommonItem(item)) {
            return ItemRarity.UNCOMMON;
        }
        
        // Default to common rarity
        return ItemRarity.COMMON;
    }
    
    /**
     * Check if an item is considered special (quest rewards, crafting components)
     */
    private static boolean isSpecialItem(Item item) {
        // Special quest items and rare crafting components
        return item == Items.NETHER_STAR || 
               item == Items.DRAGON_EGG || 
               item == Items.DRAGON_HEAD || 
               item == Items.ENCHANTED_GOLDEN_APPLE ||
               item == Items.BEDROCK;
    }
    
    /**
     * Check if an item is considered legendary (extremely rare or end-game)
     */
    private static boolean isLegendaryItem(Item item) {
        // End-game and extremely rare items
        return item == Items.ELYTRA || 
               item == Items.BEACON || 
               item == Items.END_CRYSTAL ||
               item == Items.COMMAND_BLOCK ||
               item == Items.BARRIER;
    }
    
    /**
     * Check if an item is considered mythic (extraordinarily rare or unique)
     */
    private static boolean isMythicItem(Item item) {
        // Custom mythic items would go here
        return item == Items.DRAGON_EGG ||
               item == Items.COMMAND_BLOCK_MINECART ||
               item == Items.STRUCTURE_BLOCK;
    }
    
    /**
     * Check if an item is considered epic (diamond tier, high-value)
     */
    private static boolean isEpicItem(Item item) {
        // Check if item is diamond tier
        if (item instanceof TieredItem tieredItem && tieredItem.getTier() == Tiers.NETHERITE) {
            return true;
        }
        
        // Check for diamond armor
        if (item instanceof ArmorItem armorItem) {
            ArmorMaterial material = armorItem.getMaterial();
            String materialName = material.getName();
            if (materialName.contains("netherite")) {
                return true;
            }
        }
        
        // Check for other epic items
        return item == Items.NETHERITE_BLOCK ||
               item == Items.NETHERITE_INGOT ||
               item == Items.NETHERITE_SCRAP ||
               item == Items.ANCIENT_DEBRIS;
    }
    
    /**
     * Check if an item is considered rare (diamond tier, high-value)
     */
    private static boolean isRareItem(Item item) {
        // Check if item is diamond tier
        if (item instanceof TieredItem tieredItem && tieredItem.getTier() == Tiers.DIAMOND) {
            return true;
        }
        
        // Check for diamond armor
        if (item instanceof ArmorItem armorItem) {
            ArmorMaterial material = armorItem.getMaterial();
            String materialName = material.getName();
            if (materialName.contains("diamond")) {
                return true;
            }
        }
        
        // Check for other rare items
        return item == Items.DIAMOND ||
               item == Items.DIAMOND_BLOCK ||
               item == Items.ENCHANTED_BOOK ||
               item == Items.GOLDEN_APPLE ||
               item == Items.EXPERIENCE_BOTTLE ||
               item == Items.END_CRYSTAL;
    }
    
    /**
     * Check if an item should be considered uncommon based on its material
     */
    private static boolean isUncommonItem(Item item) {
        try {
            if (item instanceof TieredItem tieredItem) {
                Tier tier = tieredItem.getTier();
                // Check if it's a vanilla tier without casting
                if (tier.toString().equals("IRON") || tier.toString().equals("GOLD")) {
                    return true;
                }
                
                // Check durability as a fallback for modded tiers
                // Items with durability between iron and diamond can be considered uncommon
                if (tier.getUses() >= 250 && tier.getUses() < 1561) {
                    return true;
                }
            }
            
            // Rest of your existing checks...
            
        } catch (Exception e) {
            // Log error but don't crash
            FlazeSMPItems.LOGGER.debug("Error checking if item is uncommon: {}", e.getMessage());
        }
        return false;
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
        
        // Save config file
        ConfigManager.saveItemConfig(item);
    }
    
    /**
     * Gets the category for an item with automatic determination if not set
     * 
     * @param item The item
     * @return The item's category, or a determined category, or null if not determinable
     */
    public static String getItemCategory(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        String category = ITEM_CATEGORIES.get(id);
        
        // If manually set, return that category
        if (category != null) {
            return category;
        }
        
        // Otherwise determine automatically
        return determineItemCategory(item);
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
        
        // Save config file
        ConfigManager.saveItemConfig(item);
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
        
        // Save config file
        ConfigManager.saveItemConfig(item);
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
            
            // Save config file
            ConfigManager.saveItemConfig(item);
        }
    }
    
    /**
     * Clear all custom data for an item and reset to defaults
     * 
     * @param item The item to clear data for
     */
    public static void clearItemData(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        
        // Get the default rarity before clearing any data
        ItemRarity defaultRarity = getDefaultRarity(item);
        LOGGER.info("Clearing data for {}, default rarity: {}", id, defaultRarity.getName());
        
        // Remove custom data
        ITEM_RARITIES.remove(id);
        ITEM_CATEGORIES.remove(id);
        CUSTOM_NAMES.remove(id);
        TOOLTIPS.remove(id);
        AUTO_RARITY_CACHE.remove(id);
        
        // Restore default rarity (if not common)
        // CRITICAL: For all items, restore the default rarity that was stored previously  
        ITEM_RARITIES.put(id, defaultRarity);
        LOGGER.info("Restored default rarity: {}", defaultRarity.getName());
        
        // Also delete the config file
        ConfigManager.deleteItemConfig(item);
        
        LOGGER.info("Cleared all custom data for item: {}. Reset to default rarity: {}", 
            id, defaultRarity.getName());
    }
    
    /**
     * Apply custom data to an ItemStack
     * 
     * @param stack The ItemStack to modify
     */
    public static void applyCustomDataToItemStack(ItemStack stack) {
        // Add a check to prevent re-adding tooltips if they're already present
        if (stack.isEmpty() || stack.hasTag() && stack.getTag().contains("TooltipProcessed")) {
            return;
        }
        
        Item item = stack.getItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        
        // Apply custom name if set
        String customName = CUSTOM_NAMES.get(id);
        if (customName != null && !customName.isEmpty()) {
            stack.setHoverName(Component.literal(customName));
        }
        
        // Get tag outside the conditional block
        CompoundTag tag = stack.getOrCreateTag();
        
        // Apply tooltips if set
        Map<Integer, String> tooltipLines = TOOLTIPS.get(id);
        if (tooltipLines != null && !tooltipLines.isEmpty()) {
            CompoundTag display = tag.contains("display") ? 
                tag.getCompound("display") : new CompoundTag();
                
            // Create lore list
            ListTag lore = new ListTag();
            
            // Add a blank line before custom tooltips if we have any
            lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
            
            // Sort tooltips by line number and add them
            TreeMap<Integer, String> sortedTooltips = new TreeMap<>(tooltipLines);
            for (Map.Entry<Integer, String> entry : sortedTooltips.entrySet()) {
                String line = entry.getValue();
                if (line != null && !line.isEmpty()) {
                    lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
                }
            }
            
            // Add a blank line after custom tooltips
            lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
            
            if (!lore.isEmpty()) {
                display.put("Lore", lore);
                tag.put("display", display);
            }
        }
        
        // Add a marker tag to avoid duplicate processing
        tag.putBoolean("TooltipProcessed", true);
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

    /**
     * Determines an item's category automatically if not set manually
     */
    public static String determineItemCategory(Item item) {
        // Check if item is a potion
        if (item == Items.POTION) {
            return "Potion";
        } else if (item == Items.SPLASH_POTION) {
            return "Splash Potion";
        } else if (item == Items.LINGERING_POTION) {
            return "Lingering Potion";
        } else if (item == Items.TIPPED_ARROW) {
            return "Arrow with Effect";
        }
        
        // Weapons category
        if (item instanceof SwordItem || 
            item == Items.BOW || 
            item == Items.CROSSBOW || 
            item == Items.TRIDENT) {
            return "Weapon";
        }
        
        // Tools category
        if (item instanceof DiggerItem ||
            item == Items.SHEARS ||
            item == Items.FISHING_ROD ||
            item == Items.FLINT_AND_STEEL) {
            return "Tool";
        }
        
        // Armor category
        if (item instanceof ArmorItem) {
            return "Armor";
        }
        
        // Food category
        if (item.isEdible()) {
            return "Food";
        }
        
        // Music category
        if (item instanceof RecordItem || 
            item.getDescriptionId().contains("music_disc")) {
            return "Music";
        }
        
        // Blocks category - check if it's a block item
        if (item.getClass().getName().contains("BlockItem")) {
            return "Block";
        }
        
        // Resources category
        if (item == Items.DIAMOND ||
            item == Items.EMERALD ||
            item == Items.IRON_INGOT ||
            item == Items.GOLD_INGOT ||
            item == Items.NETHERITE_INGOT ||
            item == Items.COAL ||
            item == Items.LAPIS_LAZULI ||
            item == Items.REDSTONE) {
            return "Resource";
        }
        
        // Return null if no category can be determined
        return null;
    }

    /**
     * Helper method to set rarity for an item by its Item instance
     * Used primarily for setting up manual rarities
     * 
     * @param item The item to set rarity for
     * @param rarity The rarity to assign
     */
    private static void setRarityForItem(Item item, ItemRarity rarity) {
        if (item == null) return;
        
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id != null) {
            ITEM_RARITIES.put(id, rarity);
            AUTO_RARITY_CACHE.remove(id); // Clear from cache if it was there
        }
    }

    public static boolean hasCustomizations(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return ITEM_RARITIES.containsKey(id) ||
               CUSTOM_NAMES.containsKey(id) ||
               TOOLTIPS.containsKey(id) ||
               ITEM_CATEGORIES.containsKey(id);
    }

    /**
     * Gets the appropriate type suffix for an item, with support for custom overrides
     * 
     * @param item The item to check
     * @return The item type suffix, or empty string if no specific type
     */
    public static String getItemTypeSuffix(Item item) {
        // Check for custom suffix first
        String customSuffix = ConfigManager.getCustomTypeSuffix(item);
        if (customSuffix != null && !customSuffix.isEmpty()) {
            return customSuffix;
        }
        
        // Otherwise use automatic detection
        return determineItemTypeSuffix(item);
    }

    /**
     * Determines the appropriate type suffix for an item
     */
    private static String determineItemTypeSuffix(Item item) {
        // Weapons
        if (item instanceof SwordItem) {
            return "SWORD";
        } else if (item == Items.BOW) {
            return "BOW";
        } else if (item == Items.CROSSBOW) {
            return "CROSSBOW";
        } else if (item == Items.TRIDENT) {
            return "TRIDENT";
        } else if (item instanceof AxeItem && ((AxeItem)item).getTier() != Tiers.WOOD) {
            // Only consider non-wooden axes as weapons
            return "AXE";
        }
        
        // Tools
        if (item instanceof PickaxeItem) {
            return "PICKAXE";
        } else if (item instanceof ShovelItem) {
            return "SHOVEL";
        } else if (item instanceof HoeItem) {
            return "HOE";
        } else if (item instanceof AxeItem) { // Wooden axes are considered tools
            return "AXE";
        } else if (item instanceof ShearsItem) {
            return "SHEARS";
        } else if (item == Items.FISHING_ROD) {
            return "FISHING ROD";
        } else if (item == Items.FLINT_AND_STEEL) {
            return "FLINT AND STEEL";
        }
        
        // Armor
        if (item instanceof ArmorItem) {
            ArmorItem armorItem = (ArmorItem)item;
            switch (armorItem.getEquipmentSlot()) {
                case HEAD: return "HELMET";
                case CHEST: return "CHESTPLATE";
                case LEGS: return "LEGGINGS";
                case FEET: return "BOOTS";
                default: return "ARMOR";
            }
        }
        
        // Elytra is special
        if (item == Items.ELYTRA) {
            return "CHESTPLATE";
        }
        
        // Shields
        if (item == Items.SHIELD) {
            return "SHIELD";
        }
        
        // Consumables
        if (item.isEdible()) {
            if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE) {
                return "APPLE";
            } else {
                return "FOOD";
            }
        }
        
        // Special items
        if (item == Items.TOTEM_OF_UNDYING) {
            return "ARTIFACT";
        } else if (item instanceof BlockItem) {
            Block block = ((BlockItem)item).getBlock();
            // Check for special blocks
            if (block instanceof ChestBlock) {
                return "CHEST";
            } else if (block instanceof EnchantmentTableBlock) {
                return "TABLE";
            } else if (block instanceof AnvilBlock) {
                return "ANVIL";
            } else if (block instanceof BedBlock) {
                return "BED";
            } else if (block instanceof BeaconBlock) {
                return "BEACON";
            }
            // Generic block
            return "BLOCK";
        }
        
        // Try to determine based on item name
        String itemName = item.getDescriptionId().toLowerCase();
        if (itemName.contains("helmet")) {
            return "HELMET";
        } else if (itemName.contains("chestplate")) {
            return "CHESTPLATE"; 
        } else if (itemName.contains("leggings")) {
            return "LEGGINGS";
        } else if (itemName.contains("boots")) {
            return "BOOTS";
        } else if (itemName.contains("sword")) {
            return "SWORD";
        } else if (itemName.contains("pickaxe")) {
            return "PICKAXE";
        } else if (itemName.contains("axe")) {
            return "AXE";
        } else if (itemName.contains("shovel") || itemName.contains("spade")) {
            return "SHOVEL";
        } else if (itemName.contains("hoe")) {
            return "HOE";
        } else if (itemName.contains("bow") && !itemName.contains("bowl")) {
            return "BOW";
        }
        
        // Additional categorization for common items
        if (item == Items.DIAMOND || item == Items.EMERALD || item == Items.GOLD_INGOT || 
            item == Items.IRON_INGOT || item == Items.NETHERITE_INGOT) {
            return "GEM";
        } else if (item instanceof ItemNameBlockItem) { // This includes most seeds and saplings
            return "MATERIAL";
        }
        
        // No specific type detected
        return "";
    }

    /**
     * Gets the default rarity for an item (what it would have without custom settings)
     * 
     * @param item The item to check
     * @return The default rarity
     */
    public static ItemRarity getDefaultRarity(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id != null && DEFAULT_RARITIES.containsKey(id)) {
            ItemRarity rarity = DEFAULT_RARITIES.get(id);
            LOGGER.debug("Found stored default rarity for {}: {}", id, rarity.getName());
            return rarity;
        }
        
        // If not stored, calculate it
        ItemRarity calculatedRarity = determineItemRarity(item);
        if (id != null) {
            DEFAULT_RARITIES.put(id, calculatedRarity); // Store for future
        }
        LOGGER.debug("Calculated default rarity for {}: {}", id, calculatedRarity.getName());
        return calculatedRarity;
    }

    /**
     * Save the default rarities to a config file
     */
    private static void saveDefaultRaritiesToConfig() {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            Files.createDirectories(configDir);

            File defaultRaritiesFile = configDir.resolve("itemtooltipenhancer-default-rarities.json").toFile();

            if (defaultRaritiesFile.exists()) {
                return; // don't overwrite existing file
            }

            JsonObject rootJson = new JsonObject();
            JsonObject defaultRaritiesJson = new JsonObject();
            
            // Sort the entries for better readability
            List<Map.Entry<ResourceLocation, ItemRarity>> sortedEntries = 
                new ArrayList<>(DEFAULT_RARITIES.entrySet());
            sortedEntries.sort(Comparator.comparing(e -> e.getKey().toString()));
            
            for (Map.Entry<ResourceLocation, ItemRarity> entry : sortedEntries) {
                defaultRaritiesJson.addProperty(entry.getKey().toString(), entry.getValue().name());
            }
            
            rootJson.add("default_rarities", defaultRaritiesJson);
            
            try (FileWriter writer = new FileWriter(defaultRaritiesFile)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(rootJson, writer);
            }
            
            LOGGER.info("Saved default rarities to config file");
        } catch (Exception e) {
            LOGGER.error("Failed to save default rarities to config file", e);
        }
    }

    /**
     * Load default rarities from config file (if it exists)
     * This allows server admins to customize default rarities
     */
    public static void loadDefaultRaritiesFromConfig() {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            File defaultRaritiesFile = configDir.resolve("itemtooltipenhancer-default-rarities.json").toFile();
            
            if (!defaultRaritiesFile.exists()) {
                LOGGER.info("No default rarities config file found, using calculated defaults");
                return;
            }
            
            JsonObject rootJson;
            try (FileReader reader = new FileReader(defaultRaritiesFile)) {
                rootJson = new Gson().fromJson(reader, JsonObject.class);
            }
            
            if (rootJson.has("default_rarities")) {
                JsonObject defaultRaritiesJson = rootJson.getAsJsonObject("default_rarities");
                
                for (Map.Entry<String, JsonElement> entry : defaultRaritiesJson.entrySet()) {
                    try {
                        ResourceLocation id = new ResourceLocation(entry.getKey());
                        String rarityStr = entry.getValue().getAsString();
                        ItemRarity rarity = ItemRarity.valueOf(rarityStr);
                        
                        DEFAULT_RARITIES.put(id, rarity);
                    } catch (Exception e) {
                        LOGGER.warn("Invalid default rarity entry: {} = {}", 
                            entry.getKey(), entry.getValue().getAsString());
                    }
                }
                
                LOGGER.info("Loaded {} default rarities from config file", DEFAULT_RARITIES.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load default rarities from config file", e);
        }
    }

    /**
     * Stores the default rarities for all items after manual assignments
     */
    private static void saveDefaultRarities() {
        // Clear the map first
        DEFAULT_RARITIES.clear();
        
        // Go through all registered items and calculate their default rarities
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) continue;
            
            // Determine the rarity - either manually set or calculated
            ItemRarity rarity;
            if (ITEM_RARITIES.containsKey(id)) {
                rarity = ITEM_RARITIES.get(id);
            } else {
                rarity = determineItemRarity(item);
            }
            
            // Store as default
            DEFAULT_RARITIES.put(id, rarity);
        }
        
        // Save the default rarities to a config file for reference/editing
        saveDefaultRaritiesToConfig();
        
        LOGGER.info("Stored default rarities for {} items", DEFAULT_RARITIES.size());
    }
}