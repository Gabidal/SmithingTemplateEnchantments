package com.gg.mixin;

import com.gg.SmithingTemplateEnchantmentsConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin that allows any configured material to be used in smithing trim recipes.
 * This overrides the vanilla behavior which only allows specific gems/ingots
 * for armor trims. Instead, we allow any material that is defined in the
 * global MATERIAL_LEVELS configuration.
 */
@Mixin(SmithingTrimRecipe.class)
public class SmithingTrimRecipeIngredientMixin {
    
    @Shadow @Final
    Ingredient addition;

    /**
     * Overrides the recipe matching logic to accept any configured material
     * and allow re-applying the same trim for enchantment re-rolling.
     * If a material is defined in the MATERIAL_LEVELS configuration, it can be
     * used in the smithing table, regardless of the vanilla recipe restrictions.
     * Also allows duplicate trims when using configured templates.
     * 
     * This uses a wildcard injection since the exact method signature may vary.
     * 
     * @param input The smithing recipe input
     * @param level The world level
     * @param cir Callback info returnable for the match result
     */
    @Inject(method = "matches", at = @At("HEAD"), cancellable = true, require = 0)
    private void allowConfiguredMaterials(SmithingRecipeInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        // Check if the addition material is in our configured materials
        ItemStack additionStack = input.addition();
        if (!additionStack.isEmpty()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(additionStack.getItem());
            if (SmithingTemplateEnchantmentsConfig.MATERIAL_LEVELS.containsKey(itemId.toString())) {
                // Check if template and base are valid
                ItemStack template = input.template();
                ItemStack base = input.base();
                
                if (!template.isEmpty() && !base.isEmpty()) {
                    // Also verify the template is configured
                    ResourceLocation templateId = BuiltInRegistries.ITEM.getKey(template.getItem());
                    if (SmithingTemplateEnchantmentsConfig.CONFIG.containsKey(templateId.toString())) {
                        // If the material is configured and template is valid, consider the recipe as matching
                        // This allows re-applying the same trim for enchantment re-rolling
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}
