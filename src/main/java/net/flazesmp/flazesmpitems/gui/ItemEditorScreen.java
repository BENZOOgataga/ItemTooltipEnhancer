package net.flazesmp.flazesmpitems.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.flazesmp.flazesmpitems.FlazeSMPItems;
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
import java.util.Arrays;
import java.util.List;

public class ItemEditorScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
    private static final int PANEL_WIDTH = 236;
    private static final int PANEL_HEIGHT = 222;
    
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
        this.guiLeft = (this.width - PANEL_WIDTH) / 2;
        this.guiTop = (this.height - PANEL_HEIGHT) / 2;
        
        // Title at the top
        String title = this.itemId != null ? this.itemId.toString() : "unknown";
        
        // Display name field
        this.displayNameBox = new EditBox(this.font, this.guiLeft + 90, this.guiTop + 25, 130, 20,
                Component.translatable("itemtooltipenhancer.gui.editor.displayName"));
        this.displayNameBox.setValue(this.displayName);
        this.displayNameBox.setMaxLength(100);
        this.addRenderableWidget(this.displayNameBox);
        
        // Tooltip lines (starting with empty if none)
        this.tooltipLineBoxes.clear();
        if (this.tooltipLines.isEmpty()) {
            this.tooltipLines.add("");
        }
        
        for (int i = 0; i < this.tooltipLines.size(); i++) {
            if (i > 4) break; // Limit to 5 tooltip lines
            
            EditBox tooltipBox = new EditBox(this.font, this.guiLeft + 90, this.guiTop + 60 + (i * 25), 130, 20,
                    Component.translatable("itemtooltipenhancer.gui.editor.tooltipLine", i + 1));
            tooltipBox.setValue(this.tooltipLines.get(i));
            tooltipBox.setMaxLength(100);
            this.tooltipLineBoxes.add(tooltipBox);
            this.addRenderableWidget(tooltipBox);
        }
        
        // Rarity selection - Fixed implementation for Forge 1.20.1
        CycleButton.Builder<ItemRarity> builder = CycleButton.builder(rarity -> Component.literal(rarity.getName()));
        this.rarityButton = builder
                .withValues(ItemRarity.values())
                .withInitialValue(this.rarity)
                .create(this.guiLeft + 90, this.guiTop + 170, 130, 20, Component.empty(), (button, value) -> {
                    this.rarity = value;
                });
        this.addRenderableWidget(this.rarityButton);
        
        // Add/remove tooltip line buttons
        this.addTooltipLineButton = Button.builder(Component.literal("+"), button -> {
            if (tooltipLines.size() < 5) {
                tooltipLines.add("");
                this.init(); // Refresh the screen
            }
        }).bounds(this.guiLeft + 225, this.guiTop + 60, 20, 20).build();
        
        this.removeTooltipLineButton = Button.builder(Component.literal("-"), button -> {
            if (tooltipLines.size() > 1) {
                tooltipLines.remove(tooltipLines.size() - 1);
                this.init(); // Refresh the screen
            }
        }).bounds(this.guiLeft + 225, this.guiTop + 85, 20, 20).build();
        
        this.addRenderableWidget(this.addTooltipLineButton);
        this.addRenderableWidget(this.removeTooltipLineButton);
        
        // Save and cancel buttons
        this.saveButton = Button.builder(Component.translatable("itemtooltipenhancer.gui.editor.save"), button -> {
            saveChanges();
            Minecraft.getInstance().setScreen(this.parentScreen);
        }).bounds(this.guiLeft + 40, this.guiTop + 200, 70, 20).build();
        
        this.cancelButton = Button.builder(Component.translatable("itemtooltipenhancer.gui.editor.cancel"), button -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
        }).bounds(this.guiLeft + 120, this.guiTop + 200, 70, 20).build();
        
        this.addRenderableWidget(this.saveButton);
        this.addRenderableWidget(this.cancelButton);
        
        // Delete button (only enabled for ITE-created items)
        this.deleteButton = Button.builder(Component.translatable("itemtooltipenhancer.gui.editor.delete"), button -> {
            if (this.isModItem) {
                // Send delete packet to server
                NetworkHandler.sendToServer(new UpdateItemPacket(this.itemId, null, null, null, true));
                Minecraft.getInstance().setScreen(this.parentScreen);
            } else {
                // Show an error message for non-ITE items
                Minecraft.getInstance().setScreen(new MessageScreen(
                    Component.translatable("itemtooltipenhancer.gui.editor.cannotDelete"),
                    Component.translatable("itemtooltipenhancer.gui.editor.cannotDeleteInfo"),
                    this
                ));
            }
        }).bounds(this.guiLeft + PANEL_WIDTH - 60, this.guiTop + 20, 50, 20).build();
        
        this.deleteButton.active = this.isModItem;
        this.addRenderableWidget(this.deleteButton);
    }
    
    private void saveChanges() {
        // Get values from UI
        String newDisplayName = this.displayNameBox.getValue().trim();
        
        List<String> newTooltipLines = new ArrayList<>();
        for (EditBox box : this.tooltipLineBoxes) {
            String line = box.getValue().trim();
            if (!line.isEmpty()) {
                newTooltipLines.add(line);
            }
        }
        
        ItemRarity newRarity = this.rarityButton.getValue();
        
        // Send to server
        NetworkHandler.sendToServer(new UpdateItemPacket(this.itemId, newDisplayName, newTooltipLines, newRarity, false));
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // Draw background
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.guiLeft, this.guiTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        
        // Draw title
        String title = this.itemId != null ? this.itemId.toString() : "unknown";
        guiGraphics.drawCenteredString(this.font, title, this.width / 2, this.guiTop + 6, 0xFFFFFF);
        
        // Draw item preview
        ItemStack stack = new ItemStack(this.item);
        guiGraphics.renderItem(stack, this.guiLeft + 50, this.guiTop + 30);
        
        // Draw labels
        guiGraphics.drawString(this.font, Component.translatable("itemtooltipenhancer.gui.editor.displayName"), 
                this.guiLeft + 15, this.guiTop + 30, 0xFFFFFF);
                
        guiGraphics.drawString(this.font, Component.translatable("itemtooltipenhancer.gui.editor.tooltip"), 
                this.guiLeft + 15, this.guiTop + 65, 0xFFFFFF);
                
        guiGraphics.drawString(this.font, Component.translatable("itemtooltipenhancer.gui.editor.rarity"), 
                this.guiLeft + 15, this.guiTop + 175, 0xFFFFFF);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}