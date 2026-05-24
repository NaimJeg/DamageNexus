package io.github.naimjeg.damagenexus.core.util;

import org.jetbrains.annotations.ApiStatus;

/** Shared boundary for errors that must never be isolated or logged away. */
@ApiStatus.Internal
public final class JvmFatalErrors {

    private JvmFatalErrors() {
    }

    public static void rethrowIfFatal(Throwable throwable) {
        if (throwable instanceof VirtualMachineError error) {
            throw error;
        }
        if (throwable instanceof LinkageError error) {
            throw error;
        }
    }
}