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
     * Path to the configuration file in the Fabric config directory.
     */
    public static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("trim_data.json").toFile();

    /**
     * Main configuration map storing template ID to template configuration mappings.
     * This map is populated during mod initialization and used at runtime to determine
     * which enchantments to apply.
     */
    public static Map<String, TemplateConfig> CONFIG = new HashMap<>();

    /**
     * Configuration for a single smithing template.
     * Defines the default enchantment, level, slot restrictions, and material-specific overrides.
     */
    public static class TemplateConfig {
        /**
         * The default enchantment ID to apply (e.g., "minecraft:protection").
         */
        public String enchantment;
        
        /**
         * The default level of the enchantment to apply.
         */
        public int level = 1;
        
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
         * Map of material IDs to material-specific enchantment configurations.
         * Allows different materials to grant different enchantments or levels.
         */
        public Map<String, MaterialConfig> materials = new HashMap<>();

        /**
         * Creates a new template configuration.
         * 
         * @param enchantment The enchantment ID to apply
         * @param level The level of the enchantment
         */
        public TemplateConfig(String enchantment, int level) {
            this.enchantment = enchantment;
            this.level = level;
        }
    }

    /**
     * Configuration for a specific material when used with a template.
     * Allows overriding the default enchantment or level for specific materials.
     */
    public static class MaterialConfig {
        /**
         * Optional override for the enchantment ID.
         * If null, uses the template's default enchantment.
         */
        public String enchantment;
        
        /**
         * Optional override for the enchantment level.
         * If null, uses the template's default level.
         */
        public Integer level;

        /**
         * Creates a material configuration with only a level override.
         * 
         * @param level The enchantment level for this material
         */
        public MaterialConfig(Integer level) {
            this.level = level;
        }

        /**
         * Creates a material configuration with both enchantment and level overrides.
         * 
         * @param enchantment The enchantment ID override
         * @param level The enchantment level override
         */
        public MaterialConfig(String enchantment, Integer level) {
            this.enchantment = enchantment;
            this.level = level;
        }
    }

    /**
     * Loads the configuration from disk.
     * If the configuration file does not exist, creates a default configuration
     * with predefined template-enchantment mappings and saves it to disk.
     */
    public static void load() {
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
     * Saves the current configuration to disk in JSON format.
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
     * Creates the default configuration with predefined template-enchantment mappings.
     * Sets up all vanilla Minecraft smithing templates with appropriate enchantments
     * and material-based level scaling.
     */
    private static void createDefaultConfig() {
        // Create default material tier mappings for level scaling
        Map<String, MaterialConfig> defaultMaterials = new HashMap<>();
        defaultMaterials.put("minecraft:netherite_ingot", new MaterialConfig(5));
        defaultMaterials.put("minecraft:diamond", new MaterialConfig(4));
        defaultMaterials.put("minecraft:emerald", new MaterialConfig(3));
        defaultMaterials.put("minecraft:iron_ingot", new MaterialConfig(2));
        defaultMaterials.put("minecraft:copper_ingot", new MaterialConfig(1));

        addTemplate("minecraft:sentry_armor_trim_smithing_template", "minecraft:projectile_protection", 4, defaultMaterials);
        addTemplate("minecraft:dune_armor_trim_smithing_template", "minecraft:fire_protection", 4, defaultMaterials);
        addTemplate("minecraft:coast_armor_trim_smithing_template", "minecraft:respiration", 3, defaultMaterials, java.util.Arrays.asList("head"));
        addTemplate("minecraft:wild_armor_trim_smithing_template", "minecraft:thorns", 3, defaultMaterials);
        addTemplate("minecraft:ward_armor_trim_smithing_template", "minecraft:swift_sneak", 3, defaultMaterials, java.util.Arrays.asList("legs"));
        addTemplate("minecraft:eye_armor_trim_smithing_template", "minecraft:unbreaking", 3, defaultMaterials);
        addTemplate("minecraft:vex_armor_trim_smithing_template", "minecraft:soul_speed", 3, defaultMaterials, java.util.Arrays.asList("feet"));
        addTemplate("minecraft:tide_armor_trim_smithing_template", "minecraft:depth_strider", 3, defaultMaterials, java.util.Arrays.asList("feet"));
        addTemplate("minecraft:snout_armor_trim_smithing_template", "minecraft:blast_protection", 4, defaultMaterials);
        addTemplate("minecraft:rib_armor_trim_smithing_template", "minecraft:protection", 4, defaultMaterials);
        addTemplate("minecraft:spire_armor_trim_smithing_template", "minecraft:feather_falling", 4, defaultMaterials, java.util.Arrays.asList("feet"));
        addTemplate("minecraft:wayfinder_armor_trim_smithing_template", "minecraft:frost_walker", 2, defaultMaterials, java.util.Arrays.asList("feet"));
        addTemplate("minecraft:shaper_armor_trim_smithing_template", "minecraft:mending", 1, defaultMaterials);
        addTemplate("minecraft:silence_armor_trim_smithing_template", "minecraft:swift_sneak", 3, defaultMaterials, java.util.Arrays.asList("legs"));
        addTemplate("minecraft:raiser_armor_trim_smithing_template", "minecraft:blast_protection", 4, defaultMaterials);
        addTemplate("minecraft:host_armor_trim_smithing_template", "minecraft:projectile_protection", 4, defaultMaterials);
        addTemplate("minecraft:flow_armor_trim_smithing_template", "minecraft:aqua_affinity", 1, defaultMaterials, java.util.Arrays.asList("head"));
        addTemplate("minecraft:bolt_armor_trim_smithing_template", "minecraft:protection", 4, defaultMaterials);
    }

    /**
     * Helper method to add a template configuration without slot restrictions.
     * 
     * @param id The template item ID
     * @param enchantment The default enchantment ID
     * @param level The default enchantment level
     * @param materials Map of material-specific configurations
     */
    private static void addTemplate(String id, String enchantment, int level, Map<String, MaterialConfig> materials) {
        addTemplate(id, enchantment, level, materials, null);
    }

    /**
     * Helper method to add a template configuration with optional slot restrictions.
     * 
     * @param id The template item ID
     * @param enchantment The default enchantment ID
     * @param level The default enchantment level
     * @param materials Map of material-specific configurations
     * @param allowedSlots List of allowed armor slots, or null for no restrictions
     */
    private static void addTemplate(String id, String enchantment, int level, Map<String, MaterialConfig> materials, java.util.List<String> allowedSlots) {
        TemplateConfig config = new TemplateConfig(enchantment, level);
        config.materials.putAll(materials);
        config.allowedSlots = allowedSlots;
        CONFIG.put(id, config);
    }
}
