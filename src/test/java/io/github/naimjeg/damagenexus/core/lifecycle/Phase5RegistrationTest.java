package io.github.naimjeg.damagenexus.core.lifecycle;

import io.github.naimjeg.damagenexus.api.damage.DamageAttributionResolver;
import io.github.naimjeg.damagenexus.api.event.DamageNexusRegistrar;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleSource;
import io.github.naimjeg.damagenexus.core.attribution.DamageAttributionResolvers;
import io.github.naimjeg.damagenexus.core.rule.ExternalItemRuleSources;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Phase5RegistrationTest {

    private DamageNexusRegistrationAccess access;

    @AfterEach
    void reset() {
        if (access != null) access.close();
        DamageNexusLifecycle.resetForTesting();
    }

    @Test
    void resolverIdsKeepExternalNamespacesAndSortDeterministically() {
        DamageNexusRegistrar registrar = registrar();
        registrar.registerAttributionResolver(id("zmod", "same"), 4, emptyResolver());
        registrar.registerAttributionResolver(id("amod", "same"), 4, emptyResolver());
        registrar.registerAttributionResolver(id("midmod", "high"), 9, emptyResolver());
        DamageAttributionResolvers.freeze(access);

        assertEquals(
                List.of(
                        id("midmod", "high"),
                        id("amod", "same"),
                        id("zmod", "same")
                ),
                DamageAttributionResolvers.orderedIds()
        );
    }

    @Test
    void duplicateIdsPrioritiesExpiryAndFreezeAreEnforced() {
        DamageNexusRegistrationSession session = session();
        Identifier resolverId = id("contentmod", "proxy");
        Identifier sourceId = id("slotmod", "rings");
        session.registerAttributionResolver(resolverId, 0, emptyResolver());
        session.registerEquippedItemRuleSource(sourceId, 0, emptySource());

        assertThrows(
                IllegalArgumentException.class,
                () -> session.registerAttributionResolver(
                        resolverId, 1, emptyResolver()
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> session.registerEquippedItemRuleSource(
                        sourceId, 1, emptySource()
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> session.registerAttributionResolver(
                        id("contentmod", "bad_priority"), 10_001, emptyResolver()
                )
        );

        DamageAttributionResolvers.freeze(access);
        ExternalItemRuleSources.freeze(access);
        assertThrows(
                IllegalStateException.class,
                () -> session.registerAttributionResolver(
                        id("contentmod", "late"), 0, emptyResolver()
                )
        );

        session.close();
        assertThrows(
                IllegalStateException.class,
                () -> session.registerEquippedItemRuleSource(
                        id("slotmod", "expired"), 0, emptySource()
                )
        );
    }

    @Test
    void externalSourcesSortByPriorityThenFullIdentifier() {
        DamageNexusRegistrar registrar = registrar();
        registrar.registerEquippedItemRuleSource(id("zmod", "belt"), 2, emptySource());
        registrar.registerEquippedItemRuleSource(id("amod", "ring"), 2, emptySource());
        registrar.registerEquippedItemRuleSource(id("bmod", "charm"), 5, emptySource());
        ExternalItemRuleSources.freeze(access);

        assertEquals(
                List.of(
                        id("bmod", "charm"),
                        id("amod", "ring"),
                        id("zmod", "belt")
                ),
                ExternalItemRuleSources.orderedIds()
        );
    }

    private DamageNexusRegistrar registrar() {
        return session();
    }

    private DamageNexusRegistrationSession session() {
        access = DamageNexusLifecycle.beginRegistering();
        return new DamageNexusRegistrationSession(access);
    }

    private static DamageAttributionResolver emptyResolver() {
        return query -> Optional.empty();
    }

    private static EquippedItemRuleSource emptySource() {
        return query -> List.of();
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
