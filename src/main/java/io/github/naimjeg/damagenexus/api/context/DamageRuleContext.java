package io.github.naimjeg.damagenexus.api.context;

/**
 * Public rule-facing context.
 * <p>
 * Rules can inspect stable damage state through DamageContextView and mutate
 * damage only through DamageMutationContext.
 */
public interface DamageRuleContext
        extends DamageContextView, DamageMutationContext {
}
