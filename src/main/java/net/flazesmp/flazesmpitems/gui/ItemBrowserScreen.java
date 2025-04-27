package net.flazesmp.flazesmpitems.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.config.CustomItemsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBrowserScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    private static final int INVENTORY_WIDTH = 195;
    private static final int INVENTORY_HEIGHT = 136;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_WIDTH = 9;
    private static final int GRID_HEIGHT = 5;
    
    private int guiLeft;
    private int guiTop;
    private int currentPage = 0;
    private int maxPage = 0;
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
        this.guiLeft = (this.width - INVENTORY_WIDTH) / 2;
        this.guiTop = (this.height - INVENTORY_HEIGHT) / 2;
        
        // Search box
        this.searchBox = new EditBox(this.font, this.guiLeft + 10, this.guiTop - 20, 175, 14,
                Component.translatable("itemtooltipenhancer.gui.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(true);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(0xFFFFFF);
        this.searchBox.setValue("");
        this.searchBox.setResponder(this::updateFilteredItems);
        this.addRenderableWidget(this.searchBox);
        
        // Navigation buttons
        this.prevPageButton = Button.builder(Component.literal("<"), button -> {
            if (this.currentPage > 0) {
                this.currentPage--;
            }
        }).bounds(this.guiLeft - 25, this.guiTop + INVENTORY_HEIGHT/2, 20, 20).build();
        
        this.nextPageButton = Button.builder(Component.literal(">"), button -> {
            if (this.currentPage < this.maxPage - 1) {
                this.currentPage++;
            }
        }).bounds(this.guiLeft + INVENTORY_WIDTH + 5, this.guiTop + INVENTORY_HEIGHT/2, 20, 20).build();
        
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
            }
        ).bounds(this.guiLeft + INVENTORY_WIDTH/2 - 70, this.guiTop + INVENTORY_HEIGHT + 10, 140, 20).build();
        
        this.addRenderableWidget(this.prevPageButton);
        this.addRenderableWidget(this.nextPageButton);
        this.addRenderableWidget(this.customItemsButton);
        
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
        
        int itemsPerPage = GRID_WIDTH * GRID_HEIGHT;
        this.maxPage = (int) Math.ceil((double) filteredItems.size() / itemsPerPage);
        this.maxPage = Math.max(1, this.maxPage);
        
        // Adjust current page if needed
        if (this.currentPage >= this.maxPage) {
            this.currentPage = this.maxPage - 1;
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // Draw background
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.guiLeft, this.guiTop, 0, 0, INVENTORY_WIDTH, INVENTORY_HEIGHT);
        
        // Draw title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.guiTop - 35, 0xFFFFFF);
        
        // Draw page info
        String pageInfo = String.format("%d/%d", this.currentPage + 1, this.maxPage);
        guiGraphics.drawCenteredString(this.font, pageInfo, this.width / 2, this.guiTop + INVENTORY_HEIGHT + 35, 0xFFFFFF);
        
        // Draw items
        int itemsPerPage = GRID_WIDTH * GRID_HEIGHT;
        int startIndex = this.currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredItems.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int index = i - startIndex;
            int row = index / GRID_WIDTH;
            int col = index % GRID_WIDTH;
            
            int x = this.guiLeft + 8 + col * SLOT_SIZE;
            int y = this.guiTop + 18 + row * SLOT_SIZE;
            
            Item item = filteredItems.get(i);
            ItemStack stack = new ItemStack(item);
            
            // Draw item
            guiGraphics.renderItem(stack, x, y);
            guiGraphics.renderItemDecorations(this.font, stack, x, y);
            
            // Highlight custom items
            if (CustomItemsConfig.hasCustomItemData(item)) {
                guiGraphics.fill(x, y, x + 16, y + 16, 0x3300FF00);
            }
        }
        
        // Draw hover tooltip
        if (mouseX >= this.guiLeft + 8 && mouseX <= this.guiLeft + 8 + GRID_WIDTH * SLOT_SIZE &&
            mouseY >= this.guiTop + 18 && mouseY <= this.guiTop + 18 + GRID_HEIGHT * SLOT_SIZE) {
            
            int col = (mouseX - (this.guiLeft + 8)) / SLOT_SIZE;
            int row = (mouseY - (this.guiTop + 18)) / SLOT_SIZE;
            
            if (col >= 0 && col < GRID_WIDTH && row >= 0 && row < GRID_HEIGHT) {
                int index = startIndex + row * GRID_WIDTH + col;
                
                if (index < filteredItems.size()) {
                    Item item = filteredItems.get(index);
                    ItemStack stack = new ItemStack(item);
                    
                    guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
            }
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle item clicks
        if (mouseX >= this.guiLeft + 8 && mouseX <= this.guiLeft + 8 + GRID_WIDTH * SLOT_SIZE &&
            mouseY >= this.guiTop + 18 && mouseY <= this.guiTop + 18 + GRID_HEIGHT * SLOT_SIZE) {
            
            int col = (int) ((mouseX - (this.guiLeft + 8)) / SLOT_SIZE);
            int row = (int) ((mouseY - (this.guiTop + 18)) / SLOT_SIZE);
            
            if (col >= 0 && col < GRID_WIDTH && row >= 0 && row < GRID_HEIGHT) {
                int itemsPerPage = GRID_WIDTH * GRID_HEIGHT;
                int startIndex = this.currentPage * itemsPerPage;
                int index = startIndex + row * GRID_WIDTH + col;
                
                if (index < filteredItems.size()) {
                    Item item = filteredItems.get(index);
                    
                    // Open item editor screen
                    Minecraft.getInstance().setScreen(new ItemEditorScreen(item, this));
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}