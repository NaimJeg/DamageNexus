package io.github.naimjeg.damagenexus.core.request;

import io.github.naimjeg.damagenexus.api.damage.DamageFailureReason;
import io.github.naimjeg.damagenexus.api.damage.DamageRequest;
import io.github.naimjeg.damagenexus.api.damage.DamageResult;
import io.github.naimjeg.damagenexus.api.damage.DamageOrigin;
import io.github.naimjeg.damagenexus.core.pipeline.DamageExecutionSummary;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementCompletion;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Short-lived server-thread association between a public request and the
 * vanilla Incoming event created by its hurtServer call.
 */
public final class DamageRequestSubmissionTracker {

    private static final ThreadLocal<Deque<Submission>> ACTIVE =
            new ThreadLocal<>();

    private DamageRequestSubmissionTracker() {
    }

    static Submission open(
            DamageRequest request,
            DamageSource source,
            DamageAdmissionResult admission,
            DamageOrigin origin
    ) {
        if (!Objects.requireNonNull(admission, "admission").admitted()) {
            throw new IllegalArgumentException(
                    "A rejected admission cannot open a submission"
            );
        }
        Submission submission = new Submission(
                request, source, admission, origin
        );
        Deque<Submission> stack = ACTIVE.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            ACTIVE.set(stack);
        }

        stack.addLast(submission);
        return submission;
    }

    public static boolean hasActiveSubmission() {
        Deque<Submission> stack = ACTIVE.get();
        return stack != null && !stack.isEmpty();
    }

    /**
     * Returns whether the current innermost public submission owns this exact
     * target/source pair. This is an identity check used by the hurtServer
     * boundary; it is deliberately narrower than {@link #hasActiveSubmission()}.
     */
    @ApiStatus.Internal
    public static boolean matchesActiveSubmission(
            LivingEntity target,
            DamageSource source
    ) {
        if (target == null || source == null) {
            return false;
        }
        Deque<Submission> stack = ACTIVE.get();
        Submission submission = stack == null ? null : stack.peekLast();
        return submission != null
                && submission.request.target() == target
                && submission.source == source;
    }

    public static boolean attachSettlement(
            net.minecraft.world.entity.LivingEntity target,
            DamageSource source,
            DamageSettlementCompletion completion
    ) {
        Deque<Submission> stack = ACTIVE.get();
        if (stack == null) {
            return false;
        }
        Submission submission = stack.peekLast();
        if (submission == null
                || submission.source != source
                || submission.request.target() != target) {
            return false;
        }
        if (submission.settlement != null) {
            throw new IllegalStateException(
                    "Damage request settlement was attached more than once"
            );
        }
        if (!completion.snapshot().lineage().equals(
                submission.request.lineage()
        )) {
            throw new IllegalStateException(
                    "Damage request settlement lineage does not match request"
            );
        }
        if (completion.snapshot().origin() != submission.origin) {
            throw new IllegalStateException(
                    "Damage request settlement did not retain the authoritative origin"
            );
        }
        submission.settlement = Objects.requireNonNull(
                completion,
                "completion"
        );
        return true;
    }

    public static @Nullable Claim claimIncoming(
            LivingIncomingDamageEvent event
    ) {
        Submission submission = matching(event);
        if (submission == null) {
            return null;
        }

        submission.claimed = true;
        return new Claim(
                submission.request,
                submission.origin,
                submission.admission
        );
    }

    public static void recordPipelineResult(
            LivingIncomingDamageEvent event,
            DamageExecutionSummary summary
    ) {
        Submission submission = matching(event);
        if (submission == null) {
            return;
        }

        if (submission.summary != null) {
            throw new IllegalStateException(
                    "Damage request pipeline result was recorded more than once"
            );
        }

        submission.summary = Objects.requireNonNull(
                summary,
                "Damage execution summary must not be null"
        );
    }

    static int activeDepthForTests() {
        Deque<Submission> stack = ACTIVE.get();
        return stack == null ? 0 : stack.size();
    }

    private static @Nullable Submission matching(
            LivingIncomingDamageEvent event
    ) {
        if (event == null) {
            return null;
        }

        Deque<Submission> stack = ACTIVE.get();
        if (stack == null) {
            return null;
        }
        Submission submission = stack.peekLast();

        if (submission == null
                || submission.source != event.getSource()
                || submission.request.target() != event.getEntity()) {
            return null;
        }

        return submission;
    }

    static final class Submission implements AutoCloseable {

        private final DamageRequest request;
        private final DamageSource source;
        private final DamageAdmissionResult admission;
        private final DamageOrigin origin;
        private @Nullable DamageExecutionSummary summary;
        private @Nullable DamageSettlementCompletion settlement;
        private boolean claimed;
        private boolean closed;

        private Submission(
                DamageRequest request,
                DamageSource source,
                DamageAdmissionResult admission,
                DamageOrigin origin
        ) {
            this.request = Objects.requireNonNull(request, "request");
            this.source = Objects.requireNonNull(source, "source");
            this.admission = Objects.requireNonNull(admission, "admission");
            this.origin = Objects.requireNonNull(origin, "origin");
        }

        DamageResult finish(boolean vanillaAccepted) {
            if (settlement != null) {
                return DamageResult.fromSettlement(
                        request,
                        settlement.snapshot(),
                        vanillaAccepted
                );
            }

            return DamageResult.failed(
                    request,
                    DamageFailureReason.PIPELINE_NOT_OBSERVED,
                    claimed && summary != null
                            ? "DamageNexus calculated the request but no "
                                    + "completed settlement was observed"
                            : "The vanilla damage call did not enter the "
                                    + "DamageNexus Incoming pipeline",
                    claimed && summary != null,
                    vanillaAccepted,
                    summary == null ? 0.0f : summary.resolvedDamage(),
                    summary != null && summary.critical(),
                    summary != null && summary.cancelled()
            );
        }

        @Nullable DamageSettlementCompletion settlement() {
            return settlement;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            Deque<Submission> stack = ACTIVE.get();
            if (stack == null) {
                throw new IllegalStateException(
                        "Damage request submission stack is missing"
                );
            }
            Submission active = stack.peekLast();

            if (active != this) {
                throw new IllegalStateException(
                        "Damage request submission stack closed out of order"
                );
            }

            stack.removeLast();
            closed = true;

            if (stack.isEmpty()) {
                ACTIVE.remove();
            }
        }
    }

    /** Public-request association claimed exactly once by its Incoming event. */
    public record Claim(
            DamageRequest request,
            DamageOrigin origin,
            DamageAdmissionResult admission
    ) {
        public Claim {
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(origin, "origin");
            if (!request.lineage().equals(origin.lineage())) {
                throw new IllegalArgumentException(
                        "Incoming claim origin lineage must match its request"
                );
            }
            if (!Objects.requireNonNull(admission, "admission").admitted()) {
                throw new IllegalArgumentException(
                        "Incoming claim requires an admitted request"
                );
            }
        }
    }
}
