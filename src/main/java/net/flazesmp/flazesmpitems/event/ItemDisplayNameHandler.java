package net.flazesmp.flazesmpitems.event;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.config.ConfigManager;
import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

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
        ItemStack stack = event.getItemStack();
        RarityManager.applyCustomDataToItemStack(stack);
        
        // Optionally reload config on demand for admins holding a debug stick
        if (event.getEntity().hasPermissions(2) && stack.is(net.minecraft.world.item.Items.DEBUG_STICK)) {
            // Only reload config if the player is sneaking and it's a right click
            if (event instanceof PlayerInteractEvent.RightClickItem && event.getEntity().isShiftKeyDown()) {
                ConfigManager.loadAllConfigs();
                event.getEntity().sendSystemMessage(net.minecraft.network.chat.Component.literal("ItemTooltipEnhancer configs reloaded"));
            }
        }
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
     * Apply custom data when player picks up an item
     */
    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        RarityManager.applyCustomDataToItemStack(event.getStack());
    }
    
    /**
     * Apply custom data when player changes held item slot
     */
    @SubscribeEvent
    public static void onItemHeld(net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player && 
            event.getSlot() == net.minecraft.world.entity.EquipmentSlot.MAINHAND) {
            
            ItemStack stack = event.getTo();
            if (!stack.isEmpty()) {
                RarityManager.applyCustomDataToItemStack(stack);
            }
        }
    }
}