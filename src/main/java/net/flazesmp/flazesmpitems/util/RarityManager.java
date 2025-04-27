package net.flazesmp.flazesmpitems.util;

import net.minecraft.world.item.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class RarityManager {
    private static final Map<Item, ItemRarity> ITEM_RARITY_MAP = new HashMap<>();
    private static final Map<Item, String> ITEM_CATEGORY_MAP = new HashMap<>();
    
    // Categories based on vanilla groups
    private static final Map<String, String> CATEGORY_MAPPING = new HashMap<>();
    
    static {
        // Initialize category mappings
        CATEGORY_MAPPING.put("building_blocks", "Building Blocks");
        CATEGORY_MAPPING.put("decorations", "Decoration");
        CATEGORY_MAPPING.put("redstone", "Redstone");
        CATEGORY_MAPPING.put("transportation", "Transportation");
        CATEGORY_MAPPING.put("misc", "Miscellaneous");
        CATEGORY_MAPPING.put("food", "Food");
        CATEGORY_MAPPING.put("tools", "Tools");
        CATEGORY_MAPPING.put("combat", "Combat");
        CATEGORY_MAPPING.put("brewing", "Brewing");
        CATEGORY_MAPPING.put("materials", "Materials");
        CATEGORY_MAPPING.put("spawn_eggs", "Spawn Eggs");
    }

    public static void initialize() {
        // Assign rarities to all registered items
        ForgeRegistries.ITEMS.forEach(item -> {
            ItemRarity rarity = determineRarity(item);
            ITEM_RARITY_MAP.put(item, rarity);
            
            // Determine category and store it
            String category = determineCategory(item);
            if (category != null) {
                ITEM_CATEGORY_MAP.put(item, category);
            }
        });
    }

    public static ItemRarity getRarity(Item item) {
        return ITEM_RARITY_MAP.getOrDefault(item, ItemRarity.COMMON);
    }
    
    public static String getItemCategory(Item item) {
        return ITEM_CATEGORY_MAP.get(item);
    }
    
    private static String determineCategory(Item item) {
        // Try to determine category based on item properties
        if (item instanceof ArmorItem) {
            return "Armor";
        } else if (item instanceof SwordItem) {
            return "Weapons";
        } else if (item instanceof TieredItem) {
            if (item instanceof AxeItem) {
                return "Tools";
            } else if (item instanceof PickaxeItem) {
                return "Tools";
            } else if (item instanceof ShovelItem) {
                return "Tools";
            } else if (item instanceof HoeItem) {
                return "Tools";
            }
        } else if (item instanceof BlockItem) {
            return "Blocks";
        } else if (item instanceof PotionItem || item == Items.BREWING_STAND) {
            return "Brewing";
        } else if (item.isEdible()) {
            return "Food";
        } else if (item instanceof EnchantedBookItem) {
            return "Enchanted Books";
        }
        
        // Special cases
        if (item == Items.IRON_INGOT || item == Items.GOLD_INGOT || item == Items.DIAMOND ||
            item == Items.EMERALD || item == Items.NETHERITE_INGOT || item == Items.COAL ||
            item == Items.REDSTONE || item == Items.LAPIS_LAZULI || item == Items.QUARTZ) {
            return "Materials";
        }
        
        // For items where we can't determine a clear category
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(item);
        if (registryName != null) {
            String path = registryName.getPath();
            
            // Try to infer category based on item name
            if (path.contains("_sword") || path.contains("_axe") || path.contains("bow") ||
                path.contains("arrow") || path.contains("shield")) {
                return "Weapons";
            } else if (path.contains("_pickaxe") || path.contains("_shovel") || path.contains("_hoe")) {
                return "Tools";
            } else if (path.contains("_helmet") || path.contains("_chestplate") || 
                       path.contains("_leggings") || path.contains("_boots")) {
                return "Armor";
            } else if (path.contains("potion") || path.contains("brew")) {
                return "Brewing";
            }
        }
        
        return null;
    }

    private static ItemRarity determineRarity(Item item) {
        if (item instanceof EnchantedBookItem) {
            return ItemRarity.RARE;
        }
        
        // Check for specific items
        if (item == Items.NETHER_STAR || item == Items.DRAGON_EGG || 
            item == Items.END_CRYSTAL || item == Items.BEACON || 
            item == Items.ELYTRA || item == Items.TOTEM_OF_UNDYING || 
            item == Items.TRIDENT || item == Items.ENCHANTED_GOLDEN_APPLE) {
            return ItemRarity.LEGENDARY;
        }
        
        // Check for netherite items
        if (item instanceof TieredItem tieredItem && tieredItem.getTier() == Tiers.NETHERITE) {
            return ItemRarity.LEGENDARY;
        }
        
        if (item instanceof ArmorItem armorItem) {
            if (armorItem.getMaterial() == ArmorMaterials.NETHERITE) {
                return ItemRarity.LEGENDARY;
            } else if (armorItem.getMaterial() == ArmorMaterials.DIAMOND) {
                return ItemRarity.EPIC;
            } else if (armorItem.getMaterial() == ArmorMaterials.GOLD || 
                       armorItem.getMaterial() == ArmorMaterials.IRON) {
                return ItemRarity.RARE;
            } else if (armorItem.getMaterial() == ArmorMaterials.CHAIN || 
                       armorItem.getMaterial() == ArmorMaterials.LEATHER) {
                return ItemRarity.UNCOMMON;
            }
        }

        // Check for specific tools and weapons
        if (item instanceof TieredItem tieredItem) {
            if (tieredItem.getTier() == Tiers.DIAMOND) {
                return ItemRarity.EPIC;
            } else if (tieredItem.getTier() == Tiers.GOLD || 
                       tieredItem.getTier() == Tiers.IRON) {
                return ItemRarity.RARE;
            } else if (tieredItem.getTier() == Tiers.STONE) {
                return ItemRarity.UNCOMMON;
            }
        }
        
        if (item == Items.DRAGON_BREATH || 
            item == Items.HEART_OF_THE_SEA || 
            item == Items.NAUTILUS_SHELL || 
            item == Items.END_PORTAL_FRAME || 
            item == Items.CONDUIT || 
            item == Items.SHULKER_SHELL || 
            item == Items.MUSIC_DISC_PIGSTEP ||
            item instanceof RecordItem) {
            return ItemRarity.EPIC;
        }
        
        if (item == Items.BLAZE_ROD || 
            item == Items.GHAST_TEAR || 
            item == Items.ENDER_PEARL || 
            item == Items.ENDER_EYE || 
            item == Items.DIAMOND || 
            item == Items.EMERALD || 
            item == Items.EXPERIENCE_BOTTLE) {
            return ItemRarity.RARE;
        }
        
        if (item == Items.GOLD_INGOT || 
            item == Items.IRON_INGOT || 
            item == Items.QUARTZ || 
            item == Items.LAPIS_LAZULI || 
            item == Items.REDSTONE ||
            item == Items.SLIME_BALL ||
            item == Items.TURTLE_EGG) {
            return ItemRarity.UNCOMMON;
        }

        // Default rarity
        return ItemRarity.COMMON;
    }
}