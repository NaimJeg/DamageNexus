package io.github.naimjeg.damagenexus.core.rule;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.*;
import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusDiagnosticState;
import io.github.naimjeg.damagenexus.diagnostics.logging.DiagnosticTextSanitizer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.*;

public final class DamageRuleStackingResolver {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Identifier FALLBACK_RULE_ID =
            DamageNexusIds.id("invalid_rule");

    private static final Identifier FALLBACK_STACKING_GROUP =
            DamageNexusIds.id("invalid_stacking_group");

    private DamageRuleStackingResolver() {
    }

    public static DamageRuleStackingResult resolve(List<RuntimeDamageRule> input) {
        if (input == null || input.isEmpty()) {
            return new DamageRuleStackingResult(List.of(), List.of());
        }

        List<RuntimeDamageRule> sanitized = sanitizeInput(input);

        if (sanitized.isEmpty()) {
            return new DamageRuleStackingResult(List.of(), List.of());
        }

        if (sanitized.size() == 1) {
            return new DamageRuleStackingResult(sanitized, List.of());
        }

        if (allStack(sanitized)) {
            return new DamageRuleStackingResult(sanitized, List.of());
        }

        List<RuntimeDamageRule> stacked = new ArrayList<>(sanitized.size());
        List<StackingTrace> traces = null;

        Map<StackingKey, RuntimeDamageRule> uniqueSource = null;
        Map<StackingKey, RuntimeDamageRule> highestValue = null;
        Map<StackingKey, RuntimeDamageRule> lowestValue = null;
        Map<StackingKey, RuntimeDamageRule> replace = null;

        for (RuntimeDamageRule runtimeRule : sanitized) {
            DamageRuleDefinition rule = runtimeRule.definition();

            DamageRuleStacking stacking = safeStacking(rule);

            switch (stacking) {
                case STACK -> stacked.add(runtimeRule);

                case UNIQUE_SOURCE -> {
                    if (uniqueSource == null) {
                        uniqueSource = new LinkedHashMap<>();
                    }

                    traces = mergeWithTrace(
                            uniqueSource,
                            keyOf(runtimeRule, true),
                            runtimeRule,
                            traces,
                            DamageRuleStacking.UNIQUE_SOURCE,
                            DamageRuleStackingResolver::chooseHigherPriority
                    );
                }

                case HIGHEST_VALUE -> {
                    if (highestValue == null) {
                        highestValue = new LinkedHashMap<>();
                    }

                    traces = mergeWithTrace(
                            highestValue,
                            keyOf(runtimeRule, false),
                            runtimeRule,
                            traces,
                            DamageRuleStacking.HIGHEST_VALUE,
                            DamageRuleStackingResolver::chooseHigherValue
                    );
                }

                case LOWEST_VALUE -> {
                    if (lowestValue == null) {
                        lowestValue = new LinkedHashMap<>();
                    }

                    traces = mergeWithTrace(
                            lowestValue,
                            keyOf(runtimeRule, false),
                            runtimeRule,
                            traces,
                            DamageRuleStacking.LOWEST_VALUE,
                            DamageRuleStackingResolver::chooseLowestValue
                    );
                }

                case REPLACE -> {
                    if (replace == null) {
                        replace = new LinkedHashMap<>();
                    }

                    traces = mergeWithTrace(
                            replace,
                            keyOf(runtimeRule, false),
                            runtimeRule,
                            traces,
                            DamageRuleStacking.REPLACE,
                            DamageRuleStackingResolver::chooseReplacement
                    );
                }
            }
        }

        if (uniqueSource != null) {
            stacked.addAll(uniqueSource.values());
        }

        if (highestValue != null) {
            stacked.addAll(highestValue.values());
        }

        if (lowestValue != null) {
            stacked.addAll(lowestValue.values());
        }

        if (replace != null) {
            stacked.addAll(replace.values());
        }

        return new DamageRuleStackingResult(
                List.copyOf(stacked),
                traces != null ? List.copyOf(traces) : List.of()
        );
    }

    private static List<RuntimeDamageRule> sanitizeInput(
            List<RuntimeDamageRule> input
    ) {
        List<RuntimeDamageRule> result = new ArrayList<>(input.size());

        for (RuntimeDamageRule runtimeRule : input) {
            if (runtimeRule == null) {
                handleFailure(
                        "sanitize/null_runtime_rule",
                        null,
                        new NullPointerException("runtime rule is null")
                );
                continue;
            }

            DamageRuleDefinition rule = runtimeRule.definition();

            if (rule == null) {
                handleFailure(
                        "sanitize/null_definition",
                        null,
                        new NullPointerException("rule definition is null")
                );
                continue;
            }

            if (rule.id() == null) {
                handleFailure(
                        "sanitize/null_id",
                        rule,
                        new NullPointerException("rule id is null")
                );
                continue;
            }

            if (rule.phase() == null) {
                handleFailure(
                        "sanitize/null_phase",
                        rule,
                        new NullPointerException("rule phase is null")
                );
                continue;
            }

            if (rule.role() == null) {
                handleFailure(
                        "sanitize/null_role",
                        rule,
                        new NullPointerException("rule role is null")
                );
                continue;
            }

            if (rule.stacking() == null) {
                handleFailure(
                        "sanitize/null_stacking",
                        rule,
                        new NullPointerException("rule stacking policy is null")
                );
                continue;
            }

            if (rule.stacking() != DamageRuleStacking.STACK
                    && rule.stackingKey() == null) {
                handleFailure(
                        "sanitize/null_stacking_key",
                        rule,
                        new NullPointerException("rule stacking key is null")
                );
                continue;
            }

            result.add(runtimeRule);
        }

        return result;
    }

    private static boolean allStack(List<RuntimeDamageRule> input) {
        for (RuntimeDamageRule runtimeRule : input) {
            if (safeStacking(runtimeRule.definition()) != DamageRuleStacking.STACK) {
                return false;
            }
        }

        return true;
    }

    private static List<StackingTrace> mergeWithTrace(
            Map<StackingKey, RuntimeDamageRule> map,
            StackingKey key,
            RuntimeDamageRule candidate,
            List<StackingTrace> traces,
            DamageRuleStacking policy,
            RuleChooser chooser
    ) {
        RuntimeDamageRule existing = map.get(key);

        if (existing == null) {
            map.put(key, candidate);
            return traces;
        }

        RuntimeDamageRule chosen;

        try {
            chosen = chooser.choose(existing, candidate);
        } catch (Exception exception) {
            handleFailure(
                    "merge/choose/" + policy,
                    candidate.definition(),
                    exception
            );

            /*
             * Tolerant mode:
             * Keep the existing rule if candidate comparison fails.
             */
            chosen = existing;
        }

        RuntimeDamageRule dropped = chosen == existing ? candidate : existing;

        map.put(key, chosen);

        if (traces == null) {
            traces = new ArrayList<>(2);
        }

        DamageRuleDefinition chosenDefinition = chosen.definition();
        DamageRuleDefinition droppedDefinition = dropped.definition();

        traces.add(new StackingTrace(
                safePhase(candidate.definition()),
                safeId(chosenDefinition),
                safeId(droppedDefinition),
                policy,
                safeStackingValue(chosenDefinition),
                safeStackingValue(droppedDefinition)
        ));

        return traces;
    }

    private static RuntimeDamageRule chooseHigherPriority(
            RuntimeDamageRule existing,
            RuntimeDamageRule candidate
    ) {
        int existingPriority = safePriority(existing.definition());
        int candidatePriority = safePriority(candidate.definition());

        if (candidatePriority > existingPriority) {
            return candidate;
        }

        return existing;
    }

    private static RuntimeDamageRule chooseReplacement(
            RuntimeDamageRule existing,
            RuntimeDamageRule candidate
    ) {
        int existingPriority = safePriority(existing.definition());
        int candidatePriority = safePriority(candidate.definition());

        if (candidatePriority >= existingPriority) {
            return candidate;
        }

        return existing;
    }

    private static RuntimeDamageRule chooseHigherValue(
            RuntimeDamageRule existing,
            RuntimeDamageRule candidate
    ) {
        float existingValue = safeStackingValue(existing.definition());
        float candidateValue = safeStackingValue(candidate.definition());

        int valueCompare = Float.compare(candidateValue, existingValue);

        if (valueCompare > 0) {
            return candidate;
        }

        if (valueCompare == 0
                && safePriority(candidate.definition()) > safePriority(existing.definition())) {
            return candidate;
        }

        return existing;
    }

    private static RuntimeDamageRule chooseLowestValue(
            RuntimeDamageRule existing,
            RuntimeDamageRule candidate
    ) {
        float existingValue = safeStackingValue(existing.definition());
        float candidateValue = safeStackingValue(candidate.definition());

        int valueCompare = Float.compare(candidateValue, existingValue);

        if (valueCompare < 0) {
            return candidate;
        }

        if (valueCompare == 0
                && safePriority(candidate.definition()) > safePriority(existing.definition())) {
            return candidate;
        }

        return existing;
    }

    private static StackingKey keyOf(
            RuntimeDamageRule runtimeRule,
            boolean includeSource
    ) {
        DamageRuleDefinition rule = runtimeRule.definition();

        return new StackingKey(
                safeStackingKey(rule),
                safePhase(rule),
                safeRole(rule),
                includeSource
                        ? RuleSourceSignature.from(runtimeRule.executionContext())
                        : RuleSourceSignature.GROUP_ONLY
        );
    }

    private static float safeStackingValue(DamageRuleDefinition rule) {
        if (rule == null || rule.operations() == null || rule.operations().isEmpty()) {
            return 0.0f;
        }

        float result = 0.0f;
        boolean hasValue = false;

        for (DamageRuleOperation operation : rule.operations()) {
            if (operation == null) {
                continue;
            }

            float value;

            try {
                value = operation.stackingValue();
            } catch (Exception exception) {
                handleFailure(
                        "stackingValue/" + safeOperationType(operation),
                        rule,
                        exception
                );
                continue;
            }

            if (!Float.isFinite(value)) {
                handleFailure(
                        "stackingValue/non_finite/" + safeOperationType(operation),
                        rule,
                        new IllegalStateException("non-finite stacking value: " + value)
                );
                continue;
            }

            if (!hasValue || value > result) {
                result = value;
                hasValue = true;
            }
        }

        return hasValue ? result : 0.0f;
    }

    private static int safePriority(DamageRuleDefinition rule) {
        return rule == null ? 0 : rule.priority();
    }

    private static Identifier safeId(DamageRuleDefinition rule) {
        if (rule == null || rule.id() == null) {
            return FALLBACK_RULE_ID;
        }

        return rule.id();
    }

    private static Identifier safeStackingKey(DamageRuleDefinition rule) {
        if (rule == null) {
            return FALLBACK_STACKING_GROUP;
        }

        try {
            Identifier key = rule.stackingKey();
            return key != null ? key : FALLBACK_STACKING_GROUP;
        } catch (Exception exception) {
            handleFailure(
                    "stackingKey",
                    rule,
                    exception
            );

            return FALLBACK_STACKING_GROUP;
        }
    }

    private static DamagePhase safePhase(DamageRuleDefinition rule) {
        if (rule == null || rule.phase() == null) {
            return DamagePhase.BASE_MODIFICATION;
        }

        return rule.phase();
    }

    private static DamageRuleRole safeRole(DamageRuleDefinition rule) {
        if (rule == null || rule.role() == null) {
            return DamageRuleRole.ANY;
        }

        return rule.role();
    }

    private static DamageRuleStacking safeStacking(DamageRuleDefinition rule) {
        if (rule == null || rule.stacking() == null) {
            return DamageRuleStacking.STACK;
        }

        return rule.stacking();
    }

    private static String safeOperationType(DamageRuleOperation operation) {
        try {
            return operation.type().toString();
        } catch (Exception ignored) {
            return "<unknown_operation>";
        }
    }

    private static void handleFailure(
            String stage,
            DamageRuleDefinition rule,
            Exception exception
    ) {
        if (DamageNexusSettings.strictRuleErrors()) {
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new IllegalStateException(
                    "[DamageNexus] Rule stacking failure at "
                            + stage
                            + ": rule="
                            + safeId(rule),
                    exception
            );
        }

        if (DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.STACKING,
                safeId(rule).toString(),
                stage,
                exception.getClass().getName()
        )) {
            LOGGER.error(
                    "[DamageNexus] Rule stacking failure ignored. stage={} rule={} reason={}: {}",
                    DiagnosticTextSanitizer.sanitizeLine(stage),
                    safeId(rule),
                    exception.getClass().getSimpleName(),
                    DiagnosticTextSanitizer.sanitizeLine(
                            exception.getMessage()
                    ),
                    exception
            );
        }
    }

    @FunctionalInterface
    private interface RuleChooser {
        RuntimeDamageRule choose(RuntimeDamageRule existing, RuntimeDamageRule candidate);
    }

    private record RuleSourceSignature(
            DamageRuleProviderType providerType,
            RuleSourceLocation sourceLocation,
            DamageRuleRole runtimeRole,
            Identifier itemId,
            String equipmentSlot,
            int ownerId,
            int sourceEntityId
    ) {
        private static final Identifier EMPTY_ITEM_ID =
                DamageNexusIds.id("empty_item");

        private static final RuleSourceSignature GROUP_ONLY =
                new RuleSourceSignature(
                        null,
                        null,
                        null,
                        null,
                        null,
                        Integer.MIN_VALUE,
                        Integer.MIN_VALUE
                );

        private static RuleSourceSignature from(
                RuleExecutionContext executionContext
        ) {
            if (executionContext == null) {
                return GROUP_ONLY;
            }

            return new RuleSourceSignature(
                    executionContext.providerType(),
                    executionContext.sourceLocation(),
                    executionContext.role(),
                    itemId(executionContext.sourceStack()),
                    executionContext.equipmentSlot() == null
                            ? null
                            : executionContext.equipmentSlot().name(),
                    executionContext.owner() == null
                            ? Integer.MIN_VALUE
                            : executionContext.owner().getId(),
                    executionContext.sourceEntity() == null
                            ? Integer.MIN_VALUE
                            : executionContext.sourceEntity().getId()
            );
        }

        private static Identifier itemId(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return EMPTY_ITEM_ID;
            }

            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());

            return id != null ? id : EMPTY_ITEM_ID;
        }
    }

    private record StackingKey(
            Identifier group,
            DamagePhase phase,
            DamageRuleRole role,
            RuleSourceSignature source
    ) {
    }
}
