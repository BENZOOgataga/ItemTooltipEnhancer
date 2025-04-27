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
        
        // If empty potion with just water, say so
        if (effects.isEmpty() && potion.getName("").equals("")) {
            tooltip.add(Component.literal("")); // Empty line
            tooltip.add(Component.literal("No Effects")
                .withStyle(ChatFormatting.GRAY));
            return;
        }

        // Only add the EFFECTS header if we actually have effects to display
        if (!effects.isEmpty()) {
            // Add the EFFECTS header
            tooltip.add(Component.literal("")); // Empty line before the effects section
            tooltip.add(Component.literal("EFFECTS")
                .withStyle(ChatFormatting.AQUA)
                .withStyle(ChatFormatting.BOLD));
            
            // Add each effect with duration on its own line
            for (MobEffectInstance effect : effects) {
                MobEffect mobEffect = effect.getEffect();
                String effectName = Component.translatable(mobEffect.getDescriptionId()).getString();
                
                // Format effect level (I, II, III, etc.)
                if (effect.getAmplifier() > 0) {
                    effectName += " " + toRomanNumeral(effect.getAmplifier() + 1);
                }
                
                // Get duration
                String duration = formatDuration(effect);
                
                // Determine color based on beneficial or harmful effect
                ChatFormatting effectColor = getEffectColor(mobEffect);
                
                // Add effect name
                tooltip.add(Component.literal(effectName)
                    .withStyle(effectColor));
                
                // Add duration on a separate line with indentation if effect has duration
                if (effect.getDuration() > 20) { // More than 1 second
                    tooltip.add(Component.literal("  Duration: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(duration)
                        .withStyle(ChatFormatting.WHITE)));
                }
            }
        } else {
            // Handle potions with a potion type but no effects 
            // (rare case, but could happen with some mods or custom potions)
            if (!potion.getName("").equals("")) {
                tooltip.add(Component.literal("")); // Empty line
                tooltip.add(Component.literal("Custom Potion")
                    .withStyle(ChatFormatting.GRAY));
            }
        }
        
        // Get potion type info (splash, lingering, arrow)
        // NOTE: Ne pas ajouter cette information ici, car elle sera ajoutée par le système de catégorie
        // dans ItemTooltipEventHandler
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
        // Get milliseconds
        float durationInTicks = effect.getDuration();
        float durationInSeconds = durationInTicks / 20.0F;
        
        int minutes = (int)(durationInSeconds / 60.0F);
        int seconds = (int)(durationInSeconds % 60.0F);
        
        return String.format("%d:%02d", minutes, seconds);
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
        
        // Get track name - handle both vanilla and modded discs
        String trackName = getTrackName(stack);
        
        // Add formatted track info
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Track: ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(trackName)
            .withStyle(ChatFormatting.AQUA)));
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
    
    /**
     * Try to extract custom potion effects from the stack if they exist
     * This helps with modded potions that might store effects differently
     */
    private static List<MobEffectInstance> extractCustomEffects(ItemStack stack) {
        List<MobEffectInstance> customEffects = new ArrayList<>();
        
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            
            // Check for vanilla custom potion effects
            if (tag.contains("CustomPotionEffects", 9)) {
                ListTag listTag = tag.getList("CustomPotionEffects", 10);
                
                for (int i = 0; i < listTag.size(); i++) {
                    CompoundTag effectTag = listTag.getCompound(i);
                    MobEffectInstance effect = MobEffectInstance.load(effectTag);
                    if (effect != null) {
                        customEffects.add(effect);
                    }
                }
            }
        }
        
        return customEffects;
    }
}