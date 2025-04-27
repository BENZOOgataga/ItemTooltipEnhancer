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
    private static final int GUI_HEIGHT = 130;
    private static final int GRID_WIDTH = 9;
    private static final int GRID_HEIGHT = 5;
    private static final int SLOT_SIZE = 18;
    
    // Colors
    private static final int BACKGROUND_COLOR = 0xB0000000;  // Semi-transparent black
    private static final int PANEL_COLOR = 0xE0303030;       // Dark gray panel
    private static final int HEADER_COLOR = 0xFF404040;      // Header color
    private static final int PRIMARY_TEXT = 0xFFEEEEEE;      // White text
    private static final int SECONDARY_TEXT = 0xFFAAAAAA;    // Light gray text
    private static final int SLOT_BG_COLOR = 0xFF202020;     // Darker slot background
    private static final int SLOT_BORDER = 0xFF505050;       // Slot border
    private static final int SLOT_HIGHLIGHT = 0x80FFFFFF;    // White highlight
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
        
        // Create the search box
        this.searchBox = new EditBox(this.font, this.guiLeft + 82, this.guiTop - 16, 80, 14,
                Component.translatable("itemtooltipenhancer.gui.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(PRIMARY_TEXT);
        this.searchBox.setValue("");
        this.searchBox.setResponder(this::updateFilteredItems);
        this.addRenderableWidget(this.searchBox);

        // Navigation buttons with better spacing
        this.prevPageButton = Button.builder(Component.literal("<"), button -> {
            if (this.currentPage > 0) {
                this.currentPage--;
                Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F
                    )
                );
                updateButtonStates();
            }
        }).bounds(this.guiLeft - 25, this.guiTop + 55, 20, 20).build();
        
        this.nextPageButton = Button.builder(Component.literal(">"), button -> {
            if (this.currentPage < this.maxPage - 1) {
                this.currentPage++;
                Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F
                    )
                );
                updateButtonStates();
            }
        }).bounds(this.guiLeft + GUI_WIDTH + 5, this.guiTop + 55, 20, 20).build();
        
        // Toggle button for showing all/custom items
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
                
                Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F
                    )
                );
            }
        ).bounds(this.guiLeft + (GUI_WIDTH/2) - 70, this.guiTop + GUI_HEIGHT + 20, 140, 20).build();
        
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
    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        // Draw slot background
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BG_COLOR);
        
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
        drawRoundedRect(guiGraphics, this.guiLeft - 5, this.guiTop - 25, GUI_WIDTH + 10, GUI_HEIGHT + 55, 8, PANEL_COLOR);
        
        // Draw a header bar
        guiGraphics.fill(this.guiLeft - 5, this.guiTop - 25, this.guiLeft + GUI_WIDTH + 5, this.guiTop - 5, HEADER_COLOR);
        
        // Draw title
        guiGraphics.drawString(this.font, this.title, this.guiLeft, this.guiTop - 20, ACCENT_COLOR);
        
        // Draw search label
        guiGraphics.drawString(this.font, Component.translatable("itemGroup.search"), 
                this.guiLeft + 50, this.guiTop - 15, PRIMARY_TEXT);
        
        // Draw search box background
        guiGraphics.fill(this.guiLeft + 82, this.guiTop - 16, this.guiLeft + 162, this.guiTop - 2, 0xFF101010);
        guiGraphics.fill(this.guiLeft + 82, this.guiTop - 15, this.guiLeft + 162, this.guiTop - 3, 0xFF303030);
        
        // Draw page counter with proper spacing
        String pageInfo = String.format("%d/%d", this.currentPage + 1, this.maxPage);
        guiGraphics.drawCenteredString(this.font, pageInfo, this.width / 2, this.guiTop + GUI_HEIGHT + 5, PRIMARY_TEXT);
        
        // Draw grid background panel
        guiGraphics.fill(this.guiLeft, this.guiTop, this.guiLeft + GUI_WIDTH, this.guiTop + GUI_HEIGHT, 0xFF202020);
        
        // Draw grid slots
        int firstSlotX = this.guiLeft + 8;  
        int firstSlotY = this.guiTop + 8; 
        
        for (int row = 0; row < GRID_HEIGHT; row++) {
            for (int col = 0; col < GRID_WIDTH; col++) {
                drawSlot(guiGraphics, firstSlotX + col * SLOT_SIZE, firstSlotY + row * SLOT_SIZE);
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
            
            // Position each item in its proper grid cell
            int x = firstSlotX + col * SLOT_SIZE + 1;
            int y = firstSlotY + row * SLOT_SIZE + 1;
            
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
        
        // Draw tooltips for items when hovered
        if (mouseX >= firstSlotX && mouseX < firstSlotX + (GRID_WIDTH * SLOT_SIZE) &&
            mouseY >= firstSlotY && mouseY < firstSlotY + (GRID_HEIGHT * SLOT_SIZE)) {
            
            int col = (mouseX - firstSlotX) / SLOT_SIZE;
            int row = (mouseY - firstSlotY) / SLOT_SIZE;
            
            if (col >= 0 && col < GRID_WIDTH && row >= 0 && row < GRID_HEIGHT) {
                int index = startIndex + row * GRID_WIDTH + col;
                
                if (index < filteredItems.size()) {
                    Item item = filteredItems.get(index);
                    ItemStack stack = new ItemStack(item);
                    
                    // Highlight the hovered item
                    int slotX = firstSlotX + col * SLOT_SIZE;
                    int slotY = firstSlotY + row * SLOT_SIZE;
                    guiGraphics.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, SLOT_HIGHLIGHT);
                    
                    // Draw tooltip
                    guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
            }
        }
        
        // Draw count information
        String countInfo = String.format("%d items", filteredItems.size());
        guiGraphics.drawString(this.font, countInfo, this.guiLeft + GUI_WIDTH - this.font.width(countInfo) - 8, 
                this.guiTop - 20, SECONDARY_TEXT);
        
        // Render all UI components (buttons, search box, etc.)
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Handle item grid clicks
        int firstSlotX = this.guiLeft + 8;
        int firstSlotY = this.guiTop + 8;
        
        if (mouseX >= firstSlotX && mouseX < firstSlotX + (GRID_WIDTH * SLOT_SIZE) &&
            mouseY >= firstSlotY && mouseY < firstSlotY + (GRID_HEIGHT * SLOT_SIZE)) {
            
            int col = (int)((mouseX - firstSlotX) / SLOT_SIZE);
            int row = (int)((mouseY - firstSlotY) / SLOT_SIZE);
            
            if (col >= 0 && col < GRID_WIDTH && row >= 0 && row < GRID_HEIGHT) {
                int itemsPerPage = GRID_WIDTH * GRID_HEIGHT;
                int startIndex = this.currentPage * itemsPerPage;
                int index = startIndex + row * GRID_WIDTH + col;
                
                if (index < filteredItems.size()) {
                    Item item = filteredItems.get(index);
                    
                    // Play item select sound
                    Minecraft.getInstance().getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 0.8F
                        )
                    );
                        
                    // Open item editor screen
                    Minecraft.getInstance().setScreen(new ItemEditorScreen(item, this));
                    return true;
                }
            }
        }
        
        return false;
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