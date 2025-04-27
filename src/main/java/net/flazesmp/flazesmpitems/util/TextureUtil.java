package net.flazesmp.flazesmpitems.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.HashMap;

public class TextureUtil {
    // Cache for texture paths to avoid redundant lookups
    private static final Map<Item, String> TEXTURE_PATH_CACHE = new HashMap<>();
    
    /**
     * Gets the texture path for an item
     * 
     * @param item The item to get the texture path for
     * @return The texture path as a string
     */
    public static String getItemTexturePath(Item item) {
        // Check the cache first
        if (TEXTURE_PATH_CACHE.containsKey(item)) {
            return TEXTURE_PATH_CACHE.get(item);
        }
        
        // If client side, try to get the actual texture path
        if (isClientSide()) {
            String clientPath = getClientItemTexturePath(item);
            if (clientPath != null) {
                TEXTURE_PATH_CACHE.put(item, clientPath);
                return clientPath;
            }
        }
        
        // Fallback: use a standard naming convention based on registry name
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(item);
        if (registryName != null) {
            String fallbackPath = registryName.getNamespace() + ":item/" + registryName.getPath();
            TEXTURE_PATH_CACHE.put(item, fallbackPath);
            return fallbackPath;
        }
        
        return "unknown";
    }
    
    /**
     * Checks if we're on the client side
     * 
     * @return true if on client side
     */
    private static boolean isClientSide() {
        try {
            return Minecraft.getInstance() != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets the texture path for an item on client side
     * Can access client-side rendering classes
     * 
     * @param item The item to get the texture path for
     * @return The texture path or null if not found
     */
    @OnlyIn(Dist.CLIENT)
    private static String getClientItemTexturePath(Item item) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            ItemStack stack = new ItemStack(item);
            BakedModel model = minecraft.getItemRenderer().getItemModelShaper().getItemModel(stack);
            
            if (model != null && !model.isCustomRenderer() && model.getParticleIcon() != null) {
                TextureAtlasSprite sprite = model.getParticleIcon();
                ResourceLocation location = sprite.contents().name();
                return location.toString();
            }
        } catch (Exception e) {
            // Silently fail and use fallback
        }
        return null;
    }
}