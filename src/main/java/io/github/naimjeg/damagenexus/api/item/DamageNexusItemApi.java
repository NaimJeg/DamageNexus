package io.github.naimjeg.damagenexus.api.item;

import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSelectionResolver;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixValidator;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySelectionResolver;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryValidator;
import io.github.naimjeg.damagenexus.api.item.template.DamageAffixTemplateReference;
import io.github.naimjeg.damagenexus.api.item.template.DamageEntryTemplateReference;
import io.github.naimjeg.damagenexus.api.item.template.DamageItemTemplateReferences;
import io.github.naimjeg.damagenexus.core.security.DamageNexusItemSecurity;
import io.github.naimjeg.damagenexus.registry.ModDataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class DamageNexusItemApi {

    private static final String SOURCE_SET_ENTRIES = "item_api/set_entries";
    private static final String SOURCE_SET_AFFIXES = "item_api/set_affixes";
    private static final String SOURCE_ADD_ENTRY = "item_api/add_entry";
    private static final String SOURCE_ADD_AFFIX = "item_api/add_affix";

    private DamageNexusItemApi() {
    }

    /**
     * Returns only definitions materialized directly in the
     * {@code damage_entries}/{@code damage_affixes} components. Payload-free
     * template references are available through {@link #getTemplateReferences}
     * and are not resolved by this client-safe accessor.
     */
    public static DamageNexusItemEntries get(ItemStack stack) {
        if (isUnavailable(stack)) {
            return DamageNexusItemEntries.EMPTY;
        }

        List<DamageEntryDefinition> entries = stack.getOrDefault(
                ModDataComponents.DAMAGE_ENTRIES.get(),
                List.of()
        );

        List<DamageAffixDefinition> affixes = stack.getOrDefault(
                ModDataComponents.DAMAGE_AFFIXES.get(),
                List.of()
        );

        return new DamageNexusItemEntries(entries, affixes);
    }

    /**
     * Replaces the two materialized definition components. Template references
     * are a separate component and are left unchanged.
     */
    public static boolean set(
            ItemStack stack,
            DamageNexusItemEntries value
    ) {
        if (isUnavailable(stack)) {
            return false;
        }

        DamageNexusItemEntries normalized =
                value == null ? DamageNexusItemEntries.EMPTY : value;

        DamageNexusItemSecurity.ValidatedItemRules validated =
                DamageNexusItemSecurity.validateDefinitions(
                        normalized.entries(),
                        normalized.affixes(),
                        "item_api/set"
                );

        if (!validated.authoritative()) {
            return false;
        }

        setRawEntries(stack, validated.entries());
        setRawAffixes(stack, validated.affixes());

        return true;
    }

    /** Clears materialized definitions and payload-free template references. */
    public static boolean clear(ItemStack stack) {
        if (isUnavailable(stack)) {
            return false;
        }

        boolean changed =
                stack.has(ModDataComponents.DAMAGE_ENTRIES.get())
                        || stack.has(ModDataComponents.DAMAGE_AFFIXES.get())
                        || stack.has(ModDataComponents
                        .DAMAGE_TEMPLATE_REFERENCES.get());

        stack.remove(ModDataComponents.DAMAGE_ENTRIES.get());
        stack.remove(ModDataComponents.DAMAGE_AFFIXES.get());
        stack.remove(ModDataComponents.DAMAGE_TEMPLATE_REFERENCES.get());

        return changed;
    }

    /** True when either materialized definitions or template references exist. */
    public static boolean hasAny(ItemStack stack) {
        return !get(stack).isEmpty() || !getTemplateReferences(stack).isEmpty();
    }

    /** Returns ordered, payload-free references without resolving them. */
    public static DamageItemTemplateReferences getTemplateReferences(
            ItemStack stack
    ) {
        if (isUnavailable(stack)) {
            return DamageItemTemplateReferences.EMPTY;
        }
        return stack.getOrDefault(
                ModDataComponents.DAMAGE_TEMPLATE_REFERENCES.get(),
                DamageItemTemplateReferences.EMPTY
        );
    }

    /**
     * Stores payload-free IDs. A {@code true} result only means the component
     * was stored; it does not mean the IDs are currently resolved, validated
     * against server channels, or executable. Server execution performs those
     * checks through the authoritative template snapshot and item-security path.
     */
    public static boolean setTemplateReferences(
            ItemStack stack,
            DamageItemTemplateReferences references
    ) {
        if (isUnavailable(stack)) {
            return false;
        }
        DamageItemTemplateReferences safe = references == null
                ? DamageItemTemplateReferences.EMPTY
                : references;
        if (safe.isEmpty()) {
            stack.remove(ModDataComponents.DAMAGE_TEMPLATE_REFERENCES.get());
        } else {
            stack.set(ModDataComponents.DAMAGE_TEMPLATE_REFERENCES.get(), safe);
        }
        return true;
    }

    public static List<DamageEntryTemplateReference>
    getEntryTemplateReferences(ItemStack stack) {
        return getTemplateReferences(stack).entries();
    }

    public static List<DamageAffixTemplateReference>
    getAffixTemplateReferences(ItemStack stack) {
        return getTemplateReferences(stack).affixes();
    }

    public static boolean addEntryTemplateReference(
            ItemStack stack,
            DamageEntryTemplateReference reference
    ) {
        Objects.requireNonNull(reference, "reference");
        return setTemplateReferences(
                stack,
                getTemplateReferences(stack).withAddedEntry(reference));
    }

    public static boolean addAffixTemplateReference(
            ItemStack stack,
            DamageAffixTemplateReference reference
    ) {
        Objects.requireNonNull(reference, "reference");
        return setTemplateReferences(
                stack,
                getTemplateReferences(stack).withAddedAffix(reference));
    }

    public static int removeEntryTemplateReferences(
            ItemStack stack,
            Identifier id
    ) {
        Objects.requireNonNull(id, "id");
        DamageItemTemplateReferences current = getTemplateReferences(stack);
        List<DamageEntryTemplateReference> kept = current.entries().stream()
                .filter(reference -> !reference.id().equals(id))
                .toList();
        int removed = current.entries().size() - kept.size();
        if (removed > 0) {
            setTemplateReferences(stack,
                    new DamageItemTemplateReferences(kept, current.affixes()));
        }
        return removed;
    }

    public static int removeAffixTemplateReferences(
            ItemStack stack,
            Identifier id
    ) {
        Objects.requireNonNull(id, "id");
        DamageItemTemplateReferences current = getTemplateReferences(stack);
        List<DamageAffixTemplateReference> kept = current.affixes().stream()
                .filter(reference -> !reference.id().equals(id))
                .toList();
        int removed = current.affixes().size() - kept.size();
        if (removed > 0) {
            setTemplateReferences(stack,
                    new DamageItemTemplateReferences(current.entries(), kept));
        }
        return removed;
    }

    /** Returns materialized entries without resolving template references. */
    public static List<DamageEntryDefinition> getMaterializedEntries(
            ItemStack stack
    ) {
        return get(stack).entries();
    }

    /**
     * Applies entry stacking resolution to materialized entries.
     * "Resolved" here does not mean template-registry resolution.
     */
    public static List<DamageEntryDefinition> getResolvedMaterializedEntries(
            ItemStack stack
    ) {
        return DamageEntrySelectionResolver.resolve(
                getMaterializedEntries(stack));
    }

    public static boolean setEntries(
            ItemStack stack,
            List<DamageEntryDefinition> entries
    ) {
        if (isUnavailable(stack)) {
            return false;
        }

        DamageNexusItemSecurity.ValidatedItemRules validated =
                DamageNexusItemSecurity.validateDefinitions(
                        entries,
                        getMaterializedAffixes(stack),
                        SOURCE_SET_ENTRIES
                );

        if (!validated.authoritative()) {
            return false;
        }

        setRawEntries(stack, validated.entries());
        setRawAffixes(stack, validated.affixes());
        return true;
    }

    public static boolean addEntry(
            ItemStack stack,
            DamageEntryDefinition entry
    ) {
        return addEntry(stack, entry, SOURCE_ADD_ENTRY);
    }

    public static boolean addEntry(
            ItemStack stack,
            DamageEntryDefinition entry,
            String source
    ) {
        if (isUnavailable(stack) || entry == null) {
            return false;
        }

        String effectiveSource =
                source == null || source.isBlank()
                        ? SOURCE_ADD_ENTRY
                        : source;

        List<DamageEntryDefinition> valid =
                DamageEntryValidator.filterValid(
                        List.of(entry),
                        effectiveSource
                );

        if (valid.isEmpty()) {
            return false;
        }

        List<DamageEntryDefinition> next =
                new ArrayList<>(getMaterializedEntries(stack));

        next.addAll(valid);

        return setEntries(stack, next);
    }

    public static boolean removeEntry(
            ItemStack stack,
            Identifier entryId
    ) {
        Objects.requireNonNull(entryId, "entryId must not be null");

        return removeEntries(
                stack,
                entry -> entry.id().equals(entryId)
        ) > 0;
    }

    public static int removeEntries(
            ItemStack stack,
            Predicate<DamageEntryDefinition> predicate
    ) {
        if (isUnavailable(stack) || predicate == null) {
            return 0;
        }

        List<DamageEntryDefinition> current = getMaterializedEntries(stack);

        if (current.isEmpty()) {
            return 0;
        }

        List<DamageEntryDefinition> kept = new ArrayList<>();
        int removed = 0;

        for (DamageEntryDefinition entry : current) {
            if (predicate.test(entry)) {
                removed++;
            } else {
                kept.add(entry);
            }
        }

        if (removed == 0) {
            return 0;
        }

        setRawEntries(stack, kept);
        return removed;
    }

    public static boolean hasEntry(
            ItemStack stack,
            Identifier entryId
    ) {
        Objects.requireNonNull(entryId, "entryId must not be null");

        for (DamageEntryDefinition entry : getMaterializedEntries(stack)) {
            if (entry.id().equals(entryId)) {
                return true;
            }
        }

        return false;
    }

    /** Returns materialized affixes without resolving template references. */
    public static List<DamageAffixDefinition> getMaterializedAffixes(
            ItemStack stack
    ) {
        return get(stack).affixes();
    }

    /**
     * Applies affix stacking resolution to materialized affixes.
     * "Resolved" here does not mean template-registry resolution.
     */
    public static List<DamageAffixDefinition> getResolvedMaterializedAffixes(
            ItemStack stack
    ) {
        return DamageAffixSelectionResolver.resolve(
                getMaterializedAffixes(stack));
    }

    public static boolean setAffixes(
            ItemStack stack,
            List<DamageAffixDefinition> affixes
    ) {
        if (isUnavailable(stack)) {
            return false;
        }

        DamageNexusItemSecurity.ValidatedItemRules validated =
                DamageNexusItemSecurity.validateDefinitions(
                        getMaterializedEntries(stack),
                        affixes,
                        SOURCE_SET_AFFIXES
                );

        if (!validated.authoritative()) {
            return false;
        }

        setRawEntries(stack, validated.entries());
        setRawAffixes(stack, validated.affixes());
        return true;
    }

    public static boolean addAffix(
            ItemStack stack,
            DamageAffixDefinition affix
    ) {
        return addAffix(stack, affix, SOURCE_ADD_AFFIX);
    }

    public static boolean addAffix(
            ItemStack stack,
            DamageAffixDefinition affix,
            String source
    ) {
        if (isUnavailable(stack) || affix == null) {
            return false;
        }

        String effectiveSource =
                source == null || source.isBlank()
                        ? SOURCE_ADD_AFFIX
                        : source;

        List<DamageAffixDefinition> valid =
                DamageAffixValidator.filterValid(
                        List.of(affix),
                        effectiveSource
                );

        if (valid.isEmpty()) {
            return false;
        }

        List<DamageAffixDefinition> next =
                new ArrayList<>(getMaterializedAffixes(stack));

        next.addAll(valid);

        return setAffixes(stack, next);
    }

    public static boolean removeAffix(
            ItemStack stack,
            Identifier affixId
    ) {
        Objects.requireNonNull(affixId, "affixId must not be null");

        return removeAffixes(
                stack,
                affix -> affix.id().equals(affixId)
        ) > 0;
    }

    public static int removeAffixes(
            ItemStack stack,
            Predicate<DamageAffixDefinition> predicate
    ) {
        if (isUnavailable(stack) || predicate == null) {
            return 0;
        }

        List<DamageAffixDefinition> current = getMaterializedAffixes(stack);

        if (current.isEmpty()) {
            return 0;
        }

        List<DamageAffixDefinition> kept = new ArrayList<>();
        int removed = 0;

        for (DamageAffixDefinition affix : current) {
            if (predicate.test(affix)) {
                removed++;
            } else {
                kept.add(affix);
            }
        }

        if (removed == 0) {
            return 0;
        }

        setRawAffixes(stack, kept);
        return removed;
    }

    public static boolean hasAffix(
            ItemStack stack,
            Identifier affixId
    ) {
        Objects.requireNonNull(affixId, "affixId must not be null");

        for (DamageAffixDefinition affix : getMaterializedAffixes(stack)) {
            if (affix.id().equals(affixId)) {
                return true;
            }
        }

        return false;
    }

    public static int removeEntriesFromNamespace(
            ItemStack stack,
            String namespace
    ) {
        if (namespace == null || namespace.isBlank()) {
            return 0;
        }

        return removeEntries(
                stack,
                entry -> namespace.equals(entry.id().getNamespace())
        );
    }

    public static int removeAffixesFromNamespace(
            ItemStack stack,
            String namespace
    ) {
        if (namespace == null || namespace.isBlank()) {
            return 0;
        }

        return removeAffixes(
                stack,
                affix -> namespace.equals(affix.id().getNamespace())
        );
    }

    private static void setRawEntries(
            ItemStack stack,
            List<DamageEntryDefinition> entries
    ) {
        if (entries == null || entries.isEmpty()) {
            stack.remove(ModDataComponents.DAMAGE_ENTRIES.get());
            return;
        }

        stack.set(
                ModDataComponents.DAMAGE_ENTRIES.get(),
                List.copyOf(entries)
        );
    }

    private static void setRawAffixes(
            ItemStack stack,
            List<DamageAffixDefinition> affixes
    ) {
        if (affixes == null || affixes.isEmpty()) {
            stack.remove(ModDataComponents.DAMAGE_AFFIXES.get());
            return;
        }

        stack.set(
                ModDataComponents.DAMAGE_AFFIXES.get(),
                List.copyOf(affixes)
        );
    }

    private static boolean isUnavailable(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }
}
