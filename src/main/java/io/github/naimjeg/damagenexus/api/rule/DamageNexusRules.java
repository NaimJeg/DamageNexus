package io.github.naimjeg.damagenexus.api.rule;

import io.github.naimjeg.damagenexus.api.rule.builder.DamageRuleBuilder;
import net.minecraft.resources.Identifier;

public final class DamageNexusRules {

    private DamageNexusRules() {
    }

    public static DamageRuleBuilder offensive(Identifier id) {
        return DamageRuleBuilder.offensive(id);
    }

    public static DamageRuleBuilder defensive(Identifier id) {
        return DamageRuleBuilder.defensive(id);
    }

    public static DamageRuleBuilder any(Identifier id) {
        return DamageRuleBuilder.any(id);
    }
}
