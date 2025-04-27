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
    private static final int GUI_WIDTH = 260;
    private static final int GUI_HEIGHT = 230;
    private static final int HEADER_HEIGHT = 35;
    
    // Spacing constants
    private static final int FIELD_HEIGHT = 20;
    private static final int FIELD_SPACING = 30;
    private static final int LABEL_PADDING = 8;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 10;
    private static final int BOTTOM_MARGIN = 15;
    
    // Colors
    private static final int BACKGROUND_COLOR = 0xB0000000;   // Semi-transparent black
    private static final int PANEL_COLOR = 0xE0303030;        // Dark gray panel
    private static final int HEADER_COLOR = 0xFF404040;       // Header color
    private static final int TITLE_COLOR = 0xFFFFAA00;        // Orange title
    private static final int PRIMARY_TEXT = 0xFFEEEEEE;       // White text
    private static final int SECONDARY_TEXT = 0xFFAAAAAA;     // Light gray text
    private static final int DIVIDER_COLOR = 0xFF606060;      // Divider line color
    private static final int FIELD_BG_COLOR = 0xFF202020;     // Text field background
    private static final int FIELD_BORDER = 0xFF505050;       // Text field border
    private static final int FIELD_BORDER_FOCUS = 0xFF7089A9; // Focus border color
    private static final int BUTTON_HOVER = 0xFF3D6185;       // Button hover color
    private static final int SLOT_BG_COLOR = 0xFF101010;      // Item slot background
    private static final int SLOT_BORDER = 0xFF505050;        // Item slot border
    
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
        
        // Calculate consistent field positions
        int labelWidth = 80;
        int fieldX = this.guiLeft + labelWidth + LABEL_PADDING;
        int fieldWidth = GUI_WIDTH - labelWidth - 30;
        
        // Starting Y position for the first field
        int currentY = this.guiTop + HEADER_HEIGHT + 10;
        
        // Display name field
        this.displayNameBox = new EditBox(this.font, fieldX, currentY, fieldWidth, FIELD_HEIGHT,
                Component.translatable("itemtooltipenhancer.gui.editor.displayName"));
        this.displayNameBox.setValue(this.displayName);
        this.displayNameBox.setMaxLength(100);
        this.displayNameBox.setBordered(false);
        this.displayNameBox.setTextColor(PRIMARY_TEXT);
        this.addRenderableWidget(this.displayNameBox);
        
        // Move to tooltip section
        currentY += FIELD_SPACING + 10;
        
        // Tooltip lines
        this.tooltipLineBoxes.clear();
        if (this.tooltipLines.isEmpty()) {
            this.tooltipLines.add("");
        }
        
        int tooltipStartY = currentY;
        int tooltipLineHeight = FIELD_HEIGHT + 5;
        int maxLines = Math.min(this.tooltipLines.size(), 5);
        
        for (int i = 0; i < maxLines; i++) {
            EditBox tooltipBox = new EditBox(this.font, fieldX, tooltipStartY + (i * tooltipLineHeight), 
                    fieldWidth - 25, FIELD_HEIGHT, 
                    Component.translatable("itemtooltipenhancer.gui.editor.tooltipLine", i + 1));
            tooltipBox.setValue(this.tooltipLines.get(i));
            tooltipBox.setMaxLength(100);
            tooltipBox.setBordered(false);
            tooltipBox.setTextColor(PRIMARY_TEXT);
            this.tooltipLineBoxes.add(tooltipBox);
            this.addRenderableWidget(tooltipBox);
        }
        
        // Add/remove tooltip line buttons
        this.addTooltipLineButton = Button.builder(Component.literal("+"), button -> {
            if (tooltipLines.size() < 5) {
                tooltipLines.add("");
                this.init();
                
                // Play sound
                playSoundFeedback();
            }
        }).bounds(fieldX + fieldWidth - 22, tooltipStartY, 20, 20).build();
        
        this.removeTooltipLineButton = Button.builder(Component.literal("-"), button -> {
            if (tooltipLines.size() > 1) {
                tooltipLines.remove(tooltipLines.size() - 1);
                this.init();
                
                // Play sound
                playSoundFeedback();
            }
        }).bounds(fieldX + fieldWidth - 22, tooltipStartY + 22, 20, 20).build();
        
        this.addRenderableWidget(this.addTooltipLineButton);
        this.addRenderableWidget(this.removeTooltipLineButton);
        
        // Move to rarity section
        currentY = tooltipStartY + (maxLines * tooltipLineHeight) + 20;
        
        // Rarity selection
        CycleButton.Builder<ItemRarity> builder = CycleButton.builder(rarity -> 
                Component.literal(rarity.getName())
                .withStyle(style -> style.withColor(rarity.getColor())));
                
        this.rarityButton = builder
                .withValues(ItemRarity.values())
                .withInitialValue(this.rarity)
                .create(fieldX, currentY, fieldWidth, FIELD_HEIGHT, 
                        Component.translatable("itemtooltipenhancer.gui.editor.rarity"),
                        (button, value) -> {
                            this.rarity = value;
                        });
        this.addRenderableWidget(this.rarityButton);
        
        // Bottom buttons - centered and properly sized
        int totalButtonWidth = (BUTTON_WIDTH * 2) + BUTTON_SPACING;
        int buttonsY = this.guiTop + GUI_HEIGHT - BUTTON_HEIGHT - BOTTOM_MARGIN;
        int buttonsStartX = this.width / 2 - totalButtonWidth / 2;
        
        this.saveButton = Button.builder(Component.translatable("itemtooltipenhancer.gui.editor.save"), button -> {
            saveChanges();
            playSoundFeedback();
            Minecraft.getInstance().setScreen(this.parentScreen);
        }).bounds(buttonsStartX, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        
        this.cancelButton = Button.builder(Component.translatable("itemtooltipenhancer.gui.editor.cancel"), button -> {
            playSoundFeedback();
            Minecraft.getInstance().setScreen(this.parentScreen);
        }).bounds(buttonsStartX + BUTTON_WIDTH + BUTTON_SPACING, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        
        this.addRenderableWidget(this.saveButton);
        this.addRenderableWidget(this.cancelButton);
        
        // Delete button in the top right corner
        this.deleteButton = Button.builder(Component.translatable("itemtooltipenhancer.gui.editor.delete"), button -> {
            if (this.isModItem) {
                NetworkHandler.sendToServer(new UpdateItemPacket(this.itemId, null, null, null, true));
                
                // Play sound
                Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.ITEM_BREAK, 1.0F
                    )
                );
                
                Minecraft.getInstance().setScreen(this.parentScreen);
            } else {
                Minecraft.getInstance().setScreen(new MessageScreen(
                    Component.translatable("itemtooltipenhancer.gui.editor.cannotDelete"),
                    Component.translatable("itemtooltipenhancer.gui.editor.cannotDeleteInfo"),
                    this
                ));
            }
        }).bounds(this.guiLeft + GUI_WIDTH - 65, this.guiTop + 5, 55, 20).build();
        
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
    
    // Draw a rounded rectangle
    private void drawRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int color) {
        // Draw main rectangle (excluding corners)
        guiGraphics.fill(x + radius, y, x + width - radius, y + height, color);
        guiGraphics.fill(x, y + radius, x + width, y + height - radius, color);
        
        // Draw corners
        for (int i = 0; i < radius; i++) {
            int cornerWidth = radius - i;
            guiGraphics.fill(x + i, y + radius - cornerWidth, x + i + 1, y + radius, color);
            guiGraphics.fill(x + width - i - 1, y + radius - cornerWidth, x + width - i, y + radius, color);
            guiGraphics.fill(x + i, y + height - radius, x + i + 1, y + height - radius + cornerWidth, color);
            guiGraphics.fill(x + width - i - 1, y + height - radius, x + width - i, y + height - radius + cornerWidth, color);
        }
    }
    
    // Draw a field background
    private void drawFieldBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean isFocused) {
        // Fill the background
        guiGraphics.fill(x, y, x + width, y + height, FIELD_BG_COLOR);
        
        // Draw border with focus state
        int borderColor = isFocused ? FIELD_BORDER_FOCUS : FIELD_BORDER;
        
        // Draw border
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + 1, x + 1, y + height, borderColor);
        guiGraphics.fill(x + 1, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y + 1, x + width, y + height - 1, borderColor);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render darkened background
        this.renderBackground(guiGraphics);
        
        // Draw a semi-transparent full screen overlay
        guiGraphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);
        
        // Draw main panel with rounded corners
        drawRoundedRect(guiGraphics, this.guiLeft - 5, this.guiTop - 5, GUI_WIDTH + 10, GUI_HEIGHT + 10, 10, PANEL_COLOR);
        
        // Draw header bar
        guiGraphics.fill(this.guiLeft - 5, this.guiTop - 5, this.guiLeft + GUI_WIDTH + 5, this.guiTop + HEADER_HEIGHT, HEADER_COLOR);
        
        // Draw title - properly centered
        Component titleComponent = Component.translatable("itemtooltipenhancer.gui.editor.title");
        int titleWidth = this.font.width(titleComponent);
        guiGraphics.drawString(this.font, titleComponent, this.width / 2 - titleWidth / 2, this.guiTop + 10, TITLE_COLOR);
        
        // Draw item ID - centered
        String idText = this.itemId != null ? this.itemId.toString() : "unknown";
        guiGraphics.drawCenteredString(this.font, idText, this.width / 2, this.guiTop + 22, SECONDARY_TEXT);
        
        // Draw section dividers
        int dividerY1 = this.guiTop + HEADER_HEIGHT + 40;
        int dividerY2 = this.guiTop + GUI_HEIGHT - 50;
        
        guiGraphics.fill(this.guiLeft + 10, dividerY1, this.guiLeft + GUI_WIDTH - 10, dividerY1 + 1, DIVIDER_COLOR);
        guiGraphics.fill(this.guiLeft + 10, dividerY2, this.guiLeft + GUI_WIDTH - 10, dividerY2 + 1, DIVIDER_COLOR);
        
        // Draw section labels with consistent alignment
        int labelX = this.guiLeft + 10;
        
        guiGraphics.drawString(this.font, Component.translatable("itemtooltipenhancer.gui.editor.displayName"), 
                labelX, this.displayNameBox.getY() + 5, PRIMARY_TEXT);
        
        guiGraphics.drawString(this.font, Component.translatable("itemtooltipenhancer.gui.editor.tooltip"), 
                labelX, this.tooltipLineBoxes.get(0).getY() - 15, PRIMARY_TEXT);
        
        guiGraphics.drawString(this.font, Component.translatable("itemtooltipenhancer.gui.editor.rarity"), 
                labelX, this.rarityButton.getY() + 5, PRIMARY_TEXT);
        
        // Draw field backgrounds with focus state
        for (int i = 0; i < this.tooltipLineBoxes.size(); i++) {
            EditBox box = this.tooltipLineBoxes.get(i);
            drawFieldBackground(guiGraphics, box.getX() - 1, box.getY() - 1, 
                    box.getWidth() + 2, box.getHeight() + 2, box.isFocused());
        }
        
        // Display name field background
        drawFieldBackground(guiGraphics, this.displayNameBox.getX() - 1, this.displayNameBox.getY() - 1, 
                this.displayNameBox.getWidth() + 2, this.displayNameBox.getHeight() + 2, this.displayNameBox.isFocused());
        
        // Draw item preview
        ItemStack stack = new ItemStack(this.item);
        int itemX = this.guiLeft + 40;
        int itemY = this.guiTop + 135;
        
        // Draw item slot - rounded square
        drawRoundedRect(guiGraphics, itemX - 10, itemY - 10, 32, 32, 4, SLOT_BG_COLOR);
        
        // Item slot border
        int borderSize = 1;
        guiGraphics.fill(itemX - 10, itemY - 10 + 4, itemX - 10 + borderSize, itemY - 10 + 32 - 4, SLOT_BORDER);  // Left
        guiGraphics.fill(itemX - 10 + 32 - borderSize, itemY - 10 + 4, itemX - 10 + 32, itemY - 10 + 32 - 4, SLOT_BORDER);  // Right
        guiGraphics.fill(itemX - 10 + 4, itemY - 10, itemX - 10 + 32 - 4, itemY - 10 + borderSize, SLOT_BORDER);  // Top
        guiGraphics.fill(itemX - 10 + 4, itemY - 10 + 32 - borderSize, itemX - 10 + 32 - 4, itemY - 10 + 32, SLOT_BORDER);  // Bottom
        
        // Draw the item with centered position
        guiGraphics.renderItem(stack, itemX, itemY);
        guiGraphics.renderItemDecorations(this.font, stack, itemX, itemY, null);
        
        // Draw "Is Custom Item" indicator if applicable
        if (this.isCustomItem) {
            String customText = "Custom Item";
            guiGraphics.drawString(this.font, customText, 
                    itemX - this.font.width(customText) / 2, itemY + 20, TITLE_COLOR);
        }
        
        // Render button hover states
        renderButtonHoverStates(guiGraphics, mouseX, mouseY);
        
        // Render all UI components
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Button tooltips
        renderButtonTooltips(guiGraphics, mouseX, mouseY);
    }
    
    private void renderButtonHoverStates(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Add hover effects for buttons
        renderButtonHoverEffect(guiGraphics, this.saveButton, mouseX, mouseY);
        renderButtonHoverEffect(guiGraphics, this.cancelButton, mouseX, mouseY);
        renderButtonHoverEffect(guiGraphics, this.deleteButton, mouseX, mouseY);
        renderButtonHoverEffect(guiGraphics, this.addTooltipLineButton, mouseX, mouseY);
        renderButtonHoverEffect(guiGraphics, this.removeTooltipLineButton, mouseX, mouseY);
    }
    
    private void renderButtonHoverEffect(GuiGraphics guiGraphics, Button button, int mouseX, int mouseY) {
        if (button.isHoveredOrFocused() && button.active) {
            // Draw a hovering effect for the button
            int x = button.getX();
            int y = button.getY();
            int width = button.getWidth();
            int height = button.getHeight();
            
            // Brighten the button background when hovered
            guiGraphics.fill(x, y, x + width, y + height, BUTTON_HOVER);
            
            // Keep the button text visible
            Component message = button.getMessage();
            int textX = x + (width / 2) - (this.font.width(message) / 2);
            int textY = y + (height - 8) / 2;
            guiGraphics.drawString(this.font, message, textX, textY, 0xFFFFFFFF);
        }
    }
    
    private void renderButtonTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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
    
    private void playSoundFeedback() {
        Minecraft.getInstance().getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F
            )
        );
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}