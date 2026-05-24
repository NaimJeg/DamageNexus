package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.damage.*;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.critical.CriticalDecisionSnapshot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.Objects;
import java.util.Set;

/**
 * Public-extension view of the internal damage context.
 *
 * <p>The implementation is deliberately package-private so third-party
 * processors, providers, conditions, and operations cannot downcast the public
 * {@link DamageRuleContext} they receive to {@link DamageNexusContext} and
 * bypass the guarded mutation API.</p>
 */
final class RestrictedDamageRuleContext implements DamageRuleContext {

    private final DamageRuleContext delegate;

    RestrictedDamageRuleContext(DamageRuleContext delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public boolean isManaged() {
        return delegate.isManaged();
    }

    @Override
    public DamageOrigin origin() {
        return delegate.origin();
    }

    @Override
    public DamageAttribution attribution() {
        return delegate.attribution();
    }

    @Override
    public @Nullable LivingEntity logicalAttacker() {
        return delegate.logicalAttacker();
    }

    @Override
    public @Nullable Entity directEntity() {
        return delegate.directEntity();
    }

    @Override
    public @Nullable Entity effectOwner() {
        return delegate.effectOwner();
    }

    @Override
    public @Nullable LivingEntity equipmentOwner() {
        return delegate.equipmentOwner();
    }

    @Override
    public DamageRequestKind requestKind() {
        return delegate.requestKind();
    }

    @Override
    public DamageLineage lineage() {
        return delegate.lineage();
    }

    @Override
    public Optional<Identifier> actionId() {
        return delegate.actionId();
    }

    @Override
    public Set<Identifier> sourceTags() {
        return delegate.sourceTags();
    }

    @Override
    public DamageMetadata metadata() {
        return delegate.metadata();
    }

    @Override
    public DamageAttributionProvenance attributionProvenance() {
        return delegate.attributionProvenance();
    }

    @Override
    public LivingEntity victim() {
        return delegate.victim();
    }

    @Override
    public DamageSource source() {
        return delegate.source();
    }

    @Override
    public long damageId() {
        return delegate.damageId();
    }

    @Override
    public DamageChannel getInitialChannel() {
        return delegate.getInitialChannel();
    }

    @Override
    public boolean hasActiveDamageInChannel(DamageChannel channel) {
        return delegate.hasActiveDamageInChannel(channel);
    }

    @Override
    public DamagePhase currentPhase() {
        return delegate.currentPhase();
    }

    @Override
    public boolean isCritical() {
        return delegate.isCritical();
    }

    @Override
    public CriticalDecisionSnapshot criticalDecision() {
        return delegate.criticalDecision();
    }

    @Override
    public boolean isDamageCancelled() {
        return delegate.isDamageCancelled();
    }

    @Override
    public DamageMutationResult tryAddBaseDamage(
            DamageChannel channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceId
    ) {
        return delegate.tryAddBaseDamage(channel, bucket, value, sourceId);
    }

    @Override
    public DamageMutationResult tryAddBaseDamage(
            DamageChannel channel,
            float value,
            String sourceId
    ) {
        return delegate.tryAddBaseDamage(channel, value, sourceId);
    }

    @Override
    public DamageMutationResult tryAddChannelPreMultiplier(
            DamageChannel channel,
            int modifierId,
            float value,
            String sourceId
    ) {
        return delegate.tryAddChannelPreMultiplier(
                channel,
                modifierId,
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddApplicationPreMultiplier(
            DamageApplicationBucket bucket,
            int modifierId,
            float value,
            String sourceId
    ) {
        return delegate.tryAddApplicationPreMultiplier(
                bucket,
                modifierId,
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddGlobalPreMultiplier(
            int modifierId,
            float value,
            String sourceId
    ) {
        return delegate.tryAddGlobalPreMultiplier(
                modifierId,
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddChannelPostMultiplier(
            DamageChannel channel,
            float value,
            String sourceId
    ) {
        return delegate.tryAddChannelPostMultiplier(channel, value, sourceId);
    }

    @Override
    public DamageMutationResult tryAddGlobalPostMultiplier(
            float value,
            String sourceId
    ) {
        return delegate.tryAddGlobalPostMultiplier(value, sourceId);
    }

    @Override
    public DamageMutationResult tryConvertDamage(
            DamageChannel from,
            DamageChannel to,
            float ratio,
            String sourceId
    ) {
        return delegate.tryConvertDamage(from, to, ratio, sourceId);
    }

    @Override
    public DamageMutationResult tryGainExtraDamage(
            DamageChannel from,
            DamageChannel to,
            float ratio,
            String sourceId
    ) {
        return delegate.tryGainExtraDamage(from, to, ratio, sourceId);
    }

    @Override
    public DamageMutationResult tryAddTrueDamage(
            DamageChannel channel,
            float value,
            String sourceId
    ) {
        return delegate.tryAddTrueDamage(channel, value, sourceId);
    }

    @Override
    public DamageMutationResult tryMultiplyArmorEffectiveness(
            float multiplier,
            String sourceId
    ) {
        return delegate.tryMultiplyArmorEffectiveness(multiplier, sourceId);
    }

    @Override
    public DamageMutationResult tryAddTemporaryResistance(
            DamageChannel channel,
            float rating,
            String sourceId
    ) {
        return delegate.tryAddTemporaryResistance(channel, rating, sourceId);
    }

    @Override
    public DamageMutationResult tryAddChannelMitigation(
            DamageChannel channel,
            float reductionPercent,
            String sourceId
    ) {
        return delegate.tryAddChannelMitigation(
                channel,
                reductionPercent,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddGlobalMitigation(
            float reductionPercent,
            String sourceId
    ) {
        return delegate.tryAddGlobalMitigation(reductionPercent, sourceId);
    }

    @Override
    public DamageMutationResult tryOverrideFinalDamage(
            float amount,
            String sourceId
    ) {
        return delegate.tryOverrideFinalDamage(amount, sourceId);
    }

    @Override
    public DamageMutationResult tryCancelDamage(String sourceId) {
        return delegate.tryCancelDamage(sourceId);
    }
}
