package io.github.naimjeg.damagenexus.core.lifecycle;

import io.github.naimjeg.damagenexus.api.critical.CriticalDecision;
import io.github.naimjeg.damagenexus.api.critical.CriticalDecisionProvider;
import io.github.naimjeg.damagenexus.core.critical.CriticalDecisionProviders;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Phase7RegistrationTest {
    private DamageNexusRegistrationAccess access;

    @AfterEach
    void reset() {
        if (access != null) access.close();
        DamageNexusLifecycle.resetForTesting();
    }

    @Test
    void fullExternalIdsAndPriorityOrderingAreDeterministic() {
        DamageNexusRegistrationSession session = session();
        session.registerCriticalDecisionProvider(id("zmod", "same"), 5, force());
        session.registerCriticalDecisionProvider(id("amod", "same"), 5, force());
        session.registerCriticalDecisionProvider(id("midmod", "high"), 9, force());
        CriticalDecisionProviders.freeze(access);
        assertEquals(List.of(
                id("midmod", "high"), id("amod", "same"), id("zmod", "same")),
                CriticalDecisionProviders.orderedIds());
    }

    @Test
    void duplicatePriorityBoundsExpiryAndFreezeAreEnforced() {
        DamageNexusRegistrationSession session = session();
        Identifier id = id("contentmod", "decision");
        session.registerCriticalDecisionProvider(id, 0, force());
        assertThrows(IllegalArgumentException.class,
                () -> session.registerCriticalDecisionProvider(id, 1, force()));
        assertThrows(IllegalArgumentException.class,
                () -> session.registerCriticalDecisionProvider(
                        id("contentmod", "too_high"), 10_001, force()));
        CriticalDecisionProviders.freeze(access);
        assertThrows(IllegalStateException.class,
                () -> session.registerCriticalDecisionProvider(
                        id("contentmod", "late"), 0, force()));
        session.close();
        assertThrows(IllegalStateException.class,
                () -> session.registerCriticalDecisionProvider(
                        id("contentmod", "expired"), 0, force()));
    }

    private DamageNexusRegistrationSession session() {
        access = DamageNexusLifecycle.beginRegistering();
        return new DamageNexusRegistrationSession(access);
    }

    private static CriticalDecisionProvider force() {
        return (context, collector) ->
                collector.contribute(CriticalDecision.FORCE_CRITICAL);
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
