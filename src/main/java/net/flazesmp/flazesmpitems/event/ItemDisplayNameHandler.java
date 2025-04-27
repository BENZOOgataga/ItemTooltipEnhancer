package net.flazesmp.flazesmpitems.event;

import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ItemDisplayNameHandler {

    /**
     * Event handler to modify item display names with proper rarity colors
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        // Activer la fonctionnalité par défaut
        // Plus tard, nous pourrons ajouter une option de configuration
        
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        // Get the item's rarity
        Item item = stack.getItem();
        ItemRarity rarity = RarityManager.getRarity(item);

        // Get the display name component from the tooltip
        Component displayName = event.getToolTip().get(0);
        
        // Create a new display name with the rarity's color
        MutableComponent newName;
        
        // If the item has a custom name (renamed with an anvil), preserve that formatting
        if (stack.hasCustomHoverName()) {
            // Just adjust color if needed, but keep custom formatting
            Style originalStyle = displayName.getStyle();
            Style newStyle = originalStyle.withColor(rarity.getColor().getColor());
            newName = ((MutableComponent)displayName).withStyle(newStyle);
        } else {
            // For regular items, completely replace the name with proper rarity color
            String plainName = displayName.getString();
            newName = Component.literal(plainName).withStyle(Style.EMPTY.withColor(rarity.getColor().getColor()));
        }
        
        // Replace the first line of the tooltip with our color-corrected name
        event.getToolTip().set(0, newName);
    }
    
    /**
     * Helper method to determine if a specific chat formatting is applied to a component
     */
    private static boolean hasFormatting(Component component, ChatFormatting formatting) {
        Style style = component.getStyle();
        if (formatting.isColor()) {
            return style.getColor() != null && 
                   style.getColor().getValue() == formatting.getColor();
        } else {
            return switch (formatting) {
                case BOLD -> style.isBold();
                case ITALIC -> style.isItalic();
                case UNDERLINE -> style.isUnderlined();
                case STRIKETHROUGH -> style.isStrikethrough();
                case OBFUSCATED -> style.isObfuscated();
                default -> false;
            };
        }
    }
}