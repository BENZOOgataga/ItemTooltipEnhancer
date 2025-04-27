package net.flazesmp.flazesmpitems;

import net.flazesmp.flazesmpitems.util.RarityManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("itemtooltipenhancer")
public class FlazeSMPItems {
    public static final String MOD_ID = "itemtooltipenhancer";
    public static final Logger LOGGER = LoggerFactory.getLogger("ItemTooltipEnhancer");

    public FlazeSMPItems() {
        // Register to the mod event bus
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Initialize the RarityManager
        RarityManager.initialize();
        
        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
        
        LOGGER.info("ItemTooltipEnhancer initialized");
    }
    
    /**
     * Helper method to create a mod-specific ResourceLocation
     */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}