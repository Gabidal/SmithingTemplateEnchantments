package com.gg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class SmithingTemplateEnchantmentsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("trim_data.json").toFile();

    public static Map<String, TemplateConfig> CONFIG = new HashMap<>();

    public static class TemplateConfig {
        public String enchantment;
        public int level = 1;
        public String displayName; // Optional: custom display name (falls back to translation key if null)
        public java.util.List<String> allowedSlots; // Optional: restrict to specific slots (e.g., ["feet", "legs", "chest", "head"])
        public Map<String, MaterialConfig> materials = new HashMap<>();

        public TemplateConfig(String enchantment, int level) {
            this.enchantment = enchantment;
            this.level = level;
        }
    }

    public static class MaterialConfig {
        public String enchantment;
        public Integer level;

        public MaterialConfig(Integer level) {
            this.level = level;
        }

        public MaterialConfig(String enchantment, Integer level) {
            this.enchantment = enchantment;
            this.level = level;
        }
    }

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

    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(CONFIG, writer);
        } catch (IOException e) {
            SmithingTemplateEnchantments.LOGGER.error("Failed to save config", e);
        }
    }

    private static void createDefaultConfig() {
        // Helper to add default material tiers
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

    private static void addTemplate(String id, String enchantment, int level, Map<String, MaterialConfig> materials) {
        addTemplate(id, enchantment, level, materials, null);
    }

    private static void addTemplate(String id, String enchantment, int level, Map<String, MaterialConfig> materials, java.util.List<String> allowedSlots) {
        TemplateConfig config = new TemplateConfig(enchantment, level);
        config.materials.putAll(materials);
        config.allowedSlots = allowedSlots;
        CONFIG.put(id, config);
    }
}
