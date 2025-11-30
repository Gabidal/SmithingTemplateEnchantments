package com.gg.mixin;

import com.gg.SmithingTemplateEnchantmentsConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(SmithingTrimRecipe.class)
public class SmithingTrimRecipeMixin {

	@Inject(method = "assemble", at = @At("HEAD"), cancellable = true)
	private void checkSlotCompatibility(SmithingRecipeInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack template = input.template();
		ItemStack base = input.base();
		if (template.isEmpty() || base.isEmpty()) return;

		ResourceLocation templateId = BuiltInRegistries.ITEM.getKey(template.getItem());
		SmithingTemplateEnchantmentsConfig.TemplateConfig templateConfig = SmithingTemplateEnchantmentsConfig.CONFIG.get(templateId.toString());

		if (templateConfig != null) {
			// Check if there are slot restrictions
			if (templateConfig.allowedSlots != null && !templateConfig.allowedSlots.isEmpty()) {
				String itemSlot = getArmorSlot(base);
				if (itemSlot == null || !templateConfig.allowedSlots.contains(itemSlot)) {
					cir.setReturnValue(ItemStack.EMPTY); // Return empty stack to prevent recipe
					return;
				}
			}
		}
	}

	@Inject(method = "assemble", at = @At("RETURN"), cancellable = true)
	private void onAssemble(SmithingRecipeInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack result = cir.getReturnValue();
		if (result.isEmpty()) return;

		ItemStack template = input.template();
		ItemStack addition = input.addition();
		if (template.isEmpty()) return;

		ResourceLocation templateId = BuiltInRegistries.ITEM.getKey(template.getItem());
		SmithingTemplateEnchantmentsConfig.TemplateConfig templateConfig = SmithingTemplateEnchantmentsConfig.CONFIG.get(templateId.toString());

		if (templateConfig != null) {
			// Check if there are slot restrictions
			if (templateConfig.allowedSlots != null && !templateConfig.allowedSlots.isEmpty()) {
				String itemSlot = getArmorSlot(result);
				if (itemSlot == null || !templateConfig.allowedSlots.contains(itemSlot)) {
					return; // Don't apply enchantment if armor slot doesn't match
				}
			}

			ItemStack newResult = result.copy();
			
			String enchantmentId = templateConfig.enchantment;
			int level = templateConfig.level;

			if (!addition.isEmpty()) {
				ResourceLocation additionId = BuiltInRegistries.ITEM.getKey(addition.getItem());
				SmithingTemplateEnchantmentsConfig.MaterialConfig materialConfig = templateConfig.materials.get(additionId.toString());
				if (materialConfig != null) {
					if (materialConfig.enchantment != null) {
						enchantmentId = materialConfig.enchantment;
					}
					if (materialConfig.level != null) {
						level = materialConfig.level;
					}
				}
			}
			
			var enchantmentRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
			var enchantmentKey = ResourceLocation.parse(enchantmentId);
			Optional<net.minecraft.core.Holder.Reference<Enchantment>> enchantment = enchantmentRegistry.get(ResourceKey.create(Registries.ENCHANTMENT, enchantmentKey));
			
			if (enchantment.isPresent()) {
				newResult.enchant(enchantment.get(), level);
				cir.setReturnValue(newResult);
			}
		}
	}

	private String getArmorSlot(ItemStack stack) {
		var equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable == null) return null;
		
		var slot = equippable.slot();
		return switch (slot) {
			case HEAD -> "head";
			case CHEST -> "chest";
			case LEGS -> "legs";
			case FEET -> "feet";
			default -> null;
		};
	}
}
