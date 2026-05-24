package io.github.naimjeg.damagenexus.api.rule.entry;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DamageEntrySelectionResolver {

    private DamageEntrySelectionResolver() {
    }

    public static List<DamageEntryDefinition> resolve(
            List<DamageEntryDefinition> input
    ) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }

        List<DamageEntryDefinition> stacked =
                new ArrayList<>(input.size());

        Map<Identifier, DamageEntryDefinition> uniqueEntry = null;
        Map<Identifier, DamageEntryDefinition> uniqueGroup = null;
        Map<Identifier, DamageEntryDefinition> replace = null;

        for (DamageEntryDefinition entry : input) {
            if (entry == null) {
                continue;
            }

            DamageEntryStacking stacking = safeStacking(entry);

            switch (stacking) {
                case STACK -> stacked.add(entry);

                case UNIQUE_ENTRY -> {
                    if (uniqueEntry == null) {
                        uniqueEntry = new LinkedHashMap<>();
                    }

                    uniqueEntry.putIfAbsent(entry.id(), entry);
                }

                case UNIQUE_GROUP -> {
                    if (uniqueGroup == null) {
                        uniqueGroup = new LinkedHashMap<>();
                    }

                    uniqueGroup.putIfAbsent(entry.stackingKey(), entry);
                }

                case REPLACE -> {
                    if (replace == null) {
                        replace = new LinkedHashMap<>();
                    }

                    replace.put(entry.stackingKey(), entry);
                }
            }
        }

        if (uniqueEntry != null) {
            stacked.addAll(uniqueEntry.values());
        }

        if (uniqueGroup != null) {
            stacked.addAll(uniqueGroup.values());
        }

        if (replace != null) {
            stacked.addAll(replace.values());
        }

        return List.copyOf(stacked);
    }

    private static DamageEntryStacking safeStacking(
            DamageEntryDefinition entry
    ) {
        DamageEntryStacking stacking = entry.stacking();
        return stacking == null
                ? DamageEntryStacking.STACK
                : stacking;
    }
}