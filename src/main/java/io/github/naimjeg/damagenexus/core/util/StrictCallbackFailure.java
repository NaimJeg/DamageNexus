package io.github.naimjeg.damagenexus.core.util;

import org.jetbrains.annotations.ApiStatus;

/** Internal marker preserving strict rule failures across processor isolation. */
@ApiStatus.Internal
public final class StrictCallbackFailure extends RuntimeException {

    public StrictCallbackFailure(String message, Throwable cause) {
        super(message, cause);
    }
}
