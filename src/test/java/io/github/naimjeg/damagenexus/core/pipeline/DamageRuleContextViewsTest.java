package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.damage.*;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageTypes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DamageRuleContextViewsTest {

    @Test
    void restrictedViewHidesTheConcreteDelegateAndDelegatesApiCalls() {
        DamageRuleContext delegate = testDelegate();
        DamageRuleContext restricted =
                DamageRuleContextViews.restricted(delegate);

        assertNotSame(delegate, restricted);
        assertFalse(restricted instanceof DamageNexusContext);
        assertTrue(restricted.isManaged());
        assertEquals(42L, restricted.damageId());
        assertEquals(DamagePhase.FINAL_OVERRIDE, restricted.currentPhase());
        assertEquals(
                DamageMutationResult.APPLIED,
                restricted.tryCancelDamage("test/source")
        );
        assertSame(
                restricted,
                DamageRuleContextViews.restricted(restricted)
        );
    }

    @Test
    void restrictedViewForwardsCompleteImmutableOriginWithoutInternalDowncast() {
        Identifier action = Identifier.fromNamespaceAndPath("viewtest", "action");
        Identifier tag = Identifier.fromNamespaceAndPath("viewtest", "tag");
        DamageLineage lineage = DamageLineage.newRoot();
        DamageOrigin origin = new DamageOrigin(
                lineage,
                DamageRequestKind.ENVIRONMENTAL,
                DamageAttribution.ENVIRONMENT,
                DamageSourceDescriptor.of(DamageTypes.GENERIC),
                1.0f,
                Optional.of(action),
                Set.of(tag),
                DamageTriggerPolicy.ALL_ALLOWED,
                DamageMetadata.empty()
        );
        DamageRuleContext delegate = (DamageRuleContext) Proxy.newProxyInstance(
                DamageRuleContext.class.getClassLoader(),
                new Class<?>[]{DamageRuleContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "origin" -> origin;
                    case "attribution" -> origin.attribution();
                    case "logicalAttacker", "directEntity", "effectOwner",
                         "equipmentOwner", "attacker" -> null;
                    case "requestKind" -> origin.requestKind();
                    case "lineage" -> origin.lineage();
                    case "actionId" -> origin.actionId();
                    case "sourceTags" -> origin.sourceTags();
                    case "metadata" -> origin.metadata();
                    case "attributionProvenance" -> origin.attributionProvenance();
                    default -> defaultValue(method.getReturnType());
                }
        );

        DamageRuleContext restricted = DamageRuleContextViews.restricted(delegate);
        assertFalse(restricted instanceof DamageNexusContext);
        assertSame(origin, restricted.origin());
        assertSame(origin.attribution(), restricted.attribution());
        assertNull(restricted.logicalAttacker());
        assertNull(restricted.logicalAttacker());
        assertEquals(DamageRequestKind.ENVIRONMENTAL, restricted.requestKind());
        assertSame(lineage, restricted.lineage());
        assertEquals(Optional.of(action), restricted.actionId());
        assertEquals(Set.of(tag), restricted.sourceTags());
        assertSame(origin.metadata(), restricted.metadata());
        assertEquals(
                DamageAttributionSource.PUBLIC_REQUEST,
                restricted.attributionProvenance().source()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> restricted.sourceTags().add(
                        Identifier.fromNamespaceAndPath("viewtest", "mutate")
                )
        );
    }

    private static DamageRuleContext testDelegate() {
        return (DamageRuleContext) Proxy.newProxyInstance(
                DamageRuleContext.class.getClassLoader(),
                new Class<?>[]{DamageRuleContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isManaged" -> true;
                    case "damageId" -> 42L;
                    case "currentPhase" -> DamagePhase.FINAL_OVERRIDE;
                    case "tryCancelDamage" -> DamageMutationResult.APPLIED;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }

        if (type == boolean.class) {
            return false;
        }

        if (type == long.class) {
            return 0L;
        }

        if (type == int.class) {
            return 0;
        }

        if (type == float.class) {
            return 0.0f;
        }

        throw new IllegalStateException("Unsupported primitive: " + type);
    }
}
