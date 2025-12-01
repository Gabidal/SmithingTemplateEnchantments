package com.gg.mixin;

import com.gg.SmithingTemplateEnchantmentsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin that adds custom tooltip information to smithing templates.
 * Displays which enchantment will be granted by the template, including:
 * - The enchantment type (level is determined by material used)
 * - Material-specific enchantment levels (when advanced tooltips are enabled)
 */
@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {

    /**
     * Adds enchantment information to smithing template tooltips.
     * Shows the enchantment that will be granted when the template is used,
     * and displays material-specific overrides when advanced tooltips are enabled (F3+H).
     * 
     * @param context The tooltip context
     * @param player The player viewing the tooltip
     * @param flag Tooltip flags (controls advanced tooltip display)
     * @param cir Callback info returnable containing the tooltip lines
     */
    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void addEnchantmentTooltip(Item.TooltipContext context, net.minecraft.world.entity.player.Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        
        // Check if this item has an enchantment configuration
        SmithingTemplateEnchantmentsConfig.TemplateConfig config = SmithingTemplateEnchantmentsConfig.CONFIG.get(itemId.toString());
        
        if (config != null && config.enchantment != null) {
            List<Component> tooltip = cir.getReturnValue();
            
            // Always show the "Prefix:" line for the template's configured enchantment
            String enchantmentKey = "smithingtemplateenchantments.enchantment." + config.enchantment;
            Component enchantmentName = Component.translatable(enchantmentKey);
            
            Component prefixText = Component.translatable("smithingtemplateenchantments.tooltip.prefix")
                    .withStyle(ChatFormatting.GRAY);
            
            Component fullText = Component.empty()
                    .append(prefixText)
                    .append(enchantmentName.copy().withStyle(ChatFormatting.AQUA));
            
            tooltip.add(fullText);
            
            // Check if this template has stored enchantments (from loot)
            ItemEnchantments storedEnchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            
            if (!storedEnchantments.isEmpty()) {
                // This is an enchanted template from loot - also show the added enchantments
                
                // Remove default enchantment tooltip lines that Minecraft adds
                tooltip.removeIf(component -> {
                    String text = component.getString();
                    // Remove lines that look like enchantments (colored text with enchantment names)
                    for (var entry : storedEnchantments.entrySet()) {
                        String enchName = entry.getKey().value().description().getString();
                        if (text.contains(enchName)) {
                            return true;
                        }
                    }
                    return false;
                });
                
                // Build list of enchantment names (without levels)
                List<String> enchantmentNames = new ArrayList<>();
                for (var entry : storedEnchantments.entrySet()) {
                    String enchName = entry.getKey().value().description().getString();
                    enchantmentNames.add(enchName);
                }
                
                // Add custom "postfix:" line with comma-separated names
                Component postfixText = Component.translatable("smithingtemplateenchantments.tooltip.postfix")
                        .withStyle(ChatFormatting.GRAY);
                
                Component enchantmentList = Component.literal(String.join(", ", enchantmentNames))
                        .withStyle(ChatFormatting.LIGHT_PURPLE);
                
                tooltip.add(Component.empty().append(postfixText).append(enchantmentList));
            }
            
            // Show material levels when advanced tooltips are enabled (F3+H)
            if (flag.isAdvanced() && !SmithingTemplateEnchantmentsConfig.MATERIAL_LEVELS.isEmpty()) {
                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Material Levels:").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                
                // Show global material levels
                for (var entry : SmithingTemplateEnchantmentsConfig.MATERIAL_LEVELS.entrySet()) {
                    String materialId = entry.getKey();
                    int level = entry.getValue();
                    String materialName = materialId.replace("minecraft:", "");
                    
                    // Check if this template has an enchantment override for this material
                    String displayEnchant = config.enchantment;
                    if (config.materials != null && config.materials.containsKey(materialId)) {
                        SmithingTemplateEnchantmentsConfig.MaterialConfig matConfig = config.materials.get(materialId);
                        if (matConfig != null && matConfig.enchantment != null) {
                            displayEnchant = matConfig.enchantment;
                        }
                    }
                    
                    Component materialLine = Component.literal("  • " + materialName + ": ")
                            .withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.translatable("smithingtemplateenchantments.enchantment." + displayEnchant)
                                    .withStyle(ChatFormatting.BLUE))
                            .append(Component.literal(" " + level).withStyle(ChatFormatting.GRAY));
                    
                    tooltip.add(materialLine);
                }
            }
        }
    }
}
