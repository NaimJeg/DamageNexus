package io.github.naimjeg.damagenexus.api.damage;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable description of where one managed damage transaction originated.
 *
 * <p>This value describes both public {@link DamageRequest} submissions and
 * native damage entering through NeoForge. It never contains pipeline state,
 * mutable rule buckets, or a DamageNexus transaction.</p>
 */
public final class DamageOrigin {

    private final DamageLineage lineage;
    private final DamageRequestKind requestKind;
    private final DamageAttribution attribution;
    private final DamageSourceDescriptor source;
    private final float initialBaseDamage;
    private final Optional<Identifier> actionId;
    private final Set<Identifier> sourceTags;
    private final DamageTriggerPolicy triggerPolicy;
    private final DamageMetadata metadata;
    private final DamageAttributionProvenance attributionProvenance;

    public DamageOrigin(
            DamageLineage lineage,
            DamageRequestKind requestKind,
            DamageAttribution attribution,
            DamageSourceDescriptor source,
            float initialBaseDamage,
            Optional<Identifier> actionId,
            Set<Identifier> sourceTags,
            DamageTriggerPolicy triggerPolicy,
            DamageMetadata metadata
    ) {
        this(
                lineage,
                requestKind,
                attribution,
                source,
                initialBaseDamage,
                actionId,
                sourceTags,
                triggerPolicy,
                metadata,
                DamageAttributionProvenance.publicRequest()
        );
    }

    private DamageOrigin(
            DamageLineage lineage,
            DamageRequestKind requestKind,
            DamageAttribution attribution,
            DamageSourceDescriptor source,
            float initialBaseDamage,
            Optional<Identifier> actionId,
            Set<Identifier> sourceTags,
            DamageTriggerPolicy triggerPolicy,
            DamageMetadata metadata,
            DamageAttributionProvenance attributionProvenance
    ) {
        if (!Float.isFinite(initialBaseDamage) || initialBaseDamage < 0.0f) {
            throw new IllegalArgumentException(
                    "Damage origin base damage must be finite and non-negative"
            );
        }

        this.lineage = Objects.requireNonNull(lineage, "lineage");
        this.requestKind = Objects.requireNonNull(
                requestKind,
                "requestKind"
        );
        this.attribution = Objects.requireNonNull(
                attribution,
                "attribution"
        );
        this.source = Objects.requireNonNull(source, "source");
        this.initialBaseDamage = initialBaseDamage;
        this.actionId = actionId == null ? Optional.empty() : actionId;

        Set<Identifier> safeTags = sourceTags == null
                ? Set.of()
                : new LinkedHashSet<>(sourceTags);
        if (safeTags.size() > DamageRequest.MAX_SOURCE_TAGS
                || safeTags.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Damage origin contains invalid source tags"
            );
        }
        this.sourceTags = Collections.unmodifiableSet(safeTags);
        this.triggerPolicy = Objects.requireNonNull(
                triggerPolicy,
                "triggerPolicy"
        );
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.attributionProvenance = Objects.requireNonNull(
                attributionProvenance,
                "attributionProvenance"
        );
    }

    public DamageLineage lineage() {
        return lineage;
    }

    public DamageRequestKind requestKind() {
        return requestKind;
    }

    public DamageAttribution attribution() {
        return attribution;
    }

    public DamageSourceDescriptor source() {
        return source;
    }

    public float initialBaseDamage() {
        return initialBaseDamage;
    }

    public Optional<Identifier> actionId() {
        return actionId;
    }

    public Set<Identifier> sourceTags() {
        return sourceTags;
    }

    public DamageTriggerPolicy triggerPolicy() {
        return triggerPolicy;
    }

    public DamageMetadata metadata() {
        return metadata;
    }

    public DamageAttributionProvenance attributionProvenance() {
        return attributionProvenance;
    }

    public DamageAttributionSource attributionSource() {
        return attributionProvenance.source();
    }

    public Optional<Identifier> attributionResolverId() {
        return attributionProvenance.resolverId();
    }

    /**
     * Framework integration hook. Public request builders never expose this
     * provenance parameter, and the submission service replaces attribution
     * only with a registry-authored resolution.
     */
    @org.jetbrains.annotations.ApiStatus.Internal
    public DamageOrigin withResolvedAttribution(
            DamageAttribution resolvedAttribution,
            DamageAttributionProvenance provenance
    ) {
        return new DamageOrigin(
                lineage,
                requestKind,
                Objects.requireNonNull(resolvedAttribution, "resolvedAttribution"),
                source,
                initialBaseDamage,
                actionId,
                sourceTags,
                triggerPolicy,
                metadata,
                Objects.requireNonNull(provenance, "provenance")
        );
    }
}
