package net.flazesmp.flazesmpitems.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.networking.ModMessages;
import net.flazesmp.flazesmpitems.networking.packet.SaveItemDataPacket;
import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

@OnlyIn(Dist.CLIENT)
public class ItemEditorScreen extends Screen {
    private final Screen parentScreen;
    private final Item editItem;
    private final boolean isNewItem;
    
    private EditBox itemIdBox;
    private EditBox displayNameBox;
    private EditBox texturePathBox;
    private CycleButton<ItemRarity> rarityButton;
    private EditBox tooltipLineBox;
    private final List<String> tooltipLines = new ArrayList<>();
    private int selectedTooltipLine = -1;
    
    public ItemEditorScreen(Screen parentScreen, Item editItem) {
        super(Component.literal(editItem == null ? "Create Item" : "Edit Item"));
        this.parentScreen = parentScreen;
        this.editItem = editItem;
        this.isNewItem = editItem == null;
        
        if (!isNewItem) {
            // Load existing tooltip lines if any
            // This is just a placeholder - in a real implementation you'd get the actual tooltip lines
            tooltipLines.add("Example tooltip line 1");
            tooltipLines.add("Example tooltip line 2");
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = width / 2;
        int centerY = height / 2;
        int fieldWidth = 200;
        
        // Item ID field
        addLabel(centerX - 100, centerY - 110, "Item ID:");
        itemIdBox = new EditBox(font, centerX - 100, centerY - 95, fieldWidth, 20, Component.empty());
        itemIdBox.setMaxLength(100);
        
        if (isNewItem) {
            itemIdBox.setValue(FlazeSMPItems.MOD_ID + ":custom_item");
            itemIdBox.setEditable(true);
        } else {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(editItem);
            itemIdBox.setValue(itemId != null ? itemId.toString() : "");
            itemIdBox.setEditable(false); // Can't edit existing item IDs
        }
        
        addRenderableWidget(itemIdBox);
        
        // Display Name field
        addLabel(centerX - 100, centerY - 70, "Display Name:");
        displayNameBox = new EditBox(font, centerX - 100, centerY - 55, fieldWidth, 20, Component.empty());
        displayNameBox.setMaxLength(100);
        displayNameBox.setValue(isNewItem ? "Custom Item" : editItem.getDescription().getString());
        addRenderableWidget(displayNameBox);
        
        // Texture Path field
        addLabel(centerX - 100, centerY - 30, "Texture Path:");
        texturePathBox = new EditBox(font, centerX - 100, centerY - 15, fieldWidth, 20, Component.empty());
        texturePathBox.setMaxLength(100);
        texturePathBox.setValue(isNewItem ? "minecraft:item/diamond" : "minecraft:item/diamond"); // Replace with actual texture path
        addRenderableWidget(texturePathBox);
        
        // Rarity selector with fixed type inference
        addLabel(centerX - 100, centerY + 10, "Rarity:");
        
        // Create a proper function that handles ItemRarity specifically
        Function<ItemRarity, Component> nameFunc = (ItemRarity rarity) -> Component.literal(rarity.name());
        
        // Fixed CycleButton creation with proper parameters and explicit types
        rarityButton = CycleButton.<ItemRarity>builder(nameFunc)
                .withValues(ItemRarity.values())
                .withInitialValue(ItemRarity.COMMON)
                .create(centerX - 100, centerY + 25, fieldWidth, 20, Component.literal("Rarity"));
        
        addRenderableWidget(rarityButton);
        
        // Tooltip editor
        addLabel(centerX - 100, centerY + 50, "Tooltip Lines:");
        tooltipLineBox = new EditBox(font, centerX - 100, centerY + 65, fieldWidth - 60, 20, Component.empty());
        tooltipLineBox.setMaxLength(100);
        addRenderableWidget(tooltipLineBox);
        
        // Add button for tooltip lines
        addRenderableWidget(Button.builder(Component.literal("+"), b -> addTooltipLine())
                .pos(centerX + fieldWidth - 155, centerY + 65)
                .size(25, 20)
                .build());
        
        // Remove button for tooltip lines
        addRenderableWidget(Button.builder(Component.literal("-"), b -> removeTooltipLine())
                .pos(centerX + fieldWidth - 125, centerY + 65)
                .size(25, 20)
                .build());
        
        // Save button
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveItem())
                .pos(centerX - 100, centerY + 120)
                .size(95, 20)
                .build());
        
        // Cancel button
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> cancel())
                .pos(centerX + 5, centerY + 120)
                .size(95, 20)
                .build());
                
        // Delete button for existing items
        if (!isNewItem) {
            boolean isModItem = ForgeRegistries.ITEMS.getKey(editItem).getNamespace().equals(FlazeSMPItems.MOD_ID);
            
            Button deleteButton = Button.builder(Component.literal("Delete Item"), b -> deleteItem())
                    .pos(centerX - 60, centerY + 150)
                    .size(120, 20)
                    .build();
            
            // Only allow deletion of mod's own items
            deleteButton.active = isModItem;
            
            addRenderableWidget(deleteButton);
        }
    }
    
    private void addLabel(int x, int y, String text) {
        // Labels will be rendered in the render method
    }
    
    private void addTooltipLine() {
        String line = tooltipLineBox.getValue().trim();
        if (!line.isEmpty()) {
            tooltipLines.add(line);
            tooltipLineBox.setValue("");
            selectedTooltipLine = tooltipLines.size() - 1;
        }
    }
    
    private void removeTooltipLine() {
        if (selectedTooltipLine >= 0 && selectedTooltipLine < tooltipLines.size()) {
            tooltipLines.remove(selectedTooltipLine);
            selectedTooltipLine = -1;
            tooltipLineBox.setValue("");
        }
    }
    
    private void saveItem() {
        String itemId = itemIdBox.getValue().trim();
        String displayName = displayNameBox.getValue().trim();
        String texturePath = texturePathBox.getValue().trim();
        ItemRarity rarity = rarityButton.getValue();
        
        // Send packet to server to save item
        ModMessages.sendToServer(new SaveItemDataPacket(
            itemId, displayName, texturePath, rarity, tooltipLines, isNewItem, false));
        
        // Return to parent screen
        minecraft.setScreen(parentScreen);
    }
    
    private void deleteItem() {
        if (isNewItem) return; // Can't delete an item that doesn't exist yet
        
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(editItem);
        if (itemId != null) {
            // Send packet to server to delete item
            ModMessages.sendToServer(new SaveItemDataPacket(
                itemId.toString(), "", "", ItemRarity.COMMON, new ArrayList<>(), false, true));
        }
        
        // Return to parent screen
        minecraft.setScreen(parentScreen);
    }
    
    private void cancel() {
        minecraft.setScreen(parentScreen);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        
        // Draw title
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 140, 0xFFFFFF);
        
        // Draw item preview if editing an existing item
        if (!isNewItem) {
            ItemStack stack = new ItemStack(editItem);
            graphics.renderItem(stack, width / 2 + 120, height / 2 - 110);
            graphics.renderItemDecorations(font, stack, width / 2 + 120, height / 2 - 110);
        }
        
        // Draw labels
        int centerX = width / 2;
        int centerY = height / 2;
        
        graphics.drawString(font, "Item ID:", centerX - 100, centerY - 110, 0xFFFFFF);
        graphics.drawString(font, "Display Name:", centerX - 100, centerY - 70, 0xFFFFFF);
        graphics.drawString(font, "Texture Path:", centerX - 100, centerY - 30, 0xFFFFFF);
        graphics.drawString(font, "Rarity:", centerX - 100, centerY + 10, 0xFFFFFF);
        graphics.drawString(font, "Tooltip Lines:", centerX - 100, centerY + 50, 0xFFFFFF);
        
        // Draw tooltip lines
        int startY = centerY + 90;
        for (int i = 0; i < tooltipLines.size(); i++) {
            boolean isSelected = i == selectedTooltipLine;
            int lineColor = isSelected ? 0xFFFF55 : 0xFFFFFF;
            graphics.drawString(font, tooltipLines.get(i), centerX - 90, startY + i * 12, lineColor);
        }
        
        if (!isNewItem) {
            boolean isModItem = ForgeRegistries.ITEMS.getKey(editItem).getNamespace().equals(FlazeSMPItems.MOD_ID);
            if (!isModItem) {
                graphics.drawCenteredString(font, "Only ITE mod items can be deleted", centerX, centerY + 175, 0xFF5555);
            }
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check for clicks on tooltip lines
        int centerY = height / 2;
        int startY = centerY + 90;
        int centerX = width / 2;
        
        for (int i = 0; i < tooltipLines.size(); i++) {
            int lineY = startY + i * 12;
            if (mouseY >= lineY && mouseY < lineY + 12 && 
                mouseX >= centerX - 90 && mouseX < centerX + 90) {
                
                selectedTooltipLine = i;
                tooltipLineBox.setValue(tooltipLines.get(i));
                return true;
            }
        }
        
        // Reset selected tooltip line if clicked elsewhere
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}