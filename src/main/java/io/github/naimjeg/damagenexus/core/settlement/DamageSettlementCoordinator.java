package io.github.naimjeg.damagenexus.core.settlement;

import io.github.naimjeg.damagenexus.config.DamageSafetySettings;
import io.github.naimjeg.damagenexus.core.request.DamageRequestSubmissionTracker;
import io.github.naimjeg.damagenexus.core.request.DamageTransactionActivity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;

/** Completion handoff shared by native and public-request damage. */
public final class DamageSettlementCoordinator {

    /*
     * One synchronous dispatch cannot lawfully enqueue more managed
     * completions than the hard per-server-tick admission ceiling. Keeping the
     * FIFO at that same ceiling makes it bounded without rejecting a legal
     * safety configuration after damage has already committed.
     */
    private static final int MAX_PENDING_SETTLEMENTS =
            DamageSafetySettings.HARD_MAX_MANAGED_REQUESTS_PER_SERVER_TICK;
    private static final ThreadLocal<Deque<DamageSettlementCompletion>> PENDING =
            new ThreadLocal<>();
    private static final ThreadLocal<Boolean> DRAINING = new ThreadLocal<>();

    private DamageSettlementCoordinator() {
    }

    public static void handoff(
            DamageSettlementCompletion completion,
            LivingEntity target,
            DamageSource source
    ) {
        if (completion == null) {
            return;
        }

        DamageRequestSubmissionTracker.attachSettlement(
                target,
                source,
                completion
        );

        enqueue(completion);
        drainIfSafe();
    }

    private static void enqueue(DamageSettlementCompletion completion) {
        Deque<DamageSettlementCompletion> pending = PENDING.get();
        if (pending == null) {
            pending = new ArrayDeque<>();
            PENDING.set(pending);
        }
        if (pending.size() >= MAX_PENDING_SETTLEMENTS) {
            throw new IllegalStateException(
                    "Damage settlement handoff capacity exceeded: "
                            + MAX_PENDING_SETTLEMENTS
            );
        }
        pending.addLast(completion);
    }

    public static void drainIfSafe() {
        if (DamageTransactionActivity.isActive()
                || DamageRequestSubmissionTracker.hasActiveSubmission()
                || DamageSettlementTracker.hasActiveHurt()
                || DamageSettlementDispatchScope.isActive()
                || Boolean.TRUE.equals(DRAINING.get())) {
            return;
        }

        Deque<DamageSettlementCompletion> pending = PENDING.get();
        if (pending == null) {
            return;
        }

        DRAINING.set(Boolean.TRUE);
        try {
            while (!pending.isEmpty()) {
                DamageSettlementEventPublisher.publish(
                        pending.removeFirst()
                );
            }
        } finally {
            DRAINING.remove();
            if (pending.isEmpty()) {
                PENDING.remove();
            }
        }
    }

    public static int pendingCountForTests() {
        Deque<DamageSettlementCompletion> pending = PENDING.get();
        return pending == null ? 0 : pending.size();
    }

    public static boolean drainingForTests() {
        return Boolean.TRUE.equals(DRAINING.get());
    }
}
