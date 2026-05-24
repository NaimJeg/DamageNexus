package io.github.naimjeg.damagenexus.builtin.bridge;

import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.DamageProcessorPriorities;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.config.DamageNexusConfig;
import io.github.naimjeg.damagenexus.core.DamageComponent;
import io.github.naimjeg.damagenexus.core.contribution.VanillaContributionDescriptors;
import io.github.naimjeg.damagenexus.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class VanillaDamageProtectionProcessor implements DamagePhaseProcessor {

    private static final String TRACE_ID = "vanilla:damage_protection";

    @Override
    public void apply(DamageRuleContext context) {
        DamageNexusContext ctx = DamageInternalContexts.require(
                context,
                "phase processor"
        );

        if (ctx.victim() == null) return;
        if (!(ctx.victim().level() instanceof ServerLevel serverLevel)) return;

        if (ctx.source().is(DamageTypeTags.BYPASSES_EFFECTS)) return;
        if (ctx.source().is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) return;

        float score = EnchantmentHelper.getDamageProtection(
                serverLevel,
                ctx.victim(),
                ctx.source()
        );

        if (score <= 0.0f) {
            return;
        }

        float rating = score * DamageNexusConfig.current()
                .formulas()
                .ratingPerProtScore();

        for (int i = 0; i < ctx.getActiveComponentCount(); i++) {
            DamageComponent component = ctx.getActiveComponent(i);

            DamageMutationResult result = ctx.tryAddTemporaryResistance(
                    component.channel,
                    rating,
                    TRACE_ID
            );

            ctx.contributions().record(
                    result,
                    () -> VanillaContributionDescriptors.vanillaTemporaryResistance(
                            DamageNexusIds.id(
                                    "vanilla_damage_protection/"
                                            + component.channel.id().getPath()
                            ),
                            DamagePhase.MITIGATION_SETUP,
                            component.channel.id(),
                            rating,
                            TRACE_ID
                    )
            );
        }

        if (ctx.trace().enabled()) {
            ctx.trace().calculation().enchantmentProtection(
                    TRACE_ID,
                    0,
                    score,
                    rating
            );
        }
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.MITIGATION_SETUP;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_DAMAGE_PROTECTION;
    }
}
