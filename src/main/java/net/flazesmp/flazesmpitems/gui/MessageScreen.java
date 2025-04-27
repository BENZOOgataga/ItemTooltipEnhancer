package net.flazesmp.flazesmpitems.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public class MessageScreen extends Screen {
    private final Component message;
    private final Component details;
    private final Screen parentScreen;
    
    public MessageScreen(Component message, Component details, Screen parentScreen) {
        super(Component.translatable("itemtooltipenhancer.gui.message.title"));
        this.message = message;
        this.details = details;
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        Button okButton = Button.builder(Component.literal("OK"), button -> {
            Minecraft.getInstance().setScreen(parentScreen);
        }).bounds(this.width / 2 - 50, this.height / 2 + 50, 100, 20).build();
        
        this.addRenderableWidget(okButton);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, this.message, this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        
        // Draw details text with word wrapping
        int lineHeight = this.font.lineHeight;
        int maxWidth = 280;
        
        java.util.List<FormattedCharSequence> lines = this.font.split(this.details, maxWidth);
        for (int i = 0; i < lines.size(); i++) {
            int x = this.width / 2 - this.font.width(lines.get(i)) / 2;
            guiGraphics.drawString(this.font, lines.get(i), x, this.height / 2 + i * lineHeight, 0xCCCCCC);
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}