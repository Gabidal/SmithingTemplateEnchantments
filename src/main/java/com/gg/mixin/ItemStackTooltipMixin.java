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

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void addEnchantmentTooltip(Item.TooltipContext context, net.minecraft.world.entity.player.Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        
        SmithingTemplateEnchantmentsConfig.TemplateConfig config = SmithingTemplateEnchantmentsConfig.CONFIG.get(itemId.toString());
        
        if (config != null && config.enchantment != null) {
            List<Component> tooltip = cir.getReturnValue();
            
            // Get the enchantment display name from language file
            String enchantmentKey = "smithingtemplateenchantments.enchantment." + config.enchantment;
            Component enchantmentName = Component.translatable(enchantmentKey);
            
            // Format: "Grants: Enchantment Name (Level X)"
            Component grantsText = Component.translatable("smithingtemplateenchantments.tooltip.grants")
                    .withStyle(ChatFormatting.GRAY);
            
            Component levelText = Component.translatable("smithingtemplateenchantments.tooltip.level", config.level)
                    .withStyle(ChatFormatting.GRAY);
            
            Component fullText = Component.empty()
                    .append(grantsText)
                    .append(enchantmentName.copy().withStyle(ChatFormatting.AQUA))
                    .append(levelText);
            
            tooltip.add(fullText);
            
            // Add material-specific overrides if they exist
            if (!config.materials.isEmpty()) {
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
