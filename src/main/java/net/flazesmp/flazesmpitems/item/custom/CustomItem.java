package net.flazesmp.flazesmpitems.item.custom;

import java.util.List;

import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class CustomItem extends Item {
    private final String displayName;
    private final List<String> tooltipLines;
    private final ItemRarity rarity;
    
    public CustomItem(Properties properties, String displayName, List<String> tooltipLines, ItemRarity rarity) {
        super(properties);
        this.displayName = displayName;
        this.tooltipLines = tooltipLines;
        this.rarity = rarity;
    }
    
    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(displayName).withStyle(rarity.getColor());
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, 
                               TooltipFlag flag) {
        // Add custom tooltip lines
        for (String line : tooltipLines) {
            tooltip.add(Component.literal(line));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
    
    public ItemRarity getRarityValue() {
        return rarity;
    }
}