package net.flazesmp.flazesmpitems.network;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.config.CustomItemsConfig;
import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class UpdateItemPacket {
    private final ResourceLocation itemId;
    private final String displayName;
    private final List<String> tooltipLines;
    private final ItemRarity rarity;
    private final boolean delete;
    
    public UpdateItemPacket(ResourceLocation itemId, String displayName, List<String> tooltipLines, ItemRarity rarity, boolean delete) {
        this.itemId = itemId;
        this.displayName = displayName;
        this.tooltipLines = tooltipLines;
        this.rarity = rarity;
        this.delete = delete;
    }
    
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(itemId);
        buffer.writeBoolean(delete);
        
        if (!delete) {
            buffer.writeUtf(displayName != null ? displayName : "");
            
            if (tooltipLines != null) {
                buffer.writeInt(tooltipLines.size());
                for (String line : tooltipLines) {
                    buffer.writeUtf(line);
                }
            } else {
                buffer.writeInt(0);
            }
            
            buffer.writeEnum(rarity);
        }
    }
    
    public static UpdateItemPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation itemId = buffer.readResourceLocation();
        boolean delete = buffer.readBoolean();
        
        if (delete) {
            return new UpdateItemPacket(itemId, null, null, null, true);
        } else {
            String displayName = buffer.readUtf();
            
            int tooltipSize = buffer.readInt();
            List<String> tooltipLines = new ArrayList<>(tooltipSize);
            for (int i = 0; i < tooltipSize; i++) {
                tooltipLines.add(buffer.readUtf());
            }
            
            ItemRarity rarity = buffer.readEnum(ItemRarity.class);
            
            return new UpdateItemPacket(itemId, displayName, tooltipLines, rarity, false);
        }
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Only process on server
            if (!ctx.get().getDirection().getReceptionSide().isClient()) {
                // Check if player has permission (op)
                if (!ctx.get().getSender().hasPermissions(2)) {
                    FlazeSMPItems.LOGGER.warn("Player {} tried to edit items without permission", 
                        ctx.get().getSender().getScoreboardName());
                    return;
                }
                
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                if (item == null) {
                    FlazeSMPItems.LOGGER.error("Received item edit for unknown item: {}", itemId);
                    return;
                }
                
                if (delete) {
                    // Delete the custom item data
                    boolean wasModItem = false;
                    CustomItemsConfig.CustomItemData existingData = CustomItemsConfig.getCustomItemData(item);
                    if (existingData != null) {
                        wasModItem = existingData.isModItem;
                    }
                    
                    // Only allow deletion of mod-created items
                    if (wasModItem) {
                        CustomItemsConfig.removeCustomItemData(item);
                        CustomItemsConfig.save();
                        FlazeSMPItems.LOGGER.info("Deleted custom item data for {}", itemId);
                    } else {
                        FlazeSMPItems.LOGGER.warn("Attempted to delete non-mod item: {}", itemId);
                    }
                } else {
                    // Check if this is a mod item
                    boolean isModItem = false;
                    CustomItemsConfig.CustomItemData existingData = CustomItemsConfig.getCustomItemData(item);
                    if (existingData != null) {
                        isModItem = existingData.isModItem;
                    }
                    
                    // Update the item data
                    CustomItemsConfig.setCustomItemData(item, 
                        new CustomItemsConfig.CustomItemData(displayName, tooltipLines, rarity, isModItem));
                    CustomItemsConfig.save();
                    
                    FlazeSMPItems.LOGGER.info("Updated custom item data for {}", itemId);
                }
            }
        });
        
        ctx.get().setPacketHandled(true);
    }
}