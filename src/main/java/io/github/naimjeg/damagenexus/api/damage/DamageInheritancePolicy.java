package io.github.naimjeg.damagenexus.api.damage;

/**
 * Declares which request description fields were inherited from a parent.
 *
 * <p>Lineage is always explicit. Inherited description fields are resolved
 * once at build time and caller-explicit fields override the corresponding
 * inherited fields regardless of Builder call order. Source tags are merged
 * as a deduplicated set and explicit metadata values replace inherited values
 * with the same typed key. Critical state, channel state, collected rules,
 * pipeline buckets, and settlement values are never inherited.</p>
 */
public enum DamageInheritancePolicy {
    NONE(false, false),
    SOURCE_METADATA(false, true),
    ATTRIBUTION_AND_SOURCE_METADATA(true, true);

    private final boolean attribution;
    private final boolean sourceMetadata;

    DamageInheritancePolicy(
            boolean attribution,
            boolean sourceMetadata
    ) {
        this.attribution = attribution;
        this.sourceMetadata = sourceMetadata;
    }

    public boolean inheritsAttribution() {
        return attribution;
    }

    public boolean inheritsSourceMetadata() {
        return sourceMetadata;
    }
}
