package com.gg.mixin.client;

import com.gg.SmithingTemplateEnchantmentsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Client-side mixin that modifies item tooltips in the smithing table result slot.
 * Hides new enchantment details (not from base item) and shows obfuscated "?????" text
 * to create mystery similar to enchantment tables, while preserving base item enchantments.
 */
@Mixin(ItemStack.class)
public class SmithingResultTooltipMixin {

	@Inject(method = "getTooltipLines", at = @At("RETURN"))
	private void modifySmithingResultTooltip(Item.TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
		ItemStack stack = (ItemStack) (Object) this;
		
		// Check if we're viewing this from a smithing table
		if (player != null && player.containerMenu instanceof SmithingMenu) {
			SmithingMenu menu = (SmithingMenu) player.containerMenu;
			
			// Check if this is the result slot (slot 3)
			ItemStack resultItem = menu.getSlot(3).getItem();
			if (resultItem == stack) {
				// Get the template to check if it's configured
				ItemStack template = menu.getSlot(0).getItem();
				if (!template.isEmpty()) {
					ResourceLocation templateId = BuiltInRegistries.ITEM.getKey(template.getItem());
					SmithingTemplateEnchantmentsConfig.TemplateConfig templateConfig = 
						SmithingTemplateEnchantmentsConfig.CONFIG.get(templateId.toString());
					
					// If template is configured and item has enchantment glint, show mystery text
					if (templateConfig != null && templateConfig.enchantment != null) {
						List<Component> tooltip = cir.getReturnValue();
						
						// Get base item enchantments (from slot 1 - the armor being upgraded)
						ItemStack baseItem = menu.getSlot(1).getItem();
						ItemEnchantments baseEnchantments = baseItem.isEmpty() ? ItemEnchantments.EMPTY : 
							baseItem.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
						
						// Get result enchantments
						ItemEnchantments resultEnchantments = stack.get(DataComponents.ENCHANTMENTS);
						if (resultEnchantments != null && !resultEnchantments.isEmpty()) {
							// Count only NEW enchantments (not present on base item)
							int newEnchantCount = 0;
							for (var entry : resultEnchantments.entrySet()) {
								var enchantmentHolder = entry.getKey();
								// Check if this enchantment was NOT on the base item
								if (!baseEnchantments.keySet().contains(enchantmentHolder)) {
									newEnchantCount++;
								}
							}
							
							// Only modify tooltip if there are new enchantments from template
							if (newEnchantCount > 0) {
								// Remove enchantment lines that are NEW (not from base item)
								tooltip.removeIf(component -> {
									String text = component.getString();
									// Remove lines that look like enchantments (typically colored and start with enchantment names)
									// Only remove if they're new enchantments
									for (var entry : resultEnchantments.entrySet()) {
										if (!baseEnchantments.keySet().contains(entry.getKey())) {
											String enchName = entry.getKey().value().description().getString();
											if (text.contains(enchName)) {
												return true;
											}
										}
									}
									return false;
								});
								
							}
						}
                        // Add mystery text for the user to see that they are getting enchant
                        MutableComponent enchantDisplayer = Component.literal("Enchantment: ");
                        MutableComponent mysteryText = Component.literal("?????")
                            .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.DARK_PURPLE)
                                .withObfuscated(true));
                        
                        // now let's append the mystery text after the enchantment text
                        tooltip.add(enchantDisplayer.append(mysteryText));
					}
				}
			}
		}
	}
}
