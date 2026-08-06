package io.github.naimjeg.damagenexus.api.rule;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AttackerHealthAboveCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AttackerHealthBelowCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.TargetHealthAboveCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.TargetHealthBelowCondition;
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddChannelPostMultiplierOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddChannelPreMultiplierOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddGlobalPostMultiplierOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddGlobalPreMultiplierOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.ConvertDamageOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.GainExtraDamageOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.MultiplyArmorEffectivenessOperation;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Central structural budgets for all authored, stored, synchronized and
 * Java-created executable item rules.
 */
public final class DamageRuleLimits {

    public static final int MAX_ITEM_ENTRIES = 32;
    public static final int MAX_ITEM_AFFIXES = 16;
    public static final int MAX_AFFIX_ENTRIES = 16;
    public static final int MAX_ENTRY_RULES = 32;
    public static final int MAX_RULE_CONDITIONS = 32;
    public static final int MAX_RULE_OPERATIONS = 32;
    public static final int MAX_CONDITION_DEPTH = 16;
    public static final int MAX_CONDITION_NODES = 256;
    public static final int MAX_EXPANDED_ITEM_RULES = 128;
    public static final int MAX_TOOLTIP_LINES = 32;
    public static final int MAX_TRANSLATION_ARGS = 16;
    public static final int MAX_DISPLAY_CODE_POINTS = 256;
    public static final int MAX_TRACE_LABEL_CODE_POINTS = 128;
    public static final int MAX_RAW_CODEC_DEPTH = 64;
    public static final int MAX_RAW_CODEC_NODES = 4_096;
    public static final int MAX_COMPONENT_RAW_NODES = 16_384;
    public static final int MAX_COMPONENT_RULES = 128;
    public static final int MAX_COMPONENT_CONDITION_NODES = 4_096;
    public static final int MAX_COMPONENT_OPERATIONS = 2_048;
    public static final int MAX_COMPONENT_DISPLAY_CODE_POINTS = 16_384;
    public static final int MAX_REFERENCED_CHANNELS_PER_NODE = 32;
    public static final float MAX_ABSOLUTE_DAMAGE_VALUE = 1_000_000.0f;
    public static final float MAX_ABSOLUTE_MULTIPLIER = 100.0f;

    private DamageRuleLimits() {
    }

    public static <T> Codec<List<T>> boundedList(
            Codec<T> elementCodec,
            int maximum,
            String name
    ) {
        return elementCodec
                .listOf(0, maximum)
                .xmap(List::copyOf, List::copyOf)
                .validate(values -> values.size() <= maximum
                        ? DataResult.success(values)
                        : DataResult.error(() ->
                        name + " exceeds maximum " + maximum));
    }

    public static Codec<String> boundedString(
            int maximumCodePoints,
            String name
    ) {
        return Codec.STRING.validate(value -> {
            int length = value.codePointCount(0, value.length());

            if (length <= maximumCodePoints) {
                return DataResult.success(value);
            }

            return DataResult.error(() ->
                    name + " exceeds " + maximumCodePoints
                            + " Unicode code points"
            );
        });
    }

    /**
     * Performs an iterative generic map/list preflight before a recursive
     * rule codec sees attacker-controlled JSON or NBT.
     */
    public static <A> Codec<A> guardRawStructure(
            Codec<A> codec,
            String name
    ) {
        return guardRawStructure(
                codec,
                name,
                MAX_RAW_CODEC_DEPTH,
                MAX_RAW_CODEC_NODES
        );
    }

    public static <A> Codec<A> guardRawStructure(
            Codec<A> codec,
            String name,
            int maximumDepth,
            int maximumNodes
    ) {
        return Codec.of(codec, new Decoder<>() {
            @Override
            public <T> DataResult<Pair<A, T>> decode(
                    DynamicOps<T> ops,
                    T input
            ) {
                Optional<String> problem = rawProblem(
                        ops,
                        input,
                        maximumDepth,
                        maximumNodes,
                        RawTraversalObserver.NONE
                );

                if (problem.isPresent()) {
                    return DataResult.error(() ->
                            name + " rejected before decode: "
                                    + problem.get()
                    );
                }

                return codec.decode(ops, input);
            }
        });
    }

    public static DataResult<DamageRuleDefinition> validateRuleCodec(
            DamageRuleDefinition rule
    ) {
        Optional<String> problem = findRuleProblem(rule);

        return problem.<DataResult<DamageRuleDefinition>>map(message ->
                        DataResult.error(() ->
                                "Invalid DamageNexus rule structure. rule="
                                        + safeRuleId(rule)
                                        + " reason="
                                        + message
                        ))
                .orElseGet(() -> DataResult.success(rule));
    }

    public static Optional<String> findRuleProblem(
            DamageRuleDefinition rule
    ) {
        if (rule == null) {
            return Optional.of("rule_is_null");
        }

        if (rule.conditions() == null) {
            return Optional.of("conditions_are_null");
        }

        if (rule.conditions().size() > MAX_RULE_CONDITIONS) {
            return Optional.of(
                    "condition_count=" + rule.conditions().size()
                            + " maximum=" + MAX_RULE_CONDITIONS
            );
        }

        if (rule.operations() == null) {
            return Optional.of("operations_are_null");
        }

        if (rule.operations().size() > MAX_RULE_OPERATIONS) {
            return Optional.of(
                    "operation_count=" + rule.operations().size()
                            + " maximum=" + MAX_RULE_OPERATIONS
            );
        }

        if (rule.traceLabel() != null && rule.traceLabel().isPresent()
                && codePointLength(rule.traceLabel().get())
                > MAX_TRACE_LABEL_CODE_POINTS) {
            return Optional.of(
                    "trace_label_too_long maximum="
                            + MAX_TRACE_LABEL_CODE_POINTS
            );
        }

        Optional<String> conditionProblem =
                findConditionGraphProblem(rule.conditions());

        if (conditionProblem.isPresent()) {
            return conditionProblem;
        }

        for (DamageRuleOperation operation : rule.operations()) {
            if (operation == null) {
                return Optional.of("null_operation");
            }

            Optional<String> referenceProblem =
                    operationReferenceProblem(operation);

            if (referenceProblem.isPresent()) {
                return referenceProblem;
            }

            float value;

            try {
                value = operation.stackingValue();
            } catch (Exception exception) {
                return Optional.of(
                        "operation_stacking_value_failed type="
                                + safeOperationType(operation)
                                + " exception="
                                + exception.getClass().getSimpleName()
                );
            }

            if (!Float.isFinite(value)) {
                return Optional.of(
                        "non_finite_operation_value type="
                                + safeOperationType(operation)
                );
            }

            float maximum = isMultiplierLike(operation)
                    ? MAX_ABSOLUTE_MULTIPLIER
                    : MAX_ABSOLUTE_DAMAGE_VALUE;

            if (Math.abs(value) > maximum) {
                return Optional.of(
                        "operation_value_out_of_range type="
                                + safeOperationType(operation)
                                + " value="
                                + value
                                + " maximum_absolute="
                                + maximum
                );
            }
        }

        return Optional.empty();
    }

    public static Optional<String> findItemProblem(
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes
    ) {
        List<DamageEntryDefinition> safeEntries =
                entries == null ? List.of() : entries;
        List<DamageAffixDefinition> safeAffixes =
                affixes == null ? List.of() : affixes;

        if (safeEntries.size() > MAX_ITEM_ENTRIES) {
            return Optional.of(
                    "item_entry_count=" + safeEntries.size()
                            + " maximum=" + MAX_ITEM_ENTRIES
            );
        }

        if (safeAffixes.size() > MAX_ITEM_AFFIXES) {
            return Optional.of(
                    "item_affix_count=" + safeAffixes.size()
                            + " maximum=" + MAX_ITEM_AFFIXES
            );
        }

        int expandedRules = 0;

        for (DamageEntryDefinition entry : safeEntries) {
            Optional<String> problem = findEntryProblem(entry);

            if (problem.isPresent()) {
                return problem;
            }

            expandedRules += entry.rules().size();

            if (expandedRules > MAX_EXPANDED_ITEM_RULES) {
                return Optional.of(
                        "expanded_rule_count=" + expandedRules
                                + " maximum=" + MAX_EXPANDED_ITEM_RULES
                );
            }
        }

        for (DamageAffixDefinition affix : safeAffixes) {
            if (affix == null) {
                return Optional.of("null_affix");
            }

            if (affix.entries().size() > MAX_AFFIX_ENTRIES) {
                return Optional.of(
                        "affix=" + affix.id()
                                + " entry_count="
                                + affix.entries().size()
                                + " maximum="
                                + MAX_AFFIX_ENTRIES
                );
            }

            Optional<String> displayProblem =
                    findDisplayProblem(
                            affix.display().name(),
                            affix.display().authoredSummary(),
                            affix.display().flavorText()
                    );

            if (displayProblem.isPresent()) {
                return Optional.of(
                        "affix=" + affix.id() + " " + displayProblem.get()
                );
            }

            for (DamageEntryDefinition entry : affix.entries()) {
                Optional<String> problem = findEntryProblem(entry);

                if (problem.isPresent()) {
                    return problem;
                }

                expandedRules += entry.rules().size();

                if (expandedRules > MAX_EXPANDED_ITEM_RULES) {
                    return Optional.of(
                            "expanded_rule_count=" + expandedRules
                                    + " maximum="
                                    + MAX_EXPANDED_ITEM_RULES
                    );
                }
            }
        }

        return findAggregateBudgetProblem(
                safeEntries,
                safeAffixes
        );
    }

    public static DataResult<List<DamageEntryDefinition>>
    validateEntryComponentCodec(List<DamageEntryDefinition> entries) {
        Optional<String> problem = findEntryComponentProblem(entries);

        return problem
                .<DataResult<List<DamageEntryDefinition>>>map(reason ->
                        DataResult.error(() ->
                                "Invalid DamageNexus entry component: "
                                        + reason
                        ))
                .orElseGet(() -> DataResult.success(List.copyOf(entries)));
    }

    public static DataResult<List<DamageAffixDefinition>>
    validateAffixComponentCodec(List<DamageAffixDefinition> affixes) {
        Optional<String> problem = findAffixComponentProblem(affixes);

        return problem
                .<DataResult<List<DamageAffixDefinition>>>map(reason ->
                        DataResult.error(() ->
                                "Invalid DamageNexus affix component: "
                                        + reason
                        ))
                .orElseGet(() -> DataResult.success(List.copyOf(affixes)));
    }

    public static Optional<String> findEntryComponentProblem(
            List<DamageEntryDefinition> entries
    ) {
        return findComponentProblem(
                entries == null ? List.of() : entries,
                List.of()
        );
    }

    public static Optional<String> findAffixComponentProblem(
            List<DamageAffixDefinition> affixes
    ) {
        return findComponentProblem(
                List.of(),
                affixes == null ? List.of() : affixes
        );
    }

    public static Optional<RuleCost> measureRuleCost(
            DamageRuleDefinition rule
    ) {
        if (rule == null
                || rule.conditions() == null
                || rule.operations() == null) {
            return Optional.empty();
        }

        if (findRuleProblem(rule).isPresent()) {
            return Optional.empty();
        }

        Optional<Integer> conditionNodes =
                countConditionNodes(rule.conditions());
        return conditionNodes.map(nodes -> new RuleCost(
                nodes,
                rule.operations().size()
        ));
    }

    public static Optional<String> findEntryProblem(
            DamageEntryDefinition entry
    ) {
        if (entry == null) {
            return Optional.of("null_entry");
        }

        if (entry.rules().size() > MAX_ENTRY_RULES) {
            return Optional.of(
                    "entry=" + entry.id()
                            + " rule_count="
                            + entry.rules().size()
                            + " maximum="
                            + MAX_ENTRY_RULES
            );
        }

        Optional<String> displayProblem =
                findDisplayProblem(
                        entry.display().name(),
                        entry.display().authoredSummary(),
                        entry.display().flavorText()
                );

        if (displayProblem.isPresent()) {
            return Optional.of(
                    "entry=" + entry.id() + " " + displayProblem.get()
            );
        }

        for (DamageRuleDefinition rule : entry.rules()) {
            Optional<String> ruleProblem = findRuleProblem(rule);

            if (ruleProblem.isPresent()) {
                return Optional.of(
                        "entry=" + entry.id()
                                + " rule="
                                + safeRuleId(rule)
                                + " "
                                + ruleProblem.get()
                );
            }
        }

        return Optional.empty();
    }

    public static Optional<String> findDisplayTextProblem(
            DisplayText text
    ) {
        if (text == null) {
            return Optional.of("display_text_is_null");
        }

        return DisplayText.validationProblem(text);
    }

    private static Optional<String> findDisplayProblem(
            Optional<DisplayText> name,
            List<DisplayText> tooltip,
            Optional<DisplayText> flavor
    ) {
        if (tooltip == null) {
            return Optional.of("tooltip_is_null");
        }

        if (tooltip.size() > MAX_TOOLTIP_LINES) {
            return Optional.of(
                    "tooltip_line_count=" + tooltip.size()
                            + " maximum=" + MAX_TOOLTIP_LINES
            );
        }

        if (name == null) {
            return Optional.of("display_name_optional_is_null");
        }
        if (name.isPresent()) {
            Optional<String> nameProblem = findDisplayTextProblem(name.get());
            if (nameProblem.isPresent()) {
                return nameProblem;
            }
        }

        for (DisplayText line : tooltip) {
            Optional<String> lineProblem = findDisplayTextProblem(line);

            if (lineProblem.isPresent()) {
                return lineProblem;
            }
        }

        if (flavor != null && flavor.isPresent()) {
            return findDisplayTextProblem(flavor.get());
        }

        return Optional.empty();
    }

    private static Optional<String> findComponentProblem(
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes
    ) {
        return findItemProblem(entries, affixes);
    }

    private static Optional<String> findAggregateBudgetProblem(
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes
    ) {
        ComponentBudget budget = new ComponentBudget();

        for (DamageEntryDefinition entry : entries) {
            Optional<String> problem = budget.addEntry(entry);

            if (problem.isPresent()) {
                return problem;
            }
        }

        for (DamageAffixDefinition affix : affixes) {
            Optional<String> displayProblem =
                    budget.addDisplay(affix.display());

            if (displayProblem.isPresent()) {
                return displayProblem;
            }

            for (DamageEntryDefinition entry : affix.entries()) {
                Optional<String> problem = budget.addEntry(entry);

                if (problem.isPresent()) {
                    return problem;
                }
            }
        }

        return Optional.empty();
    }

    private static Optional<Integer> countConditionNodes(
            List<DamageRuleCondition> roots
    ) {
        Deque<DamageRuleCondition> pending = new ArrayDeque<>();

        for (int index = roots.size() - 1; index >= 0; index--) {
            pending.push(roots.get(index));
        }

        int count = 0;

        while (!pending.isEmpty()) {
            DamageRuleCondition condition = pending.pop();
            count++;

            List<DamageRuleCondition> children;
            try {
                children = children(condition);
            } catch (RuntimeException exception) {
                return Optional.empty();
            }
            if (children == null
                    || children.size() > MAX_RULE_CONDITIONS
                    || children.stream().anyMatch(java.util.Objects::isNull)) {
                return Optional.empty();
            }

            for (int index = children.size() - 1; index >= 0; index--) {
                pending.push(children.get(index));
            }
        }

        return Optional.of(count);
    }

    private static long displayCodePoints(DisplayText text) {
        long total = 0L;

        if (text == null) {
            return total;
        }

        switch (text) {
            case DisplayText.Literal literal ->
                    total += codePointLength(literal.text());
            case DisplayText.Translatable translatable -> {
                total += codePointLength(translatable.key());
                if (translatable.fallback().isPresent()) {
                    total += codePointLength(translatable.fallback().get());
                }
                for (String argument : translatable.args()) {
                    total += codePointLength(argument);
                }
            }
        }

        return total;
    }

    private static long displayCodePoints(
            Optional<DisplayText> name,
            List<DisplayText> tooltip,
            Optional<DisplayText> flavor
    ) {
        long total = name.map(DamageRuleLimits::displayCodePoints).orElse(0L);

        for (DisplayText line : tooltip) {
            total += displayCodePoints(line);
        }

        if (flavor.isPresent()) {
            total += displayCodePoints(flavor.get());
        }

        return total;
    }

    private static Optional<String> findConditionGraphProblem(
            List<DamageRuleCondition> roots
    ) {
        IdentityHashMap<DamageRuleCondition, VisitState> states =
                new IdentityHashMap<>();
        Deque<ConditionFrame> stack = new ArrayDeque<>();
        int nodeCount = 0;

        for (int index = roots.size() - 1; index >= 0; index--) {
            stack.push(new ConditionFrame(roots.get(index), 1, false));
        }

        while (!stack.isEmpty()) {
            ConditionFrame frame = stack.pop();
            DamageRuleCondition condition = frame.condition();

            if (condition == null) {
                return Optional.of("null_condition");
            }

            if (frame.exiting()) {
                states.remove(condition);
                continue;
            }

            VisitState state = states.get(condition);

            if (state == VisitState.VISITING) {
                return Optional.of(
                        "condition_cycle type=" + safeConditionType(condition)
                );
            }

            if (frame.depth() > MAX_CONDITION_DEPTH) {
                return Optional.of(
                        "condition_depth=" + frame.depth()
                                + " maximum=" + MAX_CONDITION_DEPTH
                );
            }

            nodeCount++;

            if (nodeCount > MAX_CONDITION_NODES) {
                return Optional.of(
                        "condition_nodes=" + nodeCount
                                + " maximum=" + MAX_CONDITION_NODES
                );
            }

            if (condition instanceof ChannelReferencingCondition references) {
                List<net.minecraft.resources.Identifier> channels;

                try {
                    channels = references.referencedChannels();
                } catch (Exception exception) {
                    return Optional.of(
                            "condition_channel_reference_failed type="
                                    + safeConditionType(condition)
                    );
                }

                if (channels == null) {
                    return Optional.of(
                            "condition_channels_are_null type="
                                    + safeConditionType(condition)
                    );
                }

                if (channels.size() > MAX_REFERENCED_CHANNELS_PER_NODE) {
                    return Optional.of(
                            "condition_channel_count=" + channels.size()
                                    + " maximum="
                                    + MAX_REFERENCED_CHANNELS_PER_NODE
                    );
                }

                if (channels.stream().anyMatch(java.util.Objects::isNull)) {
                    return Optional.of(
                            "condition_has_null_channel type="
                                    + safeConditionType(condition)
                    );
                }
            }

            Optional<String> numericProblem =
                    conditionNumericProblem(condition);

            if (numericProblem.isPresent()) {
                return numericProblem;
            }

            List<DamageRuleCondition> children;
            try {
                children = children(condition);
            } catch (RuntimeException exception) {
                return Optional.of(
                        "condition_children_failed type="
                                + safeConditionType(condition)
                );
            }

            if (children == null) {
                return Optional.of(
                        "condition_children_are_null type="
                                + safeConditionType(condition)
                );
            }

            if (children.size() > MAX_RULE_CONDITIONS) {
                return Optional.of(
                        "condition_child_count=" + children.size()
                                + " maximum=" + MAX_RULE_CONDITIONS
                );
            }

            if (children.stream().anyMatch(java.util.Objects::isNull)) {
                return Optional.of(
                        "condition_has_null_child type="
                                + safeConditionType(condition)
                );
            }

            states.put(condition, VisitState.VISITING);
            stack.push(new ConditionFrame(
                    condition,
                    frame.depth(),
                    true
            ));

            for (int index = children.size() - 1; index >= 0; index--) {
                stack.push(new ConditionFrame(
                        children.get(index),
                        frame.depth() + 1,
                        false
                ));
            }
        }

        return Optional.empty();
    }

    private static List<DamageRuleCondition> children(
            DamageRuleCondition condition
    ) {
        if (condition instanceof CompositeDamageRuleCondition composite) {
            return composite.childConditions();
        }

        return List.of();
    }

    private static boolean isMultiplierLike(DamageRuleOperation operation) {
        return operation instanceof AddChannelPreMultiplierOperation
                || operation instanceof AddChannelPostMultiplierOperation
                || operation instanceof AddGlobalPreMultiplierOperation
                || operation instanceof AddGlobalPostMultiplierOperation
                || operation instanceof ConvertDamageOperation
                || operation instanceof GainExtraDamageOperation
                || operation instanceof MultiplyArmorEffectivenessOperation;
    }

    private static Optional<String> operationReferenceProblem(
            DamageRuleOperation operation
    ) {
        if (operation instanceof ChannelReferencingOperation references) {
            List<net.minecraft.resources.Identifier> channels;

            try {
                channels = references.referencedChannels();
            } catch (Exception exception) {
                return Optional.of(
                        "operation_channel_reference_failed type="
                                + safeOperationType(operation)
                );
            }

            Optional<String> problem = referenceListProblem(
                    channels,
                    "operation_channel"
            );

            if (problem.isPresent()) {
                return problem;
            }
        }

        if (operation instanceof PreMultiplierBucketReferencingOperation
                references) {
            List<net.minecraft.resources.Identifier> buckets;

            try {
                buckets =
                        references.referencedPreMultiplierBuckets();
            } catch (Exception exception) {
                return Optional.of(
                        "operation_bucket_reference_failed type="
                                + safeOperationType(operation)
                );
            }

            return referenceListProblem(
                    buckets,
                    "operation_bucket"
            );
        }

        return Optional.empty();
    }

    private static Optional<String> referenceListProblem(
            List<net.minecraft.resources.Identifier> references,
            String category
    ) {
        if (references == null) {
            return Optional.of(category + "_references_are_null");
        }

        if (references.size() > MAX_REFERENCED_CHANNELS_PER_NODE) {
            return Optional.of(
                    category + "_reference_count=" + references.size()
                            + " maximum="
                            + MAX_REFERENCED_CHANNELS_PER_NODE
            );
        }

        if (references.stream().anyMatch(java.util.Objects::isNull)) {
            return Optional.of(category + "_has_null_reference");
        }

        return Optional.empty();
    }

    private static Optional<String> conditionNumericProblem(
            DamageRuleCondition condition
    ) {
        Float ratio = null;

        if (condition instanceof AttackerHealthAboveCondition value) {
            ratio = value.threshold();
        } else if (condition instanceof AttackerHealthBelowCondition value) {
            ratio = value.threshold();
        } else if (condition instanceof TargetHealthAboveCondition value) {
            ratio = value.threshold();
        } else if (condition instanceof TargetHealthBelowCondition value) {
            ratio = value.threshold();
        }

        if (ratio == null) {
            return Optional.empty();
        }

        if (!Float.isFinite(ratio) || ratio < 0.0f || ratio > 1.0f) {
            return Optional.of(
                    "condition_ratio_out_of_range type="
                            + safeConditionType(condition)
                            + " value="
                            + ratio
                            + " expected=0..1"
            );
        }

        return Optional.empty();
    }

    private static int codePointLength(String value) {
        return value.codePointCount(0, value.length());
    }

    private static String safeRuleId(DamageRuleDefinition rule) {
        return rule == null || rule.id() == null
                ? "<null>"
                : rule.id().toString();
    }

    private static String safeConditionType(
            DamageRuleCondition condition
    ) {
        try {
            return String.valueOf(condition.type());
        } catch (Exception exception) {
            return "<unknown_condition>";
        }
    }

    private static String safeOperationType(
            DamageRuleOperation operation
    ) {
        try {
            return String.valueOf(operation.type());
        } catch (Exception exception) {
            return "<unknown_operation>";
        }
    }

    static <T> Optional<String> rawProblemForTesting(
            DynamicOps<T> ops,
            T input,
            int maximumDepth,
            int maximumNodes,
            RawTraversalObserver observer
    ) {
        return rawProblem(
                ops,
                input,
                maximumDepth,
                maximumNodes,
                observer
        );
    }

    private static <T> Optional<String> rawProblem(
            DynamicOps<T> ops,
            T input,
            int maximumDepth,
            int maximumNodes,
            RawTraversalObserver observer
    ) {
        if (maximumDepth <= 0 || maximumNodes <= 0) {
            return Optional.of("raw_budget_is_not_positive");
        }

        RawTraversal<T> traversal = new RawTraversal<>(
                maximumDepth,
                maximumNodes,
                observer == null ? RawTraversalObserver.NONE : observer
        );
        Optional<String> rootProblem = traversal.discover(input, 1);

        if (rootProblem.isPresent()) {
            return rootProblem;
        }

        while (!traversal.stack.isEmpty()) {
            RawFrame<T> frame = traversal.stack.pop();

            Optional<com.mojang.serialization.MapLike<T>> map =
                    ops.getMap(frame.value()).result();

            if (map.isPresent()) {
                try (Stream<Pair<T, T>> entries = map.get().entries()) {
                    Iterator<Pair<T, T>> iterator = entries.iterator();

                    while (iterator.hasNext()) {
                        Pair<T, T> pair = iterator.next();
                        Optional<String> keyProblem = traversal.discover(
                                pair.getFirst(),
                                frame.depth() + 1
                        );

                        if (keyProblem.isPresent()) {
                            return keyProblem;
                        }

                        Optional<String> valueProblem = traversal.discover(
                                pair.getSecond(),
                                frame.depth() + 1
                        );

                        if (valueProblem.isPresent()) {
                            return valueProblem;
                        }
                    }
                }

                continue;
            }

            Optional<Stream<T>> list =
                    ops.getStream(frame.value()).result();

            if (list.isPresent()) {
                try (Stream<T> values = list.get()) {
                    Iterator<T> iterator = values.iterator();

                    while (iterator.hasNext()) {
                        Optional<String> childProblem = traversal.discover(
                                iterator.next(),
                                frame.depth() + 1
                        );

                        if (childProblem.isPresent()) {
                            return childProblem;
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    private enum VisitState {
        VISITING
    }

    public record RuleCost(
            int conditionNodes,
            int operations
    ) {
    }

    private static final class ComponentBudget {
        private long rules;
        private long conditionNodes;
        private long operations;
        private long displayCodePoints;

        private Optional<String> addEntry(
                DamageEntryDefinition entry
        ) {
            Optional<String> displayProblem = addDisplay(entry.display());

            if (displayProblem.isPresent()) {
                return displayProblem;
            }

            for (DamageRuleDefinition rule : entry.rules()) {
                Optional<RuleCost> cost = measureRuleCost(rule);

                if (cost.isEmpty()) {
                    return Optional.of("component_rule_is_not_measurable");
                }

                Optional<String> problem = add(
                        "component_rule_count",
                        1L,
                        MAX_COMPONENT_RULES,
                        ValueKind.RULES
                );

                if (problem.isPresent()) {
                    return problem;
                }

                problem = add(
                        "component_condition_nodes",
                        cost.get().conditionNodes(),
                        MAX_COMPONENT_CONDITION_NODES,
                        ValueKind.CONDITIONS
                );

                if (problem.isPresent()) {
                    return problem;
                }

                problem = add(
                        "component_operations",
                        cost.get().operations(),
                        MAX_COMPONENT_OPERATIONS,
                        ValueKind.OPERATIONS
                );

                if (problem.isPresent()) {
                    return problem;
                }
            }

            return Optional.empty();
        }

        private Optional<String> addDisplay(
                DamageEntryDisplay display
        ) {
            return addDisplay(
                    display.name(),
                    display.authoredSummary(),
                    display.flavorText()
            );
        }

        private Optional<String> addDisplay(
                io.github.naimjeg.damagenexus.api.rule.affix
                        .DamageAffixDisplay display
        ) {
            return addDisplay(
                    display.name(),
                    display.authoredSummary(),
                    display.flavorText()
            );
        }

        private Optional<String> addDisplay(
                Optional<DisplayText> name,
                List<DisplayText> tooltip,
                Optional<DisplayText> flavor
        ) {
            return add(
                    "component_display_code_points",
                    DamageRuleLimits.displayCodePoints(
                            name,
                            tooltip,
                            flavor
                    ),
                    MAX_COMPONENT_DISPLAY_CODE_POINTS,
                    ValueKind.DISPLAY
            );
        }

        private Optional<String> add(
                String name,
                long amount,
                long maximum,
                ValueKind kind
        ) {
            long current = switch (kind) {
                case RULES -> rules;
                case CONDITIONS -> conditionNodes;
                case OPERATIONS -> operations;
                case DISPLAY -> displayCodePoints;
            };

            if (amount < 0L || current > maximum - amount) {
                return Optional.of(
                        name + "="
                                + (amount < 0L
                                ? "overflow"
                                : current + amount)
                                + " maximum=" + maximum
                );
            }

            long next = current + amount;

            switch (kind) {
                case RULES -> rules = next;
                case CONDITIONS -> conditionNodes = next;
                case OPERATIONS -> operations = next;
                case DISPLAY -> displayCodePoints = next;
            }

            return Optional.empty();
        }
    }

    private enum ValueKind {
        RULES,
        CONDITIONS,
        OPERATIONS,
        DISPLAY
    }

    private record ConditionFrame(
            DamageRuleCondition condition,
            int depth,
            boolean exiting
    ) {
    }

    private record RawFrame<T>(T value, int depth) {
    }

    @FunctionalInterface
    interface RawTraversalObserver {
        RawTraversalObserver NONE =
                (discoveredNodes, pendingNodes) -> {
                };

        void discovered(int discoveredNodes, int pendingNodes);
    }

    private static final class RawTraversal<T> {
        private final int maximumDepth;
        private final int maximumNodes;
        private final RawTraversalObserver observer;
        private final Deque<RawFrame<T>> stack = new ArrayDeque<>();
        private int discoveredNodes;

        private RawTraversal(
                int maximumDepth,
                int maximumNodes,
                RawTraversalObserver observer
        ) {
            this.maximumDepth = maximumDepth;
            this.maximumNodes = maximumNodes;
            this.observer = observer;
        }

        private Optional<String> discover(T value, int depth) {
            if (depth > maximumDepth) {
                return Optional.of(
                        "raw_depth=" + depth
                                + " maximum=" + maximumDepth
                );
            }

            if (discoveredNodes >= maximumNodes) {
                return Optional.of(
                        "raw_nodes=" + ((long) discoveredNodes + 1L)
                                + " maximum=" + maximumNodes
                );
            }

            discoveredNodes++;
            stack.push(new RawFrame<>(value, depth));
            observer.discovered(discoveredNodes, stack.size());
            return Optional.empty();
        }
    }
}
