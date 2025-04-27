package net.flazesmp.flazesmpitems.util;

import net.minecraft.ChatFormatting;

/**
 * Enum representing different item rarities
 */
public enum ItemRarity {
    COMMON("Common", ChatFormatting.WHITE),
    UNCOMMON("Uncommon", ChatFormatting.GREEN),
    RARE("Rare", ChatFormatting.BLUE),
    EPIC("Epic", ChatFormatting.DARK_PURPLE),
    LEGENDARY("Legendary", ChatFormatting.GOLD),
    MYTHIC("Mythic", ChatFormatting.LIGHT_PURPLE),
    SPECIAL("Special", ChatFormatting.AQUA),
    ADMIN("Admin", ChatFormatting.RED);

    private final String name;
    private final ChatFormatting color;

    ItemRarity(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return this.name;
    }

    public ChatFormatting getColor() {
        return this.color;
    }

    /**
     * Parse a rarity from its string name
     */
    public static ItemRarity fromString(String rarityName) {
        for (ItemRarity rarity : values()) {
            if (rarity.name().equalsIgnoreCase(rarityName) || 
                rarity.getName().equalsIgnoreCase(rarityName)) {
                return rarity;
            }
        }
        return COMMON; // Default to common if not found
    }
}