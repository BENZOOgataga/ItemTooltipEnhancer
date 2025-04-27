package net.flazesmp.flazesmpitems.util;

import net.minecraft.ChatFormatting;

public enum ItemRarity {
    COMMON("Common", ChatFormatting.WHITE),
    UNCOMMON("Uncommon", ChatFormatting.GREEN),
    RARE("Rare", ChatFormatting.BLUE),
    EPIC("Epic", ChatFormatting.DARK_PURPLE),
    LEGENDARY("Legendary", ChatFormatting.GOLD);

    private final String name;
    private final ChatFormatting color;

    ItemRarity(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String getFormattedName() {
        return color + name;
    }
}