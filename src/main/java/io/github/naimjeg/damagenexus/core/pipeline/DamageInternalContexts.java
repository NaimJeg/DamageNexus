package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;

/**
 * Internal adapter for built-in systems that still need the concrete pipeline
 * context while public APIs receive DamageRuleContext.
 */
public final class DamageInternalContexts {

    private DamageInternalContexts() {
    }

    public static DamageNexusContext require(
            DamageRuleContext ctx,
            String usage
    ) {
        if (ctx instanceof DamageNexusContext internal) {
            return internal;
        }

        throw new IllegalArgumentException(
                "DamageNexus internal context required for "
                        + usage
                        + ": "
                        + (ctx == null ? "<null>" : ctx.getClass().getName())
        );
    }
}
