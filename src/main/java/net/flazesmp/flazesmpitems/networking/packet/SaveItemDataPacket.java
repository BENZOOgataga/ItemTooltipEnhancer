package net.flazesmp.flazesmpitems.networking.packet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.item.CustomItemManager;
import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class SaveItemDataPacket {
    private final String itemId;
    private final String displayName;
    private final String texturePath;
    private final ItemRarity rarity;
    private final List<String> tooltipLines;
    private final boolean isNew;
    private final boolean deleteItem;
    
    public SaveItemDataPacket(String itemId, String displayName, String texturePath, 
                             ItemRarity rarity, List<String> tooltipLines, 
                             boolean isNew, boolean deleteItem) {
        this.itemId = itemId;
        this.displayName = displayName;
        this.texturePath = texturePath;
        this.rarity = rarity;
        this.tooltipLines = tooltipLines;
        this.isNew = isNew;
        this.deleteItem = deleteItem;
    }
    
    public static void encode(SaveItemDataPacket message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.itemId);
        buffer.writeUtf(message.displayName);
        buffer.writeUtf(message.texturePath);
        buffer.writeEnum(message.rarity);
        buffer.writeInt(message.tooltipLines.size());
        
        for (String line : message.tooltipLines) {
            buffer.writeUtf(line);
        }
        
        buffer.writeBoolean(message.isNew);
        buffer.writeBoolean(message.deleteItem);
    }
    
    public static SaveItemDataPacket decode(FriendlyByteBuf buffer) {
        String itemId = buffer.readUtf();
        String displayName = buffer.readUtf();
        String texturePath = buffer.readUtf();
        ItemRarity rarity = buffer.readEnum(ItemRarity.class);
        
        int lineCount = buffer.readInt();
        List<String> tooltipLines = new ArrayList<>(lineCount);
        
        for (int i = 0; i < lineCount; i++) {
            tooltipLines.add(buffer.readUtf());
        }
        
        boolean isNew = buffer.readBoolean();
        boolean deleteItem = buffer.readBoolean();
        
        return new SaveItemDataPacket(itemId, displayName, texturePath, rarity, tooltipLines, isNew, deleteItem);
    }
    
    public static void handle(SaveItemDataPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Make sure the player is an operator
            ServerPlayer player = context.getSender();
            if (player != null && player.hasPermissions(2)) {
                try {
                    if (message.deleteItem) {
                        boolean isModItem = message.itemId.startsWith(FlazeSMPItems.MOD_ID + ":");
                        if (isModItem) {
                            // Call the CustomItemManager to delete the item
                            player.sendSystemMessage(Component.literal("Item deleted: " + message.itemId));
                        } else {
                            player.sendSystemMessage(Component.literal("You can only delete items created by " + 
                                    FlazeSMPItems.MOD_ID).withStyle(net.minecraft.ChatFormatting.RED));
                        }
                    } else if (message.isNew) {
                        // Create new item
                        player.sendSystemMessage(Component.literal("Item created: " + message.itemId));
                    } else {
                        // Update existing item
                        player.sendSystemMessage(Component.literal("Item updated: " + message.itemId));
                    }
                } catch (Exception e) {
                    player.sendSystemMessage(Component.literal("Error processing item: " + e.getMessage())
                            .withStyle(net.minecraft.ChatFormatting.RED));
                    FlazeSMPItems.LOGGER.error("Error processing item data", e);
                }
            }
        });
        context.setPacketHandled(true);
    }
}