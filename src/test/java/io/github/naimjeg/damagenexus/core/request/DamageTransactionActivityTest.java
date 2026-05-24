package io.github.naimjeg.damagenexus.core.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageTransactionActivityTest {

    @Test
    void nestedScopesRemainActiveUntilTheOutermostScopeCloses() {
        assertFalse(DamageTransactionActivity.isActive());

        DamageTransactionActivity.Scope outer = DamageTransactionActivity.enter();
        assertTrue(DamageTransactionActivity.isActive());

        DamageTransactionActivity.Scope inner = DamageTransactionActivity.enter();
        assertTrue(DamageTransactionActivity.isActive());

        inner.close();
        assertTrue(DamageTransactionActivity.isActive());

        outer.close();
        outer.close();
        assertFalse(DamageTransactionActivity.isActive());
    }

    @Test
    void exceptionDoesNotLeakActivityState() {
        assertThrows(IllegalStateException.class, () -> {
            try (DamageTransactionActivity.Scope ignored =
                         DamageTransactionActivity.enter()) {
                assertTrue(DamageTransactionActivity.isActive());
                throw new IllegalStateException("test pipeline failure");
            }
        });

        assertFalse(DamageTransactionActivity.isActive());
    }

    @Test
    void scopesRejectOutOfOrderClose() {
        DamageTransactionActivity.Scope outer =
                DamageTransactionActivity.enter();
        DamageTransactionActivity.Scope inner =
                DamageTransactionActivity.enter();

        assertThrows(IllegalStateException.class, outer::close);
        assertTrue(DamageTransactionActivity.isActive());
        inner.close();
        outer.close();
        assertFalse(DamageTransactionActivity.isActive());
    }
}
