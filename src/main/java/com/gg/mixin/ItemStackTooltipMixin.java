package com.gg.mixin;

import com.gg.SmithingTemplateEnchantmentsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
            
            // Build the enchantment display name using translation keys
            String enchantmentKey = "smithingtemplateenchantments.enchantment." + config.enchantment;
            Component enchantmentName = Component.translatable(enchantmentKey);
            
            // Build the tooltip line: "Grants: Enchantment Name" (level determined by material)
            Component grantsText = Component.translatable("smithingtemplateenchantments.tooltip.grants")
                    .withStyle(ChatFormatting.GRAY);
            
            Component fullText = Component.empty()
                    .append(grantsText)
                    .append(enchantmentName.copy().withStyle(ChatFormatting.AQUA));
            
            tooltip.add(fullText);
            
            // Show material-specific overrides when advanced tooltips are enabled (F3+H)
            if (!config.materials.isEmpty()) {
                // Check if any material has different enchantment or level
                boolean hasOverrides = config.materials.values().stream()
                        .anyMatch(m -> m.enchantment != null || (m.level != null && !m.level.equals(config.level)));
                
                if (hasOverrides && flag.isAdvanced()) {
                    tooltip.add(Component.empty());
                    tooltip.add(Component.literal("Material Overrides:").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                    
                    for (var entry : config.materials.entrySet()) {
                        SmithingTemplateEnchantmentsConfig.MaterialConfig matConfig = entry.getValue();
                        if (matConfig.enchantment != null || (matConfig.level != null && !matConfig.level.equals(config.level))) {
                            String materialName = entry.getKey().replace("minecraft:", "");
                            String displayEnchant = matConfig.enchantment != null ? matConfig.enchantment : config.enchantment;
                            int displayLevel = matConfig.level != null ? matConfig.level : config.level;
                            
                            Component materialLine = Component.literal("  • " + materialName + ": ")
                                    .withStyle(ChatFormatting.DARK_GRAY)
                                    .append(Component.translatable("smithingtemplateenchantments.enchantment." + displayEnchant)
                                            .withStyle(ChatFormatting.BLUE))
                                    .append(Component.literal(" " + displayLevel).withStyle(ChatFormatting.GRAY));
                            
                            tooltip.add(materialLine);
                        }
                    }
                }
            }
        }
    }
}
