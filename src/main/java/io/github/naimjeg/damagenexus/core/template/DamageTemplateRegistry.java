package io.github.naimjeg.damagenexus.core.template;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusLifecycle;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusRegistrationAccess;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.core.security.DamageNexusItemSecurity;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusLifecycleLog;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Frozen Java registrations plus atomically replaceable datapack templates. */
@EventBusSubscriber(modid = DamageNexus.MODID)
public final class DamageTemplateRegistry {
    private static final Map<Identifier, DamageEntryDefinition> JAVA_ENTRIES =
            new LinkedHashMap<>();
    private static final Map<Identifier, DamageAffixDefinition> JAVA_AFFIXES =
            new LinkedHashMap<>();
    private static volatile DamageTemplateSnapshot snapshot =
            unvalidatedSnapshot(0L, Map.of(), Map.of());
    private static Map<Identifier, DamageEntryDefinition> frozenJavaEntries =
            Map.of();
    private static Map<Identifier, DamageAffixDefinition> frozenJavaAffixes =
            Map.of();
    private static boolean frozen;

    private DamageTemplateRegistry() {}

    public static synchronized void registerEntry(
            DamageNexusRegistrationAccess access,
            Identifier id,
            DamageEntryDefinition definition
    ) {
        DamageNexusLifecycle.requireRegistering(access, "registerEntryTemplate");
        requireMutable();
        Identifier safeId = Objects.requireNonNull(id, "id");
        DamageEntryDefinition safeDefinition =
                Objects.requireNonNull(definition, "definition");
        requireMatchingId(safeId, safeDefinition.id(), "entry");
        DamageTemplateDefinitionValidator.requireEntry(
                safeDefinition, "java_template/entry/" + safeId, false);
        if (JAVA_ENTRIES.putIfAbsent(safeId, safeDefinition) != null) {
            throw new IllegalArgumentException(
                    "Duplicate entry template ID: " + safeId);
        }
    }

    public static synchronized void registerAffix(
            DamageNexusRegistrationAccess access,
            Identifier id,
            DamageAffixDefinition definition
    ) {
        DamageNexusLifecycle.requireRegistering(access, "registerAffixTemplate");
        requireMutable();
        Identifier safeId = Objects.requireNonNull(id, "id");
        DamageAffixDefinition safeDefinition =
                Objects.requireNonNull(definition, "definition");
        requireMatchingId(safeId, safeDefinition.id(), "affix");
        DamageTemplateDefinitionValidator.requireAffix(
                safeDefinition, "java_template/affix/" + safeId, false);
        if (JAVA_AFFIXES.putIfAbsent(safeId, safeDefinition) != null) {
            throw new IllegalArgumentException(
                    "Duplicate affix template ID: " + safeId);
        }
    }

    public static synchronized void freeze(
            DamageNexusRegistrationAccess access
    ) {
        DamageNexusLifecycle.requireRegistering(access, "freezeTemplates");
        if (frozen) {
            throw new IllegalStateException("Template registry already frozen");
        }
        Map<Identifier, DamageEntryDefinition> candidateEntries =
                sortedCopy(JAVA_ENTRIES);
        Map<Identifier, DamageAffixDefinition> candidateAffixes =
                sortedCopy(JAVA_AFFIXES);
        DamageTemplateLimits.requireWithinLimits(
                candidateEntries, candidateAffixes);
        frozenJavaEntries = candidateEntries;
        frozenJavaAffixes = candidateAffixes;
        snapshot = unvalidatedSnapshot(
                snapshot.revision(), frozenJavaEntries, frozenJavaAffixes);
        frozen = true;
    }

    public static Optional<DamageEntryDefinition> entry(Identifier id) {
        return snapshot.entry(id);
    }

    public static Optional<DamageAffixDefinition> affix(Identifier id) {
        return snapshot.affix(id);
    }

    public static long revision() {
        return snapshot.revision();
    }

    public static DamageTemplateSnapshot snapshot() {
        return snapshot;
    }

    /**
     * Returns a transaction-pinnable view. New callers fail closed when the
     * current channel content revision differs from the validated revision.
     */
    public static DamageTemplateSnapshot executionSnapshot() {
        DamageTemplateSnapshot current = snapshot;
        return current.isCompatibleWith(DamageChannelRegistry.contentRevision())
                ? current
                : current.failClosed();
    }

    public static boolean serverExecutionReady() {
        return snapshot.isCompatibleWith(
                DamageChannelRegistry.contentRevision());
    }

    public static long validatedChannelRevision() {
        return snapshot.validatedChannelRevision();
    }

    static Map<Identifier, DamageEntryDefinition> javaEntries() {
        return frozenJavaEntries;
    }

    static Map<Identifier, DamageAffixDefinition> javaAffixes() {
        return frozenJavaAffixes;
    }

    static synchronized void publishDatapack(
            Map<Identifier, DamageEntryDefinition> datapackEntries,
            Map<Identifier, DamageAffixDefinition> datapackAffixes,
            long validatedChannelRevision
    ) {
        if (!frozen) {
            throw new IllegalStateException("Template registry is not frozen");
        }
        Map<Identifier, DamageEntryDefinition> combinedEntries =
                new TreeMap<>(java.util.Comparator.comparing(Identifier::toString));
        combinedEntries.putAll(frozenJavaEntries);
        for (var entry : datapackEntries.entrySet()) {
            if (combinedEntries.putIfAbsent(entry.getKey(), entry.getValue())
                    != null) {
                throw new IllegalArgumentException(
                        "Datapack entry template conflicts with Java template: "
                                + entry.getKey());
            }
        }
        Map<Identifier, DamageAffixDefinition> combinedAffixes =
                new TreeMap<>(java.util.Comparator.comparing(Identifier::toString));
        combinedAffixes.putAll(frozenJavaAffixes);
        for (var entry : datapackAffixes.entrySet()) {
            if (combinedAffixes.putIfAbsent(entry.getKey(), entry.getValue())
                    != null) {
                throw new IllegalArgumentException(
                        "Datapack affix template conflicts with Java template: "
                                + entry.getKey());
            }
        }
        DamageTemplateLimits.requireWithinLimits(
                combinedEntries, combinedAffixes);
        if (validatedChannelRevision
                != DamageChannelRegistry.contentRevision()) {
            throw new IllegalStateException(
                    "template_channel_revision_changed expected="
                            + validatedChannelRevision + " actual="
                            + DamageChannelRegistry.contentRevision());
        }
        snapshot = new DamageTemplateSnapshot(
                Math.addExact(snapshot.revision(), 1L),
                validatedChannelRevision,
                true,
                combinedEntries,
                combinedAffixes
        );
        DamageNexusItemSecurity.invalidateExecutionCache();
        DamageNexusLifecycleLog.templatesLoaded(
                combinedEntries.size(),
                combinedAffixes.size(),
                snapshot.revision(),
                validatedChannelRevision);
    }

    @SubscribeEvent
    public static synchronized void onServerStopping(ServerStoppingEvent event) {
        if (frozen) {
            snapshot = unvalidatedSnapshot(
                    Math.addExact(snapshot.revision(), 1L),
                    frozenJavaEntries,
                    frozenJavaAffixes
            );
            DamageNexusItemSecurity.invalidateExecutionCache();
        }
    }

    private static void requireMutable() {
        if (frozen) {
            throw new IllegalStateException("Template registry is frozen");
        }
    }

    private static void requireMatchingId(
            Identifier registryId,
            Identifier definitionId,
            String kind
    ) {
        if (!registryId.equals(definitionId)) {
            throw new IllegalArgumentException(
                    "Template ID mismatch for " + kind + ": registry="
                            + registryId + " definition=" + definitionId);
        }
    }

    private static <T> Map<Identifier, T> sortedCopy(Map<Identifier, T> input) {
        TreeMap<Identifier, T> sorted = new TreeMap<>(
                java.util.Comparator.comparing(Identifier::toString));
        sorted.putAll(input);
        return Map.copyOf(sorted);
    }

    @ApiStatus.Internal
    public static synchronized void resetForTesting() {
        JAVA_ENTRIES.clear();
        JAVA_AFFIXES.clear();
        frozenJavaEntries = Map.of();
        frozenJavaAffixes = Map.of();
        snapshot = unvalidatedSnapshot(0L, Map.of(), Map.of());
        frozen = false;
    }

    private static DamageTemplateSnapshot unvalidatedSnapshot(
            long revision,
            Map<Identifier, DamageEntryDefinition> entries,
            Map<Identifier, DamageAffixDefinition> affixes
    ) {
        return new DamageTemplateSnapshot(
                revision,
                -1L,
                false,
                entries,
                affixes
        );
    }
}
