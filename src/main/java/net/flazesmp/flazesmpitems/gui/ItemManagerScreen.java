package net.flazesmp.flazesmpitems.gui;

import java.util.List;
import java.util.stream.Collectors;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

@OnlyIn(Dist.CLIENT)
public class ItemManagerScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    
    private EditBox searchBox;
    private List<Item> filteredItems;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 54; // 9x6 grid
    private static final int GRID_SIZE = 18;
    
    public ItemManagerScreen() {
        super(Component.translatable("gui." + FlazeSMPItems.MOD_ID + ".item_manager"));
    }
    
    public static void open() {
        Minecraft.getInstance().setScreen(new ItemManagerScreen());
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = width / 2;
        int centerY = height / 2;
        
        // Add search box
        searchBox = new EditBox(font, centerX - 80, centerY - 120, 160, 20, Component.literal("Search..."));
        searchBox.setMaxLength(50);
        searchBox.setVisible(true);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setBordered(true);
        searchBox.setResponder(this::updateFilteredItems);
        addRenderableWidget(searchBox);
        
        // Add navigation buttons
        addRenderableWidget(new Button.Builder(Component.literal("<"), b -> prevPage())
                .pos(centerX - 90, centerY + 90)
                .size(20, 20)
                .build());
        
        addRenderableWidget(new Button.Builder(Component.literal(">"), b -> nextPage())
                .pos(centerX + 70, centerY + 90)
                .size(20, 20)
                .build());
        
        // Add "New Item" button
        addRenderableWidget(new Button.Builder(Component.literal("Create New Item"), b -> createNewItem())
                .pos(centerX - 60, centerY + 90)
                .size(120, 20)
                .build());
        
        // Initialize item list
        updateFilteredItems("");
    }
    
    private void updateFilteredItems(String filter) {
        // Get all registered items
        filteredItems = ForgeRegistries.ITEMS.getValues().stream()
                .filter(item -> {
                    ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
                    if (itemId == null) return false;
                    
                    String itemName = itemId.toString();
                    return filter.isEmpty() || itemName.toLowerCase().contains(filter.toLowerCase());
                })
                .collect(Collectors.toList());
        
        // Reset to first page
        currentPage = 0;
    }
    
    private void nextPage() {
        if ((currentPage + 1) * ITEMS_PER_PAGE < filteredItems.size()) {
            currentPage++;
        }
    }
    
    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
        }
    }
    
    private void createNewItem() {
        minecraft.setScreen(new ItemEditorScreen(this, null));
    }
    
    private void editItem(Item item) {
        minecraft.setScreen(new ItemEditorScreen(this, item));
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        
        PoseStack poseStack = graphics.pose();
        
        // Draw title
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 140, 0xFFFFFF);
        
        // Draw page info
        int totalPages = (int) Math.ceil((double) filteredItems.size() / ITEMS_PER_PAGE);
        String pageInfo = "Page " + (currentPage + 1) + "/" + Math.max(1, totalPages);
        graphics.drawCenteredString(font, pageInfo, width / 2, height / 2 + 70, 0xFFFFFF);
        
        // Calculate grid dimensions
        int gridWidth = 9 * GRID_SIZE;
        int gridHeight = 6 * GRID_SIZE;
        int startX = width / 2 - gridWidth / 2;
        int startY = height / 2 - gridHeight / 2;
        
        // Draw grid background
        graphics.fill(startX - 4, startY - 4, startX + gridWidth + 4, startY + gridHeight + 4, 0xFF555555);
        graphics.fill(startX - 2, startY - 2, startX + gridWidth + 2, startY + gridHeight + 2, 0xFF222222);
        
        // Draw items in grid
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredItems.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int gridPos = i - startIndex;
            int gridX = gridPos % 9;
            int gridY = gridPos / 9;
            
            int x = startX + gridX * GRID_SIZE;
            int y = startY + gridY * GRID_SIZE;
            
            // Draw slot background
            graphics.fill(x, y, x + GRID_SIZE, y + GRID_SIZE, 0xFF444444);
            graphics.fill(x + 1, y + 1, x + GRID_SIZE - 1, y + GRID_SIZE - 1, 0xFF333333);
            
            // Draw item
            Item item = filteredItems.get(i);
            graphics.renderItem(new ItemStack(item), x + 1, y + 1);
        }
        
        // Check for mouse hover over items
        if (mouseX >= startX && mouseX < startX + gridWidth && 
            mouseY >= startY && mouseY < startY + gridHeight) {
            
            int gridX = (mouseX - startX) / GRID_SIZE;
            int gridY = (mouseY - startY) / GRID_SIZE;
            
            int hoverIndex = startIndex + gridY * 9 + gridX;
            
            if (hoverIndex >= startIndex && hoverIndex < endIndex) {
                Item hoverItem = filteredItems.get(hoverIndex);
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(hoverItem);
                
                // Draw item name tooltip
                if (itemId != null) {
                    graphics.renderTooltip(font, Component.literal(itemId.toString()), mouseX, mouseY);
                }
            }
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Calculate grid dimensions
        int gridWidth = 9 * GRID_SIZE;
        int gridHeight = 6 * GRID_SIZE;
        int startX = width / 2 - gridWidth / 2;
        int startY = height / 2 - gridHeight / 2;
        
        // Check if click is within grid
        if (mouseX >= startX && mouseX < startX + gridWidth && 
            mouseY >= startY && mouseY < startY + gridHeight) {
            
            int gridX = (int) ((mouseX - startX) / GRID_SIZE);
            int gridY = (int) ((mouseY - startY) / GRID_SIZE);
            
            int clickIndex = currentPage * ITEMS_PER_PAGE + gridY * 9 + gridX;
            
            if (clickIndex >= 0 && clickIndex < filteredItems.size()) {
                Item clickedItem = filteredItems.get(clickIndex);
                editItem(clickedItem);
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}