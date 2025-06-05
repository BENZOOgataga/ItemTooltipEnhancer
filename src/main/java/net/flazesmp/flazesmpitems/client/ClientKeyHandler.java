package net.flazesmp.flazesmpitems.client;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.client.gui.ExampleScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FlazeSMPItems.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientKeyHandler {
    public static final KeyMapping OPEN_SCREEN_KEY = new KeyMapping(
        "key.itemtooltipenhancer.openscreen",
        InputConstants.KEY_G,
        "key.categories.itemtooltipenhancer"
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SCREEN_KEY);
    }
}

@Mod.EventBusSubscriber(modid = FlazeSMPItems.MOD_ID, value = Dist.CLIENT)
class ClientKeyEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            while (ClientKeyHandler.OPEN_SCREEN_KEY.consumeClick()) {
                Minecraft.getInstance().setScreen(new ExampleScreen());
            }
        }
    }
}
