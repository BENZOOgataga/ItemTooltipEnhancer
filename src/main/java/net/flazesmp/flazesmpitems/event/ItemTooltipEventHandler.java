package net.flazesmp.flazesmpitems.event;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.config.CustomItemsConfig;
import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
        
        // Check for custom item data first
        CustomItemsConfig.CustomItemData customData = CustomItemsConfig.getCustomItemData(item);
        if (customData != null) {
            // Apply custom display name if set
            if (customData.displayName != null && !customData.displayName.isEmpty()) {
                Component coloredName = Component.literal(customData.displayName)
                        .withStyle(Style.EMPTY.withColor(customData.rarity.getColor().getColor()));
                event.getToolTip().set(0, coloredName);
            } else {
                // Apply rarity color to original name
                Component originalName = event.getItemStack().getHoverName();
                Component coloredName = Component.literal(originalName.getString())
                        .withStyle(Style.EMPTY.withColor(customData.rarity.getColor().getColor()));
                event.getToolTip().set(0, coloredName);
            }
            
            // Insert custom tooltip lines after the name and before rarity
            int insertIndex = 1;
            if (customData.tooltipLines != null && !customData.tooltipLines.isEmpty()) {
                for (String line : customData.tooltipLines) {
                    Component tooltipLine = Component.literal(line)
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                    event.getToolTip().add(insertIndex++, tooltipLine);
                }
            }
            
            // Add rarity line
            Component rarityLine = Component.literal(customData.rarity.getName().toUpperCase())
                .withStyle(Style.EMPTY.withColor(customData.rarity.getColor().getColor()).withBold(true));
            
            // Get item category
            String category = RarityManager.getItemCategory(item);
            if (category != null && !category.isEmpty()) {
                Component categoryLine = Component.literal(category)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY));
                event.getToolTip().add(insertIndex++, categoryLine);
            }
            
            event.getToolTip().add(insertIndex, rarityLine);
        } else {
            // Normal rarity handling for non-custom items
            ItemRarity rarity = RarityManager.getRarity(item);
            
            // Apply rarity color to item name
            Component originalName = event.getItemStack().getHoverName();
            Component coloredName = Component.literal(originalName.getString())
                    .withStyle(Style.EMPTY.withColor(rarity.getColor().getColor()));
            event.getToolTip().set(0, coloredName);
            
            // Get item category
            String category = RarityManager.getItemCategory(item);
            
            // Create rarity component
            Component rarityLine = Component.literal(rarity.getName().toUpperCase())
                .withStyle(Style.EMPTY.withColor(rarity.getColor().getColor()).withBold(true));
                
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