package net.flazesmp.flazesmpitems.event;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles ensuring display names are applied consistently
 */
@Mod.EventBusSubscriber(modid = FlazeSMPItems.MOD_ID)
public class ItemDisplayNameHandler {

    /**
     * Apply custom data when a player interacts with an item
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemInteract(PlayerInteractEvent event) {
        RarityManager.applyCustomDataToItemStack(event.getItemStack());
    }
    
    /**
     * Apply custom data when a player opens a container
     * This ensures items in chests, etc. display correctly
     */
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        event.getContainer().getItems().forEach(RarityManager::applyCustomDataToItemStack);
    }
    
    /**
     * Apply custom data when player swaps items
     */
    @SubscribeEvent
    public static void onItemSwap(PlayerEvent.ItemPickupEvent event) {
        RarityManager.applyCustomDataToItemStack(event.getStack());
    }
    
    /**
     * Apply custom data when player changes held item slot
     * Using PlayerItemHeldEvent which is more widely available in Forge
     */
    @SubscribeEvent
    public static void onItemHeld(net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent event) {
        // Check if the entity is a player and the slot is the main hand
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player && event.getSlot() == net.minecraft.world.entity.EquipmentSlot.MAINHAND) {
            ItemStack stack = event.getTo();
            if (!stack.isEmpty()) {
                RarityManager.applyCustomDataToItemStack(stack);
            }
        }
    }
}