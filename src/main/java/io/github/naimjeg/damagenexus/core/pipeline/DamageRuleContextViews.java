package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;

import java.util.Objects;

/**
 * Creates API-safe views for calls into third-party extension code.
 */
public final class DamageRuleContextViews {

    private DamageRuleContextViews() {
    }

    public static DamageRuleContext restricted(DamageRuleContext context) {
        Objects.requireNonNull(context, "context");

        if (context instanceof RestrictedDamageRuleContext) {
            return context;
        }

        if (context instanceof DamageNexusContext internal) {
            return internal.restrictedView();
        }

        return new RestrictedDamageRuleContext(context);
    }
}
