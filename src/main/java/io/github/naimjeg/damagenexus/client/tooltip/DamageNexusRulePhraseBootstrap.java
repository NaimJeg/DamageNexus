package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.client.phrase.*;
import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperationIds;
import io.github.naimjeg.damagenexus.builtin.rule.condition.*;
import io.github.naimjeg.damagenexus.builtin.rule.operation.*;

import java.util.List;
import java.util.Set;

import static io.github.naimjeg.damagenexus.api.client.phrase.DamageNexusRulePhrases.*;

final class DamageNexusRulePhraseBootstrap {
    private DamageNexusRulePhraseBootstrap() {
    }

    static void register(RulePhraseRegistry registry) {
        registerSchemas(registry);
        registerConditions(registry);
        registerOperations(registry);
    }

    private static void registerSchemas(RulePhraseRegistry registry) {
        variants(registry, ADD_FLAT_DAMAGE, Set.of(PhraseVariant.INCREASE, PhraseVariant.DECREASE), CHANNEL, AMOUNT);
        variants(registry, ADD_TRUE_DAMAGE, Set.of(PhraseVariant.INCREASE, PhraseVariant.DECREASE), CHANNEL, AMOUNT);
        variants(registry, CHANGE_CHANNEL_DAMAGE, directions(), CHANNEL, PERCENT);
        variants(registry, CHANGE_GLOBAL_DAMAGE, directions(), PERCENT);
        variants(registry, ADD_RESISTANCE, directions(), CHANNEL, AMOUNT);
        variants(registry, ADD_CHANNEL_MITIGATION, directions(), CHANNEL, PERCENT);
        variants(registry, ADD_GLOBAL_MITIGATION, directions(), PERCENT);
        variants(registry, MULTIPLY_ARMOR_EFFECTIVENESS, directions(), PERCENT);
        variants(registry, CONVERT_DAMAGE, Set.of(PhraseVariant.DEFAULT), PERCENT, FROM_CHANNEL, TO_CHANNEL);
        variants(registry, GAIN_EXTRA_DAMAGE, directions(), PERCENT, FROM_CHANNEL, TO_CHANNEL);
        variants(registry, OVERRIDE_FINAL_DAMAGE, Set.of(PhraseVariant.DEFAULT), AMOUNT);
        variants(registry, CANCEL_DAMAGE, Set.of(PhraseVariant.DEFAULT));

        variants(registry, ALWAYS, Set.of(PhraseVariant.DEFAULT));
        variants(registry, ALL_OF, Set.of(PhraseVariant.DEFAULT));
        variants(registry, ANY_OF, Set.of(PhraseVariant.DEFAULT));
        variants(registry, NOT, Set.of(PhraseVariant.DEFAULT));
        variants(registry, HAS_EFFECT, Set.of(PhraseVariant.DEFAULT), ENTITY_ROLE, EFFECT);
        variants(registry, MATCHES_EFFECT_TAG, Set.of(PhraseVariant.DEFAULT), ENTITY_ROLE, TAG);
        variants(registry, HEALTH_THRESHOLD, Set.of(PhraseVariant.ABOVE, PhraseVariant.BELOW), ENTITY_ROLE, PERCENT);
        variants(registry, ENTITY_TYPE_IS, Set.of(PhraseVariant.DEFAULT), ENTITY_ROLE, ENTITY_TYPE);
        variants(registry, ENTITY_TYPE_TAG, Set.of(PhraseVariant.DEFAULT), ENTITY_ROLE, TAG);
        variants(registry, MOB_CATEGORY_IS, Set.of(PhraseVariant.DEFAULT), ENTITY_ROLE, MOB_CATEGORY);
        variants(registry, DAMAGE_CHANNEL_IS, Set.of(PhraseVariant.DEFAULT), CHANNEL);
        variants(registry, DAMAGE_TYPE_IS, Set.of(PhraseVariant.DEFAULT), IDENTIFIER);
        variants(registry, DAMAGE_TYPE_TAG, Set.of(PhraseVariant.DEFAULT), TAG);
        variants(registry, REQUEST_KIND_IS, Set.of(PhraseVariant.DEFAULT), REQUEST_KIND);
        variants(registry, SOURCE_ACTION_IS, Set.of(PhraseVariant.DEFAULT), IDENTIFIER);
        variants(registry, SOURCE_TAG, Set.of(PhraseVariant.DEFAULT), TAG);
        variants(registry, IS_BOSS, Set.of(PhraseVariant.DEFAULT), ENTITY_ROLE);
        variants(registry, IS_BURNING, Set.of(PhraseVariant.DEFAULT), ENTITY_ROLE);
        variants(registry, IS_CRITICAL, Set.of(PhraseVariant.DEFAULT));
        variants(registry, HAS_PARENT_DAMAGE, Set.of(PhraseVariant.DEFAULT));
        variants(registry, PROC_ALLOWED, Set.of(PhraseVariant.DEFAULT));
        variants(registry, UNKNOWN_CONDITION, Set.of(PhraseVariant.DEFAULT));
        variants(registry, UNKNOWN_EFFECT, Set.of(PhraseVariant.DEFAULT));
    }

    private static void registerConditions(RulePhraseRegistry registry) {
        registry.registerCondition(DamageNexusConditionIds.ALWAYS, AlwaysCondition.class,
                (value, phrases) -> phrase(phrases, ALWAYS, PhraseVariant.DEFAULT));
        registry.registerCondition(DamageNexusConditionIds.ALL_OF, AllOfCondition.class,
                (value, phrases) -> phrase(phrases, ALL_OF, PhraseVariant.DEFAULT));
        registry.registerCondition(DamageNexusConditionIds.ANY_OF, AnyOfCondition.class,
                (value, phrases) -> phrase(phrases, ANY_OF, PhraseVariant.DEFAULT));
        registry.registerCondition(DamageNexusConditionIds.NOT, NotCondition.class,
                (value, phrases) -> phrase(phrases, NOT, PhraseVariant.DEFAULT));
        registry.registerCondition(DamageNexusConditionIds.IS_CRITICAL, IsCriticalCondition.class,
                (value, phrases) -> phrase(phrases, IS_CRITICAL, PhraseVariant.DEFAULT));
        registry.registerCondition(DamageNexusConditionIds.TARGET_ON_FIRE, TargetOnFireCondition.class,
                (value, phrases) -> rolePhrase(phrases, IS_BURNING, EntityRoleValue.Role.TARGET));
        registry.registerCondition(DamageNexusConditionIds.ATTACKER_HAS_EFFECT, AttackerHasEffectCondition.class,
                (value, phrases) -> effectPhrase(phrases, EntityRoleValue.Role.ATTACKER, value.effect()));
        registry.registerCondition(DamageNexusConditionIds.TARGET_HAS_EFFECT, TargetHasEffectCondition.class,
                (value, phrases) -> effectPhrase(phrases, EntityRoleValue.Role.TARGET, value.effect()));
        registry.registerCondition(DamageNexusConditionIds.ATTACKER_EFFECT_TAG, AttackerEffectTagCondition.class,
                (value, phrases) -> tagRolePhrase(phrases, MATCHES_EFFECT_TAG, EntityRoleValue.Role.ATTACKER,
                        new TagValue(TagValue.Kind.MOB_EFFECT, value.tag().location())));
        registry.registerCondition(DamageNexusConditionIds.TARGET_EFFECT_TAG, TargetEffectTagCondition.class,
                (value, phrases) -> tagRolePhrase(phrases, MATCHES_EFFECT_TAG, EntityRoleValue.Role.TARGET,
                        new TagValue(TagValue.Kind.MOB_EFFECT, value.tag().location())));
        registry.registerCondition(DamageNexusConditionIds.SOURCE_ACTION_IS, SourceActionIsCondition.class,
                (value, phrases) -> phrase(phrases, SOURCE_ACTION_IS, PhraseVariant.DEFAULT,
                        PhraseArguments.builder().put(IDENTIFIER, new IdentifierValue(value.action())).build()));
        registry.registerCondition(DamageNexusConditionIds.SOURCE_TAG, SourceTagCondition.class,
                (value, phrases) -> phrase(phrases, SOURCE_TAG, PhraseVariant.DEFAULT,
                        PhraseArguments.builder().put(TAG, new TagValue(TagValue.Kind.SOURCE, value.tag())).build()));
        registry.registerCondition(DamageNexusConditionIds.REQUEST_KIND_IS, RequestKindIsCondition.class,
                (value, phrases) -> requestKind(phrases, value.kind()));
        registry.registerCondition(DamageNexusConditionIds.IS_PRIMARY_DAMAGE, IsPrimaryDamageCondition.class,
                (value, phrases) -> requestKind(phrases, DamageRequestKind.PRIMARY));
        registry.registerCondition(DamageNexusConditionIds.IS_PROC_DAMAGE, IsProcDamageCondition.class,
                (value, phrases) -> requestKind(phrases, DamageRequestKind.PROC));
        registry.registerCondition(DamageNexusConditionIds.HAS_PARENT_DAMAGE, HasParentDamageCondition.class,
                (value, phrases) -> phrase(phrases, HAS_PARENT_DAMAGE, PhraseVariant.DEFAULT));
        registry.registerCondition(DamageNexusConditionIds.PROC_ALLOWED, ProcAllowedCondition.class,
                (value, phrases) -> phrase(phrases, PROC_ALLOWED, PhraseVariant.DEFAULT));
        registry.registerCondition(DamageNexusConditionIds.DAMAGE_CHANNEL_IS, DamageChannelIsCondition.class,
                (value, phrases) -> phrase(phrases, DAMAGE_CHANNEL_IS, PhraseVariant.DEFAULT,
                        PhraseArguments.builder().put(CHANNEL, new ChannelValue(value.channelId())).build()));
        registry.registerCondition(DamageNexusConditionIds.DAMAGE_TYPE_IS, DamageTypeIsCondition.class,
                (value, phrases) -> phrase(phrases, DAMAGE_TYPE_IS, PhraseVariant.DEFAULT,
                        PhraseArguments.builder().put(IDENTIFIER, new IdentifierValue(value.damageType())).build()));
        registry.registerCondition(DamageNexusConditionIds.DAMAGE_TYPE_TAG, DamageTypeTagCondition.class,
                (value, phrases) -> phrase(phrases, DAMAGE_TYPE_TAG, PhraseVariant.DEFAULT,
                        PhraseArguments.builder().put(TAG, new TagValue(TagValue.Kind.DAMAGE_TYPE, value.tag().location())).build()));

        registry.registerCondition(DamageNexusConditionIds.ATTACKER_HEALTH_BELOW, AttackerHealthBelowCondition.class,
                (value, phrases) -> health(phrases, EntityRoleValue.Role.ATTACKER, PhraseVariant.BELOW, value.threshold()));
        registry.registerCondition(DamageNexusConditionIds.TARGET_HEALTH_BELOW, TargetHealthBelowCondition.class,
                (value, phrases) -> health(phrases, EntityRoleValue.Role.TARGET, PhraseVariant.BELOW, value.threshold()));
        registry.registerCondition(DamageNexusConditionIds.ATTACKER_HEALTH_ABOVE, AttackerHealthAboveCondition.class,
                (value, phrases) -> health(phrases, EntityRoleValue.Role.ATTACKER, PhraseVariant.ABOVE, value.threshold()));
        registry.registerCondition(DamageNexusConditionIds.TARGET_HEALTH_ABOVE, TargetHealthAboveCondition.class,
                (value, phrases) -> health(phrases, EntityRoleValue.Role.TARGET, PhraseVariant.ABOVE, value.threshold()));

        registry.registerCondition(DamageNexusConditionIds.TARGET_ENTITY_TYPE_IS, TargetEntityTypeIsCondition.class,
                (value, phrases) -> entityType(phrases, EntityRoleValue.Role.TARGET, value.entityType()));
        registry.registerCondition(DamageNexusConditionIds.ATTACKER_ENTITY_TYPE_IS, AttackerEntityTypeIsCondition.class,
                (value, phrases) -> entityType(phrases, EntityRoleValue.Role.ATTACKER, value.entityType()));
        registry.registerCondition(DamageNexusConditionIds.TARGET_ENTITY_TYPE_TAG, TargetEntityTypeTagCondition.class,
                (value, phrases) -> tagRolePhrase(phrases, ENTITY_TYPE_TAG, EntityRoleValue.Role.TARGET,
                        new TagValue(TagValue.Kind.ENTITY_TYPE, value.tag().location())));
        registry.registerCondition(DamageNexusConditionIds.ATTACKER_ENTITY_TYPE_TAG, AttackerEntityTypeTagCondition.class,
                (value, phrases) -> tagRolePhrase(phrases, ENTITY_TYPE_TAG, EntityRoleValue.Role.ATTACKER,
                        new TagValue(TagValue.Kind.ENTITY_TYPE, value.tag().location())));
        registry.registerCondition(DamageNexusConditionIds.TARGET_MOB_CATEGORY_IS, TargetMobCategoryIsCondition.class,
                (value, phrases) -> mobCategory(phrases, EntityRoleValue.Role.TARGET, value.category()));
        registry.registerCondition(DamageNexusConditionIds.ATTACKER_MOB_CATEGORY_IS, AttackerMobCategoryIsCondition.class,
                (value, phrases) -> mobCategory(phrases, EntityRoleValue.Role.ATTACKER, value.category()));
        registry.registerCondition(DamageNexusConditionIds.TARGET_IS_BOSS, TargetIsBossCondition.class,
                (value, phrases) -> rolePhrase(phrases, IS_BOSS, EntityRoleValue.Role.TARGET));
        registry.registerCondition(DamageNexusConditionIds.ATTACKER_IS_BOSS, AttackerIsBossCondition.class,
                (value, phrases) -> rolePhrase(phrases, IS_BOSS, EntityRoleValue.Role.ATTACKER));
    }

    private static void registerOperations(RulePhraseRegistry registry) {
        registry.registerOperation(DamageNexusOperationIds.ADD_BASE_DAMAGE, AddBaseDamageOperation.class,
                (value, phrases) -> flatDamage(phrases, value.channelId(), value.value()));
        registry.registerOperation(DamageNexusOperationIds.ADD_TRUE_DAMAGE, AddTrueDamageOperation.class,
                (value, phrases) -> phrase(phrases, ADD_TRUE_DAMAGE, direction(value.value()),
                        PhraseArguments.builder()
                                .put(CHANNEL, new ChannelValue(value.channelId()))
                                .put(AMOUNT, new NumberValue(Math.abs(value.value())))
                                .build()));
        registry.registerOperation(DamageNexusOperationIds.ADD_CHANNEL_PRE_MULTIPLIER, AddChannelPreMultiplierOperation.class,
                (value, phrases) -> channelChange(phrases, value.channelId(), value.value()));
        registry.registerOperation(DamageNexusOperationIds.ADD_CHANNEL_POST_MULTIPLIER, AddChannelPostMultiplierOperation.class,
                (value, phrases) -> channelChange(phrases, value.channelId(), value.value()));
        registry.registerOperation(DamageNexusOperationIds.ADD_GLOBAL_PRE_MULTIPLIER, AddGlobalPreMultiplierOperation.class,
                (value, phrases) -> globalChange(phrases, value.value()));
        registry.registerOperation(DamageNexusOperationIds.ADD_GLOBAL_POST_MULTIPLIER, AddGlobalPostMultiplierOperation.class,
                (value, phrases) -> globalChange(phrases, value.value()));
        registry.registerOperation(DamageNexusOperationIds.ADD_TEMPORARY_RESISTANCE, AddTemporaryResistanceOperation.class,
                (value, phrases) -> phrase(phrases, ADD_RESISTANCE, direction(value.value()),
                        PhraseArguments.builder()
                                .put(CHANNEL, new ChannelValue(value.channelId()))
                                .put(AMOUNT, new NumberValue(Math.abs(value.value())))
                                .build()));
        registry.registerOperation(DamageNexusOperationIds.ADD_CHANNEL_MITIGATION, AddChannelMitigationOperation.class,
                (value, phrases) -> phrase(phrases, ADD_CHANNEL_MITIGATION, direction(value.value()),
                        PhraseArguments.builder()
                                .put(CHANNEL, new ChannelValue(value.channelId()))
                                .put(PERCENT, new PercentValue(Math.abs(value.value())))
                                .build()));
        registry.registerOperation(DamageNexusOperationIds.ADD_GLOBAL_MITIGATION, AddGlobalMitigationOperation.class,
                (value, phrases) -> phrase(phrases, ADD_GLOBAL_MITIGATION, direction(value.value()),
                        PhraseArguments.builder().put(PERCENT, new PercentValue(Math.abs(value.value()))).build()));
        registry.registerOperation(DamageNexusOperationIds.MULTIPLY_ARMOR_EFFECTIVENESS, MultiplyArmorEffectivenessOperation.class,
                (value, phrases) -> phrase(phrases, MULTIPLY_ARMOR_EFFECTIVENESS,
                        value.value() >= 1.0f ? PhraseVariant.INCREASE : PhraseVariant.DECREASE,
                        PhraseArguments.builder().put(PERCENT, new PercentValue(Math.abs(value.value() - 1.0f))).build()));
        registry.registerOperation(DamageNexusOperationIds.CONVERT_DAMAGE, ConvertDamageOperation.class,
                (value, phrases) -> phrase(phrases, CONVERT_DAMAGE, PhraseVariant.DEFAULT,
                        PhraseArguments.builder()
                                .put(PERCENT, new PercentValue(Math.abs(value.ratio())))
                                .put(FROM_CHANNEL, new ChannelValue(value.fromChannel()))
                                .put(TO_CHANNEL, new ChannelValue(value.toChannel()))
                                .build()));
        registry.registerOperation(DamageNexusOperationIds.GAIN_EXTRA_DAMAGE, GainExtraDamageOperation.class,
                (value, phrases) -> phrase(phrases, GAIN_EXTRA_DAMAGE, direction(value.ratio()),
                        PhraseArguments.builder()
                                .put(PERCENT, new PercentValue(Math.abs(value.ratio())))
                                .put(FROM_CHANNEL, new ChannelValue(value.basedOnChannel()))
                                .put(TO_CHANNEL, new ChannelValue(value.toChannel()))
                                .build()));
        registry.registerOperation(DamageNexusOperationIds.OVERRIDE_FINAL_DAMAGE, OverrideFinalDamageOperation.class,
                (value, phrases) -> phrase(phrases, OVERRIDE_FINAL_DAMAGE, PhraseVariant.DEFAULT,
                        PhraseArguments.builder().put(AMOUNT, new NumberValue(value.value())).build()));
        registry.registerOperation(DamageNexusOperationIds.CANCEL_DAMAGE, CancelDamageOperation.class,
                (value, phrases) -> phrase(phrases, CANCEL_DAMAGE, PhraseVariant.DEFAULT));
    }

    private static void variants(RulePhraseRegistry registry, RulePhraseType type,
                                 Set<PhraseVariant> variants, PhraseSlot<?>... slots) {
        registry.registerSchema(new RulePhraseSchema(type, variants, List.of(slots)));
    }

    private static Set<PhraseVariant> directions() {
        return Set.of(PhraseVariant.INCREASE, PhraseVariant.DECREASE);
    }

    private static PhraseVariant direction(float value) {
        return value < 0.0f ? PhraseVariant.DECREASE : PhraseVariant.INCREASE;
    }

    private static RulePhrase phrase(RulePhraseFactory factory, RulePhraseType type, PhraseVariant variant) {
        return phrase(factory, type, variant, PhraseArguments.EMPTY);
    }

    private static RulePhrase phrase(RulePhraseFactory factory, RulePhraseType type,
                                     PhraseVariant variant, PhraseArguments arguments) {
        return factory.create(type, variant, arguments);
    }

    private static RulePhrase flatDamage(RulePhraseFactory factory, net.minecraft.resources.Identifier channel, float value) {
        return phrase(factory, ADD_FLAT_DAMAGE, direction(value), PhraseArguments.builder()
                .put(CHANNEL, new ChannelValue(channel))
                .put(AMOUNT, new NumberValue(Math.abs(value)))
                .build());
    }

    private static RulePhrase channelChange(RulePhraseFactory factory, net.minecraft.resources.Identifier channel, float value) {
        return phrase(factory, CHANGE_CHANNEL_DAMAGE, direction(value), PhraseArguments.builder()
                .put(CHANNEL, new ChannelValue(channel))
                .put(PERCENT, new PercentValue(Math.abs(value)))
                .build());
    }

    private static RulePhrase globalChange(RulePhraseFactory factory, float value) {
        return phrase(factory, CHANGE_GLOBAL_DAMAGE, direction(value), PhraseArguments.builder()
                .put(PERCENT, new PercentValue(Math.abs(value))).build());
    }

    private static RulePhrase rolePhrase(RulePhraseFactory factory, RulePhraseType type, EntityRoleValue.Role role) {
        return phrase(factory, type, PhraseVariant.DEFAULT, PhraseArguments.builder()
                .put(ENTITY_ROLE, new EntityRoleValue(role)).build());
    }

    private static RulePhrase effectPhrase(RulePhraseFactory factory, EntityRoleValue.Role role,
                                           net.minecraft.resources.Identifier effect) {
        return phrase(factory, HAS_EFFECT, PhraseVariant.DEFAULT, PhraseArguments.builder()
                .put(ENTITY_ROLE, new EntityRoleValue(role))
                .put(EFFECT, new EffectValue(effect)).build());
    }

    private static RulePhrase tagRolePhrase(RulePhraseFactory factory, RulePhraseType type,
                                            EntityRoleValue.Role role, TagValue tag) {
        return phrase(factory, type, PhraseVariant.DEFAULT, PhraseArguments.builder()
                .put(ENTITY_ROLE, new EntityRoleValue(role)).put(TAG, tag).build());
    }

    private static RulePhrase requestKind(RulePhraseFactory factory, DamageRequestKind kind) {
        return phrase(factory, REQUEST_KIND_IS, PhraseVariant.DEFAULT, PhraseArguments.builder()
                .put(REQUEST_KIND, new RequestKindValue(kind)).build());
    }

    private static RulePhrase health(RulePhraseFactory factory, EntityRoleValue.Role role,
                                     PhraseVariant variant, float threshold) {
        return phrase(factory, HEALTH_THRESHOLD, variant, PhraseArguments.builder()
                .put(ENTITY_ROLE, new EntityRoleValue(role))
                .put(PERCENT, new PercentValue(threshold)).build());
    }

    private static RulePhrase entityType(RulePhraseFactory factory, EntityRoleValue.Role role,
                                         net.minecraft.resources.Identifier entityType) {
        return phrase(factory, ENTITY_TYPE_IS, PhraseVariant.DEFAULT, PhraseArguments.builder()
                .put(ENTITY_ROLE, new EntityRoleValue(role))
                .put(ENTITY_TYPE, new EntityTypeValue(entityType)).build());
    }

    private static RulePhrase mobCategory(RulePhraseFactory factory, EntityRoleValue.Role role,
                                          net.minecraft.world.entity.MobCategory category) {
        return phrase(factory, MOB_CATEGORY_IS, PhraseVariant.DEFAULT, PhraseArguments.builder()
                .put(ENTITY_ROLE, new EntityRoleValue(role))
                .put(MOB_CATEGORY, new MobCategoryValue(category)).build());
    }
}
