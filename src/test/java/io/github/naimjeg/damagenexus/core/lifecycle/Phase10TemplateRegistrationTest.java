package io.github.naimjeg.damagenexus.core.lifecycle;

import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.event.DamageNexusRegistrar;
import io.github.naimjeg.damagenexus.api.item.template.DamageNexusTemplates;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperations;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleStacking;
import io.github.naimjeg.damagenexus.api.rule.affix.*;
import io.github.naimjeg.damagenexus.api.rule.builder.DamageRuleBuilder;
import io.github.naimjeg.damagenexus.api.rule.entry.*;
import io.github.naimjeg.damagenexus.core.template.DamageTemplateRegistry;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class Phase10TemplateRegistrationTest {
    private DamageNexusRegistrationAccess access;

    @AfterEach
    void reset() {
        if (access != null) access.close();
        DamageNexusLifecycle.resetForTesting();
    }

    @Test
    void registersTypedExternalTemplatesAndFreezesImmutableLookup() {
        DamageNexusRegistrationSession session = session();
        DamageEntryDefinition z = entry("zmod", "entry");
        DamageEntryDefinition a = entry("amod", "entry");
        DamageAffixDefinition affix = affix("contentmod", "affix");
        session.registerEntryTemplate(z.id(), z);
        session.registerEntryTemplate(a.id(), a);
        session.registerAffixTemplate(affix.id(), affix);
        DamageTemplateRegistry.freeze(access);

        assertEquals(Optional.of(a), DamageNexusTemplates.entry(a.id()));
        assertEquals(Optional.of(affix), DamageNexusTemplates.affix(affix.id()));
        assertTrue(DamageNexusTemplates.entry(affix.id()).isEmpty());
        assertTrue(DamageNexusTemplates.affix(a.id()).isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> DamageTemplateRegistry.snapshot().entries()
                        .put(id("test", "x"), a));
        assertThrows(IllegalStateException.class,
                () -> session.registerEntryTemplate(
                        id("contentmod", "late"),
                        entry("contentmod", "late")));
    }

    @Test
    void mismatchDuplicatesAndExpiredRegistrarAreRejected() {
        DamageNexusRegistrationSession session = session();
        DamageEntryDefinition entry = entry("contentmod", "entry");
        session.registerEntryTemplate(entry.id(), entry);
        assertThrows(IllegalArgumentException.class,
                () -> session.registerEntryTemplate(entry.id(), entry));
        assertThrows(IllegalArgumentException.class,
                () -> session.registerEntryTemplate(
                        id("contentmod", "other"), entry));
        session.close();
        assertThrows(IllegalStateException.class,
                () -> session.registerAffixTemplate(
                        id("contentmod", "expired"),
                        affix("contentmod", "expired")));
    }

    @Test
    void entryAndAffixNamespacesAreTypedEvenForTheSameIdentifier() {
        DamageNexusRegistrationSession session = session();
        DamageEntryDefinition entry = entry("contentmod", "shared_id");
        DamageAffixDefinition affix = affix("contentmod", "shared_id");
        session.registerEntryTemplate(entry.id(), entry);
        session.registerAffixTemplate(affix.id(), affix);
        DamageTemplateRegistry.freeze(access);

        assertEquals(Optional.of(entry), DamageNexusTemplates.entry(entry.id()));
        assertEquals(Optional.of(affix), DamageNexusTemplates.affix(affix.id()));
    }

    @Test
    void invalidRulesAreRejectedWithoutPartialRegistration() {
        DamageNexusRegistrationSession session = session();
        Identifier invalidId = id("contentmod", "invalid_rule");
        DamageRuleDefinition wrongPhase = new DamageRuleDefinition(
                id("contentmod", "wrong_phase"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                List.of(),
                List.of(DamageNexusOperations.overrideFinalDamage(3.0f)),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty());
        DamageEntryDefinition invalid = new DamageEntryDefinition(
                invalidId, DamageEntryDisplay.EMPTY, DamageEntrySlot.ITEM,
                List.of(wrongPhase), DamageEntryStacking.STACK,
                Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> session.registerEntryTemplate(invalidId, invalid));

        DamageTemplateRegistry.freeze(access);
        assertTrue(DamageNexusTemplates.entry(invalidId).isEmpty());
    }

    private DamageNexusRegistrationSession session() {
        access = DamageNexusLifecycle.beginRegistering();
        return new DamageNexusRegistrationSession(access);
    }

    static DamageEntryDefinition entry(String namespace, String path) {
        Identifier id = id(namespace, path);
        return new DamageEntryDefinition(
                id, DamageEntryDisplay.EMPTY, DamageEntrySlot.ITEM,
                List.of(DamageRuleBuilder.offensive(
                                id(namespace, path + "_rule"))
                        .addBaseDamage(DamageChannel.UNTYPED_ID, 1.0f)
                        .build()),
                DamageEntryStacking.STACK, Optional.empty());
    }

    static DamageAffixDefinition affix(String namespace, String path) {
        Identifier id = id(namespace, path);
        return new DamageAffixDefinition(
                id, DamageAffixDisplay.EMPTY, DamageAffixSlot.ITEM,
                DamageAffixRarity.COMMON,
                List.of(entry(namespace, path + "_nested")),
                DamageAffixStacking.STACK, Optional.empty());
    }

    static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
