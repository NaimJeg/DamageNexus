package io.github.naimjeg.damagenexus.core.settlement;

import io.github.naimjeg.damagenexus.api.damage.DamageParentRef;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Internal, non-recursive scope for one framework-controlled settlement
 * delivery: either the observational NeoForge event post or one registered
 * authority-bearing callback invocation.
 */
public final class DamageSettlementDispatchScope {

    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private DamageSettlementDispatchScope() {
    }

    static Scope openObservation(
            Object delivery,
            Object serverIdentity
    ) {
        return open(DeliveryKind.OBSERVATION, delivery, null, serverIdentity);
    }

    static Scope openCallback(
            Object delivery,
            @Nullable DamageParentRef authority,
            Object serverIdentity
    ) {
        return open(DeliveryKind.CALLBACK, delivery, authority, serverIdentity);
    }

    private static Scope open(
            DeliveryKind kind,
            Object delivery,
            @Nullable DamageParentRef authority,
            Object serverIdentity
    ) {
        if (CURRENT.get() != null) {
            throw new IllegalStateException(
                    "Recursive settlement delivery scope is not allowed"
            );
        }
        State state = new State(
                Objects.requireNonNull(kind, "kind"),
                Objects.requireNonNull(delivery, "delivery"),
                authority,
                Objects.requireNonNull(serverIdentity, "serverIdentity")
        );
        CURRENT.set(state);
        return new Scope(state);
    }

    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    public static boolean exposesAuthority(
            Object delivery,
            @Nullable DamageParentRef authority,
            Object serverIdentity
    ) {
        if (authority == null) {
            return false;
        }
        State state = CURRENT.get();
        return state != null
                && state.kind() == DeliveryKind.CALLBACK
                && state.delivery() == delivery
                && state.authority() == authority
                && state.serverIdentity() == serverIdentity;
    }

    public static boolean accepts(
            @Nullable DamageParentRef authority,
            Object serverIdentity
    ) {
        if (authority == null) {
            return false;
        }
        State state = CURRENT.get();
        return state != null
                && state.kind() == DeliveryKind.CALLBACK
                && state.authority() == authority
                && state.serverIdentity() == serverIdentity;
    }

    public static int depthForTests() {
        return CURRENT.get() == null ? 0 : 1;
    }

    private enum DeliveryKind {
        OBSERVATION,
        CALLBACK
    }

    private record State(
            DeliveryKind kind,
            Object delivery,
            @Nullable DamageParentRef authority,
            Object serverIdentity
    ) {
    }

    static final class Scope implements AutoCloseable {
        private final State state;
        private boolean closed;

        private Scope(State state) {
            this.state = state;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (CURRENT.get() != state) {
                throw new IllegalStateException(
                        "Settlement delivery scope is not current"
                );
            }
            CURRENT.remove();
        }
    }
}
