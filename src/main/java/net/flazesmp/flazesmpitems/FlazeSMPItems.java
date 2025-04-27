package net.flazesmp.flazesmpitems;

import com.mojang.logging.LogUtils;
import net.flazesmp.flazesmpitems.event.ItemTooltipEventHandler;
import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(FlazeSMPItems.MOD_ID)
public class FlazeSMPItems {
    public static final String MOD_ID = "flazesmpitems";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FlazeSMPItems() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ItemTooltipEventHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("ItemTooltipEnhancer: Initializing rarity system");
        RarityManager.initialize();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Server starting code
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Client setup code
        }
    }
}