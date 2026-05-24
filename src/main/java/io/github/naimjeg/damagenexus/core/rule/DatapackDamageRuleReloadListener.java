package io.github.naimjeg.damagenexus.core.rule;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.CompositeDamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleReferenceValidator;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleStacking;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleValidator;
import io.github.naimjeg.damagenexus.builtin.rule.condition.DamageChannelIsCondition;
import io.github.naimjeg.damagenexus.builtin.rule.operation.*;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusReloadAccess;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.core.registry.PreMultiplierBucketRegistry;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusLifecycleLog;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusDiagnosticState;
import io.github.naimjeg.damagenexus.diagnostics.logging.DiagnosticTextSanitizer;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.*;

public final class DatapackDamageRuleReloadListener
        extends SimpleJsonResourceReloadListener<DamageRuleDefinition> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter RULE_FILES =
            FileToIdConverter.json("damagenexus_rules");
    public static final int MAX_DATAPACK_RULES = 512;
    public static final int MAX_DATAPACK_CONDITION_NODES = 8_192;
    public static final int MAX_DATAPACK_OPERATIONS = 4_096;

    public DatapackDamageRuleReloadListener(
            DamageNexusReloadAccess access
    ) {
        super(
                DamageRuleDefinition.CODEC,
                RULE_FILES
        );
        Objects.requireNonNull(access, "access")
                .requireFrameworkOwner(
                        "DatapackDamageRuleReloadListener"
                );
    }

    @Override
    protected Map<Identifier, DamageRuleDefinition> prepare(
            ResourceManager manager,
            ProfilerFiller profiler
    ) {
        int resourceCount =
                RULE_FILES.listMatchingResources(manager).size();

        if (resourceCount > MAX_DATAPACK_RULES) {
            throw new IllegalArgumentException(
                    "DamageNexus global rule resource count exceeds "
                            + "maximum before decode: "
                            + resourceCount
                            + " > "
                            + MAX_DATAPACK_RULES
            );
        }

        return super.prepare(manager, profiler);
    }

    static boolean validateRule(
            Identifier fileId,
            DamageRuleDefinition rule
    ) {
        String source = "datapack/file/" + fileId;

        if (!DamageRuleValidator.validate(
                rule,
                source,
                DamageRuleValidator.Policy.WARN
        )) {
            return false;
        }

        boolean accepted = true;

        if (rule.operations().isEmpty()) {
            LOGGER.warn(
                    "[DamageNexus] Global datapack rule {} from {} has no operations. It will load but do nothing.",
                    rule.id(),
                    fileId
            );
        }

        if (requiresStackingGroup(rule.stacking())
                && rule.stackingGroup().isEmpty()) {
            LOGGER.warn(
                    "[DamageNexus] Global datapack rule {} from {} uses stacking={} without stacking_group. The rule id will be used as fallback, so cross-rule stacking may not work as intended.",
                    rule.id(),
                    fileId,
                    rule.stacking()
            );
        }

        Deque<DamageRuleCondition> pending =
                new ArrayDeque<>(rule.conditions());

        while (!pending.isEmpty()) {
            DamageRuleCondition condition = pending.pop();
            validateCondition(fileId, rule, condition);

            if (condition instanceof CompositeDamageRuleCondition composite) {
                List<DamageRuleCondition> children = composite.childConditions();
                if (children == null
                        || children.size() > DamageRuleLimits.MAX_RULE_CONDITIONS
                        || children.stream().anyMatch(java.util.Objects::isNull)) {
                    throw new IllegalArgumentException(
                            "Invalid composite condition child list in "
                                    + fileId
                    );
                }
                pushConditions(pending, children);
            }
        }

        for (DamageRuleOperation operation : rule.operations()) {
            if (!validateOperation(fileId, rule, operation)) {
                accepted = false;
            }
        }

        return accepted;
    }

    private static boolean validateOperation(
            Identifier fileId,
            DamageRuleDefinition rule,
            DamageRuleOperation operation
    ) {
        boolean accepted = true;

        if (operation instanceof AddBaseDamageOperation addBase) {
            validateChannelId(
                    fileId,
                    rule,
                    "operation=" + operation.type(),
                    addBase.channelId()
            );
        }

        if (operation instanceof AddChannelPreMultiplierOperation addPre) {
            validateChannelId(
                    fileId,
                    rule,
                    "operation=" + operation.type(),
                    addPre.channelId()
            );

            if (!validateBucketId(
                    fileId,
                    rule,
                    operation,
                    addPre.preMultiplierBucketId()
            )) {
                accepted = false;
            }
        }

        if (operation instanceof AddChannelPostMultiplierOperation addPost) {
            validateChannelId(
                    fileId,
                    rule,
                    "operation=" + operation.type(),
                    addPost.channelId()
            );
        }

        if (operation instanceof AddTemporaryResistanceOperation addResistance) {
            validateChannelId(
                    fileId,
                    rule,
                    "operation=" + operation.type(),
                    addResistance.channelId()
            );
        }

        if (operation instanceof AddGlobalPreMultiplierOperation addGlobalPre) {
            if (!validateBucketId(
                    fileId,
                    rule,
                    operation,
                    addGlobalPre.preMultiplierBucketId()
            )) {
                accepted = false;
            }
        }

        return accepted;
    }

    private static void validateCondition(
            Identifier fileId,
            DamageRuleDefinition rule,
            DamageRuleCondition condition
    ) {
        if (condition instanceof DamageChannelIsCondition(Identifier channelId)) {
            validateChannelId(
                    fileId,
                    rule,
                    "condition=" + condition.type(),
                    channelId
            );
        }
    }

    private static void pushConditions(
            Deque<DamageRuleCondition> pending,
            List<DamageRuleCondition> conditions
    ) {
        for (int index = conditions.size() - 1; index >= 0; index--) {
            pending.push(conditions.get(index));
        }
    }

    private static void validateChannelId(
            Identifier fileId,
            DamageRuleDefinition rule,
            String location,
            Identifier channelId
    ) {
        /* CHANNELS -> GLOBAL_RULES is an explicit reload dependency. The later
         * strict reference pass rejects this rule; this message only supplies
         * diagnostic context and never promises an untyped fallback. */
        if (!DamageChannelRegistry.containsChannel(channelId)) {
            LOGGER.warn(
                    "[DamageNexus] Global datapack rule {} from {} references unknown damage channel {} at {} and will be rejected by strict reference validation.",
                    rule.id(),
                    fileId,
                    channelId,
                    location
            );
        }
    }

    private static boolean validateBucketId(
            Identifier fileId,
            DamageRuleDefinition rule,
            DamageRuleOperation operation,
            Optional<Identifier> bucketId
    ) {
        if (bucketId.isEmpty()) {
            return true;
        }

        Identifier id = bucketId.get();

        /*
         * Fatal:
         * unknown pre-multiplier bucket id will throw at runtime when the operation applies.
         */
        if (!PreMultiplierBucketRegistry.containsPreMultiplierBucket(id)) {
            LOGGER.error(
                    "[DamageNexus] Rejecting global datapack rule {} from {} because operation {} references unknown pre-multiplier bucket {}.",
                    rule.id(),
                    fileId,
                    operation.type(),
                    id
            );

            return false;
        }

        return true;
    }

    private static boolean requiresStackingGroup(DamageRuleStacking stacking) {
        return switch (stacking) {
            case STACK -> false;
            case UNIQUE_SOURCE,
                 HIGHEST_VALUE,
                 LOWEST_VALUE,
                 REPLACE -> true;
        };
    }

    @Override
    protected void apply(
            Map<Identifier, DamageRuleDefinition> prepared,
            ResourceManager manager,
            ProfilerFiller profiler
    ) {
        DamageNexusDiagnosticState.clearAll();
        publishPrepared(prepared);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static boolean applyPreparedForTesting(
            Map<Identifier, DamageRuleDefinition> prepared
    ) {
        return publishPrepared(prepared);
    }

    private static boolean publishPrepared(
            Map<Identifier, DamageRuleDefinition> prepared
    ) {
        long channelRevision = DamageChannelRegistry.contentRevision();
        Optional<String> aggregateProblem =
                findAggregateProblem(prepared);

        if (aggregateProblem.isPresent()) {
            logAggregateRejection(aggregateProblem.get());
            return false;
        }

        List<Map.Entry<Identifier, DamageRuleDefinition>> entries =
                new ArrayList<>(prepared.entrySet());

        entries.sort(Comparator.comparing(entry -> entry.getKey().toString()));

        List<DamageRuleDefinition> accepted = new ArrayList<>();
        Map<Identifier, Identifier> firstFileByRuleId = new HashMap<>();

        int rejected = 0;

        for (Map.Entry<Identifier, DamageRuleDefinition> entry : entries) {
            Identifier fileId = entry.getKey();
            DamageRuleDefinition rule = entry.getValue();

            try {
                Identifier previousFile = firstFileByRuleId.putIfAbsent(
                        rule.id(),
                        fileId
                );

                if (previousFile != null) {
                    LOGGER.warn(
                            "[DamageNexus] Duplicate global datapack damage rule id {}. Keeping {}, skipping {}.",
                            rule.id(),
                            previousFile,
                            fileId
                    );

                    rejected++;
                    continue;
                }

                if (!validateRule(fileId, rule)) {
                    rejected++;
                    continue;
                }

                accepted.add(rule);
            } catch (Exception e) {
                rejected++;

                LOGGER.error(
                        "[DamageNexus] Failed to process global datapack damage rule from {}",
                        fileId,
                        e
                );
            }
        }

        accepted.sort((a, b) -> a.id().toString().compareTo(b.id().toString()));

        List<DamageRuleDefinition> validated = new ArrayList<>();

        for (DamageRuleDefinition rule : accepted) {
            if (DamageRuleReferenceValidator
                    .validateDatapackReferences(
                            rule,
                            "datapack/reference",
                            DamageRuleValidator.Policy.WARN
                    )) {
                validated.add(rule);
            } else {
                rejected++;
            }
        }

        try {
            DatapackDamageRuleStore.replace(validated, channelRevision);
        } catch (RuntimeException exception) {
            logAggregateRejection(exception.getMessage());
            return false;
        }

        DatapackDamageRuleStore.Snapshot published =
                DatapackDamageRuleStore.snapshot();
        DamageNexusLifecycleLog.datapackRulesLoaded(
                validated.size(),
                rejected,
                published.revision(),
                published.validatedChannelRevision()
        );

        return true;
    }

    static Optional<String> findAggregateProblem(
            Map<Identifier, DamageRuleDefinition> prepared
    ) {
        if (prepared == null) {
            return Optional.of("prepared_rules_are_null");
        }

        if (prepared.size() > MAX_DATAPACK_RULES) {
            return Optional.of(
                    "datapack_rule_count=" + prepared.size()
                            + " maximum=" + MAX_DATAPACK_RULES
            );
        }

        long conditionNodes = 0L;
        long operations = 0L;

        for (DamageRuleDefinition rule : prepared.values()) {
            Optional<DamageRuleLimits.RuleCost> cost =
                    DamageRuleLimits.measureRuleCost(rule);

            if (cost.isEmpty()) {
                return Optional.of("datapack_rule_is_not_measurable");
            }

            if (conditionNodes
                    > MAX_DATAPACK_CONDITION_NODES
                    - (long) cost.get().conditionNodes()) {
                return Optional.of(
                        "datapack_condition_nodes="
                                + (conditionNodes
                                + cost.get().conditionNodes())
                                + " maximum="
                                + MAX_DATAPACK_CONDITION_NODES
                );
            }

            conditionNodes += cost.get().conditionNodes();

            if (operations
                    > MAX_DATAPACK_OPERATIONS
                    - (long) cost.get().operations()) {
                return Optional.of(
                        "datapack_operations="
                                + (operations
                                + cost.get().operations())
                                + " maximum="
                                + MAX_DATAPACK_OPERATIONS
                );
            }

            operations += cost.get().operations();
        }

        return Optional.empty();
    }

    private static void logAggregateRejection(String reason) {
        String safeReason = DiagnosticTextSanitizer.sanitizeLine(
                reason,
                256
        );

        if (DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.DATAPACK_RELOAD,
                "global_rules",
                "aggregate_budget",
                safeReason
        )) {
            LOGGER.error(
                    "[DamageNexus] Rejecting complete global datapack "
                            + "rule reload; previous snapshot retained. "
                            + "reason={}",
                    safeReason
            );
        }
    }
}

