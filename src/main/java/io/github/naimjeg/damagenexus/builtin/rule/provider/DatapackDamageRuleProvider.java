package io.github.naimjeg.damagenexus.builtin.rule.provider;

import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.*;
import io.github.naimjeg.damagenexus.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import io.github.naimjeg.damagenexus.core.rule.DatapackDamageRuleStore;

import java.util.List;

public final class DatapackDamageRuleProvider implements DamageRuleProvider {

    public static int ruleCount() {
        return DatapackDamageRuleStore.ruleCount();
    }

    @Override
    public void collect(
            DamageRuleContext context,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    ) {
        DamageNexusContext ctx = DamageInternalContexts.require(
                context,
                "datapack rule provider"
        );

        for (DamageRuleDefinition rule
                : ctx.datapackRuleSnapshot().rules(phase)) {
            RuntimeDamageRule runtimeRule = new RuntimeDamageRule(
                    rule,
                    RuleExecutionContext.datapackRule(rule.role())
            );

            ctx.trace().rules().collected(
                    phase,
                    rule,
                    runtimeRule.executionContext()
            );

            out.add(runtimeRule);
        }
    }
}

