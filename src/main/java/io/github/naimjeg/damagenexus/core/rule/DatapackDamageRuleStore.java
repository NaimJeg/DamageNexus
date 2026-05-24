package io.github.naimjeg.damagenexus.core.rule;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.config.DiagnosticMode;
import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusDiagnosticState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Atomically published, read-only datapack rule snapshot.
 *
 * <p>Only the reload listener in this package can replace the snapshot.</p>
 */
@EventBusSubscriber(modid = DamageNexus.MODID)
public final class DatapackDamageRuleStore {

    private static volatile Snapshot snapshot = Snapshot.empty();

    private DatapackDamageRuleStore() {
    }

    public static List<DamageRuleDefinition> rules() {
        return executionSnapshot().rules;
    }

    public static List<DamageRuleDefinition> rules(DamagePhase phase) {
        if (phase == null) {
            return List.of();
        }

        return executionSnapshot().byPhase.getOrDefault(phase, List.of());
    }

    public static int ruleCount() {
        return executionSnapshot().rules.size();
    }

    static void replace(List<DamageRuleDefinition> nextRules) {
        replace(nextRules, DamageChannelRegistry.contentRevision());
    }

    static synchronized void replace(
            List<DamageRuleDefinition> nextRules,
            long validatedChannelRevision
    ) {
        long currentChannelRevision = DamageChannelRegistry.contentRevision();
        if (validatedChannelRevision != currentChannelRevision) {
            throw new IllegalStateException(
                    "global_rule_channel_revision_changed expected="
                            + validatedChannelRevision + " actual="
                            + currentChannelRevision);
        }
        snapshot = Snapshot.create(
                Math.addExact(snapshot.revision, 1L),
                validatedChannelRevision,
                true,
                nextRules);
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    /** Returns an immutable fail-closed snapshot for a new transaction. */
    public static Snapshot executionSnapshot() {
        Snapshot current = snapshot;
        long channelRevision = DamageChannelRegistry.contentRevision();
        if (current.isCompatibleWith(channelRevision)) {
            return current;
        }
        if (!current.rules.isEmpty()
                && DamageNexusSettings.diagnosticMode()
                != DiagnosticMode.OFF
                && DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.DATAPACK_RELOAD,
                "global_rules", "dependency_incompatible",
                current.revision + ":" + current.validatedChannelRevision
                        + ":" + channelRevision)) {
            DamageNexus.LOGGER.warn(
                    "[DamageNexus] Global datapack rules skipped because their channel dependency revision is incompatible; ruleRevision={} validatedChannelRevision={} currentChannelRevision={}",
                    current.revision,
                    current.validatedChannelRevision,
                    channelRevision);
        }
        return current.failClosed();
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static synchronized void resetForTesting() {
        snapshot = Snapshot.empty();
    }

    @SubscribeEvent
    public static synchronized void onServerStopping(
            ServerStoppingEvent event
    ) {
        snapshot = Snapshot.empty();
    }

    public record Snapshot(
            long revision,
            long validatedChannelRevision,
            boolean serverAuthoritative,
            List<DamageRuleDefinition> rules,
            Map<DamagePhase, List<DamageRuleDefinition>> byPhase
    ) {
        private static Snapshot empty() {
            return create(0L, -1L, false, List.of());
        }

        private static Snapshot create(
                long revision,
                long validatedChannelRevision,
                boolean serverAuthoritative,
                List<DamageRuleDefinition> nextRules
        ) {
            List<DamageRuleDefinition> immutable =
                    nextRules == null
                            ? List.of()
                            : List.copyOf(nextRules);
            Map<DamagePhase, List<DamageRuleDefinition>> byPhase =
                    new EnumMap<>(DamagePhase.class);

            for (DamagePhase phase : DamagePhase.values()) {
                byPhase.put(
                        phase,
                        immutable.stream()
                                .filter(rule -> rule.phase() == phase)
                                .toList()
                );
            }

            return new Snapshot(
                    revision,
                    validatedChannelRevision,
                    serverAuthoritative,
                    immutable,
                    Map.copyOf(byPhase)
            );
        }

        public List<DamageRuleDefinition> rules(DamagePhase phase) {
            if (phase == null) {
                return List.of();
            }
            return byPhase.getOrDefault(phase, List.of());
        }

        public boolean isCompatibleWith(long channelRevision) {
            return serverAuthoritative
                    && validatedChannelRevision == channelRevision;
        }

        private Snapshot failClosed() {
            if (!serverAuthoritative) {
                return this;
            }
            return create(
                    revision,
                    validatedChannelRevision,
                    false,
                    List.of());
        }
    }
}
