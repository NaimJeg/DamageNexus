package io.github.naimjeg.damagenexus.core.contribution;

import io.github.naimjeg.damagenexus.api.DamageNexusPreMultiplierBuckets;

import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.display.DamageContributionDescriptor;
import io.github.naimjeg.damagenexus.api.display.DamageContributionSourceKind;
import io.github.naimjeg.damagenexus.api.display.DamageContributionStatus;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.*;
import io.github.naimjeg.damagenexus.builtin.rule.operation.*;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import io.github.naimjeg.damagenexus.registry.PreMultiplierBuckets;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class DamageRuleContributionRecorder {

    private DamageRuleContributionRecorder() {
    }

    public static void recordOperation(
            DamageNexusContext ctx,
            DamagePhase runningPhase,
            RuntimeDamageRule runtimeRule,
            DamageRuleOperation operation,
            DamageMutationResult result,
            int operationIndex
    ) {
        if (ctx == null
                || runtimeRule == null
                || operation == null
                || result == null) {
            return;
        }

        ctx.contributions().record(
                result,
                () -> descriptor(
                        runningPhase,
                        runtimeRule,
                        operation,
                        result,
                        operationIndex
                )
        );
    }

    private static DamageContributionDescriptor descriptor(
            DamagePhase runningPhase,
            RuntimeDamageRule runtimeRule,
            DamageRuleOperation operation,
            DamageMutationResult result,
            int operationIndex
    ) {
        DamageRuleDefinition rule = runtimeRule.definition();
        RuleExecutionContext exec = runtimeRule.executionContext();

        Optional<String> displayName = Optional.empty();

        DamageRuleOwner owner = runtimeRule.owner();

        Optional<Identifier> displayGroup =
                owner.displayOwnerId()
                        .or(rule::stackingGroup);

        Optional<Identifier> displaySubgroup =
                owner.kind() == DamageRuleOwnerKind.AFFIX
                        ? owner.effectiveEntryId()
                        : Optional.empty();

        return new DamageContributionDescriptor(
                contributionId(rule.id(), operation.type(), operationIndex),
                sourceKind(owner),
                DamageContributionOperationKindMapper.fromRuleOperationType(
                        operation.type()
                ),
                runningPhase,

                channel(operation),
                applicationBucket(operation),
                preMultiplierBucket(operation),

                operation.stackingValue(),

                DamageContributionStatus.APPLIED,
                displayGroup,
                displaySubgroup,
                displayName,
                tooltipLines(rule, exec, owner, operation, result),

                Optional.of(operation.type().toString()),
                true,
                false
        );
    }

    private static DamageContributionSourceKind sourceKind(
            DamageRuleOwner owner
    ) {
        if (owner == null || owner.kind() == null) {
            return DamageContributionSourceKind.RULE;
        }

        return switch (owner.kind()) {
            case AFFIX -> DamageContributionSourceKind.AFFIX;
            case ENTRY -> DamageContributionSourceKind.ENTRY;
            case RULE -> DamageContributionSourceKind.RULE;
        };
    }

    private static List<String> tooltipLines(
            DamageRuleDefinition rule,
            RuleExecutionContext exec,
            DamageRuleOwner owner,
            DamageRuleOperation operation,
            DamageMutationResult result
    ) {
        List<String> lines = new ArrayList<>();

        lines.add("rule=" + rule.id());
        lines.add("operation=" + operation.type());

        channel(operation)
                .ifPresent(id -> lines.add("channel=" + id));

        applicationBucket(operation)
                .ifPresent(bucket -> lines.add("application_bucket=" + bucket));

        preMultiplierBucket(operation)
                .ifPresent(bucket -> lines.add("pre_multiplier_bucket=" + bucket));

        if (owner != null) {
            lines.add("owner_kind=" + owner.kind());

            owner.affixId()
                    .ifPresent(id -> lines.add("affix=" + id));

            owner.effectiveEntryId()
                    .ifPresent(id -> lines.add("entry=" + id));
        }

        if (exec != null) {
            lines.add("provider=" + exec.providerType());
            lines.add("source_location=" + exec.sourceLocation());
            lines.add("role=" + exec.role());
        }

        lines.add("result=" + result);

        return List.copyOf(lines);
    }

    private static Identifier contributionId(
            Identifier ruleId,
            Identifier operationType,
            int operationIndex
    ) {
        String path = "rule/"
                + sanitize(ruleId.getNamespace())
                + "/"
                + sanitize(ruleId.getPath())
                + "/"
                + sanitize(operationType.getPath())
                + "/"
                + Math.max(0, operationIndex);

        return DamageNexusIds.id(path);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value
                .replace(':', '_')
                .replace('/', '_')
                .replace(' ', '_')
                .toLowerCase(Locale.ROOT);
    }

    private static Optional<Identifier> channel(
            DamageRuleOperation operation
    ) {
        if (operation instanceof AddBaseDamageOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddChannelPreMultiplierOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddChannelPostMultiplierOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddTemporaryResistanceOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddChannelMitigationOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddTrueDamageOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof ConvertDamageOperation op) {
            return Optional.ofNullable(op.toChannel());
        }

        if (operation instanceof GainExtraDamageOperation op) {
            return Optional.ofNullable(op.toChannel());
        }

        return Optional.empty();
    }

    private static Optional<DamageApplicationBucket> applicationBucket(
            DamageRuleOperation operation
    ) {
        if (operation instanceof AddBaseDamageOperation op) {
            return Optional.ofNullable(op.applicationBucket());
        }

        if (operation instanceof AddTrueDamageOperation) {
            return Optional.of(DamageApplicationBucket.DN_TRUE_DAMAGE);
        }

        if (operation instanceof GainExtraDamageOperation) {
            return Optional.of(DamageApplicationBucket.DN_RULE_BASE);
        }

        return Optional.empty();
    }

    private static Optional<Identifier> preMultiplierBucket(
            DamageRuleOperation operation
    ) {
        if (operation instanceof AddChannelPreMultiplierOperation op) {
            return op.preMultiplierBucketId()
                    .or(() -> Optional.of(DamageNexusPreMultiplierBuckets.GENERIC_DAMAGE));
        }

        if (operation instanceof AddGlobalPreMultiplierOperation op) {
            return op.preMultiplierBucketId()
                    .or(() -> Optional.of(DamageNexusPreMultiplierBuckets.GENERIC_DAMAGE));
        }

        return Optional.empty();
    }
}
