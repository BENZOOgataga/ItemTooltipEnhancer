package net.flazesmp.flazesmpitems.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.config.CustomItemsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
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
    private static final int GUI_WIDTH = 176; // Standard inventory width
    private static final int GUI_HEIGHT = 130; // Height for just the items section
    private static final int GRID_WIDTH = 9;
    private static final int GRID_HEIGHT = 5;
    private static final int SLOT_SIZE = 18; // Size of each item slot
    
    // Colors
    private static final int BACKGROUND_COLOR = 0xFFC6C6C6; // Light gray
    private static final int BORDER_DARK = 0xFF555555;
    private static final int BORDER_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_COLOR = 0xFF8B8B8B;

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
        
        // Search box - positioned with proper spacing
        this.searchBox = new EditBox(this.font, this.guiLeft + 82, this.guiTop - 15, 80, 12,
                Component.translatable("itemtooltipenhancer.gui.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(true);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(0x000000);
        this.searchBox.setValue("");
        this.searchBox.setResponder(this::updateFilteredItems);
        this.addRenderableWidget(this.searchBox);

        // Navigation buttons - positioned with proper spacing on either side
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
        }).bounds(this.guiLeft - 30, this.guiTop + 60, 20, 20).build();
        
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
        }).bounds(this.guiLeft + GUI_WIDTH + 10, this.guiTop + 60, 20, 20).build();
        
        // Show all/custom items button - positioned with proper spacing at the bottom
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
        ).bounds(this.guiLeft + (GUI_WIDTH/2) - 70, this.guiTop + GUI_HEIGHT + 15, 140, 20).build();
        
        // Add all widgets
        this.addRenderableWidget(this.prevPageButton);
        this.addRenderableWidget(this.nextPageButton);
        this.addRenderableWidget(this.customItemsButton);
        
        // Load items
        updateFilteredItems("");
    }

    private void updateFilteredItems(String filter) {
        filteredItems.clear();
        
        // Collect all items or only custom items based on the toggle
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
    
    /**
     * Draws a Minecraft-style "slot" at the specified coordinates
     */
    private void drawItemSlot(GuiGraphics guiGraphics, int x, int y) {
        // Draw darker background
        guiGraphics.fill(x, y, x + 18, y + 18, SLOT_COLOR);
        
        // Draw slot border
        guiGraphics.fill(x, y, x + 1, y + 18, BORDER_DARK);  // Left edge
        guiGraphics.fill(x + 1, y, x + 18, y + 1, BORDER_DARK);  // Top edge
        guiGraphics.fill(x + 17, y + 1, x + 18, y + 18, BORDER_LIGHT);  // Right edge
        guiGraphics.fill(x + 1, y + 17, x + 17, y + 18, BORDER_LIGHT);  // Bottom edge
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render background texture
        this.renderBackground(guiGraphics);
        
        // Draw the custom GUI background - recreating just the items grid part
        // Outer dark border
        guiGraphics.fill(this.guiLeft - 1, this.guiTop - 1, this.guiLeft + GUI_WIDTH + 1, this.guiTop + GUI_HEIGHT + 1, BORDER_DARK);
        
        // Main background
        guiGraphics.fill(this.guiLeft, this.guiTop, this.guiLeft + GUI_WIDTH, this.guiTop + GUI_HEIGHT, BACKGROUND_COLOR);
        
        // Draw item slots (9x5 grid)
        int firstSlotX = this.guiLeft + 8;  
        int firstSlotY = this.guiTop + 17; 
        
        for (int row = 0; row < GRID_HEIGHT; row++) {
            for (int col = 0; col < GRID_WIDTH; col++) {
                drawItemSlot(guiGraphics, firstSlotX + col * SLOT_SIZE, firstSlotY + row * SLOT_SIZE);
            }
        }
        
        // Draw title with proper spacing
        guiGraphics.drawString(this.font, this.title, this.guiLeft + 8, this.guiTop - 30, 0x404040);
        
        // Draw search label with proper spacing
        guiGraphics.drawString(this.font, Component.translatable("itemGroup.search"), 
                this.guiLeft + 8, this.guiTop - 15, 0x404040);
        
        // Draw page info with proper spacing
        String pageInfo = String.format("%d/%d", this.currentPage + 1, this.maxPage);
        guiGraphics.drawCenteredString(this.font, pageInfo, 
                this.width / 2, this.guiTop + GUI_HEIGHT + 3, 0xFFFFFF);
        
        // Draw items in the grid
        int itemsPerPage = GRID_WIDTH * GRID_HEIGHT;
        int startIndex = this.currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredItems.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int index = i - startIndex;
            int row = index / GRID_WIDTH;
            int col = index % GRID_WIDTH;
            
            // Position each item in its proper grid cell
            int x = firstSlotX + col * SLOT_SIZE + 1;  // +1 to center in slot
            int y = firstSlotY + row * SLOT_SIZE + 1;  // +1 to center in slot
            
            // Draw the item
            Item item = filteredItems.get(i);
            ItemStack stack = new ItemStack(item);
            guiGraphics.renderItem(stack, x, y);
            guiGraphics.renderItemDecorations(this.font, stack, x, y);
            
            // Add subtle highlight for custom items
            if (CustomItemsConfig.hasCustomItemData(item)) {
                guiGraphics.fill(x, y, x + 16, y + 16, 0x1300FF00); // Very subtle green tint
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
                    int slotX = firstSlotX + col * SLOT_SIZE + 1;
                    int slotY = firstSlotY + row * SLOT_SIZE + 1;
                    guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80FFFFFF);
                    
                    // Draw tooltip
                    guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
            }
        }
        
        // Render all UI components (buttons, search box, etc.)
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Let widgets handle clicks first
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Handle item grid clicks
        int firstSlotX = this.guiLeft + 8;
        int firstSlotY = this.guiTop + 17;
        
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