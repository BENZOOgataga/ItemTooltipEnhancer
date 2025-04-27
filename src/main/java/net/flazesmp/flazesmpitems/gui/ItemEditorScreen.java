package net.flazesmp.flazesmpitems.gui;

import net.flazesmp.flazesmpitems.config.CustomItemsConfig;
import net.flazesmp.flazesmpitems.network.NetworkHandler;
import net.flazesmp.flazesmpitems.network.UpdateItemPacket;
import net.flazesmp.flazesmpitems.util.ItemRarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class ItemEditorScreen extends Screen {
    // GUI dimensions
    private static final int GUI_WIDTH = 240;
    private static final int GUI_HEIGHT = 200;
    
    // Colors
    private static final int BACKGROUND_COLOR = 0xDD000000; 
    private static final int PANEL_COLOR = 0xFF333333;
    private static final int PANEL_HIGHLIGHT = 0xFF444444;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int TITLE_COLOR = 0xFFFFAA00;
    private static final int TEXT_COLOR = 0xFFDDDDDD;
    private static final int ACCENT_COLOR = 0xFF3399FF;
    
    private final Screen parentScreen;
    private final Item item;
    private final ResourceLocation itemId;
    private final boolean isCustomItem;
    
    private int guiLeft;
    private int guiTop;
    
    private EditBox displayNameBox;
    private List<EditBox> tooltipLineBoxes = new ArrayList<>();
    private CycleButton<ItemRarity> rarityButton;
    private Button saveButton;
    private Button cancelButton;
    private Button deleteButton;
    private Button addTooltipLineButton;
    private Button removeTooltipLineButton;
    
    private String displayName;
    private List<String> tooltipLines = new ArrayList<>();
    private ItemRarity rarity;
    private boolean isModItem;
    
    public ItemEditorScreen(Item item, Screen parentScreen) {
        super(Component.translatable("itemtooltipenhancer.gui.editor.title"));
        this.item = item;
        this.parentScreen = parentScreen;
        this.itemId = ForgeRegistries.ITEMS.getKey(item);
        
        // Load existing custom data if any
        CustomItemsConfig.CustomItemData customData = CustomItemsConfig.getCustomItemData(item);
        this.isCustomItem = customData != null;
        
        if (customData != null) {
            this.displayName = customData.displayName;
            this.tooltipLines = new ArrayList<>(customData.tooltipLines);
            this.rarity = customData.rarity;
            this.isModItem = customData.isModItem;
        } else {
            this.displayName = item.getDescription().getString();
            this.tooltipLines = new ArrayList<>();
            this.rarity = net.flazesmp.flazesmpitems.util.RarityManager.getRarity(item);
            this.isModItem = false;
        }
    }
    
    @Override
    protected void init() {
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
        
        int labelWidth = 60; 
        int fieldX = this.guiLeft + labelWidth + 10;
        int fieldWidth = GUI_WIDTH - labelWidth - 20;
        
        // Display name field
        this.displayNameBox = new EditBox(this.font, fieldX, this.guiTop + 30, fieldWidth, 20,
                Component.translatable("itemtooltipenhancer.gui.editor.displayName"));
        this.displayNameBox.setValue(this.displayName);
        this.displayNameBox.setMaxLength(100);
        this.displayNameBox.setBordered(true);
        this.displayNameBox.setTextColor(0xFFFFFFFF);
        this.addRenderableWidget(this.displayNameBox);
        
        // Tooltip lines
        this.tooltipLineBoxes.clear();
        if (this.tooltipLines.isEmpty()) {
            this.tooltipLines.add("");
        }
        
        int tooltipStartY = this.guiTop + 70;
        int tooltipLineHeight = 22;
        int maxLines = Math.min(this.tooltipLines.size(), 5);
        
        for (int i = 0; i < maxLines; i++) {
            EditBox tooltipBox = new EditBox(this.font, fieldX, tooltipStartY + (i * tooltipLineHeight), 
                    fieldWidth - 25, 20, Component.translatable("itemtooltipenhancer.gui.editor.tooltipLine", i + 1));
            tooltipBox.setValue(this.tooltipLines.get(i));
            tooltipBox.setMaxLength(100);
            tooltipBox.setBordered(true);
            tooltipBox.setTextColor(0xFFFFFFFF);
            this.tooltipLineBoxes.add(tooltipBox);
            this.addRenderableWidget(tooltipBox);
        }
        
        // Add/remove tooltip line buttons
        this.addTooltipLineButton = Button.builder(Component.literal("+"), button -> {
            if (tooltipLines.size() < 5) {
                tooltipLines.add("");
                this.init();
            }
        }).bounds(fieldX + fieldWidth - 20, tooltipStartY, 20, 20).build();
        
        this.removeTooltipLineButton = Button.builder(Component.literal("-"), button -> {
            if (tooltipLines.size() > 1) {
                tooltipLines.remove(tooltipLines.size() - 1);
                this.init();
            }
        }).bounds(fieldX + fieldWidth - 20, tooltipStartY + 21, 20, 20).build();
        
        this.addRenderableWidget(this.addTooltipLineButton);
        this.addRenderableWidget(this.removeTooltipLineButton);
        
        // Rarity selection
        CycleButton.Builder<ItemRarity> builder = CycleButton.builder(rarity -> Component.literal(rarity.getName()));
        this.rarityButton = builder
                .withValues(ItemRarity.values())
                .withInitialValue(this.rarity)
                .create(fieldX, this.guiTop + 160, fieldWidth, 20, Component.empty(), (button, value) -> {
                    this.rarity = value;
                });
        this.addRenderableWidget(this.rarityButton);
        
        // Bottom buttons
        int buttonWidth = 60;
        int buttonSpacing = 10;
        int buttonsY = this.guiTop + GUI_HEIGHT - 25;
        int buttonsStartX = this.guiLeft + (GUI_WIDTH / 2) - buttonWidth - (buttonSpacing / 2);
        
        this.saveButton = Button.builder(Component.translatable("itemtooltipenhancer.gui.editor.save"), button -> {
            saveChanges();
            Minecraft.getInstance().setScreen(this.parentScreen);
        }).bounds(buttonsStartX, buttonsY, buttonWidth, 20).build();
        
        this.cancelButton = Button.builder(Component.translatable("itemtooltipenhancer.gui.editor.cancel"), button -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
        }).bounds(buttonsStartX + buttonWidth + buttonSpacing, buttonsY, buttonWidth, 20).build();
        
        this.addRenderableWidget(this.saveButton);
        this.addRenderableWidget(this.cancelButton);
        
        // Delete button
        this.deleteButton = Button.builder(Component.translatable("itemtooltipenhancer.gui.editor.delete"), button -> {
            if (this.isModItem) {
                NetworkHandler.sendToServer(new UpdateItemPacket(this.itemId, null, null, null, true));
                Minecraft.getInstance().setScreen(this.parentScreen);
            } else {
                Minecraft.getInstance().setScreen(new MessageScreen(
                    Component.translatable("itemtooltipenhancer.gui.editor.cannotDelete"),
                    Component.translatable("itemtooltipenhancer.gui.editor.cannotDeleteInfo"),
                    this
                ));
            }
        }).bounds(this.guiLeft + GUI_WIDTH - 60, this.guiTop + 5, 55, 16).build();
        
        this.deleteButton.active = this.isModItem;
        this.addRenderableWidget(this.deleteButton);
    }
    
    private void saveChanges() {
        String newDisplayName = this.displayNameBox.getValue().trim();
        
        List<String> newTooltipLines = new ArrayList<>();
        for (EditBox box : this.tooltipLineBoxes) {
            String line = box.getValue().trim();
            if (!line.isEmpty()) {
                newTooltipLines.add(line);
            }
        }
        
        ItemRarity newRarity = this.rarityButton.getValue();
        NetworkHandler.sendToServer(new UpdateItemPacket(this.itemId, newDisplayName, newTooltipLines, newRarity, false));
    }
    
    private void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // Outer border with gradient
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, BORDER_COLOR);
        
        // Main panel
        guiGraphics.fill(x, y, x + width, y + height, PANEL_COLOR);
        
        // Top highlight
        guiGraphics.fill(x, y, x + width, y + 1, PANEL_HIGHLIGHT);
        guiGraphics.fill(x, y + 1, x + 1, y + height, PANEL_HIGHLIGHT);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Dark transparent background
        this.renderBackground(guiGraphics);
        
        // Additional semi-transparent fullscreen background
        guiGraphics.fill(0, 0, width, height, BACKGROUND_COLOR);
        
        // Main panel background
        drawPanel(guiGraphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);
        
        // Title at the top
        String titleText = "Item Editor";
        guiGraphics.drawCenteredString(this.font, titleText, this.width / 2, this.guiTop + 10, TITLE_COLOR);
        
        // Item ID below title
        String idText = this.itemId != null ? this.itemId.toString() : "unknown";
        guiGraphics.drawCenteredString(this.font, idText, this.width / 2, this.guiTop + 22, TEXT_COLOR);
        
        // Draw item preview
        ItemStack stack = new ItemStack(this.item);
        int itemX = this.guiLeft + 25;
        int itemY = this.guiTop + 40;
        
        // Draw item slot
        guiGraphics.fill(itemX - 2, itemY - 2, itemX + 18, itemY + 18, 0xFF666666);
        guiGraphics.fill(itemX - 1, itemY - 1, itemX + 17, itemY + 17, 0xFF000000);
        
        // Draw item
        guiGraphics.renderItem(stack, itemX, itemY);
        guiGraphics.renderItemDecorations(this.font, stack, itemX, itemY, null);
        
        // Section labels
        guiGraphics.drawString(this.font, Component.translatable("itemtooltipenhancer.gui.editor.displayName"), 
                this.guiLeft + 10, this.guiTop + 35, TEXT_COLOR);
        
        guiGraphics.drawString(this.font, Component.translatable("itemtooltipenhancer.gui.editor.tooltip"), 
                this.guiLeft + 10, this.guiTop + 60, TEXT_COLOR);
        
        guiGraphics.drawString(this.font, Component.translatable("itemtooltipenhancer.gui.editor.rarity"), 
                this.guiLeft + 10, this.guiTop + 165, TEXT_COLOR);
        
        // Section dividers
        int dividerY1 = this.guiTop + 55;
        int dividerY2 = this.guiTop + 150;
        guiGraphics.fill(this.guiLeft + 10, dividerY1, this.guiLeft + GUI_WIDTH - 10, dividerY1 + 1, ACCENT_COLOR);
        guiGraphics.fill(this.guiLeft + 10, dividerY2, this.guiLeft + GUI_WIDTH - 10, dividerY2 + 1, ACCENT_COLOR);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Button tooltips
        if (this.deleteButton.isHoveredOrFocused()) {
            Component tooltip = Component.translatable(this.isModItem ? 
                "itemtooltipenhancer.gui.editor.delete.tooltip" : 
                "itemtooltipenhancer.gui.editor.cannotDelete");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
        
        if (this.addTooltipLineButton.isHoveredOrFocused()) {
            Component tooltip = Component.translatable("itemtooltipenhancer.gui.editor.addLine.tooltip");
            if (tooltipLines.size() >= 5) {
                tooltip = Component.translatable("itemtooltipenhancer.gui.editor.maxLines.tooltip");
            }
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
        
        if (this.removeTooltipLineButton.isHoveredOrFocused()) {
            Component tooltip = Component.translatable("itemtooltipenhancer.gui.editor.removeLine.tooltip");
            if (tooltipLines.size() <= 1) {
                tooltip = Component.translatable("itemtooltipenhancer.gui.editor.minLine.tooltip");
            }
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}