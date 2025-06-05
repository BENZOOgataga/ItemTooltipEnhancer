package net.flazesmp.flazesmpitems.client.gui;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ExampleScreen extends Screen {
    public ExampleScreen() {
        super(Component.literal("Item Tooltip Enhancer"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        Button button = Button.builder(Component.literal("Press Me"), b -> {
            FlazeSMPItems.LOGGER.info("Example button pressed");
        }).pos(centerX - 50, centerY).size(100, 20).build();
        addRenderableWidget(button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.fill(30, 30, width - 30, height - 30, 0xAA000000);
        guiGraphics.drawCenteredString(font, title, width / 2, 40, 0xFFFFFF);
        guiGraphics.drawString(font, "Welcome to the example GUI!", width / 2 - 90, 60, 0x55FFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
