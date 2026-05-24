package io.github.naimjeg.damagenexus.core.settlement;

import io.github.naimjeg.damagenexus.api.damage.DamageOrigin;
import io.github.naimjeg.damagenexus.api.damage.DamageFailureReason;
import io.github.naimjeg.damagenexus.core.pipeline.DamageExecutionSummary;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;

/** Always-on, server-thread settlement correlation independent of diagnostics. */
public final class DamageSettlementTracker {

    private static final int MAX_ACTIVE_HURT_SCOPES = 64;
    private static final ThreadLocal<Deque<HurtScope>> ACTIVE_HURTS =
            new ThreadLocal<>();

    private DamageSettlementTracker() {
    }

    public static HurtScope openHurt(
            LivingEntity target,
            ServerLevel level,
            DamageSource source
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(source, "source");

        Deque<HurtScope> stack = ACTIVE_HURTS.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            ACTIVE_HURTS.set(stack);
        }
        if (stack.size() >= MAX_ACTIVE_HURT_SCOPES) {
            throw new IllegalStateException(
                    "Damage settlement hurt-scope capacity exceeded: "
                            + MAX_ACTIVE_HURT_SCOPES
            );
        }

        HurtScope scope = new HurtScope(target, level, source);
        stack.addLast(scope);
        return scope;
    }

    public static boolean beginIncoming(
            LivingIncomingDamageEvent event,
            DamageOrigin origin
    ) {
        HurtScope scope = matchingOpenScope(
                event.getEntity(),
                event.getSource()
        );
        if (scope == null
                || scope.state != null
                || event.getEntity().level() != scope.level) {
            return false;
        }

        scope.state = new DamageSettlementState(
                event,
                origin,
                event.getEntity(),
                scope.level
        );
        return true;
    }

    public static void recordCalculated(
            LivingIncomingDamageEvent event,
            DamageExecutionSummary summary
    ) {
        stateFor(event.getContainer()).calculated(summary);
    }

    public static boolean rejectAdmission(
            LivingIncomingDamageEvent event,
            DamageOrigin origin,
            DamageFailureReason reason
    ) {
        if (!beginIncoming(event, origin)) {
            return false;
        }
        stateFor(event.getContainer()).rejectAdmission(reason);
        return true;
    }

    public static boolean enforceAdmissionRejection(
            LivingIncomingDamageEvent event
    ) {
        DamageSettlementState state = findState(event.getContainer());
        if (state == null
                || state.phase()
                != DamageSettlementPhase.ADMISSION_REJECTED) {
            return false;
        }
        event.setCanceled(true);
        return true;
    }

    public static void markIncomingDispatchComplete(
            LivingIncomingDamageEvent event
    ) {
        DamageSettlementState state = findState(event.getContainer());
        if (state != null && event.isCanceled()) {
            state.markLateIncomingCancellation();
        }
    }

    public static void capturePre(LivingDamageEvent.Pre event) {
        DamageSettlementState state = findState(event.getContainer());
        if (state != null
                && state.phase() == DamageSettlementPhase.CALCULATED) {
            state.capturePre(event);
        }
    }

    public static void capturePost(LivingDamageEvent.Post event) {
        HurtScope scope = matchingScope(
                event.getEntity(),
                event.getSource()
        );
        if (scope == null
                || scope.state == null
                || scope.state.phase()
                != DamageSettlementPhase.PRE_APPLY_CAPTURED) {
            return;
        }
        scope.state.capturePost(event);
    }

    public static boolean hasActiveHurt() {
        Deque<HurtScope> stack = ACTIVE_HURTS.get();
        return stack != null && !stack.isEmpty();
    }

    public static int activeDepthForTests() {
        Deque<HurtScope> stack = ACTIVE_HURTS.get();
        return stack == null ? 0 : stack.size();
    }

    private static DamageSettlementState stateFor(DamageContainer container) {
        DamageSettlementState state = findState(container);
        if (state == null) {
            throw new IllegalStateException(
                    "DamageContainer is not associated with a managed hurt"
            );
        }
        return state;
    }

    private static @Nullable DamageSettlementState findState(
            DamageContainer container
    ) {
        Deque<HurtScope> stack = ACTIVE_HURTS.get();
        if (stack == null) {
            return null;
        }

        Iterator<HurtScope> iterator = stack.descendingIterator();
        while (iterator.hasNext()) {
            DamageSettlementState state = iterator.next().state;
            if (state != null && state.container() == container) {
                return state;
            }
        }
        return null;
    }

    private static @Nullable HurtScope matchingOpenScope(
            LivingEntity target,
            DamageSource source
    ) {
        HurtScope scope = matchingScope(target, source);
        return scope != null && scope.state == null ? scope : null;
    }

    private static @Nullable HurtScope matchingScope(
            LivingEntity target,
            DamageSource source
    ) {
        Deque<HurtScope> stack = ACTIVE_HURTS.get();
        if (stack == null) {
            return null;
        }

        Iterator<HurtScope> iterator = stack.descendingIterator();
        while (iterator.hasNext()) {
            HurtScope scope = iterator.next();
            if (scope.target == target && scope.source == source) {
                return scope;
            }
        }
        return null;
    }

    public static final class HurtScope implements AutoCloseable {

        private final LivingEntity target;
        private final ServerLevel level;
        private final DamageSource source;
        private DamageSettlementState state;
        private boolean completed;
        private boolean closed;

        private HurtScope(
                LivingEntity target,
                ServerLevel level,
                DamageSource source
        ) {
            this.target = target;
            this.level = level;
            this.source = source;
        }

        public @Nullable DamageSettlementCompletion complete(
                boolean vanillaAccepted
        ) {
            if (completed) {
                throw new IllegalStateException(
                        "Damage hurt scope completed more than once"
                );
            }
            completed = true;
            return state == null
                    ? null
                    : state.completeWithoutPost(vanillaAccepted);
        }

        public void abort() {
            if (state != null) {
                state.abort();
            }
        }

        public LivingEntity target() {
            return target;
        }

        public DamageSource source() {
            return source;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;

            Deque<HurtScope> stack = ACTIVE_HURTS.get();
            if (stack == null || stack.peekLast() != this) {
                throw new IllegalStateException(
                        "Damage hurt scope closed out of order"
                );
            }
            stack.removeLast();
            if (stack.isEmpty()) {
                ACTIVE_HURTS.remove();
            }
        }
    }
}
