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

/**
 * Mixin that modifies the smithing trim recipe behavior to add enchantments.
 * Intercepts the recipe assembly process to:
 * 1. Check if the armor slot matches any configured slot restrictions
 * 2. Apply enchantments based on the template and material used
 * 3. Support material-specific enchantment and level overrides
 */
@Mixin(SmithingTrimRecipe.class)
public class SmithingTrimRecipeMixin {

	/**
	 * Checks if the armor piece being modified is compatible with the template's slot restrictions.
	 * This method is injected at the start of the recipe assembly process and will cancel
	 * the recipe if the armor slot doesn't match the allowed slots for the template.
	 * 
	 * @param input The smithing recipe input containing template, base, and addition items
	 * @param registries The holder lookup provider for accessing game registries
	 * @param cir Callback info returnable, used to cancel the recipe if slot is incompatible
	 */
	@Inject(method = "assemble", at = @At("HEAD"), cancellable = true)
	private void checkSlotCompatibility(SmithingRecipeInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack template = input.template();
		ItemStack base = input.base();
		if (template.isEmpty() || base.isEmpty()) return;

		// Get the template's configuration
		ResourceLocation templateId = BuiltInRegistries.ITEM.getKey(template.getItem());
		SmithingTemplateEnchantmentsConfig.TemplateConfig templateConfig = SmithingTemplateEnchantmentsConfig.CONFIG.get(templateId.toString());

		if (templateConfig != null) {
			// If the template has slot restrictions, verify the armor piece matches
			if (templateConfig.allowedSlots != null && !templateConfig.allowedSlots.isEmpty()) {
				String itemSlot = getArmorSlot(base);
				// Cancel recipe if armor slot doesn't match allowed slots
				if (itemSlot == null || !templateConfig.allowedSlots.contains(itemSlot)) {
					cir.setReturnValue(ItemStack.EMPTY);
					return;
				}
			}
		}
	}

	/**
	 * Applies enchantments to the result of a smithing recipe.
	 * This method is injected at the end of the recipe assembly process and adds
	 * enchantments based on the template and material used. Supports material-specific
	 * overrides for both enchantment type and level.
	 * 
	 * @param input The smithing recipe input containing template, base, and addition items
	 * @param registries The holder lookup provider for accessing game registries
	 * @param cir Callback info returnable containing the recipe result
	 */
	@Inject(method = "assemble", at = @At("RETURN"), cancellable = true)
	private void onAssemble(SmithingRecipeInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack result = cir.getReturnValue();
		if (result.isEmpty()) return;

		ItemStack template = input.template();
		ItemStack addition = input.addition();
		if (template.isEmpty()) return;

		// Get the template's configuration
		ResourceLocation templateId = BuiltInRegistries.ITEM.getKey(template.getItem());
		SmithingTemplateEnchantmentsConfig.TemplateConfig templateConfig = SmithingTemplateEnchantmentsConfig.CONFIG.get(templateId.toString());

		if (templateConfig != null) {
			// Verify slot compatibility if restrictions are configured
			if (templateConfig.allowedSlots != null && !templateConfig.allowedSlots.isEmpty()) {
				String itemSlot = getArmorSlot(result);
				if (itemSlot == null || !templateConfig.allowedSlots.contains(itemSlot)) {
					return;
				}
			}

			// Create a copy of the result to apply enchantments
			ItemStack newResult = result.copy();
			
			// Start with template's default enchantment and level
			String enchantmentId = templateConfig.enchantment;
			int level = templateConfig.level;

			// Check for material-specific overrides
			if (!addition.isEmpty()) {
				ResourceLocation additionId = BuiltInRegistries.ITEM.getKey(addition.getItem());
				SmithingTemplateEnchantmentsConfig.MaterialConfig materialConfig = templateConfig.materials.get(additionId.toString());
				if (materialConfig != null) {
					// Override enchantment if material specifies one
					if (materialConfig.enchantment != null) {
						enchantmentId = materialConfig.enchantment;
					}
					// Override level if material specifies one
					if (materialConfig.level != null) {
						level = materialConfig.level;
					}
				}
			}
			
			// Look up the enchantment in the registry
			var enchantmentRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
			var enchantmentKey = ResourceLocation.parse(enchantmentId);
			Optional<net.minecraft.core.Holder.Reference<Enchantment>> enchantment = enchantmentRegistry.get(ResourceKey.create(Registries.ENCHANTMENT, enchantmentKey));
			
			// Apply the enchantment if found
			if (enchantment.isPresent()) {
				newResult.enchant(enchantment.get(), level);
				cir.setReturnValue(newResult);
			}
		}
	}

	/**
	 * Determines which armor slot an item belongs to.
	 * 
	 * @param stack The item stack to check
	 * @return The armor slot name ("head", "chest", "legs", or "feet"), or null if not armor
	 */
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
