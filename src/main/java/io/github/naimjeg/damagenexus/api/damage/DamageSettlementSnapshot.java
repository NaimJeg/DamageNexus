package io.github.naimjeg.damagenexus.api.damage;

import io.github.naimjeg.damagenexus.api.critical.CriticalDecisionSnapshot;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable public snapshot of a completed managed damage transaction.
 *
 * <p>{@code resolvedDamage} is DamageNexus' calculated result.
 * {@code appliedDamage} is NeoForge's authoritative inflicted amount captured
 * after {@code LivingDamageEvent.Pre}. {@code healthDamage} is the observed
 * health loss, computed as {@code max(0, healthBefore - healthAfter)}.
 * {@code absorptionDamage} is the absorption loss observed as
 * {@code max(0, absorptionBefore - absorptionAfter)}. These values are
 * intentionally distinct; neither state delta replaces the authoritative
 * inflicted amount. {@link #criticalDecision()} is unresolved only when the
 * pipeline never reached the private Phase 7 decision lifecycle; otherwise it
 * is the frozen, immutable decision used by {@link #critical()}.</p>
 */
public final class DamageSettlementSnapshot {

    private static final StackWalker CALLER_WALKER =
            StackWalker.getInstance(
                    StackWalker.Option.RETAIN_CLASS_REFERENCE
            );
    private final DamageOrigin origin;
    private final LivingEntity target;
    private final ServerLevel level;
    private final Map<Identifier, Float> resolvedChannelDamage;
    private final float resolvedDamage;
    private final float appliedDamage;
    private final float healthDamage;
    private final float absorptionDamage;
    private final float healthBefore;
    private final float healthAfter;
    private final float absorptionBefore;
    private final float absorptionAfter;
    private final boolean critical;
    private final CriticalDecisionSnapshot criticalDecision;
    private final boolean cancelled;
    private final boolean pipelineExecuted;
    private final DamageSettlementStatus status;
    private final Optional<DamageFailureReason> reason;
    private final Optional<String> cancellationSourceId;

    private DamageSettlementSnapshot(
            DamageOrigin origin,
            LivingEntity target,
            ServerLevel level,
            Map<Identifier, Float> resolvedChannelDamage,
            float resolvedDamage,
            float appliedDamage,
            float healthDamage,
            float absorptionDamage,
            float healthBefore,
            float healthAfter,
            float absorptionBefore,
            float absorptionAfter,
            boolean critical,
            CriticalDecisionSnapshot criticalDecision,
            boolean cancelled,
            boolean pipelineExecuted,
            DamageSettlementStatus status,
            @Nullable DamageFailureReason reason,
            @Nullable String cancellationSourceId
    ) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.target = Objects.requireNonNull(target, "target");
        this.level = Objects.requireNonNull(level, "level");
        this.resolvedChannelDamage = copyChannels(resolvedChannelDamage);
        this.resolvedDamage = requireAmount(resolvedDamage, "resolvedDamage");
        this.appliedDamage = requireAmount(appliedDamage, "appliedDamage");
        this.healthDamage = requireAmount(healthDamage, "healthDamage");
        this.absorptionDamage = requireAmount(
                absorptionDamage,
                "absorptionDamage"
        );
        this.healthBefore = requireAmount(healthBefore, "healthBefore");
        this.healthAfter = requireAmount(healthAfter, "healthAfter");
        this.absorptionBefore = requireAmount(
                absorptionBefore,
                "absorptionBefore"
        );
        this.absorptionAfter = requireAmount(
                absorptionAfter,
                "absorptionAfter"
        );
        this.critical = critical;
        this.criticalDecision = Objects.requireNonNull(
                criticalDecision, "criticalDecision");
        if (criticalDecision.frozen()
                && criticalDecision.critical() != critical) {
            throw new IllegalArgumentException(
                    "Critical boolean must match the frozen decision snapshot");
        }
        this.cancelled = cancelled;
        this.pipelineExecuted = pipelineExecuted;
        this.status = Objects.requireNonNull(status, "status");
        this.reason = Optional.ofNullable(reason);
        this.cancellationSourceId = Optional.ofNullable(cancellationSourceId);

        if ((status == DamageSettlementStatus.APPLIED)
                == this.reason.isPresent()) {
            throw new IllegalArgumentException(
                    "Applied settlements cannot have a failure reason and "
                            + "not-applied settlements require one"
            );
        }
    }

    /**
     * Framework-only completion boundary. External consumers obtain snapshots
     * from {@code DamageSettledEvent} or {@link DamageResult}; ordinary direct,
     * reflective, and method-handle calls are rejected and cannot create
     * authoritative settlement data. Snapshots never carry child-request
     * authority.
     *
     * <p>The caller check uses the actual framework state class identity, not
     * a class-name string. Mods in the same JVM remain trusted Java code: this
     * boundary prevents accidental public-API misuse and does not claim to
     * resist malicious reflection, bytecode injection, or JVM instrumentation.
     */
    @ApiStatus.Internal
    public static DamageSettlementSnapshot completeInternal(
            DamageOrigin origin,
            LivingEntity target,
            ServerLevel level,
            Map<Identifier, Float> resolvedChannelDamage,
            float resolvedDamage,
            float appliedDamage,
            float healthDamage,
            float absorptionDamage,
            float healthBefore,
            float healthAfter,
            float absorptionBefore,
            float absorptionAfter,
            boolean critical,
            boolean cancelled,
            boolean pipelineExecuted,
            DamageSettlementStatus status,
            @Nullable DamageFailureReason reason,
        @Nullable String cancellationSourceId
    ) {
        Class<?> caller = CALLER_WALKER.getCallerClass();
        if (caller != DamageSettlementState.class) {
            throw new SecurityException(
                    "Only the completed managed settlement state may create "
                            + "an authoritative snapshot"
            );
        }
        return new DamageSettlementSnapshot(
                origin,
                target,
                level,
                resolvedChannelDamage,
                resolvedDamage,
                appliedDamage,
                healthDamage,
                absorptionDamage,
                healthBefore,
                healthAfter,
                absorptionBefore,
                absorptionAfter,
                critical,
                CriticalDecisionSnapshot.unresolved(),
                cancelled,
                pipelineExecuted,
                status,
                reason,
                cancellationSourceId
        );
    }

    /** Framework-only completion overload carrying the frozen Phase 7 decision. */
    @ApiStatus.Internal
    public static DamageSettlementSnapshot completeInternal(
            DamageOrigin origin,
            LivingEntity target,
            ServerLevel level,
            Map<Identifier, Float> resolvedChannelDamage,
            float resolvedDamage,
            float appliedDamage,
            float healthDamage,
            float absorptionDamage,
            float healthBefore,
            float healthAfter,
            float absorptionBefore,
            float absorptionAfter,
            boolean critical,
            CriticalDecisionSnapshot criticalDecision,
            boolean cancelled,
            boolean pipelineExecuted,
            DamageSettlementStatus status,
            @Nullable DamageFailureReason reason,
            @Nullable String cancellationSourceId
    ) {
        Class<?> caller = CALLER_WALKER.getCallerClass();
        if (caller != DamageSettlementState.class) {
            throw new SecurityException(
                    "Only the completed managed settlement state may create an authoritative snapshot");
        }
        return new DamageSettlementSnapshot(
                origin, target, level, resolvedChannelDamage,
                resolvedDamage, appliedDamage, healthDamage, absorptionDamage,
                healthBefore, healthAfter, absorptionBefore, absorptionAfter,
                critical, criticalDecision, cancelled, pipelineExecuted,
                status, reason, cancellationSourceId
        );
    }

    public DamageOrigin origin() {
        return origin;
    }

    public DamageLineage lineage() {
        return origin.lineage();
    }

    public long damageId() {
        return origin.lineage().damageId();
    }

    public long rootDamageId() {
        return origin.lineage().rootDamageId();
    }

    public java.util.OptionalLong parentDamageId() {
        return origin.lineage().parentDamageId();
    }

    public int recursionDepth() {
        return origin.lineage().recursionDepth();
    }

    public DamageRequestKind requestKind() {
        return origin.requestKind();
    }

    public DamageAttribution attribution() {
        return origin.attribution();
    }

    public DamageAttributionProvenance attributionProvenance() {
        return origin.attributionProvenance();
    }

    public DamageAttributionSource attributionSource() {
        return origin.attributionSource();
    }

    public Optional<Identifier> attributionResolverId() {
        return origin.attributionResolverId();
    }

    public @Nullable Entity directEntity() {
        return origin.attribution().directEntity();
    }

    public @Nullable LivingEntity logicalAttacker() {
        return origin.attribution().logicalAttacker();
    }

    public @Nullable Entity effectOwner() {
        return origin.attribution().effectOwner();
    }

    public @Nullable LivingEntity equipmentOwner() {
        return origin.attribution().equipmentOwner();
    }

    public LivingEntity target() {
        return target;
    }

    public ServerLevel level() {
        return level;
    }

    public DamageSourceDescriptor source() {
        return origin.source();
    }

    public float initialBaseDamage() {
        return origin.initialBaseDamage();
    }

    public Optional<Identifier> actionId() {
        return origin.actionId();
    }

    public java.util.Set<Identifier> sourceTags() {
        return origin.sourceTags();
    }

    public DamageTriggerPolicy triggerPolicy() {
        return origin.triggerPolicy();
    }

    public DamageMetadata metadata() {
        return origin.metadata();
    }

    public Map<Identifier, Float> resolvedChannelDamage() {
        return resolvedChannelDamage;
    }

    public float resolvedDamage() {
        return resolvedDamage;
    }

    public float appliedDamage() {
        return appliedDamage;
    }

    public float healthDamage() {
        return healthDamage;
    }

    public float absorptionDamage() {
        return absorptionDamage;
    }

    public float healthBefore() {
        return healthBefore;
    }

    public float healthAfter() {
        return healthAfter;
    }

    public float absorptionBefore() {
        return absorptionBefore;
    }

    public float absorptionAfter() {
        return absorptionAfter;
    }

    public boolean critical() {
        return critical;
    }

    public CriticalDecisionSnapshot criticalDecision() {
        return criticalDecision;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public boolean pipelineExecuted() {
        return pipelineExecuted;
    }

    public DamageSettlementStatus status() {
        return status;
    }

    public Optional<DamageFailureReason> reason() {
        return reason;
    }

    public Optional<String> cancellationSourceId() {
        return cancellationSourceId;
    }

    private static Map<Identifier, Float> copyChannels(
            Map<Identifier, Float> channels
    ) {
        if (channels == null || channels.isEmpty()) {
            return Map.of();
        }

        Map<Identifier, Float> copy = new LinkedHashMap<>();
        channels.forEach((id, amount) -> copy.put(
                Objects.requireNonNull(id, "channel id"),
                requireAmount(amount, "resolved channel damage")
        ));
        return Collections.unmodifiableMap(copy);
    }

    private static float requireAmount(float amount, String name) {
        if (!Float.isFinite(amount) || amount < 0.0f) {
            throw new IllegalArgumentException(
                    name + " must be finite and non-negative"
            );
        }
        return amount;
    }
}
