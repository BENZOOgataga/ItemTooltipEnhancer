package net.flazesmp.flazesmpitems.tooltip;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SpecialItemTooltipHandler {
    private static final Pattern DURATION_PATTERN = Pattern.compile("\\(([0-9]+:[0-9]+)\\)");
    
    /**
     * Handle special item tooltips like potions and music discs
     * 
     * @param tooltip The tooltip list to modify
     * @param stack The item stack
     * @return true if this item was handled specially
     */
    public static boolean handleSpecialItem(List<Component> tooltip, ItemStack stack) {
        if (isPotionItem(stack)) {
            formatPotionTooltip(tooltip, stack);
            return true;
        } else if (isMusicDisc(stack)) {
            formatMusicDiscTooltip(tooltip, stack);
            return true;
        }
        return false;
    }
    
    /**
     * Check if an item is a potion
     */
    private static boolean isPotionItem(ItemStack stack) {
        return stack.getItem() instanceof PotionItem || 
               stack.getItem() == Items.POTION || 
               stack.getItem() == Items.SPLASH_POTION || 
               stack.getItem() == Items.LINGERING_POTION || 
               stack.getItem() == Items.TIPPED_ARROW;
    }
    
    /**
     * Check if an item is a music disc
     */
    private static boolean isMusicDisc(ItemStack stack) {
        return stack.getItem() instanceof RecordItem || 
               stack.getItem().getDescriptionId().contains("music_disc");
    }
    
    /**
     * Format a potion tooltip in Hypixel Skyblock style
     */
    private static void formatPotionTooltip(List<Component> tooltip, ItemStack stack) {
        // Clear existing tooltip except for the first line (item name)
        if (tooltip.size() > 1) {
            tooltip.subList(1, tooltip.size()).clear();
        }
        
        // Get potion effects
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
        Potion potion = PotionUtils.getPotion(stack);
        
        // If empty potion with just water, don't add anything
        if (effects.isEmpty() && potion.getName("").equals("")) {
            tooltip.add(Component.literal("No Effects")
                .withStyle(ChatFormatting.GRAY));
            return;
        }
        
        // Add each effect with duration and amplifier
        for (MobEffectInstance effect : effects) {
            MutableComponent effectComponent = formatPotionEffect(effect);
            tooltip.add(effectComponent);
        }
        
        // Get potion type info (splash, lingering)
        String potionType = getPotionType(stack);
        if (!potionType.isEmpty()) {
            tooltip.add(Component.literal("")); // Empty line
            tooltip.add(Component.literal(potionType)
                .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
    
    /**
     * Format a single potion effect in Hypixel style
     */
    private static MutableComponent formatPotionEffect(MobEffectInstance effect) {
        MobEffect mobEffect = effect.getEffect();
        String effectName = Component.translatable(mobEffect.getDescriptionId()).getString();
        
        // Format effect level (I, II, III, etc.)
        String levelString = "";
        if (effect.getAmplifier() > 0) {
            levelString = " " + toRomanNumeral(effect.getAmplifier() + 1);
        }
        
        // Format duration
        String durationString = "";
        if (effect.getDuration() > 20) { // If duration is more than 1 second
            durationString = " (" + formatDuration(effect) + ")";
        }
        
        // Combine all parts
        ChatFormatting effectColor = getEffectColor(mobEffect);
        
        return Component.literal(effectName + levelString + durationString)
            .withStyle(effectColor);
    }
    
    /**
     * Get appropriate color based on effect type
     */
    private static ChatFormatting getEffectColor(MobEffect effect) {
        return effect.isBeneficial() ? ChatFormatting.GREEN : ChatFormatting.RED;
    }
    
    /**
     * Format duration in a readable format (mm:ss)
     */
    private static String formatDuration(MobEffectInstance effect) {
        // Use vanilla formatter but clean up the output
        String formattedTime = MobEffectUtil.formatDuration(effect, 1.0F).getString();
        return formattedTime;
    }
    
    /**
     * Get potion type string based on item
     */
    private static String getPotionType(ItemStack stack) {
        if (stack.getItem() == Items.SPLASH_POTION) {
            return "Splash Potion";
        } else if (stack.getItem() == Items.LINGERING_POTION) {
            return "Lingering Potion";
        } else if (stack.getItem() == Items.TIPPED_ARROW) {
            return "Arrow with Effect";
        }
        return "";
    }
    
    /**
     * Format music disc tooltip in Hypixel Skyblock style
     */
    private static void formatMusicDiscTooltip(List<Component> tooltip, ItemStack stack) {
        // Clear existing tooltip except for the first line (item name)
        if (tooltip.size() > 1) {
            tooltip.subList(1, tooltip.size()).clear();
        }
        
        RecordItem recordItem = (RecordItem) stack.getItem();
        
        // Get track name - handle both vanilla and modded discs
        String trackName = getTrackName(stack);
        
        // Add formatted track info
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Track: ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(trackName)
                .withStyle(ChatFormatting.YELLOW)));
                
        // Add disc type
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("MUSIC DISC")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
    }
    
    /**
     * Get the track name from a music disc
     */
    private static String getTrackName(ItemStack stack) {
        if (stack.getItem() instanceof RecordItem recordItem) {
            // Try to get the record name from the item directly
            try {
                // This works for vanilla music discs
                return Component.translatable(recordItem.getDescriptionId() + ".desc").getString();
            } catch (Exception e) {
                // Fallback for modded discs
                String itemName = stack.getHoverName().getString();
                return itemName.replace("Music Disc", "").trim();
            }
        }
        
        // Last resort fallback
        return "Unknown Track";
    }
    
    /**
     * Convert a number to Roman numeral
     */
    private static String toRomanNumeral(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }
}