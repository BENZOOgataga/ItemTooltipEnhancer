package net.flazesmp.flazesmpitems.client.gui;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

public class ExampleScreen extends Screen {
    private final String version;

    public ExampleScreen() {
        super(Component.literal("Item Tooltip Enhancer"));
        this.version = ModList.get().getModContainerById(FlazeSMPItems.MOD_ID)
            .map(mod -> mod.getModInfo().getVersion().toString())
            .orElse("Unknown");
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        Button exampleButton = Button.builder(Component.literal("Press Me"), b -> {
            FlazeSMPItems.LOGGER.info("Example button pressed");
        }).pos(centerX - 120, centerY - 10).size(80, 20).build();

        Button reloadButton = Button.builder(Component.literal("Reload Config"), b -> {
            ConfigManager.checkAndRepairConfig();
            ConfigManager.loadAllConfigs();
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("Configs reloaded!"));
            }
        }).pos(centerX - 30, centerY - 10).size(100, 20).build();

        Button closeButton = Button.builder(Component.literal("Close"), b -> onClose())
            .pos(centerX + 80, centerY - 10).size(60, 20).build();

        addRenderableWidget(exampleButton);
        addRenderableWidget(reloadButton);
        addRenderableWidget(closeButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.fill(30, 30, width - 30, height - 30, 0xAA000000);
        guiGraphics.drawCenteredString(font, title, width / 2, 40, 0xFFFFFF);
        guiGraphics.drawString(font, "Welcome to the example GUI!", width / 2 - 90, 60, 0x55FFFF);
        guiGraphics.drawCenteredString(font, "Version: " + version, width / 2, height - 40, 0xAAAAAA);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
