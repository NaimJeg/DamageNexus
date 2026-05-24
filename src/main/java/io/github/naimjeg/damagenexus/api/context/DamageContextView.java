package io.github.naimjeg.damagenexus.api.context;

import io.github.naimjeg.damagenexus.api.damage.DamageAttribution;
import io.github.naimjeg.damagenexus.api.damage.DamageAttributionProvenance;
import io.github.naimjeg.damagenexus.api.damage.DamageLineage;
import io.github.naimjeg.damagenexus.api.damage.DamageMetadata;
import io.github.naimjeg.damagenexus.api.damage.DamageOrigin;
import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.critical.CriticalDecisionSnapshot;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public interface DamageContextView {

    boolean isManaged();

    DamageOrigin origin();

    DamageAttribution attribution();

    @Nullable LivingEntity logicalAttacker();

    @Nullable Entity directEntity();

    @Nullable Entity effectOwner();

    @Nullable LivingEntity equipmentOwner();

    DamageRequestKind requestKind();

    DamageLineage lineage();

    Optional<Identifier> actionId();

    Set<Identifier> sourceTags();

    DamageMetadata metadata();

    DamageAttributionProvenance attributionProvenance();

    LivingEntity victim();

    DamageSource source();

    long damageId();

    DamageChannel getInitialChannel();

    /**
     * Returns whether the current damage packet contains positive damage in the
     * given channel at this point of the pipeline.
     */
    boolean hasActiveDamageInChannel(DamageChannel channel);

    DamagePhase currentPhase();

    /** Final frozen critical result once the CRITICAL_HIT sub-lifecycle begins. */
    boolean isCritical();

    /** Immutable view of this transaction's critical decision lifecycle. */
    CriticalDecisionSnapshot criticalDecision();

    boolean isDamageCancelled();
}
