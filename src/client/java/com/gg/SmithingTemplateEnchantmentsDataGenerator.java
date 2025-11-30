package com.gg;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Data generation entry point for Smithing Template Enchantments.
 * This class can be used to generate data files such as recipes, loot tables,
 * and language files automatically during the build process.
 * Currently no data generation is configured.
 */
public class SmithingTemplateEnchantmentsDataGenerator implements DataGeneratorEntrypoint {
	/**
	 * Initializes data generators for the mod.
	 * 
	 * @param fabricDataGenerator The data generator instance
	 */
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		// Data generators can be registered here if needed
	}
}
