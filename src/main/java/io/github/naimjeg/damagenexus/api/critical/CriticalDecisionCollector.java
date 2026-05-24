package io.github.naimjeg.damagenexus.api.critical;

/**
 * Provider-scoped collector. Retaining it beyond the callback is unsupported;
 * contributions after callback close or decision freeze throw
 * {@link IllegalStateException}.
 */
public interface CriticalDecisionCollector {
    CriticalDecisionContributionResult contribute(CriticalDecision decision);

    boolean isOpen();
}
