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
 * 1. Allow any configured material to be used in the smithing table
 * 2. Check if the armor slot matches any configured slot restrictions
 * 3. Apply enchantments based on the template and material used
 * 4. Support material-specific enchantment overrides with global level scaling
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
	 * Applies enchantments to the smithing result using a two-tier system:
	 * 1. Prefix enchantments (from trim_data.json config) - always applied at material level
	 * 2. Postfix enchantments (stored on template from loot) - probability-based with exponential decay
	 * 
	 * Enchantments are rolled fresh each time during assembly, so the smithing table preview
	 * shows different results. Always adds enchantment glint to configured templates.
	 * Allows re-applying the same trim pattern for enchantment rerolling.
	 * 
	 * @param input The smithing recipe input containing template, base, and addition items
	 * @param registries The holder lookup provider for accessing game registries
	 * @param cir Callback info returnable containing the recipe result
	 */
	@Inject(method = "assemble", at = @At("RETURN"))
	private void applyEnchantments(SmithingRecipeInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack result = cir.getReturnValue();
		if (result.isEmpty()) return;

		ItemStack template = input.template();
		ItemStack base = input.base();
		ItemStack addition = input.addition();
		if (template.isEmpty()) return;

		// Get the template's configuration
		ResourceLocation templateId = BuiltInRegistries.ITEM.getKey(template.getItem());
		SmithingTemplateEnchantmentsConfig.TemplateConfig templateConfig = SmithingTemplateEnchantmentsConfig.CONFIG.get(templateId.toString());
		
		// Check if base already has a trim - if it does and matches the new trim, remove the old trim
		// This allows re-enchanting with the same template type
		var baseTrim = base.get(DataComponents.TRIM);
		var resultTrim = result.get(DataComponents.TRIM);
		if (baseTrim != null && resultTrim != null && templateConfig != null) {
			// If the base already had the same trim pattern, this means the player is re-applying
			// In this case, we want to roll for enchantments again
			if (baseTrim.pattern().equals(resultTrim.pattern())) {
				// The trim was already on the item - this is a re-application for enchantment reroll
				// Keep the result as-is (vanilla already applied the trim)
			}
		}
		
		// Get the material level for enchantment scaling
		int materialLevel = 1;
		if (!addition.isEmpty()) {
			ResourceLocation additionId = BuiltInRegistries.ITEM.getKey(addition.getItem());
			Integer configuredLevel = SmithingTemplateEnchantmentsConfig.MATERIAL_LEVELS.get(additionId.toString());
			if (configuredLevel != null) {
				materialLevel = configuredLevel;
			}
		}

		// Separate prefix (guaranteed) and postfix (probability-based) enchantments
		java.util.Map<String, Integer> prefixEnchantments = new java.util.HashMap<>();
		java.util.Map<String, Integer> postfixEnchantments = new java.util.HashMap<>();
		
		// 1. Add postfix enchantments from the smithing template item itself (probability-based)
		var templateEnchantments = template.getEnchantments();
		if (templateEnchantments != null && !templateEnchantments.isEmpty()) {
			for (var entry : templateEnchantments.entrySet()) {
				var enchantmentHolder = entry.getKey();
				int enchLevel = entry.getIntValue();
				String enchantmentId = enchantmentHolder.getRegisteredName();
				postfixEnchantments.put(enchantmentId, enchLevel);
			}
		}
		
		// 2. Add the configured prefix enchantment from trim_data.json (always applied)
		if (templateConfig != null) {
			// Verify slot compatibility if restrictions are configured
			if (templateConfig.allowedSlots != null && !templateConfig.allowedSlots.isEmpty()) {
				String itemSlot = getArmorSlot(result);
				if (itemSlot == null || !templateConfig.allowedSlots.contains(itemSlot)) {
					return;
				}
			}
			
			// Determine the enchantment to add from config
			String configEnchantment = templateConfig.enchantment;
			
			// Check for material-specific override
			if (!addition.isEmpty() && templateConfig.materials != null) {
				ResourceLocation additionId = BuiltInRegistries.ITEM.getKey(addition.getItem());
				SmithingTemplateEnchantmentsConfig.MaterialConfig materialConfig = templateConfig.materials.get(additionId.toString());
				if (materialConfig != null && materialConfig.enchantment != null) {
					configEnchantment = materialConfig.enchantment;
				}
			}
			
			if (configEnchantment != null) {
				// Use material level for configured prefix enchantment
				prefixEnchantments.put(configEnchantment, materialLevel);
			}
		}
		
		// Always show enchantment glint if template is configured for enchantments
		if (templateConfig != null && templateConfig.enchantment != null) {
			result.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		
		var enchantmentRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
		
		// Apply prefix enchantments (ALWAYS applied, no probability check)
		for (var entry : prefixEnchantments.entrySet()) {
			String enchantmentId = entry.getKey();
			int level = entry.getValue();
			
			var enchantmentKey = ResourceLocation.parse(enchantmentId);
			Optional<net.minecraft.core.Holder.Reference<Enchantment>> enchantment = 
				enchantmentRegistry.get(ResourceKey.create(Registries.ENCHANTMENT, enchantmentKey));
			
			if (enchantment.isPresent()) {
				result.enchant(enchantment.get(), level);
			}
		}
		
		// Apply postfix enchantments (probability-based with level rolling)
		if (!postfixEnchantments.isEmpty()) {
			for (var entry : postfixEnchantments.entrySet()) {
				String enchantmentId = entry.getKey();
				
				// Get probability from config (or use default if not configured)
				double baseProbability = SmithingTemplateEnchantmentsConfig.ENCHANTMENT_PROBABILITIES.getOrDefault(enchantmentId, 0.5);
				
				// Roll for the level based on material level (1 to materialLevel)
				// Higher levels are much harder to get - exponential decay
				int finalLevel = 0;
				
				for (int testLevel = materialLevel; testLevel >= 1; testLevel--) {
					// Calculate probability for this level using exponential decay
					// Each level above 1 becomes exponentially harder to achieve
					// Formula: baseProbability / (2^(testLevel - 1))
					// Level 1: baseProbability / 1 = full probability
					// Level 2: baseProbability / 2 = 50% of base
					// Level 3: baseProbability / 4 = 25% of base
					// Level 4: baseProbability / 8 = 12.5% of base
					// Level 5: baseProbability / 16 = 6.25% of base
					double levelProbability = baseProbability / Math.pow(2, testLevel - 1);
					
					// Roll for this level
					double roll = Math.random();
					
					if (levelProbability > roll) {
						finalLevel = testLevel;
						break;
					}
				}
				
				// Apply the enchantment if we rolled a level > 0
				if (finalLevel > 0) {
					var enchantmentKey = ResourceLocation.parse(enchantmentId);
					Optional<net.minecraft.core.Holder.Reference<Enchantment>> enchantment = 
						enchantmentRegistry.get(ResourceKey.create(Registries.ENCHANTMENT, enchantmentKey));
					
					if (enchantment.isPresent()) {
						result.enchant(enchantment.get(), finalLevel);
					}
				}
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
