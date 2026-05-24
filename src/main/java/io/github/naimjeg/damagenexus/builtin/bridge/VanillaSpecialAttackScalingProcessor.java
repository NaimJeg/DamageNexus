package io.github.naimjeg.damagenexus.builtin.bridge;

import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.DamageProcessorPriorities;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.bridge.vanilla.PreEventDeltaKind;
import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaDamageCapture;
import io.github.naimjeg.damagenexus.core.contribution.VanillaContributionDescriptors;
import io.github.naimjeg.damagenexus.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class VanillaSpecialAttackScalingProcessor implements DamagePhaseProcessor {

    private static final float EPSILON = 0.0001f;
    private static final String TRACE_ID = "vanilla:special_attack_bonus";

    @Override
    public boolean canHandle(DamageRuleContext context) {
        DamageNexusContext ctx = DamageInternalContexts.require(
                context,
                "phase processor predicate"
        );

        if (!ctx.shouldRebuildVanillaPreEventDelta()) {
            return false;
        }

        VanillaDamageCapture.OffensiveSnapshot snapshot =
                ctx.getVanillaSnapshot();

        if (snapshot == null) {
            return false;
        }

        VanillaDamageCapture.PreEventDelta delta =
                snapshot.preEventDelta();

        return delta.kind() == PreEventDeltaKind.SPECIAL_ATTACK_SCALING
                && Float.isFinite(delta.delta())
                && Math.abs(delta.delta()) > EPSILON;
    }

    @Override
    public void apply(DamageRuleContext context) {
        DamageNexusContext ctx = DamageInternalContexts.require(
                context,
                "phase processor"
        );

        VanillaDamageCapture.PreEventDelta delta =
                ctx.getVanillaSnapshot().preEventDelta();

        DamageApplicationBucket bucket =
                DamageApplicationBucket.VANILLA_WEAPON_SPECIAL;

        DamageMutationResult result = ctx.tryAddVanillaBaseReconstructedDamage(
                ctx.getInitialChannel(),
                bucket,
                delta.delta(),
                TRACE_ID
        );

        ctx.contributions().record(
                result,
                () -> VanillaContributionDescriptors.vanillaBase(
                        DamageNexusIds.id(
                                "vanilla_special_attack/"
                                        + bucket.name().toLowerCase(Locale.ROOT)
                        ),
                        DamagePhase.BASE_MODIFICATION,
                        ctx.getInitialChannel().id(),
                        bucket,
                        delta.delta(),
                        TRACE_ID
                )
        );
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.BASE_MODIFICATION;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_WEAPON_SPECIAL_BONUS;
    }
}
