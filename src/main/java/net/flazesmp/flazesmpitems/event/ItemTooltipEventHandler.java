package net.flazesmp.flazesmpitems.event;

import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ItemTooltipEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        
        ItemRarity rarity = RarityManager.getRarity(item);
        
        // Apply rarity color to the item's name (without making it bold)
        Component originalName = event.getItemStack().getHoverName();
        Component coloredName = Component.literal(originalName.getString())
                .withStyle(Style.EMPTY.withColor(rarity.getColor().getColor()));
        
        // Replace the original name with our colored version
        event.getToolTip().set(0, coloredName);
        
        // Add the rarity in capital letters with color on the next line
        Component rarityLine = Component.literal(rarity.getName().toUpperCase())
                .withStyle(Style.EMPTY.withColor(rarity.getColor().getColor()).withBold(true));
        
        event.getToolTip().add(1, rarityLine);
    }
}