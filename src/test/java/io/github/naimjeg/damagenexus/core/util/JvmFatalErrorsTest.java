package io.github.naimjeg.damagenexus.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JvmFatalErrorsTest {

    @Test
    void ordinaryListenerFailureCanBeIsolated() {
        assertDoesNotThrow(() -> JvmFatalErrors.rethrowIfFatal(
                new IllegalStateException("ordinary")
        ));
    }

    @Test
    @SuppressWarnings("removal")
    void fatalJvmFailuresAlwaysEscape() {
        LinkageError linkage = new LinkageError("synthetic");
        ThreadDeath death = new ThreadDeath();
        TestVirtualMachineError virtualMachine =
                new TestVirtualMachineError();

        assertSame(linkage, assertThrows(
                LinkageError.class,
                () -> JvmFatalErrors.rethrowIfFatal(linkage)
        ));
        assertSame(death, assertThrows(
                ThreadDeath.class,
                () -> JvmFatalErrors.rethrowIfFatal(death)
        ));
        assertSame(virtualMachine, assertThrows(
                TestVirtualMachineError.class,
                () -> JvmFatalErrors.rethrowIfFatal(virtualMachine)
        ));
    }

    private static final class TestVirtualMachineError
            extends VirtualMachineError {
    }
}
