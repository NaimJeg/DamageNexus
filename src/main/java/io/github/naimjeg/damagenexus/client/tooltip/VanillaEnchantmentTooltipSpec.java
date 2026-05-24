package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public record VanillaEnchantmentTooltipSpec(
        Identifier source,
        Component displayName,
        List<DamageRuleOperation> operations,
        List<DamageRuleCondition> conditions,
        List<Component> extraLines
) {
    public VanillaEnchantmentTooltipSpec(
            Identifier source,
            Component displayName,
            List<DamageRuleOperation> operations,
            List<DamageRuleCondition> conditions
    ) {
        this(
                source,
                displayName,
                operations,
                conditions,
                List.of()
        );
    }

    public VanillaEnchantmentTooltipSpec {
        operations = operations == null
                ? List.of()
                : List.copyOf(operations);

        conditions = conditions == null
                ? List.of()
                : List.copyOf(conditions);

        extraLines = extraLines == null
                ? List.of()
                : List.copyOf(extraLines);
    }
}