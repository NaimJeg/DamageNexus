package io.github.naimjeg.damagenexus.api.damage;

import io.github.naimjeg.damagenexus.api.DamageNexusAttributes;
import io.github.naimjeg.damagenexus.api.DamageNexusPreMultiplierBuckets;
import io.github.naimjeg.damagenexus.api.event.DamageSettledEvent;
import io.github.naimjeg.damagenexus.api.event.DamageSettlementCallback;
import io.github.naimjeg.damagenexus.api.event.DamageSettlementListener;
import io.github.naimjeg.damagenexus.api.context.DamageContextView;
import io.github.naimjeg.damagenexus.api.event.DamageNexusRegistrar;
import io.github.naimjeg.damagenexus.api.critical.*;
import io.github.naimjeg.damagenexus.api.rule.RuleExecutionContext;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditions;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.CompositeDamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperationIds;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleContribution;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleSource;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleSourceQuery;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.ICancellableEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageRequestValueTypesTest {

    @Test
    void lineageCreatesUniqueRootsAndExactChildren() {
        DamageLineage first = DamageLineage.newRoot();
        DamageLineage second = DamageLineage.newRoot();
        DamageLineage child = first.newChild();
        DamageLineage grandchild = child.newChild();

        assertNotEquals(first.damageId(), second.damageId());
        assertEquals(first.damageId(), first.rootDamageId());
        assertFalse(first.hasParent());
        assertEquals(0, first.recursionDepth());

        assertNotEquals(first.damageId(), child.damageId());
        assertEquals(first.rootDamageId(), child.rootDamageId());
        assertEquals(first.damageId(), child.parentDamageId().orElseThrow());
        assertEquals(1, child.recursionDepth());

        assertEquals(first.rootDamageId(), grandchild.rootDamageId());
        assertEquals(child.damageId(), grandchild.parentDamageId().orElseThrow());
        assertEquals(2, grandchild.recursionDepth());
    }

    @Test
    void metadataIsTypedAndStructurallyImmutable() {
        DamageMetadataKey<Boolean> flag =
                DamageMetadataKey.booleanKey(id("flag"));
        DamageMetadataKey<Identifier> action =
                DamageMetadataKey.identifierKey(id("action"));
        DamageMetadataKey<UUID> owner =
                DamageMetadataKey.uuidKey(id("owner"));
        UUID ownerId = UUID.randomUUID();

        DamageMetadata metadata = DamageMetadata.builder()
                .put(flag, true)
                .put(action, id("value"))
                .put(owner, ownerId)
                .build();

        assertEquals(true, metadata.get(flag).orElseThrow());
        assertEquals(id("value"), metadata.get(action).orElseThrow());
        assertEquals(ownerId, metadata.get(owner).orElseThrow());
        assertThrows(
                UnsupportedOperationException.class,
                () -> metadata.keys().clear()
        );
    }

    @Test
    void metadataRejectsTypeAliasAndUnsafeNumbers() {
        Identifier shared = id("shared");
        DamageMetadata.Builder builder = DamageMetadata.builder()
                .put(DamageMetadataKey.booleanKey(shared), true);

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.put(
                        DamageMetadataKey.stringKey(shared),
                        "different type"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> DamageMetadata.builder().put(
                        DamageMetadataKey.doubleKey(id("number")),
                        Double.NaN
                )
        );
    }

    @Test
    void triggerPermissionsOnlyBecomeMoreRestrictive() {
        DamageTriggerPolicy child =
                DamageTriggerPolicy.ALL_ALLOWED.intersect(
                        new DamageTriggerPolicy(false, true, false)
                );

        assertTrue(child.procSuppressed());
        assertTrue(child.reflectionAllowed());
        assertFalse(child.thornsAllowed());
        assertEquals(
                DamageTriggerPolicy.NONE_ALLOWED,
                child.intersect(DamageTriggerPolicy.NONE_ALLOWED)
        );
    }

    @Test
    void sourceDescriptorCopiesPositionAndKeepsRegistryKey() {
        Vec3 position = new Vec3(1.0, 2.0, 3.0);
        DamageSourceDescriptor descriptor =
                DamageSourceDescriptor.positioned(
                        DamageTypes.GENERIC,
                        position
                );

        assertEquals(DamageTypes.GENERIC, descriptor.damageType());
        assertEquals(position, descriptor.sourcePosition().orElseThrow());
        assertNotSame(position, descriptor.sourcePosition().orElseThrow());
        assertThrows(
                IllegalArgumentException.class,
                () -> DamageSourceDescriptor.positioned(
                        DamageTypes.GENERIC,
                        new Vec3(Double.NaN, 0.0, 0.0)
                )
        );
    }

    @Test
    void requestRejectsInvalidBaseDamageBeforeRuntimeReferences() {
        DamageSourceDescriptor source =
                DamageSourceDescriptor.of(DamageTypes.GENERIC);

        assertThrows(
                IllegalArgumentException.class,
                () -> DamageRequest.builder(null, null, source, Float.NaN)
                        .build()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> DamageRequest.builder(
                        null,
                        null,
                        source,
                        Float.POSITIVE_INFINITY
                ).build()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> DamageRequest.builder(null, null, source, -0.01f)
                        .build()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> DamageRequest.builder(
                        null,
                        null,
                        source,
                        DamageRequest.MAX_BASE_DAMAGE + 1.0f
                ).build()
        );
    }

    @Test
    void requestAndResultStoreOnlyFinalInstanceFields() {
        assertTrue(Arrays.stream(DamageRequest.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .allMatch(field -> Modifier.isFinal(field.getModifiers())));
        assertTrue(Arrays.stream(DamageResult.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .allMatch(field -> Modifier.isFinal(field.getModifiers())));
    }

    @Test
    void childBuilderAcceptsOnlyCompletedSettlementParentReferences() {
        assertThrows(
                NoSuchMethodException.class,
                () -> DamageRequest.Builder.class.getMethod(
                        "parent",
                        DamageLineage.class
                )
        );
        assertThrows(
                NoSuchMethodException.class,
                () -> DamageRequest.Builder.class.getMethod(
                        "inheritFrom",
                        DamageRequest.class,
                        DamageInheritancePolicy.class
                )
        );
        assertEquals(
                DamageRequest.Builder.class,
                assertDoesNotThrow(() -> DamageRequest.Builder.class.getMethod(
                        "parent",
                        DamageParentRef.class
                )).getReturnType()
        );
        assertEquals(0, DamageParentRef.class.getConstructors().length);
        assertThrows(
                NoSuchMethodException.class,
                () -> DamageRequest.Builder.class.getMethod(
                        "attributionProvenance",
                        DamageAttributionProvenance.class
                )
        );
        assertThrows(
                NoSuchMethodException.class,
                () -> DamageRequest.Builder.class.getMethod(
                        "origin",
                        DamageOrigin.class
                )
        );
    }

    @Test
    void settlementEventIsNonCancelableAndSnapshotFieldsAreFinal() {
        assertFalse(ICancellableEvent.class.isAssignableFrom(
                DamageSettledEvent.class
        ));
        assertTrue(Arrays.stream(
                        DamageSettlementSnapshot.class.getDeclaredFields()
                )
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .allMatch(field -> Modifier.isFinal(field.getModifiers())));
        assertEquals(0, DamageSettlementSnapshot.class.getConstructors().length);
        assertFalse(Arrays.stream(
                        DamageSettlementSnapshot.class.getDeclaredFields())
                .anyMatch(field -> field.getType() == DamageParentRef.class));
        assertThrows(
                SecurityException.class,
                () -> DamageSettlementSnapshot.completeInternal(
                        null,
                        null,
                        null,
                        Map.of(),
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        false,
                        false,
                        false,
                        DamageSettlementStatus.APPLIED,
                        null,
                        null
                )
        );
    }

    @Test
    void observationTypesDoNotExposeChildAuthority() {
        assertThrows(NoSuchMethodException.class,
                () -> DamageSettlementSnapshot.class.getMethod("parentRef"));
        assertThrows(NoSuchMethodException.class,
                () -> DamageSettledEvent.class.getMethod("childAuthority"));
        assertThrows(NoSuchMethodException.class,
                () -> DamageSettledEvent.class.getMethod(
                        "createInternal",
                        DamageSettlementSnapshot.class,
                        DamageParentRef.class
                ));
        assertThrows(
                SecurityException.class,
                () -> DamageParentRef.createInternal(null)
        );
    }

    @Test
    void snapshotCompletionUsesClassIdentityAndMethodHandlesCannotHideCaller()
            throws Exception {
        assertFalse(Arrays.stream(
                        DamageSettlementSnapshot.class.getDeclaredFields()
                )
                .anyMatch(field -> field.getType() == String.class));

        MethodType completionType = MethodType.methodType(
                DamageSettlementSnapshot.class,
                DamageOrigin.class,
                net.minecraft.world.entity.LivingEntity.class,
                net.minecraft.server.level.ServerLevel.class,
                Map.class,
                float.class,
                float.class,
                float.class,
                float.class,
                float.class,
                float.class,
                float.class,
                float.class,
                boolean.class,
                boolean.class,
                boolean.class,
                DamageSettlementStatus.class,
                DamageFailureReason.class,
                String.class
        );
        var handle = MethodHandles.lookup().findStatic(
                DamageSettlementSnapshot.class,
                "completeInternal",
                completionType
        );
        Object[] arguments = {
                null, null, null, Map.of(),
                0.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 0.0f,
                false, false, false,
                DamageSettlementStatus.APPLIED, null, null
        };

        var method = DamageSettlementSnapshot.class.getMethod(
                "completeInternal",
                completionType.parameterArray()
        );
        InvocationTargetException reflected = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, arguments)
        );
        assertTrue(reflected.getCause() instanceof SecurityException);

        assertThrows(SecurityException.class, () -> {
            try {
                handle.invokeWithArguments(arguments);
            } catch (SecurityException exception) {
                throw exception;
            } catch (Throwable throwable) {
                throw new AssertionError(throwable);
            }
        });
    }

    @Test
    void requestAndSettlementApiSignaturesDoNotExposeCoreTypes() {
        List<Class<?>> apiTypes = List.of(
                DamageRequest.class,
                DamageRequest.Builder.class,
                DamageAttribution.class,
                DamageOrigin.class,
                DamageLineage.class,
                DamageParentRef.class,
                DamageResult.class,
                DamageSettlementSnapshot.class,
                DamageSettledEvent.class,
                DamageSettlementCallback.class,
                DamageSettlementListener.class,
                DamageAttributionQuery.class,
                DamageAttributionResolution.class,
                DamageAttributionResolver.class,
                DamageAttributionProvenance.class,
                DamageContextView.class,
                DamageNexusRegistrar.class,
                EquippedItemRuleSource.class,
                EquippedItemRuleSourceQuery.class,
                EquippedItemRuleContribution.class,
                RuleExecutionContext.class,
                DamageRequestKind.class,
                DamageRuleCondition.class,
                CompositeDamageRuleCondition.class,
                DamageNexusConditions.class,
                CriticalDecision.class,
                CriticalDecisionOutcome.class,
                CriticalDecisionContribution.class,
                CriticalDecisionContributionResult.class,
                CriticalDecisionSnapshot.class,
                CriticalDecisionProvider.class,
                CriticalDecisionCollector.class,
                DamageNexusAttributes.class,
                DamageNexusPreMultiplierBuckets.class,
                DamageNexusConditionIds.class,
                DamageNexusOperationIds.class,
                DamageApplicationBucket.class
        );

        for (Class<?> apiType : apiTypes) {
            Arrays.stream(apiType.getDeclaredConstructors())
                    .filter(constructor -> Modifier.isPublic(
                            constructor.getModifiers()
                    ))
                    .flatMap(constructor -> Arrays.stream(
                            constructor.getGenericParameterTypes()
                    ))
                    .forEach(type -> assertPublicApiType(apiType, type));
            Arrays.stream(apiType.getDeclaredFields())
                    .filter(field -> Modifier.isPublic(field.getModifiers())
                            || Modifier.isProtected(field.getModifiers()))
                    .map(Field::getGenericType)
                    .forEach(type -> assertPublicApiType(apiType, type));
            Arrays.stream(apiType.getDeclaredMethods())
                    .filter(method -> Modifier.isPublic(method.getModifiers())
                            || Modifier.isProtected(method.getModifiers()))
                    .forEach(method -> {
                        assertPublicApiType(
                                apiType,
                                method.getGenericReturnType()
                        );
                        Arrays.stream(method.getGenericParameterTypes())
                                .forEach(type -> assertPublicApiType(
                                        apiType,
                                        type
                                ));
                        Arrays.stream(method.getGenericExceptionTypes())
                                .forEach(type -> assertPublicApiType(apiType, type));
                    });
        }
    }

    private static void assertPublicApiType(Class<?> owner, Type type) {
        String name = type.getTypeName();
        assertFalse(
                name.contains("io.github.naimjeg.damagenexus.core.")
                        || name.contains("io.github.naimjeg.damagenexus.internal.")
                        || name.contains("io.github.naimjeg.damagenexus.registry.")
                        || name.contains("io.github.naimjeg.damagenexus.diagnostics.")
                        || name.contains("io.github.naimjeg.damagenexus.debug.")
                        || name.contains("io.github.naimjeg.damagenexus.test."),
                () -> owner.getName() + " exposes internal type " + name
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("examplemod", path);
    }

    private static <T> T allocate(Class<T> type) {
        try {
            Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
            Field field = unsafeType.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            return type.cast(unsafeType.getMethod(
                    "allocateInstance",
                    Class.class
            ).invoke(unsafe, type));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
