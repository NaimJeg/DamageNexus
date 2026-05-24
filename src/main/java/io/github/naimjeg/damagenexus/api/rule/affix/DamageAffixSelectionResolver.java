package io.github.naimjeg.damagenexus.api.rule.affix;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DamageAffixSelectionResolver {

    private DamageAffixSelectionResolver() {
    }

    public static List<DamageAffixDefinition> resolve(
            List<DamageAffixDefinition> input
    ) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }

        List<DamageAffixDefinition> stacked =
                new ArrayList<>(input.size());

        Map<Identifier, DamageAffixDefinition> uniqueAffix = null;
        Map<Identifier, DamageAffixDefinition> uniqueGroup = null;
        Map<Identifier, DamageAffixDefinition> highestLevel = null;
        Map<Identifier, DamageAffixDefinition> replace = null;

        for (DamageAffixDefinition affix : input) {
            if (affix == null) {
                continue;
            }

            DamageAffixStacking stacking = safeStacking(affix);

            switch (stacking) {
                case STACK -> stacked.add(affix);

                case UNIQUE_AFFIX -> {
                    if (uniqueAffix == null) {
                        uniqueAffix = new LinkedHashMap<>();
                    }

                    uniqueAffix.putIfAbsent(affix.id(), affix);
                }

                case UNIQUE_GROUP -> {
                    if (uniqueGroup == null) {
                        uniqueGroup = new LinkedHashMap<>();
                    }

                    uniqueGroup.putIfAbsent(affix.stackingKey(), affix);
                }

                case HIGHEST_LEVEL -> {
                    if (highestLevel == null) {
                        highestLevel = new LinkedHashMap<>();
                    }

                    highestLevel.merge(
                            affix.stackingKey(),
                            affix,
                            DamageAffixSelectionResolver::chooseHigherLevel
                    );
                }

                case REPLACE -> {
                    if (replace == null) {
                        replace = new LinkedHashMap<>();
                    }

                    replace.put(affix.stackingKey(), affix);
                }
            }
        }

        if (uniqueAffix != null) {
            stacked.addAll(uniqueAffix.values());
        }

        if (uniqueGroup != null) {
            stacked.addAll(uniqueGroup.values());
        }

        if (highestLevel != null) {
            stacked.addAll(highestLevel.values());
        }

        if (replace != null) {
            stacked.addAll(replace.values());
        }

        return List.copyOf(stacked);
    }

    private static DamageAffixDefinition chooseHigherLevel(
            DamageAffixDefinition existing,
            DamageAffixDefinition candidate
    ) {
        int existingScore = rarityScore(existing);
        int candidateScore = rarityScore(candidate);

        if (candidateScore > existingScore) {
            return candidate;
        }

        return existing;
    }

    private static int rarityScore(DamageAffixDefinition affix) {
        DamageAffixRarity rarity = affix.rarity();
        return rarity == null ? 0 : rarity.ordinal();
    }

    private static DamageAffixStacking safeStacking(
            DamageAffixDefinition affix
    ) {
        DamageAffixStacking stacking = affix.stacking();
        return stacking == null
                ? DamageAffixStacking.STACK
                : stacking;
    }
}