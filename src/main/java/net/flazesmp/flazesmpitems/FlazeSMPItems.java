package net.flazesmp.flazesmpitems;

import com.mojang.logging.LogUtils;
import net.flazesmp.flazesmpitems.item.ModItems;
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
    public static final String MOD_ID = "itemtooltipenhancer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FlazeSMPItems() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register item registry - even though we don't have test items anymore,
        // we keep this for future extensibility
        ModItems.register(modEventBus);
        
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("ItemTooltipEnhancer: Initializing rarity system");
        event.enqueueWork(RarityManager::initialize);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Server starting code if needed
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // No specific client setup needed at this point
        }
    }
}