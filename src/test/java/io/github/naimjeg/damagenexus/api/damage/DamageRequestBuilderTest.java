package io.github.naimjeg.damagenexus.api.damage;

import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageTypes;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DamageRequestBuilderTest {

    private static final DamageSourceDescriptor SOURCE =
            DamageSourceDescriptor.of(DamageTypes.GENERIC);

    @Test
    void differentParentBindingsAreRejectedImmediately() {
        DamageParentRef parentA = parent("a");
        DamageParentRef parentB = parent("b");

        DamageRequest.Builder direct = builder().parent(parentA);
        assertThrows(IllegalStateException.class, () -> direct.parent(parentB));

        DamageRequest.Builder inherited = builder().inheritFrom(
                parentA,
                DamageInheritancePolicy.SOURCE_METADATA
        );
        assertThrows(IllegalStateException.class, () -> inherited.parent(parentB));

        DamageRequest.Builder reverse = builder().parent(parentA);
        assertThrows(
                IllegalStateException.class,
                () -> reverse.inheritFrom(
                        parentB,
                        DamageInheritancePolicy.SOURCE_METADATA
                )
        );
    }

    @Test
    void repeatedSameParentAndPolicyAreIdempotent() {
        DamageParentRef parent = parent("same");
        DamageRequest.Builder.ResolvedDescription resolved = builder()
                .inheritFrom(parent, DamageInheritancePolicy.SOURCE_METADATA)
                .inheritFrom(parent, DamageInheritancePolicy.SOURCE_METADATA)
                .parent(parent)
                .resolveDescription();

        assertEquals(parent.actionId(), resolved.actionId());
        assertEquals(parent.sourceTags(), resolved.sourceTags());
        assertThrows(
                IllegalStateException.class,
                () -> builder()
                        .inheritFrom(
                                parent,
                                DamageInheritancePolicy.SOURCE_METADATA
                        )
                        .inheritFrom(
                                parent,
                                DamageInheritancePolicy
                                        .ATTRIBUTION_AND_SOURCE_METADATA
                        )
        );
    }

    @Test
    void inheritanceResolutionIsIndependentOfMethodOrder() {
        DamageMetadataKey<String> key =
                DamageMetadataKey.stringKey(id("shared_metadata"));
        DamageMetadata parentMetadata = DamageMetadata.builder()
                .put(key, "parent")
                .build();
        DamageParentRef parent = parent(
                "ordered",
                DamageAttribution.ENVIRONMENT,
                Set.of(id("parent_tag"), id("shared_tag")),
                parentMetadata
        );

        DamageRequest.Builder.ResolvedDescription inheritedFirst = explicit(
                builder().inheritFrom(
                        parent,
                        DamageInheritancePolicy
                                .ATTRIBUTION_AND_SOURCE_METADATA
                ),
                key
        ).resolveDescription();
        DamageRequest.Builder.ResolvedDescription explicitFirst =
                builderWithExplicit(
                key
        ).inheritFrom(
                parent,
                DamageInheritancePolicy.ATTRIBUTION_AND_SOURCE_METADATA
        ).resolveDescription();

        assertEquals(inheritedFirst.actionId(), explicitFirst.actionId());
        assertEquals(inheritedFirst.sourceTags(), explicitFirst.sourceTags());
        assertEquals(
                inheritedFirst.metadata().get(key),
                explicitFirst.metadata().get(key)
        );
        assertEquals(inheritedFirst.attribution(), explicitFirst.attribution());
        assertEquals(Optional.of(id("explicit_action")), inheritedFirst.actionId());
        assertEquals(
                Set.of(
                        id("parent_tag"),
                        id("shared_tag"),
                        id("explicit_tag")
                ),
                inheritedFirst.sourceTags()
        );
        assertEquals("explicit", inheritedFirst.metadata().get(key).orElseThrow());
    }

    @Test
    void childLineageIsExactAndEveryBuildAllocatesANewDamageId() {
        DamageParentRef parent = parent("lineage");
        DamageRequest.Builder builder = builder().parent(parent);
        DamageLineage first = builder.resolveDescription().lineage();
        DamageLineage second = builder.resolveDescription().lineage();

        assertEquals(parent.lineage().rootDamageId(), first.rootDamageId());
        assertEquals(
                parent.lineage().damageId(),
                first.parentDamageId().orElseThrow()
        );
        assertEquals(
                parent.lineage().recursionDepth() + 1,
                first.recursionDepth()
        );
        assertNotEquals(first.damageId(), second.damageId());
    }

    @Test
    void sourceTagsAreMergedDeduplicatedAndImmutable() {
        DamageParentRef parent = parent("tags");
        DamageRequest.Builder.ResolvedDescription resolved = builder()
                .inheritFrom(parent, DamageInheritancePolicy.SOURCE_METADATA)
                .sourceTag(id("tags_tag"))
                .sourceTag(id("child_tag"))
                .sourceTag(id("child_tag"))
                .resolveDescription();

        assertEquals(
                Set.of(id("tags_tag"), id("child_tag")),
                resolved.sourceTags()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> resolved.sourceTags().clear()
        );
    }

    @Test
    void procSuppressionIsIndependentOfKindCallOrder() {
        DamageTriggerPolicy suppressionFirst = builder()
                .suppressProcs()
                .kind(DamageRequestKind.PROC)
                .resolveTriggerPolicy();
        DamageTriggerPolicy kindFirst = builder()
                .kind(DamageRequestKind.PROC)
                .suppressProcs()
                .resolveTriggerPolicy();

        assertEquals(suppressionFirst, kindFirst);
        assertEquals(DamageTriggerPolicy.PROC_SUPPRESSED, suppressionFirst);
    }

    @Test
    void explicitPolicyIsIndependentOfKindCallOrder() {
        for (DamageRequestKind kind : DamageRequestKind.values()) {
            DamageTriggerPolicy policyFirst = builder()
                    .triggerPolicy(DamageTriggerPolicy.ALL_ALLOWED)
                    .kind(kind)
                    .resolveTriggerPolicy();
            DamageTriggerPolicy kindFirst = builder()
                    .kind(kind)
                    .triggerPolicy(DamageTriggerPolicy.ALL_ALLOWED)
                    .resolveTriggerPolicy();

            assertEquals(policyFirst, kindFirst, kind.name());
            assertEquals(
                    DamageTriggerPolicy.defaultsFor(kind),
                    policyFirst,
                    kind.name()
            );
        }

        DamageTriggerPolicy first = builder()
                .triggerPolicy(DamageTriggerPolicy.REFLECTION_SUPPRESSED)
                .suppressProcs()
                .resolveTriggerPolicy();
        DamageTriggerPolicy reverse = builder()
                .suppressProcs()
                .triggerPolicy(DamageTriggerPolicy.REFLECTION_SUPPRESSED)
                .resolveTriggerPolicy();
        assertEquals(first, reverse);
    }

    @Test
    void procKindDefaultCannotBeReopenedByAllAllowed() {
        assertEquals(
                DamageTriggerPolicy.PROC_SUPPRESSED,
                builder().kind(DamageRequestKind.PROC)
                        .triggerPolicy(DamageTriggerPolicy.ALL_ALLOWED)
                        .resolveTriggerPolicy()
        );
    }

    @Test
    void reflectedKindDefaultCannotBeReopenedByAllAllowed() {
        assertEquals(
                DamageTriggerPolicy.REFLECTION_SUPPRESSED,
                builder().kind(DamageRequestKind.REFLECTED)
                        .triggerPolicy(DamageTriggerPolicy.ALL_ALLOWED)
                        .resolveTriggerPolicy()
        );
    }

    @Test
    void thornsKindDefaultCannotBeReopenedByAllAllowed() {
        assertEquals(
                DamageTriggerPolicy.THORNS_SUPPRESSED,
                builder().kind(DamageRequestKind.THORNS)
                        .triggerPolicy(DamageTriggerPolicy.ALL_ALLOWED)
                        .resolveTriggerPolicy()
        );
    }

    @Test
    void parentAndEarlierRestrictionsCannotBeReopened() {
        DamageParentRef closedParent = parent(
                "closed",
                DamageTriggerPolicy.NONE_ALLOWED
        );
        assertEquals(
                DamageTriggerPolicy.NONE_ALLOWED,
                builder().parent(closedParent)
                        .triggerPolicy(DamageTriggerPolicy.ALL_ALLOWED)
                        .resolveTriggerPolicy()
        );
        assertEquals(
                DamageTriggerPolicy.NONE_ALLOWED,
                builder().triggerPolicy(DamageTriggerPolicy.NONE_ALLOWED)
                        .triggerPolicy(DamageTriggerPolicy.ALL_ALLOWED)
                        .resolveTriggerPolicy()
        );
    }

    private static DamageRequest.Builder explicit(
            DamageRequest.Builder builder,
            DamageMetadataKey<String> key
    ) {
        return builder
                .actionId(id("explicit_action"))
                .sourceTag(id("shared_tag"))
                .sourceTag(id("explicit_tag"))
                .metadata(key, "explicit");
    }

    private static DamageRequest.Builder builderWithExplicit(
            DamageMetadataKey<String> key
    ) {
        return explicit(builder(), key);
    }

    private static DamageRequest.Builder builder() {
        return DamageRequest.builder(
                null,
                null,
                SOURCE,
                1.0f
        ).kind(DamageRequestKind.CUSTOM);
    }

    private static DamageParentRef parent(String name) {
        DamageMetadataKey<String> key =
                DamageMetadataKey.stringKey(id("metadata_" + name));
        return parent(
                name,
                DamageAttribution.ENVIRONMENT,
                Set.of(id(name + "_tag")),
                DamageMetadata.builder().put(key, name).build()
        );
    }

    private static DamageParentRef parent(
            String name,
            DamageTriggerPolicy policy
    ) {
        return new DamageParentRef(new DamageOrigin(
                DamageLineage.newRoot(),
                DamageRequestKind.PRIMARY,
                DamageAttribution.ENVIRONMENT,
                SOURCE,
                1.0f,
                Optional.empty(),
                Set.of(),
                policy,
                DamageMetadata.empty()
        ));
    }

    private static DamageParentRef parent(
            String name,
            DamageAttribution attribution,
            Set<Identifier> sourceTags,
            DamageMetadata metadata
    ) {
        return new DamageParentRef(new DamageOrigin(
                DamageLineage.newRoot(),
                DamageRequestKind.PRIMARY,
                attribution,
                SOURCE,
                1.0f,
                Optional.of(id(name + "_action")),
                sourceTags,
                DamageTriggerPolicy.ALL_ALLOWED,
                metadata
        ));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("examplemod", path);
    }

}
