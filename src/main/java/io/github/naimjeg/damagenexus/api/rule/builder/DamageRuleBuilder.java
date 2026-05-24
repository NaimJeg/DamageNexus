package io.github.naimjeg.damagenexus.api.rule.builder;

import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;
import io.github.naimjeg.damagenexus.api.rule.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DamageRuleBuilder {

    private final Identifier id;
    private final List<DamageRuleCondition> conditions = new ArrayList<>();
    private final List<DamageRuleOperation> operations = new ArrayList<>();
    private DamageRuleRole role;
    private DamagePhase phase = DamagePhase.BASE_MODIFICATION;
    private int priority = 500;
    private DamageRuleStacking stacking = DamageRuleStacking.STACK;
    private Optional<Identifier> stackingGroup = Optional.empty();
    private Optional<String> traceLabel = Optional.empty();

    private DamageRuleBuilder(
            Identifier id,
            DamageRuleRole role
    ) {
        this.id = Objects.requireNonNull(
                id,
                "Damage rule id must not be null"
        );

        this.role = Objects.requireNonNull(
                role,
                "Damage rule role must not be null"
        );
    }

    public static DamageRuleBuilder offensive(Identifier id) {
        return new DamageRuleBuilder(id, DamageRuleRole.OFFENSIVE);
    }

    public static DamageRuleBuilder defensive(Identifier id) {
        return new DamageRuleBuilder(id, DamageRuleRole.DEFENSIVE);
    }

    public static DamageRuleBuilder any(Identifier id) {
        return new DamageRuleBuilder(id, DamageRuleRole.ANY);
    }

    public DamageRuleBuilder role(DamageRuleRole role) {
        this.role = Objects.requireNonNull(
                role,
                "Damage rule role must not be null"
        );
        return this;
    }

    public DamageRuleBuilder phase(DamagePhase phase) {
        this.phase = Objects.requireNonNull(
                phase,
                "Damage rule phase must not be null"
        );
        return this;
    }

    public DamageRuleBuilder baseModification() {
        return phase(DamagePhase.BASE_MODIFICATION);
    }

    public DamageRuleBuilder typeScaling() {
        return phase(DamagePhase.TYPE_SCALING);
    }

    public DamageRuleBuilder criticalHit() {
        return phase(DamagePhase.CRITICAL_HIT);
    }

    public DamageRuleBuilder conditionalMultiplier() {
        return phase(DamagePhase.CONDITIONAL_MULTI);
    }

    public DamageRuleBuilder globalAdjustment() {
        return phase(DamagePhase.GLOBAL_ADJUSTMENT);
    }

    public DamageRuleBuilder mitigationSetup() {
        return phase(DamagePhase.MITIGATION_SETUP);
    }

    public DamageRuleBuilder finalOverride() {
        return phase(DamagePhase.FINAL_OVERRIDE);
    }


    public DamageRuleBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    public DamageRuleBuilder when(DamageRuleCondition condition) {
        this.conditions.add(Objects.requireNonNull(
                condition,
                "Damage rule condition must not be null"
        ));
        return this;
    }

    public DamageRuleBuilder always() {
        return when(DamageNexusConditions.always());
    }

    public DamageRuleBuilder critical() {
        return when(DamageNexusConditions.critical());
    }

    public DamageRuleBuilder targetOnFire() {
        return when(DamageNexusConditions.targetOnFire());
    }

    /*
     * ---------------------------------------------------------------------
     * Condition shortcuts
     * ---------------------------------------------------------------------
     */

    public DamageRuleBuilder whenAllOf(
            List<DamageRuleCondition> conditions
    ) {
        return when(DamageNexusConditions.allOf(conditions));
    }

    public DamageRuleBuilder whenAllOf(
            DamageRuleCondition... conditions
    ) {
        return when(DamageNexusConditions.allOf(conditions));
    }

    public DamageRuleBuilder whenAnyOf(
            List<DamageRuleCondition> conditions
    ) {
        return when(DamageNexusConditions.anyOf(conditions));
    }

    public DamageRuleBuilder whenAnyOf(
            DamageRuleCondition... conditions
    ) {
        return when(DamageNexusConditions.anyOf(conditions));
    }

    public DamageRuleBuilder whenNot(
            DamageRuleCondition condition
    ) {
        return when(DamageNexusConditions.not(condition));
    }

    public DamageRuleBuilder damageChannelIs(
            Identifier channel
    ) {
        return when(DamageNexusConditions.damageChannelIs(channel));
    }

    /*
     * ---------------------------------------------------------------------
     * Health conditions
     * ---------------------------------------------------------------------
     */

    public DamageRuleBuilder attackerHealthBelow(float ratio) {
        return when(DamageNexusConditions.attackerHealthBelow(ratio));
    }

    public DamageRuleBuilder attackerHealthAbove(float ratio) {
        return when(DamageNexusConditions.attackerHealthAbove(ratio));
    }

    public DamageRuleBuilder targetHealthBelow(float ratio) {
        return when(DamageNexusConditions.targetHealthBelow(ratio));
    }

    public DamageRuleBuilder targetHealthAbove(float ratio) {
        return when(DamageNexusConditions.targetHealthAbove(ratio));
    }

    /*
     * ---------------------------------------------------------------------
     * Mob-effect conditions
     * ---------------------------------------------------------------------
     */

    public DamageRuleBuilder attackerHasEffect(
            Identifier effect
    ) {
        return when(DamageNexusConditions.attackerHasEffect(effect));
    }

    public DamageRuleBuilder targetHasEffect(
            Identifier effect
    ) {
        return when(DamageNexusConditions.targetHasEffect(effect));
    }

    public DamageRuleBuilder attackerEffectTag(TagKey<MobEffect> tag) {
        return when(DamageNexusConditions.attackerEffectTag(tag));
    }

    public DamageRuleBuilder targetEffectTag(TagKey<MobEffect> tag) {
        return when(DamageNexusConditions.targetEffectTag(tag));
    }

    public DamageRuleBuilder sourceActionIs(Identifier action) {
        return when(DamageNexusConditions.sourceActionIs(action));
    }

    public DamageRuleBuilder sourceTag(Identifier tag) {
        return when(DamageNexusConditions.sourceTag(tag));
    }

    public DamageRuleBuilder requestKindIs(DamageRequestKind kind) {
        return when(DamageNexusConditions.requestKindIs(kind));
    }

    public DamageRuleBuilder isPrimaryDamage() {
        return when(DamageNexusConditions.isPrimaryDamage());
    }

    public DamageRuleBuilder isProcDamage() {
        return when(DamageNexusConditions.isProcDamage());
    }

    public DamageRuleBuilder hasParentDamage() {
        return when(DamageNexusConditions.hasParentDamage());
    }

    public DamageRuleBuilder procAllowed() {
        return when(DamageNexusConditions.procAllowed());
    }

    /*
     * ---------------------------------------------------------------------
     * Damage source / damage type conditions
     * ---------------------------------------------------------------------
     */

    public DamageRuleBuilder damageTypeIs(
            Identifier damageType
    ) {
        return when(DamageNexusConditions.damageTypeIs(damageType));
    }

    public DamageRuleBuilder damageTypeTag(
            TagKey<DamageType> tag
    ) {
        return when(DamageNexusConditions.damageTypeTag(tag));
    }

    /*
     * ---------------------------------------------------------------------
     * Target entity conditions
     * ---------------------------------------------------------------------
     */

    public DamageRuleBuilder targetEntityTypeIs(
            Identifier entityType
    ) {
        return when(DamageNexusConditions.targetEntityTypeIs(entityType));
    }

    public DamageRuleBuilder targetEntityTypeTag(
            TagKey<EntityType<?>> tag
    ) {
        return when(DamageNexusConditions.targetEntityTypeTag(tag));
    }

    public DamageRuleBuilder targetMobCategoryIs(
            MobCategory category
    ) {
        return when(DamageNexusConditions.targetMobCategoryIs(category));
    }

    public DamageRuleBuilder targetIsBoss() {
        return when(DamageNexusConditions.targetIsBoss());
    }

    /*
     * ---------------------------------------------------------------------
     * Attacker entity conditions
     * ---------------------------------------------------------------------
     */

    public DamageRuleBuilder attackerEntityTypeIs(
            Identifier entityType
    ) {
        return when(DamageNexusConditions.attackerEntityTypeIs(entityType));
    }

    public DamageRuleBuilder attackerEntityTypeTag(
            TagKey<EntityType<?>> tag
    ) {
        return when(DamageNexusConditions.attackerEntityTypeTag(tag));
    }

    public DamageRuleBuilder attackerMobCategoryIs(
            MobCategory category
    ) {
        return when(DamageNexusConditions.attackerMobCategoryIs(category));
    }

    public DamageRuleBuilder attackerIsBoss() {
        return when(DamageNexusConditions.attackerIsBoss());
    }

    public DamageRuleBuilder operation(DamageRuleOperation operation) {
        this.operations.add(Objects.requireNonNull(
                operation,
                "Damage rule operation must not be null"
        ));
        return this;
    }

    public DamageRuleBuilder addTrueDamage(
            Identifier channel,
            float value
    ) {
        return operation(DamageNexusOperations.addTrueDamage(channel, value));
    }

    public DamageRuleBuilder addBaseDamage(
            Identifier channel,
            float value
    ) {
        return operation(DamageNexusOperations.addBaseDamage(channel, value));
    }

    public DamageRuleBuilder addBaseDamage(
            Identifier channel,
            DamageApplicationBucket applicationBucket,
            float value
    ) {
        return operation(DamageNexusOperations.addBaseDamage(
                channel,
                applicationBucket,
                value
        ));
    }

    public DamageRuleBuilder addChannelPreMultiplier(
            Identifier channel,
            float value
    ) {
        return operation(DamageNexusOperations.addChannelPreMultiplier(channel, value));
    }

    public DamageRuleBuilder addChannelPreMultiplier(
            Identifier channel,
            Identifier preMultiplierBucket,
            float value
    ) {
        return operation(DamageNexusOperations.addChannelPreMultiplier(
                channel,
                preMultiplierBucket,
                value
        ));
    }

    public DamageRuleBuilder addChannelPostMultiplier(
            Identifier channel,
            float value
    ) {
        return operation(DamageNexusOperations.addChannelPostMultiplier(channel, value));
    }

    public DamageRuleBuilder addGlobalPreMultiplier(float value) {
        return operation(DamageNexusOperations.addGlobalPreMultiplier(value));
    }

    public DamageRuleBuilder addGlobalPreMultiplier(
            Identifier preMultiplierBucket,
            float value
    ) {
        return operation(DamageNexusOperations.addGlobalPreMultiplier(
                preMultiplierBucket,
                value
        ));
    }

    public DamageRuleBuilder addGlobalPostMultiplier(float value) {
        return operation(DamageNexusOperations.addGlobalPostMultiplier(value));
    }

    public DamageRuleBuilder convertDamage(
            Identifier from,
            Identifier to,
            float ratio
    ) {
        return operation(DamageNexusOperations.convertDamage(from, to, ratio));
    }

    public DamageRuleBuilder gainExtraDamage(
            Identifier basedOn,
            Identifier to,
            float ratio
    ) {
        return operation(DamageNexusOperations.gainExtraDamage(basedOn, to, ratio));
    }

    public DamageRuleBuilder addTemporaryResistance(
            Identifier channel,
            float value
    ) {
        return operation(DamageNexusOperations.addTemporaryResistance(channel, value));
    }

    public DamageRuleBuilder addChannelMitigation(
            Identifier channel,
            float value
    ) {
        return operation(DamageNexusOperations.addChannelMitigation(channel, value));
    }

    public DamageRuleBuilder addGlobalMitigation(float value) {
        return operation(DamageNexusOperations.addGlobalMitigation(value));
    }

    public DamageRuleBuilder multiplyArmorEffectiveness(float value) {
        return operation(DamageNexusOperations.multiplyArmorEffectiveness(value));
    }

    public DamageRuleBuilder overrideFinalDamage(float value) {
        return operation(DamageNexusOperations.overrideFinalDamage(value));
    }

    public DamageRuleBuilder cancelDamage() {
        return operation(DamageNexusOperations.cancelDamage());
    }

    public DamageRuleBuilder cancelDamage(String sourceId) {
        return operation(DamageNexusOperations.cancelDamage(sourceId));
    }

    public DamageRuleBuilder stacking(DamageRuleStacking stacking) {
        this.stacking = Objects.requireNonNull(
                stacking,
                "Damage rule stacking policy must not be null"
        );
        return this;
    }

    public DamageRuleBuilder stack() {
        return stacking(DamageRuleStacking.STACK);
    }

    public DamageRuleBuilder uniqueSource() {
        return stacking(DamageRuleStacking.UNIQUE_SOURCE);
    }

    public DamageRuleBuilder highestValue() {
        return stacking(DamageRuleStacking.HIGHEST_VALUE);
    }

    public DamageRuleBuilder lowestValue() {
        return stacking(DamageRuleStacking.LOWEST_VALUE);
    }

    public DamageRuleBuilder replace() {
        return stacking(DamageRuleStacking.REPLACE);
    }

    public DamageRuleBuilder stackingGroup(Identifier group) {
        this.stackingGroup = Optional.ofNullable(group);
        return this;
    }

    public DamageRuleBuilder trace(String traceLabel) {
        this.traceLabel = Optional.ofNullable(traceLabel);
        return this;
    }

    public DamageRuleDefinition build() {
        validateBuilderState();

        DamageRuleDefinition rule = new DamageRuleDefinition(
                id,
                role,
                phase,
                priority,
                List.copyOf(conditions),
                List.copyOf(operations),
                stacking,
                safeStackingGroup(),
                safeTraceLabel()
        );

        DamageRuleValidator.requireValid(
                rule,
                "java_api/builder"
        );

        return rule;
    }

    private void validateBuilderState() {
        if (id == null) {
            throw new IllegalStateException(
                    "DamageRuleBuilder cannot build rule with null id"
            );
        }

        if (role == null) {
            throw new IllegalStateException(
                    "DamageRuleBuilder cannot build rule with null role: " + id
            );
        }

        if (phase == null) {
            throw new IllegalStateException(
                    "DamageRuleBuilder cannot build rule with null phase: " + id
            );
        }

        if (stacking == null) {
            throw new IllegalStateException(
                    "DamageRuleBuilder cannot build rule with null stacking policy: " + id
            );
        }

        if (stackingGroup == null) {
            throw new IllegalStateException(
                    "DamageRuleBuilder cannot build rule with null stacking group optional: " + id
            );
        }

        if (traceLabel == null) {
            throw new IllegalStateException(
                    "DamageRuleBuilder cannot build rule with null trace label optional: " + id
            );
        }

        if (operations.isEmpty()) {
            throw new IllegalStateException(
                    "DamageRuleBuilder cannot build rule without operations: " + id
            );
        }

        for (DamageRuleCondition condition : conditions) {
            if (condition == null) {
                throw new IllegalStateException(
                        "DamageRuleBuilder cannot build rule with null condition: " + id
                );
            }
        }

        for (DamageRuleOperation operation : operations) {
            if (operation == null) {
                throw new IllegalStateException(
                        "DamageRuleBuilder cannot build rule with null operation: " + id
                );
            }
        }
    }

    private Optional<Identifier> safeStackingGroup() {
        return stackingGroup != null ? stackingGroup : Optional.empty();
    }

    private Optional<String> safeTraceLabel() {
        return traceLabel != null ? traceLabel : Optional.empty();
    }
}
