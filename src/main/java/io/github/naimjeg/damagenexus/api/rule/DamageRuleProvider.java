package io.github.naimjeg.damagenexus.api.rule;

import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;

import java.util.List;

public interface DamageRuleProvider {

    default boolean supportsPhase(DamagePhase phase) {
        return true;
    }

    void collect(
            DamageRuleContext ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    );
}
