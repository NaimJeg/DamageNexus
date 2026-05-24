package io.github.naimjeg.damagenexus.api.damage;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable, single-submission request for authoritative server damage.
 *
 * <p>The object contains no DamageNexus transaction, mutable channel state,
 * rule buckets, or processor state. Live entity references are revalidated by
 * {@code DamageNexusApi.submitDamage} immediately before execution.</p>
 */
public final class DamageRequest {

    public static final int MAX_SOURCE_TAGS = 64;
    public static final float MAX_BASE_DAMAGE =
            DamageRuleLimits.MAX_ABSOLUTE_DAMAGE_VALUE;

    private final ServerLevel level;
    private final LivingEntity target;
    private final DamageOrigin origin;
    private final DamageInheritancePolicy inheritancePolicy;
    private final DamageParentRef parentRef;

    private DamageRequest(Builder builder) {
        validateBaseDamage(builder.baseDamage);

        this.level = Objects.requireNonNull(
                builder.level,
                "Damage request level must not be null"
        );
        this.target = Objects.requireNonNull(
                builder.target,
                "Damage request target must not be null"
        );
        DamageSourceDescriptor source = Objects.requireNonNull(
                builder.source,
                "Damage source descriptor must not be null"
        );
        DamageRequestKind kind = Objects.requireNonNull(
                builder.kind,
                "Damage request kind must not be null"
        );
        this.inheritancePolicy = Objects.requireNonNull(
                builder.inheritancePolicy,
                "Damage inheritance policy must not be null"
        );
        this.parentRef = builder.parentRef;

        if (kind == DamageRequestKind.PRIMARY
                && builder.parentRef != null) {
            throw new IllegalArgumentException(
                    "PRIMARY damage cannot have a parent damage"
            );
        }

        if (requiresParent(kind) && builder.parentRef == null) {
            throw new IllegalArgumentException(
                    kind + " damage requires a parent damage lineage"
            );
        }

        if (builder.parentRef == null
                && inheritancePolicy != DamageInheritancePolicy.NONE) {
            throw new IllegalArgumentException(
                    "Damage inheritance requires a parent damage"
            );
        }

        Builder.ResolvedDescription resolved =
                builder.resolveDescription();
        Set<Identifier> sourceTags = resolved.sourceTags();
        if (sourceTags.size() > MAX_SOURCE_TAGS) {
            throw new IllegalArgumentException(
                    "Damage request exceeds maximum source tags: "
                            + MAX_SOURCE_TAGS
            );
        }

        DamageAttribution attribution = resolved.attribution();
        DamageLineage lineage = resolved.lineage();

        DamageTriggerPolicy triggerPolicy = builder.resolveTriggerPolicy();
        this.origin = new DamageOrigin(
                lineage,
                kind,
                attribution,
                source,
                builder.baseDamage,
                resolved.actionId(),
                sourceTags,
                triggerPolicy,
                resolved.metadata()
        );
    }

    public static Builder builder(
            ServerLevel level,
            LivingEntity target,
            DamageSourceDescriptor source
    ) {
        return new Builder(level, target, source);
    }

    public static Builder builder(
            ServerLevel level,
            LivingEntity target,
            DamageSourceDescriptor source,
            float baseDamage
    ) {
        return new Builder(level, target, source).baseDamage(baseDamage);
    }

    public ServerLevel level() {
        return level;
    }

    public LivingEntity target() {
        return target;
    }

    public DamageAttribution attribution() {
        return origin.attribution();
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

    public DamageSourceDescriptor source() {
        return origin.source();
    }

    public float baseDamage() {
        return origin.initialBaseDamage();
    }

    public Optional<Identifier> actionId() {
        return origin.actionId();
    }

    public Set<Identifier> sourceTags() {
        return origin.sourceTags();
    }

    public DamageRequestKind kind() {
        return origin.requestKind();
    }

    public DamageLineage lineage() {
        return origin.lineage();
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

    public DamageInheritancePolicy inheritancePolicy() {
        return inheritancePolicy;
    }

    /** Upper bound this request grants to children after it settles. */
    public DamageTriggerPolicy triggerPolicy() {
        return origin.triggerPolicy();
    }

    /**
     * Framework-only access to the callback-scoped parent authority used to
     * build this request. Submission still verifies exact current dispatch and
     * server identity.
     */
    @ApiStatus.Internal
    public @Nullable DamageParentRef parentRefInternal() {
        return parentRef;
    }

    public boolean procSuppressed() {
        return origin.triggerPolicy().procSuppressed();
    }

    public DamageMetadata metadata() {
        return origin.metadata();
    }

    public DamageOrigin origin() {
        return origin;
    }

    private static void validateBaseDamage(float amount) {
        if (!Float.isFinite(amount)) {
            throw new IllegalArgumentException(
                    "Damage request base damage must be finite"
            );
        }

        if (amount < 0.0f) {
            throw new IllegalArgumentException(
                    "Damage request base damage must not be negative"
            );
        }

        if (amount > MAX_BASE_DAMAGE) {
            throw new IllegalArgumentException(
                    "Damage request base damage exceeds maximum: "
                            + MAX_BASE_DAMAGE
            );
        }
    }

    private static boolean requiresParent(DamageRequestKind kind) {
        return switch (kind) {
            case PROC, DOT, REFLECTED, THORNS -> true;
            default -> false;
        };
    }

    public static final class Builder {

        private final ServerLevel level;
        private final LivingEntity target;
        private final DamageSourceDescriptor source;
        private final Set<Identifier> sourceTags = new LinkedHashSet<>();
        private final DamageMetadata.Builder metadata =
                DamageMetadata.builder();

        private float baseDamage;
        private Entity directEntity;
        private LivingEntity logicalAttacker;
        private Entity effectOwner;
        private LivingEntity equipmentOwner;
        private boolean directEntitySpecified;
        private boolean logicalAttackerSpecified;
        private boolean effectOwnerSpecified;
        private boolean equipmentOwnerSpecified;
        private Identifier actionId;
        private DamageRequestKind kind = DamageRequestKind.PRIMARY;
        private DamageParentRef parentRef;
        private DamageInheritancePolicy inheritancePolicy =
                DamageInheritancePolicy.NONE;
        private boolean inheritancePolicySpecified;
        private DamageTriggerPolicy triggerRestrictions =
                DamageTriggerPolicy.ALL_ALLOWED;

        private Builder(
                ServerLevel level,
                LivingEntity target,
                DamageSourceDescriptor source
        ) {
            this.level = level;
            this.target = target;
            this.source = source;
        }

        public Builder baseDamage(float amount) {
            this.baseDamage = amount;
            return this;
        }

        public Builder directEntity(@Nullable Entity entity) {
            this.directEntity = entity;
            this.directEntitySpecified = true;
            return this;
        }

        public Builder logicalAttacker(@Nullable LivingEntity attacker) {
            this.logicalAttacker = attacker;
            this.logicalAttackerSpecified = true;
            return this;
        }

        public Builder effectOwner(@Nullable Entity owner) {
            this.effectOwner = owner;
            this.effectOwnerSpecified = true;
            return this;
        }

        public Builder equipmentOwner(@Nullable LivingEntity owner) {
            this.equipmentOwner = owner;
            this.equipmentOwnerSpecified = true;
            return this;
        }

        public Builder attribution(DamageAttribution attribution) {
            DamageAttribution safe = Objects.requireNonNull(
                    attribution,
                    "Damage attribution must not be null"
            );
            this.directEntity = safe.directEntity();
            this.logicalAttacker = safe.logicalAttacker();
            this.effectOwner = safe.effectOwner();
            this.equipmentOwner = safe.equipmentOwner();
            this.directEntitySpecified = true;
            this.logicalAttackerSpecified = true;
            this.effectOwnerSpecified = true;
            this.equipmentOwnerSpecified = true;
            return this;
        }

        public Builder actionId(Identifier actionId) {
            this.actionId = Objects.requireNonNull(
                    actionId,
                    "Damage action id must not be null"
            );
            return this;
        }

        public Builder sourceTag(Identifier sourceTag) {
            this.sourceTags.add(Objects.requireNonNull(
                    sourceTag,
                    "Damage source tag must not be null"
            ));
            return this;
        }

        public Builder sourceTags(Iterable<Identifier> sourceTags) {
            Objects.requireNonNull(
                    sourceTags,
                    "Damage source tags must not be null"
            );
            for (Identifier sourceTag : sourceTags) {
                sourceTag(sourceTag);
            }
            return this;
        }

        public Builder kind(DamageRequestKind kind) {
            this.kind = Objects.requireNonNull(
                    kind,
                    "Damage request kind must not be null"
            );
            return this;
        }

        /**
         * Binds the one parent authority obtained from the current registered
         * APPLIED settlement callback. Reusing the same reference is idempotent;
         * binding a different reference is rejected immediately. Building a
         * request does not extend the authority beyond its callback.
         */
        public Builder parent(DamageParentRef parent) {
            bindParent(parent);
            return this;
        }

        /**
         * Binds the completed parent and declares build-time inheritance.
         * Repeating the same parent and policy is idempotent. Neither the
         * parent nor its policy can later be changed on this Builder.
         */
        public Builder inheritFrom(
                DamageParentRef parent,
                DamageInheritancePolicy policy
        ) {
            DamageParentRef safeParent = Objects.requireNonNull(
                    parent,
                    "Completed parent damage reference must not be null"
            );
            DamageInheritancePolicy safePolicy = Objects.requireNonNull(
                    policy,
                    "Damage inheritance policy must not be null"
            );

            bindParent(safeParent);
            if (inheritancePolicySpecified
                    && inheritancePolicy != safePolicy) {
                throw new IllegalStateException(
                        "Damage inheritance policy was already bound for "
                                + "this completed parent"
                );
            }
            this.inheritancePolicy = safePolicy;
            this.inheritancePolicySpecified = true;

            return this;
        }

        /**
         * Records an additional caller restriction on the downstream trigger
         * permissions of the completed request. This is not an override of
         * framework defaults: the final policy is resolved at build time and
         * cannot reopen permissions disabled by the final request kind, a
         * previous explicit restriction, or the parent settlement.
         */
        public Builder triggerPolicy(DamageTriggerPolicy policy) {
            this.triggerRestrictions = triggerRestrictions.intersect(
                    Objects.requireNonNull(
                            policy,
                            "Damage trigger policy must not be null"
                    )
            );
            return this;
        }

        /**
         * Restricts this request from producing PROC children. This records
         * only a caller restriction; it does not materialize a complete
         * policy, inspect the current kind, or inspect the parent.
         */
        public Builder suppressProcs() {
            this.triggerRestrictions = triggerRestrictions.intersect(
                    DamageTriggerPolicy.PROC_SUPPRESSED
            );
            return this;
        }

        public <T> Builder metadata(
                DamageMetadataKey<T> key,
                T value
        ) {
            metadata.put(key, value);
            return this;
        }

        public DamageRequest build() {
            return new DamageRequest(this);
        }

        DamageAttribution resolveAttribution() {
            DamageAttribution inherited = parentRef != null
                    && inheritancePolicy.inheritsAttribution()
                    ? parentRef.attribution()
                    : DamageAttribution.ENVIRONMENT;
            LivingEntity resolvedLogical = logicalAttackerSpecified
                    ? logicalAttacker
                    : inherited.logicalAttacker();
            Entity resolvedDirect = directEntitySpecified
                    ? directEntity
                    : inheritancePolicy.inheritsAttribution()
                            ? inherited.directEntity()
                            : resolvedLogical;
            Entity resolvedEffectOwner = effectOwnerSpecified
                    ? effectOwner
                    : inheritancePolicy.inheritsAttribution()
                            ? inherited.effectOwner()
                            : resolvedLogical;
            LivingEntity resolvedEquipmentOwner = equipmentOwnerSpecified
                    ? equipmentOwner
                    : inheritancePolicy.inheritsAttribution()
                            ? inherited.equipmentOwner()
                            : resolvedLogical;

            return new DamageAttribution(
                    resolvedDirect,
                    resolvedLogical,
                    resolvedEffectOwner,
                    resolvedEquipmentOwner
            );
        }

        ResolvedDescription resolveDescription() {
            return new ResolvedDescription(
                    resolveAttribution(),
                    resolveActionId(),
                    resolveSourceTags(),
                    resolveMetadata(),
                    parentRef == null
                            ? DamageLineage.newRoot()
                            : parentRef.lineage().newChild()
            );
        }

        DamageTriggerPolicy resolveTriggerPolicy() {
            return DamageTriggerPolicy.defaultsFor(kind)
                    .intersect(triggerRestrictions)
                    .intersect(parentRef == null
                            ? DamageTriggerPolicy.ALL_ALLOWED
                            : parentRef.triggerPolicy());
        }

        private void bindParent(DamageParentRef parent) {
            DamageParentRef safeParent = Objects.requireNonNull(
                    parent,
                    "Completed parent damage reference must not be null"
            );
            if (parentRef != null && parentRef != safeParent) {
                throw new IllegalStateException(
                        "A damage request builder cannot bind more than one "
                                + "completed parent settlement"
                );
            }
            parentRef = safeParent;
        }

        private Optional<Identifier> resolveActionId() {
            if (actionId != null) {
                return Optional.of(actionId);
            }
            return parentRef != null
                    && inheritancePolicy.inheritsSourceMetadata()
                    ? parentRef.actionId()
                    : Optional.empty();
        }

        private Set<Identifier> resolveSourceTags() {
            Set<Identifier> resolved = new LinkedHashSet<>();
            if (parentRef != null
                    && inheritancePolicy.inheritsSourceMetadata()) {
                resolved.addAll(parentRef.sourceTags());
            }
            resolved.addAll(sourceTags);
            return Collections.unmodifiableSet(resolved);
        }

        private DamageMetadata resolveMetadata() {
            DamageMetadata explicit = metadata.build();
            if (parentRef == null
                    || !inheritancePolicy.inheritsSourceMetadata()) {
                return explicit;
            }
            return parentRef.metadata()
                    .toBuilder()
                    .putAll(explicit)
                    .build();
        }

        record ResolvedDescription(
                DamageAttribution attribution,
                Optional<Identifier> actionId,
                Set<Identifier> sourceTags,
                DamageMetadata metadata,
                DamageLineage lineage
        ) {
        }
    }
}
