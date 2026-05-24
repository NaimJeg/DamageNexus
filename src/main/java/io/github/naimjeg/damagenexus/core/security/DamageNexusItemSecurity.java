package io.github.naimjeg.damagenexus.core.security;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.item.DamageNexusItemEntries;
import io.github.naimjeg.damagenexus.api.item.template.DamageItemTemplateReferences;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleReferenceValidator;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleValidator;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixValidator;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryValidator;
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddTrueDamageOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.CancelDamageOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.OverrideFinalDamageOperation;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusDiagnosticState;
import io.github.naimjeg.damagenexus.diagnostics.logging.DiagnosticTextSanitizer;
import io.github.naimjeg.damagenexus.core.template.DamageTemplateDiagnostics;
import io.github.naimjeg.damagenexus.core.template.DamageTemplateRegistry;
import io.github.naimjeg.damagenexus.core.template.DamageTemplateSnapshot;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.registry.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Server authority boundary for executable ItemStack rule components.
 *
 * <p>No data component is treated as a trust marker. Trust comes from the
 * server-side creation path or the submitting player's real permission set,
 * and every execution path still passes through structural validation.</p>
 */
@EventBusSubscriber(modid = DamageNexus.MODID)
public final class DamageNexusItemSecurity {

    private static final int EXECUTION_CACHE_CAPACITY = 256;
    private static final CacheEntry[] EXECUTION_CACHE =
            new CacheEntry[EXECUTION_CACHE_CAPACITY];
    private static int nextCacheIndex;

    private DamageNexusItemSecurity() {
    }

    public static void sanitizeCreativeInbound(
            ServerPlayer player,
            ItemStack submittedStack
    ) {
        if (player == null) {
            return;
        }

        if (player.level().getServer() == null
                || !player.level().getServer().isSameThread()) {
            throw new IllegalStateException(
                    "Creative ItemStack security must run on the "
                            + "Minecraft server thread"
            );
        }

        if (submittedStack == null
                || submittedStack.isEmpty()
                || !hasExecutableComponents(submittedStack)) {
            return;
        }

        DamageNexusItemEntries submitted = readRaw(submittedStack);
        DamageItemTemplateReferences references = readReferences(submittedStack);
        boolean administrator = player.permissions().hasPermission(
                Permissions.COMMANDS_GAMEMASTER
        );
        InboundDecision decision = evaluateCreativeInbound(
                administrator,
                submitted,
                references
        );

        if (!stripRejectedComponents(submittedStack, decision)) {
            return;
        }

        DamageNexusItemEntries resolved = resolveDefinitions(
                submitted, references,
                DamageTemplateRegistry.executionSnapshot(),
                "creative_item_ingress", false);
        String reason = containsDangerousOperation(resolved)
                ? "dangerous_operation"
                : "executable_component";

        if (DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.ITEM_SECURITY,
                "creative_item_ingress",
                decision.name(),
                reason
        )) {
            DamageNexus.LOGGER.warn(
                    "[DamageNexus] Removed untrusted executable item components at creative-slot ingress. player={} decision={} reason={}",
                    DiagnosticTextSanitizer.sanitizeArguments(
                            player.getUUID().toString(),
                            decision,
                            reason
                    )
            );
        }
    }

    static InboundDecision sanitizeCreativeInbound(
            boolean administrator,
            ItemStack submittedStack
    ) {
        if (submittedStack == null
                || submittedStack.isEmpty()
                || !hasExecutableComponents(submittedStack)) {
            return InboundDecision.NO_EXECUTABLE_COMPONENTS;
        }

        InboundDecision decision = evaluateCreativeInbound(
                administrator,
                readRaw(submittedStack),
                readReferences(submittedStack)
        );
        stripRejectedComponents(submittedStack, decision);
        return decision;
    }

    public static InboundDecision evaluateCreativeInbound(
            boolean administrator,
            DamageNexusItemEntries submitted
    ) {
        return evaluateCreativeInbound(
                administrator,
                submitted,
                DamageItemTemplateReferences.EMPTY
        );
    }

    public static InboundDecision evaluateCreativeInbound(
            boolean administrator,
            DamageNexusItemEntries submitted,
            DamageItemTemplateReferences references
    ) {
        DamageItemTemplateReferences safeReferences = references == null
                ? DamageItemTemplateReferences.EMPTY
                : references;
        if ((submitted == null || submitted.isEmpty())
                && safeReferences.isEmpty()) {
            return InboundDecision.NO_EXECUTABLE_COMPONENTS;
        }

        if (!administrator) {
            return InboundDecision.STRIP_UNTRUSTED;
        }

        DamageNexusItemEntries safeSubmitted = submitted == null
                ? DamageNexusItemEntries.EMPTY
                : submitted;
        DamageNexusItemEntries resolved = resolveDefinitions(
                safeSubmitted,
                safeReferences,
                DamageTemplateRegistry.executionSnapshot(),
                "creative_item_ingress",
                false
        );
        ValidatedItemRules validated = validateExecutionDefinitions(
                resolved.entries(),
                resolved.affixes(),
                "creative_item_ingress",
                DamageChannelRegistry.contentRevision()
        );

        if (!validated.authoritative()) {
            return InboundDecision.STRIP_INVALID;
        }

        if (!shapeOf(validated.entries(), validated.affixes()).equals(
                shapeOf(resolved.entries(), resolved.affixes())
        )) {
            return InboundDecision.STRIP_INVALID;
        }

        return InboundDecision.ALLOW;
    }

    public static ValidatedItemRules validateDefinitions(
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes,
            String source
    ) {
        List<DamageEntryDefinition> safeEntries =
                entries == null ? List.of() : entries;
        List<DamageAffixDefinition> safeAffixes =
                affixes == null ? List.of() : affixes;
        Optional<String> problem = DamageRuleLimits.findItemProblem(
                safeEntries,
                safeAffixes
        );

        if (problem.isPresent()) {
            warnInvalidStructure(problem.get());
            return ValidatedItemRules.REJECTED;
        }

        List<DamageEntryDefinition> validEntries =
                DamageEntryValidator.filterValid(
                        safeEntries,
                        stableSource(source, "entries")
                );
        List<DamageAffixDefinition> validAffixes =
                DamageAffixValidator.filterValid(
                        safeAffixes,
                        stableSource(source, "affixes")
                );

        return new ValidatedItemRules(
                validEntries,
                validAffixes,
                true
        );
    }

    public static synchronized ValidatedItemRules validateForExecution(
            ItemStack stack,
            String source
    ) {
        return validateForExecution(
                stack, source, DamageTemplateRegistry.executionSnapshot());
    }

    public static synchronized ValidatedItemRules validateForExecution(
            ItemStack stack,
            String source,
            DamageTemplateSnapshot templateSnapshot
    ) {
        if (stack == null || stack.isEmpty()) {
            return ValidatedItemRules.EMPTY;
        }

        List<DamageEntryDefinition> entries = stack.getOrDefault(
                ModDataComponents.DAMAGE_ENTRIES.get(),
                List.of()
        );
        List<DamageAffixDefinition> affixes = stack.getOrDefault(
                ModDataComponents.DAMAGE_AFFIXES.get(),
                List.of()
        );
        DamageItemTemplateReferences references = stack.getOrDefault(
                ModDataComponents.DAMAGE_TEMPLATE_REFERENCES.get(),
                DamageItemTemplateReferences.EMPTY
        );
        DamageTemplateSnapshot safeSnapshot = Objects.requireNonNull(
                templateSnapshot, "templateSnapshot");
        long currentChannelRevision = DamageChannelRegistry.contentRevision();

        for (CacheEntry cached : EXECUTION_CACHE) {
            if (cached != null
                    && cached.matches(
                    stack, entries, affixes, references,
                    safeSnapshot.revision(),
                    safeSnapshot.validatedChannelRevision(),
                    safeSnapshot.serverAuthoritative(),
                    currentChannelRevision)) {
                return cached.validated();
            }
        }

        DamageNexusItemEntries resolved = resolveDefinitions(
                new DamageNexusItemEntries(entries, affixes),
                references,
                safeSnapshot,
                source,
                true
        );
        ValidatedItemRules validated = validateExecutionDefinitions(
                resolved.entries(),
                resolved.affixes(),
                source,
                currentChannelRevision
        );
        EXECUTION_CACHE[nextCacheIndex] = new CacheEntry(
                new WeakReference<>(stack),
                entries,
                affixes,
                references,
                safeSnapshot.revision(),
                safeSnapshot.validatedChannelRevision(),
                safeSnapshot.serverAuthoritative(),
                currentChannelRevision,
                validated
        );
        nextCacheIndex = (nextCacheIndex + 1)
                % EXECUTION_CACHE_CAPACITY;

        return validated;
    }

    static ValidatedItemRules validateExecutionDefinitions(
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes,
            String source,
            long currentChannelRevision
    ) {
        ValidatedItemRules structural = validateDefinitions(
                entries,
                affixes,
                source
        );
        if (!structural.authoritative()) {
            return ValidatedItemRules.REJECTED;
        }

        for (DamageEntryDefinition entry : structural.entries()) {
            if (!validateExecutionRules(
                    entry.rules(),
                    stableSource(source, "entry/" + entry.id()),
                    currentChannelRevision
            )) {
                return ValidatedItemRules.REJECTED;
            }
        }
        for (DamageAffixDefinition affix : structural.affixes()) {
            for (DamageEntryDefinition entry : affix.entries()) {
                if (!validateExecutionRules(
                        entry.rules(),
                        stableSource(
                                source,
                                "affix/" + affix.id() + "/entry/" + entry.id()
                        ),
                        currentChannelRevision
                )) {
                    return ValidatedItemRules.REJECTED;
                }
            }
        }
        return structural;
    }

    private static boolean validateExecutionRules(
            List<DamageRuleDefinition> rules,
            String source,
            long currentChannelRevision
    ) {
        for (DamageRuleDefinition rule : rules) {
            if (!DamageRuleReferenceValidator.validateDatapackReferences(
                    rule,
                    source + "/channel_revision/" + currentChannelRevision,
                    DamageRuleValidator.Policy.WARN
            )) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsDangerousOperation(
            DamageNexusItemEntries entries
    ) {
        if (entries == null) {
            return false;
        }

        for (DamageEntryDefinition entry : entries.entries()) {
            if (entry != null && containsDangerousOperation(entry.rules())) {
                return true;
            }
        }

        for (DamageAffixDefinition affix : entries.affixes()) {
            if (affix == null) {
                continue;
            }

            for (DamageEntryDefinition entry : affix.entries()) {
                if (entry != null
                        && containsDangerousOperation(entry.rules())) {
                    return true;
                }
            }
        }

        return false;
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        clearCaches();
    }

    static synchronized void clearCaches() {
        java.util.Arrays.fill(EXECUTION_CACHE, null);
        nextCacheIndex = 0;
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static synchronized void invalidateExecutionCache() {
        clearCaches();
    }

    private static boolean containsDangerousOperation(
            List<DamageRuleDefinition> rules
    ) {
        for (DamageRuleDefinition rule : rules) {
            if (rule == null) {
                continue;
            }

            for (DamageRuleOperation operation : rule.operations()) {
                if (operation instanceof CancelDamageOperation
                        || operation
                        instanceof OverrideFinalDamageOperation
                        || operation instanceof AddTrueDamageOperation) {
                    return true;
                }
            }
        }

        return false;
    }

    private static DefinitionShape shapeOf(
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes
    ) {
        int entryCount = entries == null ? 0 : entries.size();
        int affixCount = affixes == null ? 0 : affixes.size();
        int nestedEntryCount = 0;
        int ruleCount = 0;

        if (entries != null) {
            for (DamageEntryDefinition entry : entries) {
                if (entry != null) {
                    ruleCount += entry.rules().size();
                }
            }
        }

        if (affixes != null) {
            for (DamageAffixDefinition affix : affixes) {
                if (affix == null) {
                    continue;
                }

                nestedEntryCount += affix.entries().size();

                for (DamageEntryDefinition entry : affix.entries()) {
                    if (entry != null) {
                        ruleCount += entry.rules().size();
                    }
                }
            }
        }

        return new DefinitionShape(
                entryCount,
                affixCount,
                nestedEntryCount,
                ruleCount
        );
    }

    private static boolean hasExecutableComponents(ItemStack stack) {
        return stack.has(ModDataComponents.DAMAGE_ENTRIES.get())
                || stack.has(ModDataComponents.DAMAGE_AFFIXES.get())
                || stack.has(ModDataComponents.DAMAGE_TEMPLATE_REFERENCES.get());
    }

    private static boolean stripRejectedComponents(
            ItemStack stack,
            InboundDecision decision
    ) {
        if (decision == InboundDecision.ALLOW
                || decision
                == InboundDecision.NO_EXECUTABLE_COMPONENTS) {
            return false;
        }

        stack.remove(ModDataComponents.DAMAGE_ENTRIES.get());
        stack.remove(ModDataComponents.DAMAGE_AFFIXES.get());
        stack.remove(ModDataComponents.DAMAGE_TEMPLATE_REFERENCES.get());
        return true;
    }

    private static DamageNexusItemEntries readRaw(ItemStack stack) {
        return new DamageNexusItemEntries(
                stack.getOrDefault(
                        ModDataComponents.DAMAGE_ENTRIES.get(),
                        List.of()
                ),
                stack.getOrDefault(
                        ModDataComponents.DAMAGE_AFFIXES.get(),
                        List.of()
                )
        );
    }

    private static DamageItemTemplateReferences readReferences(
            ItemStack stack
    ) {
        return stack.getOrDefault(
                ModDataComponents.DAMAGE_TEMPLATE_REFERENCES.get(),
                DamageItemTemplateReferences.EMPTY
        );
    }

    private static DamageNexusItemEntries resolveDefinitions(
            DamageNexusItemEntries materialized,
            DamageItemTemplateReferences references,
            DamageTemplateSnapshot snapshot,
            String source,
            boolean diagnoseUnresolved
    ) {
        java.util.ArrayList<DamageEntryDefinition> entries =
                new java.util.ArrayList<>(materialized.entries());
        java.util.ArrayList<DamageAffixDefinition> affixes =
                new java.util.ArrayList<>(materialized.affixes());
        if (!references.isEmpty() && !snapshot.serverAuthoritative()) {
            if (diagnoseUnresolved) {
                DamageTemplateDiagnostics.incompatible(
                        snapshot.revision(),
                        snapshot.validatedChannelRevision(),
                        source);
            }
            return new DamageNexusItemEntries(entries, affixes);
        }
        for (var reference : references.entries()) {
            var resolved = snapshot.entry(reference.id());
            if (resolved.isPresent()) {
                entries.add(resolved.get());
            } else if (diagnoseUnresolved) {
                DamageTemplateDiagnostics.unresolved(
                        "entry", reference.id(), snapshot.revision(), source);
            }
        }
        for (var reference : references.affixes()) {
            var resolved = snapshot.affix(reference.id());
            if (resolved.isPresent()) {
                affixes.add(resolved.get());
            } else if (diagnoseUnresolved) {
                DamageTemplateDiagnostics.unresolved(
                        "affix", reference.id(), snapshot.revision(), source);
            }
        }
        return new DamageNexusItemEntries(entries, affixes);
    }

    private static String stableSource(
            String source,
            String category
    ) {
        if (source == null || source.isBlank()) {
            return "item_security/" + category;
        }

        int separator = source.indexOf('/');
        String stableCategory = separator < 0
                ? source
                : source.substring(0, separator);

        return "item_security/"
                + DiagnosticTextSanitizer.sanitizeLine(
                stableCategory,
                64
        )
                + "/"
                + category;
    }

    private static void warnInvalidStructure(String reason) {
        String stableReason = DiagnosticTextSanitizer.sanitizeLine(
                reason,
                256
        );
        if (DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.ITEM_SECURITY,
                "item_structure",
                "rejected",
                stableReason
        )) {
            DamageNexus.LOGGER.warn(
                    "[DamageNexus] Executable item rule graph rejected. category={} reason={}",
                    DiagnosticTextSanitizer.sanitizeArguments(
                            "item_structure",
                            stableReason
                    )
            );
        }
    }

    public enum InboundDecision {
        NO_EXECUTABLE_COMPONENTS,
        ALLOW,
        STRIP_UNTRUSTED,
        STRIP_INVALID
    }

    public record ValidatedItemRules(
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes,
            boolean authoritative
    ) {
        private static final ValidatedItemRules EMPTY =
                new ValidatedItemRules(List.of(), List.of(), true);
        private static final ValidatedItemRules REJECTED =
                new ValidatedItemRules(List.of(), List.of(), false);

        public ValidatedItemRules {
            entries = entries == null ? List.of() : List.copyOf(entries);
            affixes = affixes == null ? List.of() : List.copyOf(affixes);
        }

        public boolean isEmpty() {
            return entries.isEmpty() && affixes.isEmpty();
        }
    }

    private record CacheEntry(
            WeakReference<ItemStack> stack,
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes,
            DamageItemTemplateReferences references,
            long templateRevision,
            long validatedChannelRevision,
            boolean serverAuthoritative,
            long currentChannelRevision,
            ValidatedItemRules validated
    ) {
        boolean matches(
                ItemStack candidate,
                List<DamageEntryDefinition> candidateEntries,
                List<DamageAffixDefinition> candidateAffixes,
                DamageItemTemplateReferences candidateReferences,
                long candidateTemplateRevision,
                long candidateValidatedChannelRevision,
                boolean candidateServerAuthoritative,
                long candidateCurrentChannelRevision
        ) {
            return stack.get() == candidate
                    && entries == candidateEntries
                    && affixes == candidateAffixes
                    && references == candidateReferences
                    && templateRevision == candidateTemplateRevision
                    && validatedChannelRevision
                    == candidateValidatedChannelRevision
                    && serverAuthoritative == candidateServerAuthoritative
                    && currentChannelRevision
                    == candidateCurrentChannelRevision;
        }
    }

    private record DefinitionShape(
            int entryCount,
            int affixCount,
            int nestedEntryCount,
            int ruleCount
    ) {
    }

}
