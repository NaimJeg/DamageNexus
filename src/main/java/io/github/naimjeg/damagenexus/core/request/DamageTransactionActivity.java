package io.github.naimjeg.damagenexus.core.request;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Minimal Phase 2 guard marking execution inside a DamageNexus pipeline.
 *
 * <p>This compute-only marker prevents a Java rule or phase processor from
 * synchronously submitting child damage during calculation. The separate
 * settlement tracker owns the wider application lifecycle.</p>
 */
public final class DamageTransactionActivity {

    private static final ThreadLocal<Deque<Scope>> ACTIVE =
            new ThreadLocal<>();

    private DamageTransactionActivity() {
    }

    public static Scope enter() {
        Deque<Scope> stack = ACTIVE.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            ACTIVE.set(stack);
        }
        Scope scope = new Scope();
        stack.addLast(scope);
        return scope;
    }

    public static boolean isActive() {
        Deque<Scope> stack = ACTIVE.get();
        return stack != null && !stack.isEmpty();
    }

    public static final class Scope implements AutoCloseable {

        private boolean closed;

        private Scope() {
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            Deque<Scope> stack = ACTIVE.get();
            if (stack == null || stack.peekLast() != this) {
                throw new IllegalStateException(
                        "Damage transaction activity closed out of order"
                );
            }

            stack.removeLast();
            if (stack.isEmpty()) {
                ACTIVE.remove();
            }
            closed = true;
        }
    }
}
