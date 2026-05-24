package io.github.naimjeg.damagenexus.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JvmFatalErrorsTest {

    @Test
    void virtualMachineErrorsAlwaysEscape() {
        OutOfMemoryError fatal = new OutOfMemoryError("test");

        OutOfMemoryError thrown = assertThrows(
                OutOfMemoryError.class,
                () -> JvmFatalErrors.rethrowIfFatal(fatal)
        );

        assertSame(fatal, thrown);
    }

    @Test
    void linkageErrorsAlwaysEscape() {
        LinkageError fatal = new LinkageError("test");

        LinkageError thrown = assertThrows(
                LinkageError.class,
                () -> JvmFatalErrors.rethrowIfFatal(fatal)
        );

        assertSame(fatal, thrown);
    }

    @Test
    void nonFatalThrowablesAreIgnored() {
        assertDoesNotThrow(
                () -> JvmFatalErrors.rethrowIfFatal(
                        new RuntimeException("test")
                )
        );
        assertDoesNotThrow(
                () -> JvmFatalErrors.rethrowIfFatal(
                        new Exception("test")
                )
        );
        assertDoesNotThrow(
                () -> JvmFatalErrors.rethrowIfFatal(
                        new AssertionError("test")
                )
        );
        assertDoesNotThrow(
                () -> JvmFatalErrors.rethrowIfFatal(
                        new Error("test")
                )
        );
    }
}
