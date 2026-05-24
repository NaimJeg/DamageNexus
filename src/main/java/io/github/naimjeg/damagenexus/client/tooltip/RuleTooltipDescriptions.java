package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.util.IdentifierText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class RuleTooltipDescriptions {

    private static final RuleTooltipContext CONTEXT = new RuleTooltipContext();

    private static final Map<Identifier, RuleTooltipProvider<? extends DamageRuleCondition>> CONDITION_PROVIDERS =
            new HashMap<>();

    private static final Map<Identifier, RuleTooltipProvider<? extends DamageRuleOperation>> OPERATION_PROVIDERS =
            new HashMap<>();

    private RuleTooltipDescriptions() {
    }

    public static <T extends DamageRuleCondition> void registerCondition(
            Identifier type,
            RuleTooltipProvider<T> provider
    ) {
        CONDITION_PROVIDERS.put(type, provider);
    }

    public static <T extends DamageRuleOperation> void registerOperation(
            Identifier type,
            RuleTooltipProvider<T> provider
    ) {
        OPERATION_PROVIDERS.put(type, provider);
    }


    @SuppressWarnings("unchecked")
    public static MutableComponent describeCondition(
            DamageRuleCondition condition,
            RuleTooltipMode mode
    ) {
        RuleTooltipProvider<DamageRuleCondition> provider =
                (RuleTooltipProvider<DamageRuleCondition>)
                        CONDITION_PROVIDERS.get(condition.type());

        if (provider == null) {
            return Component.translatable(
                    "condition." + IdentifierText.langPath(condition.type())
            );
        }

        return provider.describe(
                condition,
                CONTEXT,
                mode
        );
    }

    @SuppressWarnings("unchecked")
    public static MutableComponent describeOperation(
            DamageRuleOperation operation,
            RuleTooltipMode mode
    ) {
        RuleTooltipProvider<DamageRuleOperation> provider =
                (RuleTooltipProvider<DamageRuleOperation>)
                        OPERATION_PROVIDERS.get(operation.type());

        if (provider == null) {
            return Component.translatable(
                    "operation." + IdentifierText.langPath(operation.type())
            );
        }

        return provider.describe(
                operation,
                CONTEXT,
                mode
        );
    }
}

