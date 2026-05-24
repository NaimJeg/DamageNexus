package io.github.naimjeg.damagenexus.api.rule;

import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.core.registry.PreMultiplierBucketRegistry;
import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class DamageRuleReferenceValidator {

    private DamageRuleReferenceValidator() {
    }

    /**
     * Strict reference validation for datapack-loaded rules.
     * <p>
     * Do not call this from Java API registration during common setup because
     * datapack-defined channels may not be loaded yet.
     */
    public static boolean validateDatapackReferences(
            DamageRuleDefinition rule,
            String source,
            DamageRuleValidator.Policy policy
    ) {
        return validateReferences(rule, source, policy, true);
    }

    /**
     * Validates references available during synchronous Java registration.
     * Damage channels are reload-defined and therefore receive their strict
     * existence check when the template reload snapshot is built; bucket and
     * null-reference checks remain strict here.
     */
    public static boolean validateJavaRegistrationReferences(
            DamageRuleDefinition rule,
            String source,
            DamageRuleValidator.Policy policy
    ) {
        return validateReferences(rule, source, policy, false);
    }

    private static boolean validateReferences(
            DamageRuleDefinition rule,
            String source,
            DamageRuleValidator.Policy policy,
            boolean requireKnownChannels
    ) {
        if (rule == null) {
            return DamageRuleValidator.problem(
                    source,
                    "<null>",
                    "cannot validate references for null rule",
                    policy
            );
        }

        String ruleId = rule.id() == null
                ? "<null>"
                : rule.id().toString();

        boolean valid = true;

        Deque<DamageRuleCondition> pending =
                new ArrayDeque<>(rule.conditions());
        Set<DamageRuleCondition> visited =
                Collections.newSetFromMap(new IdentityHashMap<>());

        while (!pending.isEmpty()) {
            DamageRuleCondition condition = pending.pop();

            if (condition == null || !visited.add(condition)) {
                continue;
            }

            valid &= validateConditionReference(
                    condition,
                    source,
                    ruleId,
                    policy,
                    requireKnownChannels
            );

            if (condition instanceof CompositeDamageRuleCondition composite) {
                List<DamageRuleCondition> children;
                try {
                    children = composite.childConditions();
                } catch (RuntimeException exception) {
                    valid &= DamageRuleValidator.problem(
                            source,
                            ruleId,
                            "condition child callback failed: "
                                    + exception.getClass().getSimpleName(),
                            policy
                    );
                    continue;
                }
                if (children == null
                        || children.size() > DamageRuleLimits.MAX_RULE_CONDITIONS
                        || children.stream().anyMatch(java.util.Objects::isNull)) {
                    valid &= DamageRuleValidator.problem(
                            source,
                            ruleId,
                            "condition has an invalid child list",
                            policy
                    );
                    continue;
                }
                pushAll(pending, children);
            }
        }

        for (DamageRuleOperation operation : rule.operations()) {
            valid &= validateOperationReference(
                    operation,
                    source,
                    ruleId,
                    policy,
                    requireKnownChannels
            );
        }

        return valid;
    }

    private static void pushAll(
            Deque<DamageRuleCondition> pending,
            List<DamageRuleCondition> conditions
    ) {
        if (conditions == null) {
            return;
        }

        for (int index = conditions.size() - 1; index >= 0; index--) {
            DamageRuleCondition condition = conditions.get(index);

            if (condition != null) {
                pending.push(condition);
            }
        }
    }

    private static boolean validateConditionReference(
            DamageRuleCondition condition,
            String source,
            String ruleId,
            DamageRuleValidator.Policy policy,
            boolean requireKnownChannels
    ) {
        if (condition == null) {
            return true;
        }

        boolean valid = true;

        if (condition instanceof ChannelReferencingCondition channelCondition) {
            List<Identifier> channelIds;

            try {
                channelIds = channelCondition.referencedChannels();
            } catch (Exception exception) {
                return DamageRuleValidator.problem(
                        source,
                        ruleId,
                        "condition channel reference callback failed: "
                                + exception.getClass().getSimpleName(),
                        policy
                );
            }

            if (channelIds == null
                    || channelIds.size()
                    > DamageRuleLimits.MAX_REFERENCED_CHANNELS_PER_NODE) {
                return DamageRuleValidator.problem(
                        source,
                        ruleId,
                        "condition has an invalid channel reference list",
                        policy
                );
            }

            for (Identifier channelId : channelIds) {
                valid &= validateChannel(
                        channelId,
                        null,
                        source,
                        ruleId,
                        policy,
                        requireKnownChannels
                );
            }
        }

        return valid;
    }

    private static boolean validateOperationReference(
            DamageRuleOperation operation,
            String source,
            String ruleId,
            DamageRuleValidator.Policy policy,
            boolean requireKnownChannels
    ) {
        if (operation == null) {
            return true;
        }

        boolean valid = true;

        if (operation instanceof ChannelReferencingOperation channelOperation) {
            List<Identifier> channelIds;

            try {
                channelIds = channelOperation.referencedChannels();
            } catch (Exception exception) {
                return DamageRuleValidator.problem(
                        source,
                        ruleId,
                        "operation channel reference callback failed: "
                                + exception.getClass().getSimpleName(),
                        policy
                );
            }

            if (channelIds == null
                    || channelIds.size()
                    > DamageRuleLimits.MAX_REFERENCED_CHANNELS_PER_NODE) {
                return DamageRuleValidator.problem(
                        source,
                        ruleId,
                        "operation has an invalid channel reference list",
                        policy
                );
            }

            for (Identifier channelId : channelIds) {
                valid &= validateChannel(
                        channelId,
                        operation,
                        source,
                        ruleId,
                        policy,
                        requireKnownChannels
                );
            }
        }

        if (operation instanceof PreMultiplierBucketReferencingOperation bucketOperation) {
            List<Identifier> bucketIds;

            try {
                bucketIds =
                        bucketOperation.referencedPreMultiplierBuckets();
            } catch (Exception exception) {
                return DamageRuleValidator.problem(
                        source,
                        ruleId,
                        "operation bucket reference callback failed: "
                                + exception.getClass().getSimpleName(),
                        policy
                );
            }

            if (bucketIds == null
                    || bucketIds.size()
                    > DamageRuleLimits.MAX_REFERENCED_CHANNELS_PER_NODE) {
                return DamageRuleValidator.problem(
                        source,
                        ruleId,
                        "operation has an invalid bucket reference list",
                        policy
                );
            }

            for (Identifier bucketId : bucketIds) {
                valid &= validatePreMultiplierBucket(
                        bucketId,
                        operation,
                        source,
                        ruleId,
                        policy
                );
            }
        }

        return valid;
    }

    private static boolean validateChannel(
            Identifier channelId,
            DamageRuleOperation operation,
            String source,
            String ruleId,
            DamageRuleValidator.Policy policy,
            boolean requireKnownChannels
    ) {
        String owner = operation == null
                ? "condition"
                : "operation " + operation.type();

        if (channelId == null) {
            return DamageRuleValidator.problem(
                    source,
                    ruleId,
                    owner + " references null damage channel",
                    policy
            );
        }

        if (DamageChannel.UNTYPED_ID.equals(channelId)) {
            return true;
        }

        if (requireKnownChannels
                && !DamageChannelRegistry.containsChannel(channelId)) {
            return DamageRuleValidator.problem(
                    source,
                    ruleId,
                    owner + " references unknown damage channel "
                            + channelId
                            + ". This would otherwise fall back to untyped.",
                    policy
            );
        }

        return true;
    }

    private static boolean validatePreMultiplierBucket(
            Identifier bucketId,
            DamageRuleOperation operation,
            String source,
            String ruleId,
            DamageRuleValidator.Policy policy
    ) {
        if (bucketId == null) {
            return DamageRuleValidator.problem(
                    source,
                    ruleId,
                    "operation " + operation.type()
                            + " references null pre-multiplier bucket",
                    policy
            );
        }

        if (!PreMultiplierBucketRegistry.containsPreMultiplierBucket(bucketId)) {
            return DamageRuleValidator.problem(
                    source,
                    ruleId,
                    "operation " + operation.type()
                            + " references unknown pre-multiplier bucket "
                            + bucketId,
                    policy
            );
        }

        return true;
    }
}
