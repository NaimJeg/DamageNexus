package io.github.naimjeg.damagenexus.builtin.bridge;

import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.DamageProcessorPriorities;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.core.DamageComponent;
import io.github.naimjeg.damagenexus.core.contribution.VanillaContributionDescriptors;
import io.github.naimjeg.damagenexus.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class VanillaResistanceEffectProcessor implements DamagePhaseProcessor {

    private static final String TRACE_ID = "vanilla:mob_effect/resistance";

    @Override
    public void apply(DamageRuleContext context) {
        DamageNexusContext ctx = DamageInternalContexts.require(
                context,
                "phase processor"
        );

        if (ctx.victim() == null) return;

        if (ctx.source().is(DamageTypeTags.BYPASSES_EFFECTS)) return;
        if (ctx.source().is(DamageTypeTags.BYPASSES_RESISTANCE)) return;

        MobEffectInstance resistance = ctx.victim().getEffect(MobEffects.RESISTANCE);

        if (resistance == null) {
            return;
        }

        int level = resistance.getAmplifier() + 1;

        float reduction = Math.max(
                0.0f,
                Math.min(1.0f, level * 0.20f)
        );

        if (reduction <= 0.0f) {
            return;
        }

        for (int i = 0; i < ctx.getActiveComponentCount(); i++) {
            DamageComponent component = ctx.getActiveComponent(i);

            DamageMutationResult result = ctx.tryAddChannelMitigation(
                    component.channel,
                    reduction,
                    TRACE_ID
            );

            ctx.contributions().record(
                    result,
                    () -> VanillaContributionDescriptors.vanillaMitigation(
                            DamageNexusIds.id(
                                    "vanilla_resistance_effect/"
                                            + component.channel.id().getPath()
                            ),
                            DamagePhase.MITIGATION_SETUP,
                            component.channel.id(),
                            reduction,
                            TRACE_ID
                    )
            );
        }
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.MITIGATION_SETUP;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_RESISTANCE_EFFECT;
    }
}
