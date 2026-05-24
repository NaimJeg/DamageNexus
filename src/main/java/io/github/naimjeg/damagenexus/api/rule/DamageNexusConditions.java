package io.github.naimjeg.damagenexus.api.rule;

import io.github.naimjeg.damagenexus.builtin.rule.condition.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.effect.MobEffect;
import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class DamageNexusConditions {

    private DamageNexusConditions() {
    }

    /*
     * ---------------------------------------------------------------------
     * Logical composition
     * ---------------------------------------------------------------------
     */

    public static DamageRuleCondition always() {
        return new AlwaysCondition();
    }

    public static DamageRuleCondition allOf(
            List<DamageRuleCondition> conditions
    ) {
        return new AllOfCondition(copyConditions(conditions));
    }

    public static DamageRuleCondition allOf(
            DamageRuleCondition... conditions
    ) {
        return allOf(Arrays.asList(conditions));
    }

    public static DamageRuleCondition anyOf(
            List<DamageRuleCondition> conditions
    ) {
        return new AnyOfCondition(copyConditions(conditions));
    }

    public static DamageRuleCondition anyOf(
            DamageRuleCondition... conditions
    ) {
        return anyOf(Arrays.asList(conditions));
    }

    public static DamageRuleCondition not(
            DamageRuleCondition condition
    ) {
        return new NotCondition(Objects.requireNonNull(
                condition,
                "condition must not be null"
        ));
    }

    /*
     * ---------------------------------------------------------------------
     * Combat state
     * ---------------------------------------------------------------------
     */

    public static DamageRuleCondition critical() {
        return new IsCriticalCondition();
    }

    public static DamageRuleCondition targetOnFire() {
        return new TargetOnFireCondition();
    }

    public static DamageRuleCondition damageChannelIs(
            Identifier channel
    ) {
        return new DamageChannelIsCondition(Objects.requireNonNull(
                channel,
                "channel must not be null"
        ));
    }

    /*
     * ---------------------------------------------------------------------
     * Health
     * ---------------------------------------------------------------------
     */

    public static DamageRuleCondition attackerHealthBelow(float ratio) {
        return new AttackerHealthBelowCondition(ratio);
    }

    public static DamageRuleCondition attackerHealthAbove(float ratio) {
        return new AttackerHealthAboveCondition(ratio);
    }

    public static DamageRuleCondition targetHealthBelow(float ratio) {
        return new TargetHealthBelowCondition(ratio);
    }

    public static DamageRuleCondition targetHealthAbove(float ratio) {
        return new TargetHealthAboveCondition(ratio);
    }

    /*
     * ---------------------------------------------------------------------
     * Mob effects
     * ---------------------------------------------------------------------
     */

    public static DamageRuleCondition attackerHasEffect(
            Identifier effect
    ) {
        return new AttackerHasEffectCondition(Objects.requireNonNull(
                effect,
                "effect must not be null"
        ));
    }

    public static DamageRuleCondition targetHasEffect(
            Identifier effect
    ) {
        return new TargetHasEffectCondition(Objects.requireNonNull(
                effect,
                "effect must not be null"
        ));
    }

    public static DamageRuleCondition attackerEffectTag(
            TagKey<MobEffect> tag
    ) {
        return new AttackerEffectTagCondition(Objects.requireNonNull(
                tag,
                "tag must not be null"
        ));
    }

    public static DamageRuleCondition targetEffectTag(
            TagKey<MobEffect> tag
    ) {
        return new TargetEffectTagCondition(Objects.requireNonNull(
                tag,
                "tag must not be null"
        ));
    }

    /*
     * ---------------------------------------------------------------------
     * Damage source / damage type
     * ---------------------------------------------------------------------
     */

    public static DamageRuleCondition damageTypeIs(
            Identifier damageType
    ) {
        return new DamageTypeIsCondition(Objects.requireNonNull(
                damageType,
                "damageType must not be null"
        ));
    }

    public static DamageRuleCondition damageTypeTag(
            TagKey<DamageType> tag
    ) {
        return new DamageTypeTagCondition(Objects.requireNonNull(
                tag,
                "tag must not be null"
        ));
    }

    /*
     * ---------------------------------------------------------------------
     * Immutable source metadata and lineage
     * ---------------------------------------------------------------------
     */

    /** Tests the immutable origin action identifier, preserving its namespace. */
    public static DamageRuleCondition sourceActionIs(Identifier action) {
        return new SourceActionIsCondition(Objects.requireNonNull(
                action,
                "action must not be null"
        ));
    }

    /**
     * Tests an immutable {@code DamageOrigin.sourceTags()} identifier. This is
     * metadata, not a Minecraft registry tag.
     */
    public static DamageRuleCondition sourceTag(Identifier tag) {
        return new SourceTagCondition(Objects.requireNonNull(
                tag,
                "tag must not be null"
        ));
    }

    public static DamageRuleCondition requestKindIs(DamageRequestKind kind) {
        return new RequestKindIsCondition(Objects.requireNonNull(
                kind,
                "kind must not be null"
        ));
    }

    public static DamageRuleCondition isPrimaryDamage() {
        return new IsPrimaryDamageCondition();
    }

    public static DamageRuleCondition isProcDamage() {
        return new IsProcDamageCondition();
    }

    public static DamageRuleCondition hasParentDamage() {
        return new HasParentDamageCondition();
    }

    /** Tests the current request's already-resolved downstream PROC permission. */
    public static DamageRuleCondition procAllowed() {
        return new ProcAllowedCondition();
    }

    /*
     * ---------------------------------------------------------------------
     * Target entity checks
     * ---------------------------------------------------------------------
     */

    public static DamageRuleCondition targetEntityTypeIs(
            Identifier entityType
    ) {
        return new TargetEntityTypeIsCondition(Objects.requireNonNull(
                entityType,
                "entityType must not be null"
        ));
    }

    public static DamageRuleCondition targetEntityTypeTag(
            TagKey<EntityType<?>> tag
    ) {
        return new TargetEntityTypeTagCondition(Objects.requireNonNull(
                tag,
                "tag must not be null"
        ));
    }

    public static DamageRuleCondition targetMobCategoryIs(
            MobCategory category
    ) {
        return new TargetMobCategoryIsCondition(Objects.requireNonNull(
                category,
                "category must not be null"
        ));
    }

    public static DamageRuleCondition targetIsBoss() {
        return new TargetIsBossCondition();
    }

    /*
     * ---------------------------------------------------------------------
     * Attacker entity checks
     * ---------------------------------------------------------------------
     */

    public static DamageRuleCondition attackerEntityTypeIs(
            Identifier entityType
    ) {
        return new AttackerEntityTypeIsCondition(Objects.requireNonNull(
                entityType,
                "entityType must not be null"
        ));
    }

    public static DamageRuleCondition attackerEntityTypeTag(
            TagKey<EntityType<?>> tag
    ) {
        return new AttackerEntityTypeTagCondition(Objects.requireNonNull(
                tag,
                "tag must not be null"
        ));
    }

    public static DamageRuleCondition attackerMobCategoryIs(
            MobCategory category
    ) {
        return new AttackerMobCategoryIsCondition(Objects.requireNonNull(
                category,
                "category must not be null"
        ));
    }

    public static DamageRuleCondition attackerIsBoss() {
        return new AttackerIsBossCondition();
    }

    private static List<DamageRuleCondition> copyConditions(
            List<DamageRuleCondition> conditions
    ) {
        Objects.requireNonNull(
                conditions,
                "conditions must not be null"
        );

        for (DamageRuleCondition condition : conditions) {
            Objects.requireNonNull(
                    condition,
                    "condition list must not contain null elements"
            );
        }

        return List.copyOf(conditions);
    }
}
