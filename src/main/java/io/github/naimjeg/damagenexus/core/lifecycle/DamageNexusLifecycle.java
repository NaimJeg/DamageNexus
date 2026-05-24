package io.github.naimjeg.damagenexus.core.lifecycle;

import io.github.naimjeg.damagenexus.core.attribution.DamageAttributionResolvers;
import io.github.naimjeg.damagenexus.core.critical.CriticalDecisionProviders;
import io.github.naimjeg.damagenexus.core.rule.ExternalItemRuleSources;

public final class DamageNexusLifecycle {

    private static DamageNexusLifecycleState state =
            DamageNexusLifecycleState.CONSTRUCTING;

    private DamageNexusLifecycle() {
    }

    public static synchronized DamageNexusLifecycleState state() {
        return state;
    }

    static synchronized DamageNexusRegistrationAccess beginRegistering() {
        transitionFrom(
                DamageNexusLifecycleState.CONSTRUCTING,
                DamageNexusLifecycleState.REGISTERING
        );

        return new DamageNexusRegistrationAccess();
    }

    static synchronized void freezeRegistration(
            DamageNexusRegistrationAccess access
    ) {
        requireRegistering(access, "freezeRegistration");
        transitionFrom(
                DamageNexusLifecycleState.REGISTERING,
                DamageNexusLifecycleState.FROZEN
        );
        access.close();
    }

    static synchronized void running() {
        transitionFrom(
                DamageNexusLifecycleState.FROZEN,
                DamageNexusLifecycleState.RUNNING
        );
    }

    public static synchronized void requireRegistering(
            DamageNexusRegistrationAccess access,
            String action
    ) {
        if (access == null) {
            throw new IllegalStateException(
                    "DamageNexus registration requires the active event "
                            + "registrar. action="
                            + action
                            + " state="
                            + state
            );
        }

        access.requireActive(action);

        if (state != DamageNexusLifecycleState.REGISTERING) {
            throw new IllegalStateException(
                    "DamageNexus registration is only allowed during "
                            + "DamageNexusRegisterEvent. action="
                            + action
                            + " state="
                            + state
            );
        }
    }

    static synchronized void resetForTesting() {
        state = DamageNexusLifecycleState.CONSTRUCTING;
        DamageAttributionResolvers.resetForTesting();
        ExternalItemRuleSources.resetForTesting();
        CriticalDecisionProviders.resetForTesting();
        io.github.naimjeg.damagenexus.core.settlement.DamageSettlementCallbacks
                .resetForTesting();
        io.github.naimjeg.damagenexus.core.template.DamageTemplateRegistry
                .resetForTesting();
        io.github.naimjeg.damagenexus.core.rule.DatapackDamageRuleStore
                .resetForTesting();
    }

    static synchronized void failBootstrap(
            DamageNexusRegistrationAccess access
    ) {
        if (access == null) {
            throw new IllegalStateException(
                    "DamageNexus bootstrap failure requires its registration "
                            + "capability. state="
                            + state
            );
        }

        if (state != DamageNexusLifecycleState.REGISTERING
                && state != DamageNexusLifecycleState.FROZEN) {
            throw invalidTransition(DamageNexusLifecycleState.FAILED);
        }

        access.close();
        state = DamageNexusLifecycleState.FAILED;
    }

    private static void transitionFrom(
            DamageNexusLifecycleState expected,
            DamageNexusLifecycleState next
    ) {
        if (state != expected) {
            throw invalidTransition(next);
        }

        state = next;
    }

    private static IllegalStateException invalidTransition(
            DamageNexusLifecycleState next
    ) {
        return new IllegalStateException(
                "Invalid DamageNexus lifecycle transition: "
                        + state
                        + " -> "
                        + next
        );
    }
}
