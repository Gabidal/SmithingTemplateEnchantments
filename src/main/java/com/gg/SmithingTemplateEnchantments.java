package com.gg;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Main entry point for the Smithing Template Enchantments mod.
 * This mod allows smithing templates to apply enchantments to armor pieces
 * when used at a smithing table, with configurable enchantments and levels
 * based on the template and material used.
 */
public class SmithingTemplateEnchantments implements ModInitializer {
	/**
	 * The unique identifier for this mod.
	 */
	public static final String MOD_ID = "smithingtemplateenchantments";

	/**
	 * Logger instance for this mod.
	 * Used to write informational messages, warnings, and errors to the console and log file.
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Initializes the mod when Minecraft loads.
	 * This method is called during the mod loading phase and sets up
	 * the configuration system for the mod.
	 */
	@Override
	public void onInitialize() {
		// Load configuration from file or create default configuration
		SmithingTemplateEnchantmentsConfig.load();
		LOGGER.info("Smithing Template Enchantments initialized successfully!");
	}
}