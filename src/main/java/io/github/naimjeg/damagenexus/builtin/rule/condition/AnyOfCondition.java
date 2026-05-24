package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.CompositeDamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;
import io.github.naimjeg.damagenexus.api.rule.RuleExecutionContext;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record AnyOfCondition(
        List<DamageRuleCondition> conditions
) implements CompositeDamageRuleCondition {

    public static final MapCodec<AnyOfCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleLimits.boundedList(
                                    DamageRuleCondition.CODEC,
                                    DamageRuleLimits.MAX_RULE_CONDITIONS,
                                    "any_of conditions"
                            )
                            .fieldOf("conditions")
                            .forGetter(AnyOfCondition::conditions)
            ).apply(instance, AnyOfCondition::new));

    public AnyOfCondition {
        Objects.requireNonNull(
                conditions,
                "conditions must not be null"
        );
        List<DamageRuleCondition> copy =
                new ArrayList<>(conditions.size());

        for (DamageRuleCondition condition : conditions) {
            copy.add(Objects.requireNonNull(
                    condition,
                    "conditions must not contain null elements"
            ));
        }

        conditions = List.copyOf(copy);
    }

    @Override
    public Identifier type() {
        return DamageNexusConditionIds.ANY_OF;
    }

    @Override
    public List<DamageRuleCondition> childConditions() {
        return conditions;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        for (DamageRuleCondition condition : conditions) {
            if (condition.test(ctx)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean test(
            DamageRuleContext ctx,
            RuleExecutionContext executionContext
    ) {
        for (DamageRuleCondition condition : conditions) {
            if (condition.test(ctx, executionContext)) {
                return true;
            }
        }

        return false;
    }
}
