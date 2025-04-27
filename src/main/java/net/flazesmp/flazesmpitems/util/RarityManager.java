package net.flazesmp.flazesmpitems.util;

import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.HashMap;
import java.util.Map;

public class RarityManager {
    private static final Map<Item, ItemRarity> ITEM_RARITY_MAP = new HashMap<>();

    public static void initialize() {
        // Assign rarities to all registered items
        ForgeRegistries.ITEMS.forEach(item -> {
            ItemRarity rarity = determineRarity(item);
            ITEM_RARITY_MAP.put(item, rarity);
        });
    }

    public static ItemRarity getRarity(Item item) {
        return ITEM_RARITY_MAP.getOrDefault(item, ItemRarity.COMMON);
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