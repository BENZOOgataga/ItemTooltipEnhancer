package net.flazesmp.flazesmpitems.event;

import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

public class ItemTooltipEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        
        ItemRarity rarity = RarityManager.getRarity(item);
        
        // Apply rarity color to the item's name
        Component originalName = event.getItemStack().getHoverName();
        Component coloredName = Component.literal(originalName.getString())
                .withStyle(Style.EMPTY.withColor(rarity.getColor().getColor()));
        
        // Replace the original name with our colored version
        event.getToolTip().set(0, coloredName);
        
        // Get the item category if available
        String category = RarityManager.getItemCategory(item);
        
        if (category != null && !category.isEmpty()) {
            // Add category line with dark gray color
            Component categoryLine = Component.literal(category)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY));
            event.getToolTip().add(1, categoryLine);
            
            // Add rarity after category
            Component rarityLine = Component.literal(rarity.getName().toUpperCase())
                    .withStyle(Style.EMPTY.withColor(rarity.getColor().getColor()).withBold(true));
            event.getToolTip().add(2, rarityLine);
        } else {
            // No category, just add rarity
            Component rarityLine = Component.literal(rarity.getName().toUpperCase())
                    .withStyle(Style.EMPTY.withColor(rarity.getColor().getColor()).withBold(true));
            event.getToolTip().add(1, rarityLine);
        }
    }
}