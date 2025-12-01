package com.gg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration manager for Smithing Template Enchantments.
 * Handles loading, saving, and managing enchantment configurations for smithing templates.
 * Configuration is stored in JSON format and allows customization of which enchantments
 * are applied by each template, including material-specific overrides.
 */
public class SmithingTemplateEnchantmentsConfig {
    /**
     * JSON serializer/deserializer with pretty printing enabled for human-readable configuration files.
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Path to the trim data configuration file in the Fabric config directory.
     */
    public static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("trim_data.json").toFile();
    
    /**
     * Path to the material data configuration file in the Fabric config directory.
     */
    public static final File MATERIAL_CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("material_data.json").toFile();
    
    /**
     * Path to the enchantment probabilities configuration file in the Fabric config directory.
     */
    public static final File PROBABILITIES_CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("probabilities_data.json").toFile();
    
    /**
     * Path to the loot configuration file in the Fabric config directory.
     */
    public static final File LOOT_CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("loot_config.json").toFile();

    /**
     * Main configuration map storing template ID to template configuration mappings.
     * This map is populated during mod initialization and used at runtime to determine
     * which enchantments to apply.
     */
    public static Map<String, TemplateConfig> CONFIG = new HashMap<>();
    
    /**
     * Global material configuration map storing material ID to level mappings.
     * This map defines which materials can be used in the smithing table and
     * what enchantment level bonus each material provides.
     */
    public static Map<String, Integer> MATERIAL_LEVELS = new HashMap<>();
    
    /**
     * Global enchantment probability map storing enchantment ID to probability weight.
     * Used for postfix enchantments (stored on templates from loot) to determine:
     * 1. Weighted selection when generating enchanted templates for loot
     * 2. Probability-based level rolling during smithing with exponential decay
     * Higher weights = more likely to be selected/applied (e.g., 0.80 = 80% base chance).
     */
    public static Map<String, Double> ENCHANTMENT_PROBABILITIES = new HashMap<>();
    
    /**
     * Configuration for enchanted template loot spawning.
     */
    public static class LootConfig {
        /**
         * Chance for a smithing template to be enchanted when spawning in loot (0.0-1.0).
         * Default: 0.654321 (65.4321% chance)
         */
        public double enchantedTemplateChance = 0.654321;
        
        /**
         * Minimum number of enchantments on an enchanted template.
         * Default: 1
         */
        public int minEnchantments = 1;
        
        /**
         * Maximum number of enchantments on an enchanted template.
         * Default: 4
         */
        public int maxEnchantments = 4;
        
        /**
         * Whether enchanted templates should spawn in loot tables.
         * Default: true
         */
        public boolean enableEnchantedLoot = true;
    }
    
    /**
     * Loot configuration instance.
     */
    public static LootConfig LOOT_CONFIG = new LootConfig();

    /**
     * Configuration for a single smithing template.
     * Defines the default enchantment, slot restrictions, and material-specific overrides.
     */
    public static class TemplateConfig {
        /**
         * The default enchantment ID to apply (e.g., "minecraft:protection").
         */
        public String enchantment;
        
        /**
         * Optional custom display name for the template.
         * If null, falls back to the translation key.
         */
        public String displayName;
        
        /**
         * Optional list of allowed armor slots for this template.
         * Valid values: "feet", "legs", "chest", "head".
         * If null or empty, the template can be applied to any armor piece.
         */
        public java.util.List<String> allowedSlots;
        
        /**
         * Map of material IDs to material-specific enchantment overrides.
         * Allows specific materials to grant different enchantments for this template.
         * If a material is not in this map, it uses the template's default enchantment
         * with the level from the global MATERIAL_LEVELS map.
         */
        public Map<String, MaterialConfig> materials = new HashMap<>();
        
        /**
         * Optional probability weight override for this template's configured enchantment.
         * If null, uses the global probability from ENCHANTMENT_PROBABILITIES.
         * If set, overrides the default probability for this template's enchantment.
         */
        public Double probability;

        /**
         * No-args constructor for GSON deserialization.
         */
        public TemplateConfig() {
        }

        /**
         * Creates a new template configuration.
         * 
         * @param enchantment The enchantment ID to apply
         */
        public TemplateConfig(String enchantment) {
            this.enchantment = enchantment;
        }
    }

    /**
     * Configuration for a specific material when used with a template.
     * Allows overriding the default enchantment for specific materials.
     * The level is always determined by the global MATERIAL_LEVELS map.
     */
    public static class MaterialConfig {
        /**
         * Optional override for the enchantment ID.
         * If null, uses the template's default enchantment.
         */
        public String enchantment;

        /**
         * No-args constructor for GSON deserialization.
         */
        public MaterialConfig() {
        }

        /**
         * Creates a material configuration with an enchantment override.
         * 
         * @param enchantment The enchantment ID override
         */
        public MaterialConfig(String enchantment) {
            this.enchantment = enchantment;
        }
    }

    /**
     * Loads the configuration from disk.
     * If the configuration files do not exist, creates default configurations
     * with predefined template-enchantment mappings, material levels, and probabilities, then saves them to disk.
     */
    public static void load() {
        // Load material levels
        if (MATERIAL_CONFIG_FILE.exists()) {
            try (Reader reader = new FileReader(MATERIAL_CONFIG_FILE)) {
                Type type = new TypeToken<Map<String, Integer>>(){}.getType();
                MATERIAL_LEVELS = GSON.fromJson(reader, type);
            } catch (IOException e) {
                SmithingTemplateEnchantments.LOGGER.error("Failed to load material config", e);
            }
        } else {
            createDefaultMaterials();
            saveMaterials();
        }
        
        // Load enchantment probabilities
        if (PROBABILITIES_CONFIG_FILE.exists()) {
            try (Reader reader = new FileReader(PROBABILITIES_CONFIG_FILE)) {
                Type type = new TypeToken<Map<String, Double>>(){}.getType();
                ENCHANTMENT_PROBABILITIES = GSON.fromJson(reader, type);
            } catch (IOException e) {
                SmithingTemplateEnchantments.LOGGER.error("Failed to load probabilities config", e);
            }
        } else {
            createDefaultProbabilities();
            saveProbabilities();
        }
        
		// Load loot configuration
		if (LOOT_CONFIG_FILE.exists()) {
			try (Reader reader = new FileReader(LOOT_CONFIG_FILE)) {
				LOOT_CONFIG = GSON.fromJson(reader, LootConfig.class);
			} catch (IOException e) {
				SmithingTemplateEnchantments.LOGGER.error("Failed to load loot config", e);
			}
		} else {
			saveLootConfig();
		}        // Load template configurations
        if (CONFIG_FILE.exists()) {
            try (Reader reader = new FileReader(CONFIG_FILE)) {
                Type type = new TypeToken<Map<String, TemplateConfig>>(){}.getType();
                CONFIG = GSON.fromJson(reader, type);
            } catch (IOException e) {
                SmithingTemplateEnchantments.LOGGER.error("Failed to load config", e);
            }
        } else {
            createDefaultConfig();
            save();
        }
    }

    /**
     * Saves the current template configuration to disk in JSON format.
     * Uses pretty printing to make the file human-readable and editable.
     */
    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(CONFIG, writer);
        } catch (IOException e) {
            SmithingTemplateEnchantments.LOGGER.error("Failed to save config", e);
        }
    }
    
    /**
     * Saves the current material configuration to disk in JSON format.
     * Uses pretty printing to make the file human-readable and editable.
     */
    public static void saveMaterials() {
        try (Writer writer = new FileWriter(MATERIAL_CONFIG_FILE)) {
            GSON.toJson(MATERIAL_LEVELS, writer);
        } catch (IOException e) {
            SmithingTemplateEnchantments.LOGGER.error("Failed to save material config", e);
        }
    }
    
    /**
     * Saves the current enchantment probabilities configuration to disk in JSON format.
     * Uses pretty printing to make the file human-readable and editable.
     */
    public static void saveProbabilities() {
        try (Writer writer = new FileWriter(PROBABILITIES_CONFIG_FILE)) {
            GSON.toJson(ENCHANTMENT_PROBABILITIES, writer);
        } catch (IOException e) {
            SmithingTemplateEnchantments.LOGGER.error("Failed to save probabilities config", e);
        }
    }
    
    /**
     * Saves the current loot configuration to disk in JSON format.
     * Uses pretty printing to make the file human-readable and editable.
     */
    public static void saveLootConfig() {
        try (Writer writer = new FileWriter(LOOT_CONFIG_FILE)) {
            GSON.toJson(LOOT_CONFIG, writer);
        } catch (IOException e) {
            SmithingTemplateEnchantments.LOGGER.error("Failed to save loot config", e);
        }
    }

    /**
     * Creates the default material level mappings.
     * Sets up common Minecraft materials with their enchantment level bonuses.
     */
    private static void createDefaultMaterials() {
        // Legendary materials (level 6)
        MATERIAL_LEVELS.put("minecraft:nether_star", 6);
        
        // Epic materials (level 5)
        MATERIAL_LEVELS.put("minecraft:netherite_ingot", 5);
        
        // Rare materials (level 4)
        MATERIAL_LEVELS.put("minecraft:diamond", 4);
        
        // Uncommon materials (level 3)
        MATERIAL_LEVELS.put("minecraft:emerald", 3);
        MATERIAL_LEVELS.put("minecraft:ender_eye", 3);
        MATERIAL_LEVELS.put("minecraft:echo_shard", 3);
        
        // Common materials (level 2)
        MATERIAL_LEVELS.put("minecraft:gold_ingot", 2);
        MATERIAL_LEVELS.put("minecraft:phantom_membrane", 2);
        MATERIAL_LEVELS.put("minecraft:blaze_rod", 2);
        
        // Basic materials (level 1)
        MATERIAL_LEVELS.put("minecraft:iron_ingot", 1);
        MATERIAL_LEVELS.put("minecraft:copper_ingot", 1);
        MATERIAL_LEVELS.put("minecraft:lapis_lazuli", 1);
        MATERIAL_LEVELS.put("minecraft:redstone", 1);
        MATERIAL_LEVELS.put("minecraft:quartz", 1);
        MATERIAL_LEVELS.put("minecraft:amethyst_shard", 1);
        MATERIAL_LEVELS.put("minecraft:prismarine_crystals", 1);
        MATERIAL_LEVELS.put("minecraft:prismarine_shard", 1);
    }
    
    /**
     * Creates the default enchantment probability mappings.
     * Sets up all Minecraft enchantments with their probability weights (0.05-0.45).
     * Higher weights mean more likely to be applied when rolling.
     */
    private static void createDefaultProbabilities() {
        // Very rare enchantments
        ENCHANTMENT_PROBABILITIES.put("minecraft:mending", 0.05);
        ENCHANTMENT_PROBABILITIES.put("minecraft:vanishing_curse", 0.05);
        ENCHANTMENT_PROBABILITIES.put("minecraft:binding_curse", 0.05);
        
        // Rare enchantments
        ENCHANTMENT_PROBABILITIES.put("minecraft:protection", 0.08);
        ENCHANTMENT_PROBABILITIES.put("minecraft:sharpness", 0.08);
        ENCHANTMENT_PROBABILITIES.put("minecraft:efficiency", 0.08);
        ENCHANTMENT_PROBABILITIES.put("minecraft:silk_touch", 0.08);
        ENCHANTMENT_PROBABILITIES.put("minecraft:unbreaking", 0.08);
        
        // Uncommon enchantments
        ENCHANTMENT_PROBABILITIES.put("minecraft:infinity", 0.12);
        ENCHANTMENT_PROBABILITIES.put("minecraft:fortune", 0.15);
        ENCHANTMENT_PROBABILITIES.put("minecraft:looting", 0.15);
        ENCHANTMENT_PROBABILITIES.put("minecraft:luck_of_the_sea", 0.18);
        
        // Common enchantments
        ENCHANTMENT_PROBABILITIES.put("minecraft:feather_falling", 0.30);
        ENCHANTMENT_PROBABILITIES.put("minecraft:thorns", 0.30);
        ENCHANTMENT_PROBABILITIES.put("minecraft:power", 0.30);
        
        // Protection variants
        ENCHANTMENT_PROBABILITIES.put("minecraft:fire_protection", 0.35);
        ENCHANTMENT_PROBABILITIES.put("minecraft:blast_protection", 0.35);
        ENCHANTMENT_PROBABILITIES.put("minecraft:projectile_protection", 0.35);
        
        // Weapon enchantments
        ENCHANTMENT_PROBABILITIES.put("minecraft:smite", 0.35);
        ENCHANTMENT_PROBABILITIES.put("minecraft:bane_of_arthropods", 0.35);
        
        // Crossbow enchantments
        ENCHANTMENT_PROBABILITIES.put("minecraft:quick_charge", 0.35);
        ENCHANTMENT_PROBABILITIES.put("minecraft:piercing", 0.35);
        
        // Water enchantments
        ENCHANTMENT_PROBABILITIES.put("minecraft:respiration", 0.38);
        ENCHANTMENT_PROBABILITIES.put("minecraft:aqua_affinity", 0.38);
        ENCHANTMENT_PROBABILITIES.put("minecraft:depth_strider", 0.38);
        
        // Combat enchantments
        ENCHANTMENT_PROBABILITIES.put("minecraft:fire_aspect", 0.42);
        ENCHANTMENT_PROBABILITIES.put("minecraft:sweeping_edge", 0.42);
        ENCHANTMENT_PROBABILITIES.put("minecraft:punch", 0.42);
        ENCHANTMENT_PROBABILITIES.put("minecraft:swift_sneak", 0.42);
        ENCHANTMENT_PROBABILITIES.put("minecraft:flame", 0.42);
        
        // Trident enchantments
        ENCHANTMENT_PROBABILITIES.put("minecraft:impaling", 0.42);
        ENCHANTMENT_PROBABILITIES.put("minecraft:riptide", 0.42);
        
        // Very common enchantments
        ENCHANTMENT_PROBABILITIES.put("minecraft:frost_walker", 0.45);
        ENCHANTMENT_PROBABILITIES.put("minecraft:soul_speed", 0.45);
        ENCHANTMENT_PROBABILITIES.put("minecraft:lure", 0.45);
        ENCHANTMENT_PROBABILITIES.put("minecraft:loyalty", 0.45);
        ENCHANTMENT_PROBABILITIES.put("minecraft:channeling", 0.45);
        ENCHANTMENT_PROBABILITIES.put("minecraft:multishot", 0.45);
        ENCHANTMENT_PROBABILITIES.put("minecraft:knockback", 0.45);
    }
    
    /**
     * Creates the default configuration with predefined template-enchantment mappings.
     * Sets up all vanilla Minecraft smithing templates with appropriate enchantments.
     */
    private static void createDefaultConfig() {

        addTemplate("minecraft:sentry_armor_trim_smithing_template", "minecraft:projectile_protection");
        addTemplate("minecraft:dune_armor_trim_smithing_template", "minecraft:fire_protection");
        addTemplate("minecraft:coast_armor_trim_smithing_template", "minecraft:respiration", java.util.Arrays.asList("head"));
        addTemplate("minecraft:wild_armor_trim_smithing_template", "minecraft:thorns");
        addTemplate("minecraft:ward_armor_trim_smithing_template", "minecraft:swift_sneak", java.util.Arrays.asList("legs"));
        addTemplate("minecraft:eye_armor_trim_smithing_template", "minecraft:unbreaking");
        addTemplate("minecraft:vex_armor_trim_smithing_template", "minecraft:soul_speed", java.util.Arrays.asList("feet"));
        addTemplate("minecraft:tide_armor_trim_smithing_template", "minecraft:depth_strider", java.util.Arrays.asList("feet"));
        addTemplate("minecraft:snout_armor_trim_smithing_template", "minecraft:blast_protection");
        addTemplate("minecraft:rib_armor_trim_smithing_template", "minecraft:protection");
        addTemplate("minecraft:spire_armor_trim_smithing_template", "minecraft:feather_falling", java.util.Arrays.asList("feet"));
        addTemplate("minecraft:wayfinder_armor_trim_smithing_template", "minecraft:frost_walker", java.util.Arrays.asList("feet"));
        addTemplate("minecraft:shaper_armor_trim_smithing_template", "minecraft:mending");
        addTemplate("minecraft:silence_armor_trim_smithing_template", "minecraft:swift_sneak", java.util.Arrays.asList("legs"));
        addTemplate("minecraft:raiser_armor_trim_smithing_template", "minecraft:blast_protection");
        addTemplate("minecraft:host_armor_trim_smithing_template", "minecraft:projectile_protection");
        addTemplate("minecraft:flow_armor_trim_smithing_template", "minecraft:aqua_affinity", java.util.Arrays.asList("head"));
        addTemplate("minecraft:bolt_armor_trim_smithing_template", "minecraft:protection");
    }

    /**
     * Helper method to add a template configuration without slot restrictions.
     * 
     * @param id The template item ID
     * @param enchantment The default enchantment ID
     */
    private static void addTemplate(String id, String enchantment) {
        addTemplate(id, enchantment, null);
    }

    /**
     * Helper method to add a template configuration with optional slot restrictions.
     * 
     * @param id The template item ID
     * @param enchantment The default enchantment ID
     * @param allowedSlots List of allowed armor slots, or null for no restrictions
     */
    private static void addTemplate(String id, String enchantment, java.util.List<String> allowedSlots) {
        TemplateConfig config = new TemplateConfig(enchantment);
        config.allowedSlots = allowedSlots;
        CONFIG.put(id, config);
    }
}
