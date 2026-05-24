package io.github.naimjeg.damagenexus.core.lifecycle;

public enum DamageNexusLifecycleState {
    CONSTRUCTING,
    REGISTERING,
    FROZEN,
    RUNNING,

    /**
     * Terminal state entered when framework bootstrap fails after
     * registration has begun.
     */
    FAILED
}
