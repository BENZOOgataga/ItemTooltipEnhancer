package net.flazesmp.flazesmpitems.gui;

import net.flazesmp.flazesmpitems.config.CustomItemsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBrowserScreen extends Screen {
    // Constants for GUI dimensions
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 140; // Slightly increased for better spacing
    private static final int GRID_WIDTH = 9;
    private static final int GRID_HEIGHT = 5;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_PADDING = 2; // Added padding between slots
    private static final int TOTAL_SLOT_SIZE = SLOT_SIZE + SLOT_PADDING;
    
    // Constants for positioning
    private static final int HEADER_HEIGHT = 30;
    private static final int TITLE_OFFSET_Y = 12; // Adjusted to center in header
    private static final int SEARCH_OFFSET_Y = 5; // Shifted down from title
    private static final int NAV_BUTTON_SPACING = 50; // Space between nav buttons
    private static final int NAV_BUTTON_Y_MARGIN = 15; // Margin below grid
    private static final int TOOLTIP_Y_OFFSET = 5; // Shift tooltips down
    
    // Colors
    private static final int BACKGROUND_COLOR = 0xB0000000;  // Semi-transparent black
    private static final int PANEL_COLOR = 0xE0303030;       // Dark gray panel
    private static final int HEADER_COLOR = 0xFF404040;      // Header color
    private static final int PRIMARY_TEXT = 0xFFEEEEEE;      // White text
    private static final int SECONDARY_TEXT = 0xFFAAAAAA;    // Light gray text
    private static final int SLOT_BG_COLOR = 0xFF202020;     // Darker slot background
    private static final int SLOT_BORDER = 0xFF505050;       // Slot border
    private static final int SLOT_HIGHLIGHT = 0x80FFFFFF;    // White highlight
    private static final int HOVER_HIGHLIGHT = 0xFF606060;   // Button hover highlight
    private static final int ACCENT_COLOR = 0xFFFFAA00;      // Orange accent
    private static final int CUSTOM_ITEM_HIGHLIGHT = 0x3500FF00; // Green tint for custom items
    
    // Position values
    private int guiLeft;
    private int guiTop;
    private int currentPage = 0;
    private int maxPage = 0;
    
    // GUI components
    private List<Item> filteredItems = new ArrayList<>();
    private EditBox searchBox;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button customItemsButton;
    private boolean showingCustomItemsOnly = false;

    public ItemBrowserScreen() {
        super(Component.translatable("itemtooltipenhancer.gui.browser.title"));
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // Calculate positioning for consistent spacing
        int gridTotalHeight = GRID_HEIGHT * TOTAL_SLOT_SIZE;
        int searchY = this.guiTop - HEADER_HEIGHT + TITLE_OFFSET_Y + SEARCH_OFFSET_Y;
        
        // Create the search box - centered horizontally
        int searchBoxWidth = 100;
        this.searchBox = new EditBox(this.font, this.width / 2 - searchBoxWidth / 2, searchY, searchBoxWidth, 16,
                Component.translatable("itemtooltipenhancer.gui.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(PRIMARY_TEXT);
        this.searchBox.setValue("");
        this.searchBox.setResponder(this::updateFilteredItems);
        this.addRenderableWidget(this.searchBox);

        // Navigation buttons with centered positioning
        int navButtonY = this.guiTop + gridTotalHeight + NAV_BUTTON_Y_MARGIN;
        int navButtonSize = 20; // Standard button size
        
        // Center the navigation controls below the grid
        this.prevPageButton = Button.builder(Component.literal("<"), button -> {
            if (this.currentPage > 0) {
                this.currentPage--;
                playSoundFeedback();
                updateButtonStates();
            }
        }).bounds(this.width / 2 - NAV_BUTTON_SPACING, navButtonY, navButtonSize, navButtonSize).build();
        
        this.nextPageButton = Button.builder(Component.literal(">"), button -> {
            if (this.currentPage < this.maxPage - 1) {
                this.currentPage++;
                playSoundFeedback();
                updateButtonStates();
            }
        }).bounds(this.width / 2 + NAV_BUTTON_SPACING - navButtonSize, navButtonY, navButtonSize, navButtonSize).build();
        
        // Toggle button for showing all/custom items - full width and positioned below navigation
        int toggleBtnWidth = 150;
        int toggleBtnHeight = 20;
        this.customItemsButton = Button.builder(
            Component.translatable(showingCustomItemsOnly 
                ? "itemtooltipenhancer.gui.showAll" 
                : "itemtooltipenhancer.gui.showCustomOnly"), 
            button -> {
                showingCustomItemsOnly = !showingCustomItemsOnly;
                button.setMessage(Component.translatable(showingCustomItemsOnly 
                    ? "itemtooltipenhancer.gui.showAll" 
                    : "itemtooltipenhancer.gui.showCustomOnly"));
                updateFilteredItems(this.searchBox.getValue());
                
                playSoundFeedback();
            }
        ).bounds(this.width / 2 - toggleBtnWidth / 2, navButtonY + navButtonSize + 5, toggleBtnWidth, toggleBtnHeight).build();
        
        // Add all widgets
        this.addRenderableWidget(this.prevPageButton);
        this.addRenderableWidget(this.nextPageButton);
        this.addRenderableWidget(this.customItemsButton);
        
        // Load items
        updateFilteredItems("");
    }

    private void updateFilteredItems(String filter) {
        filteredItems.clear();
        
        // Collect items based on selected filter mode
        if (showingCustomItemsOnly) {
            for (ResourceLocation itemId : CustomItemsConfig.getAllCustomItems().keySet()) {
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                if (item != null && item != Items.AIR) {
                    filteredItems.add(item);
                }
            }
        } else {
            filteredItems.addAll(ForgeRegistries.ITEMS.getValues().stream()
                .filter(item -> item != Items.AIR)
                .collect(Collectors.toList()));
        }
        
        // Apply search filter if not empty
        if (!filter.isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            filteredItems = filteredItems.stream()
                .filter(item -> {
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                    String name = item.getDescription().getString().toLowerCase();
                    String registryName = id != null ? id.toString().toLowerCase() : "";
                    return name.contains(lowerFilter) || registryName.contains(lowerFilter);
                })
                .collect(Collectors.toList());
        }
        
        // Sort items alphabetically
        filteredItems.sort(Comparator.comparing(item -> {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            return id != null ? id.toString() : "";
        }));
        
        // Calculate page count
        int itemsPerPage = GRID_WIDTH * GRID_HEIGHT;
        this.maxPage = Math.max(1, (int) Math.ceil((double) filteredItems.size() / itemsPerPage));
        
        // Ensure current page is valid
        if (this.currentPage >= this.maxPage) {
            this.currentPage = Math.max(0, this.maxPage - 1);
        }
        
        // Update button states
        updateButtonStates();
    }
    
    private void updateButtonStates() {
        // Fix pagination button states
        this.prevPageButton.active = this.currentPage > 0;
        this.nextPageButton.active = this.currentPage < this.maxPage - 1;
    }
    
    // Draw a single item slot with rounded corners
    private void drawSlot(GuiGraphics guiGraphics, int x, int y, boolean isHovered) {
        // Slot background color changes when hovered
        int bgColor = isHovered ? HOVER_HIGHLIGHT : SLOT_BG_COLOR;
        
        // Draw slot background
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);
        
        // Draw slot border
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + 1, SLOT_BORDER);           // Top
        guiGraphics.fill(x, y + 1, x + 1, y + SLOT_SIZE - 1, SLOT_BORDER);   // Left
        guiGraphics.fill(x + 1, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BORDER); // Bottom
        guiGraphics.fill(x + SLOT_SIZE - 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE - 1, SLOT_BORDER); // Right
    }

    // Draw a rounded rectangle
    private void drawRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int color) {
        // Draw main rectangle (excluding corners)
        guiGraphics.fill(x + radius, y, x + width - radius, y + height, color); // Top and bottom horizontal rectangles
        guiGraphics.fill(x, y + radius, x + width, y + height - radius, color); // Left and right vertical rectangles
        
        // Draw corners
        for (int i = 0; i < radius; i++) {
            int cornerWidth = radius - i;
            // Top-left corner
            guiGraphics.fill(x + i, y + radius - cornerWidth, x + i + 1, y + radius, color);
            // Top-right corner
            guiGraphics.fill(x + width - i - 1, y + radius - cornerWidth, x + width - i, y + radius, color);
            // Bottom-left corner
            guiGraphics.fill(x + i, y + height - radius, x + i + 1, y + height - radius + cornerWidth, color);
            // Bottom-right corner
            guiGraphics.fill(x + width - i - 1, y + height - radius, x + width - i, y + height - radius + cornerWidth, color);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render darkened background
        this.renderBackground(guiGraphics);
        
        // Draw a semi-transparent full screen overlay
        guiGraphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);
        
        // Draw main panel with rounded corners
        int panelHeight = GUI_HEIGHT + 60; // Added more space for buttons
        drawRoundedRect(guiGraphics, this.guiLeft - 10, this.guiTop - HEADER_HEIGHT, GUI_WIDTH + 20, panelHeight, 8, PANEL_COLOR);
        
        // Draw header bar
        guiGraphics.fill(this.guiLeft - 10, this.guiTop - HEADER_HEIGHT, this.guiLeft + GUI_WIDTH + 10, this.guiTop, HEADER_COLOR);
        
        // Draw title - centered and properly positioned in header
        Component titleComponent = this.title;
        int titleWidth = this.font.width(titleComponent);
        guiGraphics.drawString(this.font, titleComponent, 
                this.width / 2 - titleWidth / 2, 
                this.guiTop - HEADER_HEIGHT + TITLE_OFFSET_Y, 
                ACCENT_COLOR);
        
        // Draw search label - centered
        Component searchLabel = Component.translatable("itemGroup.search");
        int labelWidth = this.font.width(searchLabel);
        int labelX = this.width / 2 - this.searchBox.getWidth() / 2 - labelWidth - 5;
        guiGraphics.drawString(this.font, searchLabel, labelX, 
                this.searchBox.getY() + 2, PRIMARY_TEXT);
        
        // Draw search box background
        int searchBoxX = this.searchBox.getX() - 1;
        int searchBoxY = this.searchBox.getY() - 1;
        int searchBoxWidth = this.searchBox.getWidth() + 2;
        int searchBoxHeight = this.searchBox.getHeight() + 2;
        guiGraphics.fill(searchBoxX, searchBoxY, searchBoxX + searchBoxWidth, searchBoxY + searchBoxHeight, 0xFF101010);
        
        // Draw page counter with proper spacing
        String pageInfo = String.format("%d/%d", this.currentPage + 1, this.maxPage);
        guiGraphics.drawCenteredString(this.font, pageInfo, this.width / 2, this.prevPageButton.getY() + 5, PRIMARY_TEXT);
        
        // Draw grid background panel - with spacing adjustments
        int gridWidth = GRID_WIDTH * TOTAL_SLOT_SIZE - SLOT_PADDING;
        int gridHeight = GRID_HEIGHT * TOTAL_SLOT_SIZE - SLOT_PADDING;
        guiGraphics.fill(this.guiLeft, this.guiTop, this.guiLeft + gridWidth, this.guiTop + gridHeight, 0xFF202020);
        
        // Calculate grid starting positions to center the grid
        int firstSlotX = this.guiLeft;  
        int firstSlotY = this.guiTop; 
        
        // Track which slot is hovered
        int hoveredSlotX = -1;
        int hoveredSlotY = -1;
        
        // Check which slot is hovered
        if (mouseX >= firstSlotX && mouseX < firstSlotX + gridWidth &&
            mouseY >= firstSlotY && mouseY < firstSlotY + gridHeight) {
            
            hoveredSlotX = (mouseX - firstSlotX) / TOTAL_SLOT_SIZE;
            hoveredSlotY = (mouseY - firstSlotY) / TOTAL_SLOT_SIZE;
        }
        
        // Draw grid slots with padding
        for (int row = 0; row < GRID_HEIGHT; row++) {
            for (int col = 0; col < GRID_WIDTH; col++) {
                int x = firstSlotX + col * TOTAL_SLOT_SIZE;
                int y = firstSlotY + row * TOTAL_SLOT_SIZE;
                boolean isHovered = (col == hoveredSlotX && row == hoveredSlotY);
                drawSlot(guiGraphics, x, y, isHovered);
            }
        }
        
        // Draw items in the grid
        int itemsPerPage = GRID_WIDTH * GRID_HEIGHT;
        int startIndex = this.currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredItems.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int index = i - startIndex;
            int row = index / GRID_WIDTH;
            int col = index % GRID_WIDTH;
            
            // Position each item in its proper grid cell with padding
            int x = firstSlotX + col * TOTAL_SLOT_SIZE + 1;
            int y = firstSlotY + row * TOTAL_SLOT_SIZE + 1;
            
            // Draw the item
            Item item = filteredItems.get(i);
            ItemStack stack = new ItemStack(item);
            guiGraphics.renderItem(stack, x, y);
            guiGraphics.renderItemDecorations(this.font, stack, x, y);
            
            // Add subtle highlight for custom items
            if (CustomItemsConfig.hasCustomItemData(item)) {
                guiGraphics.fill(x, y, x + 16, y + 16, CUSTOM_ITEM_HIGHLIGHT);
            }
        }
        
        // Draw tooltips for items when hovered - with adjusted Y position
        if (hoveredSlotX >= 0 && hoveredSlotY >= 0 && hoveredSlotX < GRID_WIDTH && hoveredSlotY < GRID_HEIGHT) {
            int index = startIndex + hoveredSlotY * GRID_WIDTH + hoveredSlotX;
            
            if (index < filteredItems.size()) {
                // Highlight the hovered slot
                int slotX = firstSlotX + hoveredSlotX * TOTAL_SLOT_SIZE;
                int slotY = firstSlotY + hoveredSlotY * TOTAL_SLOT_SIZE;
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, SLOT_HIGHLIGHT);
                
                // Draw item tooltip with Y offset
                Item item = filteredItems.get(index);
                ItemStack stack = new ItemStack(item);
                guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY + TOOLTIP_Y_OFFSET);
            }
        }
        
        // Draw count information
        String countInfo = String.format("%d items", filteredItems.size());
        guiGraphics.drawString(this.font, countInfo, this.guiLeft + gridWidth - this.font.width(countInfo) - 8, 
                this.guiTop - 20, SECONDARY_TEXT);
        
        // Render hover states for buttons
        renderButtonHoverStates(guiGraphics, mouseX, mouseY);
        
        // Render all UI components (buttons, search box, etc.)
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderButtonHoverStates(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Add hover effects for buttons
        if (this.prevPageButton.isHoveredOrFocused() && this.prevPageButton.active) {
            drawButtonHoverEffect(guiGraphics, this.prevPageButton);
        }
        
        if (this.nextPageButton.isHoveredOrFocused() && this.nextPageButton.active) {
            drawButtonHoverEffect(guiGraphics, this.nextPageButton);
        }
        
        if (this.customItemsButton.isHoveredOrFocused()) {
            drawButtonHoverEffect(guiGraphics, this.customItemsButton);
        }
    }
    
    private void drawButtonHoverEffect(GuiGraphics guiGraphics, Button button) {
        // Draw a subtle highlight border around the button when hovered
        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int height = button.getHeight();
        
        // Draw border highlight
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y, ACCENT_COLOR); // Top
        guiGraphics.fill(x - 1, y + height, x + width + 1, y + height + 1, ACCENT_COLOR); // Bottom
        guiGraphics.fill(x - 1, y, x, y + height, ACCENT_COLOR); // Left
        guiGraphics.fill(x + width, y, x + width + 1, y + height, ACCENT_COLOR); // Right
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Handle item grid clicks with improved spacing
        int gridWidth = GRID_WIDTH * TOTAL_SLOT_SIZE - SLOT_PADDING;
        int gridHeight = GRID_HEIGHT * TOTAL_SLOT_SIZE - SLOT_PADDING;
        int firstSlotX = this.guiLeft;
        int firstSlotY = this.guiTop;
        
        if (mouseX >= firstSlotX && mouseX < firstSlotX + gridWidth &&
            mouseY >= firstSlotY && mouseY < firstSlotY + gridHeight) {
            
            int col = (int)((mouseX - firstSlotX) / TOTAL_SLOT_SIZE);
            int row = (int)((mouseY - firstSlotY) / TOTAL_SLOT_SIZE);
            
            if (col >= 0 && col < GRID_WIDTH && row >= 0 && row < GRID_HEIGHT) {
                int itemsPerPage = GRID_WIDTH * GRID_HEIGHT;
                int startIndex = this.currentPage * itemsPerPage;
                int index = startIndex + row * GRID_WIDTH + col;
                
                if (index < filteredItems.size()) {
                    Item item = filteredItems.get(index);
                    
                    // Play item select sound
                    playSoundFeedback(0.8F);
                        
                    // Open item editor screen
                    Minecraft.getInstance().setScreen(new ItemEditorScreen(item, this));
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void playSoundFeedback() {
        playSoundFeedback(1.0F);
    }
    
    private void playSoundFeedback(float pitch) {
        Minecraft.getInstance().getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, pitch
            )
        );
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.isFocused() && this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox.isFocused() && this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}