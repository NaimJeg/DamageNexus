package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.*;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;

public final class DefaultConditionTooltips {

    private DefaultConditionTooltips() {
    }

    public static void register() {
        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.ALWAYS,
                (AlwaysCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable("condition.damagenexus.always")
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.ALL_OF,
                (AllOfCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable("condition.damagenexus.all_of")
                                .append(joinConditions(condition.conditions(), mode))
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.ANY_OF,
                (AnyOfCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable("condition.damagenexus.any_of")
                                .append(joinConditions(condition.conditions(), mode))
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.NOT,
                (NotCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable("condition.damagenexus.not")
                                .append(RuleTooltipDescriptions.describeCondition(
                                        condition.condition(),
                                        mode
                                ))
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.IS_CRITICAL,
                (IsCriticalCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable("condition.damagenexus.is_critical")
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.DAMAGE_TYPE_IS,
                (DamageTypeIsCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.damage_type_is",
                                ctx.rawId(condition.damageType())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.DAMAGE_TYPE_TAG,
                (DamageTypeTagCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.damage_type_tag",
                                ctx.damageTypeTagName(condition.tag())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.DAMAGE_CHANNEL_IS,
                (DamageChannelIsCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.damage_channel_is",
                                ctx.channelName(condition.channelId())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.TARGET_ON_FIRE,
                (TargetOnFireCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable("condition.damagenexus.target_on_fire")
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.ATTACKER_HAS_EFFECT,
                (AttackerHasEffectCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.attacker_has_effect",
                                ctx.effectName(condition.effect())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.TARGET_HAS_EFFECT,
                (TargetHasEffectCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.target_has_effect",
                                ctx.effectName(condition.effect())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.ATTACKER_EFFECT_TAG,
                (AttackerEffectTagCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.attacker_effect_tag",
                                ctx.mobEffectTagName(condition.tag())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.TARGET_EFFECT_TAG,
                (TargetEffectTagCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.target_effect_tag",
                                ctx.mobEffectTagName(condition.tag())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.SOURCE_ACTION_IS,
                (SourceActionIsCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.source_action_is",
                                ctx.rawId(condition.action())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.SOURCE_TAG,
                (SourceTagCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.source_tag",
                                ctx.rawId(condition.tag())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.REQUEST_KIND_IS,
                (RequestKindIsCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.request_kind_is",
                                ctx.requestKindName(condition.kind())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.IS_PRIMARY_DAMAGE,
                (IsPrimaryDamageCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.is_primary_damage"
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.IS_PROC_DAMAGE,
                (IsProcDamageCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.is_proc_damage"
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.HAS_PARENT_DAMAGE,
                (HasParentDamageCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.has_parent_damage"
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.PROC_ALLOWED,
                (ProcAllowedCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.proc_allowed"
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.ATTACKER_HEALTH_BELOW,
                (AttackerHealthBelowCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.attacker_health_below",
                                ctx.percent(condition.threshold())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.ATTACKER_HEALTH_ABOVE,
                (AttackerHealthAboveCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.attacker_health_above",
                                ctx.percent(condition.threshold())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.TARGET_HEALTH_BELOW,
                (TargetHealthBelowCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.target_health_below",
                                ctx.percent(condition.threshold())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.TARGET_HEALTH_ABOVE,
                (TargetHealthAboveCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.target_health_above",
                                ctx.percent(condition.threshold())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.TARGET_ENTITY_TYPE_IS,
                (TargetEntityTypeIsCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.target_entity_type_is",
                                ctx.entityTypeName(condition.entityType())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.ATTACKER_ENTITY_TYPE_IS,
                (AttackerEntityTypeIsCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.attacker_entity_type_is",
                                ctx.entityTypeName(condition.entityType())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.TARGET_ENTITY_TYPE_TAG,
                (TargetEntityTypeTagCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.target_entity_type_tag",
                                ctx.entityTypeTagName(condition.tag())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.ATTACKER_ENTITY_TYPE_TAG,
                (AttackerEntityTypeTagCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.attacker_entity_type_tag",
                                ctx.entityTypeTagName(condition.tag())
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.TARGET_IS_BOSS,
                (TargetIsBossCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable("condition.damagenexus.target_is_boss")
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.ATTACKER_IS_BOSS,
                (AttackerIsBossCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable("condition.damagenexus.attacker_is_boss")
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.TARGET_MOB_CATEGORY_IS,
                (TargetMobCategoryIsCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.target_mob_category_is",
                                condition.category().name().toLowerCase(Locale.ROOT)
                        )
        );

        RuleTooltipDescriptions.registerCondition(
                DamageNexusConditionIds.ATTACKER_MOB_CATEGORY_IS,
                (AttackerMobCategoryIsCondition condition, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.translatable(
                                "condition.damagenexus.attacker_mob_category_is",
                                condition.category().name().toLowerCase(Locale.ROOT)
                        )
        );
    }

    private static MutableComponent joinConditions(
            List<DamageRuleCondition> conditions,
            RuleTooltipMode mode
    ) {
        MutableComponent result = Component.empty();

        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                result.append(Component.translatable("tooltip.damagenexus.separator.comma"));
            }

            result.append(RuleTooltipDescriptions.describeCondition(
                    conditions.get(i),
                    mode
            ));
        }

        return result;
    }
}

