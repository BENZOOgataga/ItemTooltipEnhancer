package net.flazesmp.flazesmpitems.event;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = FlazeSMPItems.MOD_ID)
public class ItemTooltipEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemTooltipEventHandler.class);

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        if (itemId == null) return;
        
        // Apply custom data to the stack
        RarityManager.applyCustomDataToItemStack(stack);
        
        // Get rarity from RarityManager
        ItemRarity rarity = RarityManager.getRarity(item);
        
        // Get item category
        String category = RarityManager.getItemCategory(item);
        
        // Create rarity component
        Component rarityLine = Component.literal(rarity.getName().toUpperCase())
            .withStyle(Style.EMPTY.withColor(rarity.getColor().getColor()).withBold(true));
            
        // Add rarity line and category if it exists - check if we already have this line
        boolean hasRarityLine = false;
        for (Component line : event.getToolTip()) {
            if (line.getString().equals(rarity.getName().toUpperCase())) {
                // Check if color matches - need to check if colors are available first
                if (line.getStyle().getColor() != null && 
                    line.getStyle().getColor().getValue() == rarity.getColor().getColor()) {
                    hasRarityLine = true;
                    break;
                }
            }
        }
        
        if (!hasRarityLine) {
            if (category != null && !category.isEmpty()) {
                Component categoryLine = Component.literal(category)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY));
                event.getToolTip().add(1, categoryLine);
                event.getToolTip().add(2, rarityLine);
            } else {
                event.getToolTip().add(1, rarityLine);
            }
        }
        
        // Remove mod name from tooltip
        removeModNameFromTooltip(event.getToolTip());
    }
    
    private static void removeModNameFromTooltip(List<Component> tooltip) {
        if (tooltip.size() > 1) {
            Component lastLine = tooltip.get(tooltip.size() - 1);
            if (lastLine.getStyle().getColor() != null && 
                (lastLine.getStyle().getColor().getValue() == ChatFormatting.BLUE.getColor() ||
                 lastLine.getStyle().getColor().getValue() == ChatFormatting.AQUA.getColor())) {
                tooltip.remove(tooltip.size() - 1);
            }
        }
    }
}