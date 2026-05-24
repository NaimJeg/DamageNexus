package io.github.naimjeg.damagenexus.core.lifecycle;

import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.item.template.DamageNexusTemplates;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.builder.DamageRuleBuilder;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistryTestAccess;
import io.github.naimjeg.damagenexus.core.template.DamageTemplateRegistry;
import io.github.naimjeg.damagenexus.core.template.DamageTemplateSnapshot;
import io.github.naimjeg.damagenexus.core.template.DatapackDamageTemplateReloadListener;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RegistryDependencyReadinessTest {
    private static final Identifier CUSTOM_CHANNEL = id("custom_channel");
    private DamageNexusRegistrationAccess access;
    private DamageNexusRegistrationSession session;

    @BeforeEach
    void setup() {
        DamageChannelRegistryTestAccess.reset();
        access = DamageNexusLifecycle.beginRegistering();
        session = new DamageNexusRegistrationSession(access);
    }

    @AfterEach
    void reset() {
        session.close();
        access.close();
        DamageNexusLifecycle.resetForTesting();
        DamageChannelRegistryTestAccess.reset();
    }

    @Test
    void javaFreezeIsLookupOnlyUntilFirstChannelAwareReload() {
        DamageEntryDefinition definition = entry("java", DamageChannel.UNTYPED_ID);
        session.registerEntryTemplate(definition.id(), definition);
        DamageTemplateRegistry.freeze(access);

        assertEquals(Optional.of(definition),
                DamageNexusTemplates.entry(definition.id()));
        assertFalse(DamageNexusTemplates.serverExecutionReady());
        assertEquals(-1L, DamageNexusTemplates.validatedChannelRevision());
        assertFalse(DamageTemplateRegistry.executionSnapshot()
                .serverAuthoritative());

        assertTrue(DatapackDamageTemplateReloadListener
                .applyPreparedForTesting(Map.of(), Map.of()));
        assertTrue(DamageNexusTemplates.serverExecutionReady());
        assertEquals(DamageChannelRegistry.contentRevision(),
                DamageNexusTemplates.validatedChannelRevision());
    }

    @Test
    void unknownJavaChannelRejectsFirstReloadAndRemainsFailClosed() {
        DamageEntryDefinition definition = entry("unknown", CUSTOM_CHANNEL);
        session.registerEntryTemplate(definition.id(), definition);
        DamageTemplateRegistry.freeze(access);
        long revision = DamageTemplateRegistry.revision();

        assertFalse(DatapackDamageTemplateReloadListener
                .applyPreparedForTesting(Map.of(), Map.of()));
        assertEquals(revision, DamageTemplateRegistry.revision());
        assertFalse(DamageNexusTemplates.serverExecutionReady());
        assertFalse(DamageTemplateRegistry.executionSnapshot()
                .serverAuthoritative());
    }

    @Test
    void changedChannelMakesOldSnapshotFailClosedButPinnedSnapshotStable() {
        Map<Identifier, DamageChannelRegistry.ChannelDefinition> channels =
                channels(CUSTOM_CHANNEL);
        DamageChannelRegistryTestAccess.replace(channels);
        DamageEntryDefinition definition = entry("custom", CUSTOM_CHANNEL);
        session.registerEntryTemplate(definition.id(), definition);
        DamageTemplateRegistry.freeze(access);
        assertTrue(DatapackDamageTemplateReloadListener
                .applyPreparedForTesting(Map.of(), Map.of()));
        DamageTemplateSnapshot pinned =
                DamageTemplateRegistry.executionSnapshot();
        assertTrue(pinned.serverAuthoritative());

        long channelRevision = DamageChannelRegistry.contentRevision();
        DamageChannelRegistryTestAccess.replace(channels);
        assertEquals(channelRevision, DamageChannelRegistry.contentRevision(),
                "equal channel content must not churn revisions");
        assertTrue(DamageNexusTemplates.serverExecutionReady());

        DamageChannelRegistryTestAccess.replace(Map.of());
        assertTrue(pinned.serverAuthoritative(),
                "an already pinned transaction snapshot must stay immutable");
        assertFalse(DamageNexusTemplates.serverExecutionReady());
        assertFalse(DamageTemplateRegistry.executionSnapshot()
                .serverAuthoritative());
    }

    @Test
    void failedTemplateReloadKeepsOldSnapshotOnlyWhileDependencyMatches() {
        DamageEntryDefinition definition = entry("stable", DamageChannel.UNTYPED_ID);
        session.registerEntryTemplate(definition.id(), definition);
        DamageTemplateRegistry.freeze(access);
        assertTrue(DatapackDamageTemplateReloadListener
                .applyPreparedForTesting(Map.of(), Map.of()));
        DamageTemplateSnapshot previous = DamageTemplateRegistry.snapshot();

        DamageEntryDefinition mismatch = entry("definition_id",
                DamageChannel.UNTYPED_ID);
        assertFalse(DatapackDamageTemplateReloadListener
                .applyPreparedForTesting(
                        Map.of(id("file_id"), mismatch), Map.of()));
        assertSame(previous, DamageTemplateRegistry.snapshot());
        assertTrue(DamageNexusTemplates.serverExecutionReady());

        DamageChannelRegistryTestAccess.replace(channels(CUSTOM_CHANNEL));
        assertSame(previous, DamageTemplateRegistry.snapshot());
        assertFalse(DamageNexusTemplates.serverExecutionReady());
    }

    @Test
    void listenerGraphDeclaresBothChannelDependencies() {
        List<DamageNexusResourceReloadHandler.ReloadDependency> edges =
                DamageNexusResourceReloadHandler
                        .requiredDependenciesForTesting();
        assertEquals(2, edges.size());
        assertTrue(edges.stream().anyMatch(edge ->
                edge.first().getPath().equals("channel_registry")
                        && edge.second().getPath()
                        .equals("global_damage_rules")));
        assertTrue(edges.stream().anyMatch(edge ->
                edge.first().getPath().equals("channel_registry")
                        && edge.second().getPath()
                        .equals("static_damage_templates")));
    }

    private static DamageEntryDefinition entry(
            String path,
            Identifier channel
    ) {
        Identifier id = id(path);
        DamageRuleDefinition rule = DamageRuleBuilder.offensive(id(path + "_rule"))
                .addBaseDamage(channel, 1.0f)
                .build();
        return new DamageEntryDefinition(
                id, DamageEntryDisplay.EMPTY, DamageEntrySlot.ITEM,
                List.of(rule), DamageEntryStacking.STACK, Optional.empty());
    }

    private static Map<Identifier, DamageChannelRegistry.ChannelDefinition>
    channels(Identifier channel) {
        return Map.of(id("channel_file"),
                new DamageChannelRegistry.ChannelDefinition(
                        channel, List.of(), Optional.empty(), true, 0));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("readinessmod", path);
    }
}
