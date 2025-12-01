package com.gg;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Set;

/**
 * Handles modifications to loot tables to inject enchanted smithing templates.
 * This class uses Fabric's LootTableEvents.MODIFY to apply loot functions that
 * probabilistically enchant smithing templates when they spawn in loot.
 * 
 * The modifier is compatible with other loot mods as it only adds a loot function
 * and does not remove or modify existing loot pool entries.
 */
public class LootTableModifier {
    
    /**
     * Registers the loot table modification event handler.
     * Should be called during mod initialization.
     */
	public static void register() {
		// Check if enchanted loot is enabled
		if (!SmithingTemplateEnchantmentsConfig.LOOT_CONFIG.enableEnchantedLoot) {
			return;
		}
		
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			// Only modify loot tables from the server (not from data packs or mods that shouldn't be modified)
			// This ensures compatibility with other mods
			if (source.isBuiltin()) {
				modifyLootTable(key.location(), tableBuilder, source, registries);
			}
		});
	}    /**
     * Modifies a loot table by applying the enchanted template loot function to all pools.
     * The function checks each generated item and probabilistically enchants configured
     * smithing templates based on loot_config.json settings.
     * 
     * @param lootTableId The loot table resource location
     * @param tableBuilder The loot table builder
     * @param source The loot table source
     * @param registries Holder lookup provider for accessing registries
     */
	private static void modifyLootTable(ResourceLocation lootTableId, net.minecraft.world.level.storage.loot.LootTable.Builder tableBuilder, net.fabricmc.fabric.api.loot.v3.LootTableSource source, net.minecraft.core.HolderLookup.Provider registries) {
		// Only modify relevant loot tables (chests where smithing templates spawn)
		if (!isRelevantLootTable(lootTableId)) {
			return;
		}
		
		// Apply the enchantment function to the entire table
		// This will be called for each item generated from this loot table
		tableBuilder.modifyPools(poolBuilder -> {
			poolBuilder.apply(EnchantedTemplateLootFunction.builder(registries));
		});
	}    /**
     * Determines if a loot table is relevant for smithing template spawns.
     * Currently returns true for all loot tables, allowing enchanted templates
     * to spawn anywhere smithing templates can appear.
     * 
     * @param lootTableId The loot table resource location
     * @return true if the loot table should have the enchantment function applied
     */
	private static boolean isRelevantLootTable(ResourceLocation lootTableId) {
		// Allow all loot tables to have enchanted templates
		return true;
	}    /**
     * Custom loot item function that adds enchantments to smithing templates.
     * This function is applied when the loot item is generated.
     */
    public static class EnchantedTemplateLootFunction implements LootItemFunction {
        private final net.minecraft.core.HolderLookup.Provider registries;
        
        private EnchantedTemplateLootFunction(net.minecraft.core.HolderLookup.Provider registries) {
            this.registries = registries;
        }
        
		@Override
		public ItemStack apply(ItemStack stack, net.minecraft.world.level.storage.loot.LootContext context) {
			if (stack.isEmpty()) {
				return stack;
			}
			
			// Check if this is a smithing template
			ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
			
			// Only process configured smithing templates
			if (!SmithingTemplateEnchantmentsConfig.CONFIG.containsKey(itemId.toString())) {
				return stack; // Not a configured template, return as-is
			}
			
			// Check if we should enchant this template
			if (!EnchantedTemplateGenerator.shouldEnchant()) {
				return stack;
			}
			
			// Generate an enchanted version of the template
			ItemStack enchanted = EnchantedTemplateGenerator.generateEnchantedTemplate(stack, registries);
			
			return enchanted;
		}        @Override
        public LootItemFunctionType<? extends LootItemFunction> getType() {
            // Return a default type - we're not registering this as a custom type
            return LootItemFunctions.SET_COUNT;
        }
        
        /**
         * Builder for the enchanted template loot function.
         * 
         * @param registries Holder lookup provider for accessing registries
         * @return A loot function builder
         */
        public static Builder builder(net.minecraft.core.HolderLookup.Provider registries) {
            return new Builder(registries);
        }
        
        /**
         * Builder class for creating the loot function.
         */
        public static class Builder implements LootItemFunction.Builder {
            private final net.minecraft.core.HolderLookup.Provider registries;
            
            private Builder(net.minecraft.core.HolderLookup.Provider registries) {
                this.registries = registries;
            }
            
            @Override
            public LootItemFunction build() {
                return new EnchantedTemplateLootFunction(registries);
            }
        }
    }
}
