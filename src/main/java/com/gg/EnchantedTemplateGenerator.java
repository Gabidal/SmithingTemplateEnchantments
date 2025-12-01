package com.gg;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.*;

/**
 * Utility class for generating enchanted smithing templates for loot tables.
 * Creates smithing template items with multiple enchantments (configurable max) using a
 * probability-based roll system. Uses the enchantedTemplateChance for each slot,
 * and weighted selection from candidate enchantments. Enchantments are stored at level 1
 * on the template; the actual level is determined by probability and material during smithing.
 */
public class EnchantedTemplateGenerator {
    
    private static final Random RANDOM = new Random();
    
    /**
     * Determines if a smithing template should be enchanted when spawning in loot.
     * Uses the configured chance from loot_config.json.
     * 
     * @return true if the template should be enchanted
     */
	public static boolean shouldEnchant() {
		double roll = RANDOM.nextDouble();
		double chance = SmithingTemplateEnchantmentsConfig.LOOT_CONFIG.enchantedTemplateChance;
		return roll < chance;
	}    /**
     * Generates an enchanted smithing template with multiple enchantments.
     * Uses the configured enchantedTemplateChance for each enchantment slot (uniform chance).
     * Enchantments are selected using weighted probability from a candidate list that includes
     * the template's configured enchantment, material-specific overrides, and random selections
     * from the global enchantment list. Maximum enchantments is configurable via loot_config.json.
     * All enchantments are stored at level 1; actual levels are determined by probability
     * and material when the template is applied at a smithing table.
     * 
     * @param templateStack The base smithing template item stack
     * @param registries Holder lookup provider for accessing enchantment registry
     * @return An enchanted version of the template, or the original if it can't be enchanted
     */
    public static ItemStack generateEnchantedTemplate(ItemStack templateStack, net.minecraft.core.HolderLookup.Provider registries) {
        if (templateStack.isEmpty()) {
            return templateStack;
        }
        
        // Check if this template is configured
        ResourceLocation templateId = BuiltInRegistries.ITEM.getKey(templateStack.getItem());
        SmithingTemplateEnchantmentsConfig.TemplateConfig templateConfig = 
            SmithingTemplateEnchantmentsConfig.CONFIG.get(templateId.toString());
        
        if (templateConfig == null || templateConfig.enchantment == null) {
            // Not configured, return original
            return templateStack;
        }
        
        // Create a copy to enchant
        ItemStack enchantedTemplate = templateStack.copy();
        
        // Get max enchantments from config (default to 6)
        int maxEnchantments = SmithingTemplateEnchantmentsConfig.LOOT_CONFIG.maxEnchantments;
        
        // Get the enchantment registry
        var enchantmentRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        
        // Build a list of candidate enchantments
        List<EnchantmentCandidate> candidates = buildEnchantmentCandidates(templateConfig);
        
        if (candidates.isEmpty()) {
            return enchantedTemplate;
        }
        
        // Sort candidates by probability (higher probability first for weighted selection)
        candidates.sort(Comparator.comparingDouble(c -> -c.probability));
        
        // Apply enchantments - try up to maxEnchantments times
        Set<String> appliedEnchantments = new HashSet<>();
        double enchantChance = SmithingTemplateEnchantmentsConfig.LOOT_CONFIG.enchantedTemplateChance;
        
        for (int attempt = 0; attempt < maxEnchantments; attempt++) {
            // Roll to see if we should add an enchantment this iteration
            // Use the same chance for all slots
            if (RANDOM.nextDouble() > enchantChance) {
                // Failed the roll - stop trying to add more enchantments
                break;
            }
            
            // Try to find and apply a valid enchantment
            EnchantmentCandidate candidate = null;
            int searchAttempts = 0;
            while (searchAttempts < 10) {
                candidate = selectWeightedCandidate(candidates);
                if (candidate != null && !appliedEnchantments.contains(candidate.enchantmentId)) {
                    break;
                }
                searchAttempts++;
            }
            
            if (candidate == null || appliedEnchantments.contains(candidate.enchantmentId)) {
                // No more unique enchantments available
                break;
            }
            
            // Try to apply this enchantment
            var enchantmentKey = ResourceLocation.parse(candidate.enchantmentId);
            Optional<Holder.Reference<Enchantment>> enchantmentOpt = 
                enchantmentRegistry.get(ResourceKey.create(Registries.ENCHANTMENT, enchantmentKey));
            
            if (enchantmentOpt.isPresent()) {
                // Apply enchantment at level 1 - the actual level will be determined by the material
                // when the template is used at the smithing table
                enchantedTemplate.enchant(enchantmentOpt.get(), 1);
                appliedEnchantments.add(candidate.enchantmentId);
            }
        }
        
        return enchantedTemplate;
    }
    
    /**
     * Builds a list of enchantment candidates based on the template configuration
     * and global enchantment probabilities.
     * 
     * @param templateConfig The template's configuration
     * @return List of enchantment candidates with their probabilities
     */
    private static List<EnchantmentCandidate> buildEnchantmentCandidates(SmithingTemplateEnchantmentsConfig.TemplateConfig templateConfig) {
        List<EnchantmentCandidate> candidates = new ArrayList<>();
        
        // Add the main enchantment from the template config
        if (templateConfig.enchantment != null) {
            double probability = templateConfig.probability != null ? 
                templateConfig.probability : 
                SmithingTemplateEnchantmentsConfig.ENCHANTMENT_PROBABILITIES.getOrDefault(templateConfig.enchantment, 0.5);
            candidates.add(new EnchantmentCandidate(templateConfig.enchantment, probability));
        }
        
        // Add material-specific enchantments
        if (templateConfig.materials != null) {
            for (var materialEntry : templateConfig.materials.entrySet()) {
                var materialConfig = materialEntry.getValue();
                if (materialConfig != null && materialConfig.enchantment != null) {
                    double probability = SmithingTemplateEnchantmentsConfig.ENCHANTMENT_PROBABILITIES
                        .getOrDefault(materialConfig.enchantment, 0.5);
                    
                    // Only add if not already in candidates
                    boolean alreadyAdded = candidates.stream()
                        .anyMatch(c -> c.enchantmentId.equals(materialConfig.enchantment));
                    if (!alreadyAdded) {
                        candidates.add(new EnchantmentCandidate(materialConfig.enchantment, probability));
                    }
                }
            }
        }
        
        // Add some random enchantments from the global list (for variety)
        // Pick up to 10 additional enchantments at random
        List<String> allEnchantments = new ArrayList<>(SmithingTemplateEnchantmentsConfig.ENCHANTMENT_PROBABILITIES.keySet());
        Collections.shuffle(allEnchantments, RANDOM);
        
        int additionalCount = Math.min(10, allEnchantments.size());
        for (int i = 0; i < additionalCount; i++) {
            String enchId = allEnchantments.get(i);
            boolean alreadyAdded = candidates.stream()
                .anyMatch(c -> c.enchantmentId.equals(enchId));
            
            if (!alreadyAdded) {
                double probability = SmithingTemplateEnchantmentsConfig.ENCHANTMENT_PROBABILITIES.get(enchId);
                candidates.add(new EnchantmentCandidate(enchId, probability));
            }
        }
        
        return candidates;
    }
    
    /**
     * Selects a random enchantment candidate using weighted probability.
     * Higher probability enchantments are more likely to be selected.
     * 
     * @param candidates List of enchantment candidates
     * @return A randomly selected candidate, or null if list is empty
     */
    private static EnchantmentCandidate selectWeightedCandidate(List<EnchantmentCandidate> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        
        // Calculate total weight
        double totalWeight = candidates.stream()
            .mapToDouble(c -> c.probability)
            .sum();
        
        // Pick a random value
        double randomValue = RANDOM.nextDouble() * totalWeight;
        
        // Find the candidate
        double currentWeight = 0;
        for (EnchantmentCandidate candidate : candidates) {
            currentWeight += candidate.probability;
            if (randomValue <= currentWeight) {
                return candidate;
            }
        }
        
        // Fallback to last candidate
        return candidates.get(candidates.size() - 1);
    }
    
    /**
     * Internal class to hold enchantment candidate data.
     */
    private static class EnchantmentCandidate {
        final String enchantmentId;
        final double probability;
        
        EnchantmentCandidate(String enchantmentId, double probability) {
            this.enchantmentId = enchantmentId;
            this.probability = probability;
        }
    }
}
