package io.github.naimjeg.damagenexus.builtin.processor;

import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.DamageProcessorPriorities;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.config.DamageNexusConfig;
import io.github.naimjeg.damagenexus.core.DamageComponent;
import io.github.naimjeg.damagenexus.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.Objects;

public class ResistanceMitigationProcessor implements DamagePhaseProcessor {

    @Override
    public void apply(DamageRuleContext context) {
        DamageNexusContext ctx = DamageInternalContexts.require(
                context,
                "phase processor"
        );


        for (int i = 0; i < ctx.getActiveComponentCount(); i++) {
            DamageComponent component = ctx.getActiveComponent(i);

            float currentDmg = component.getPostMitigationAmount();
            if (currentDmg <= 0.0f) {
                continue;
            }

            Holder<Attribute> channelAttribute =
                    DamageChannelRegistry.getResistanceAttribute(component.channel);
            Holder<Attribute> categoryAttribute = categoryAttribute(ctx);

            float channelRating =
                    channelAttribute != null
                            ? finiteOrZero(ctx.getVictimAttrOrZero(channelAttribute))
                            : 0.0f;

            float tempRating = finiteOrZero(
                    component.getTemporaryResistanceRating());

            float categoryRating = categoryAttribute != null
                    && !sameAttribute(channelAttribute, categoryAttribute)
                    ? finiteOrZero(ctx.getVictimAttrOrZero(categoryAttribute))
                    : 0.0f;

            float totalRating = totalRating(
                    channelRating, tempRating, categoryRating);

            if (totalRating == 0.0f) {
                if (ctx.trace().enabled()) {
                    ctx.trace().calculation().resistance(
                            component.channel.id().toString(),
                            channelRating,
                            tempRating,
                            categoryRating,
                            totalRating,
                            0.0f
                    );
                }

                continue;
            }

            float kValue = Math.max(
                    0.0001f,
                    DamageNexusConfig.current().formulas().resistanceKValue()
            );

            float reduction = reductionFor(totalRating, kValue);

            ctx.tryAddChannelMitigation(
                    component.channel,
                    reduction,
                    "dn:resistance"
            );

            if (ctx.trace().enabled()) {
                ctx.trace().calculation().resistance(
                        component.channel.id().toString(),
                        channelRating,
                        tempRating,
                        categoryRating,
                        totalRating,
                        reduction
                );
            }
        }
    }

    @Override
    public boolean canHandle(DamageRuleContext context) {
        DamageNexusContext ctx = DamageInternalContexts.require(
                context,
                "phase processor predicate"
        );

        return shouldMitigate(
                ctx.isManaged(),
                ctx.source().is(DamageTypeTags.BYPASSES_EFFECTS),
                ctx.source().is(DamageTypeTags.BYPASSES_RESISTANCE)
        );
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.MITIGATION_SETUP;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.DN_RESISTANCE_MITIGATION;
    }

    static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    static float totalRating(
            float channelRating,
            float temporaryRating,
            float categoryRating
    ) {
        double total = (double) finiteOrZero(channelRating)
                + finiteOrZero(temporaryRating)
                + finiteOrZero(categoryRating);
        if (total >= Float.MAX_VALUE) {
            return Float.MAX_VALUE;
        }
        if (total <= -Float.MAX_VALUE) {
            return -Float.MAX_VALUE;
        }
        return (float) total;
    }

    /**
     * Positive ratings use the asymptotic rating / (rating + K) formula and
     * cap at 95% reduction. Negative ratings use rating / K, so they express
     * vulnerability without a gameplay-scale negative reduction cap.
     */
    static float reductionFor(float rating, float kValue) {
        double safeRating = finiteOrZero(rating);
        double safeK = Float.isFinite(kValue)
                ? Math.max(0.0001d, kValue)
                : 0.0001d;
        double reduction = safeRating >= 0.0d
                ? safeRating / (safeRating + safeK)
                : safeRating / safeK;

        // Preserve the positive safety cap while allowing negative ratings to
        // express unbounded vulnerability within the finite float domain.
        return (float) Math.max(
                -Float.MAX_VALUE,
                Math.min(0.95d, reduction)
        );
    }

    static boolean sameAttribute(
            Holder<Attribute> first,
            Holder<Attribute> second
    ) {
        return sameReference(first, second);
    }

    static boolean shouldMitigate(
            boolean managed,
            boolean bypassesEffects,
            boolean bypassesResistance
    ) {
        return managed && !bypassesEffects && !bypassesResistance;
    }

    static boolean sameReference(Object first, Object second) {
        return first != null && second != null
                && (first == second || Objects.equals(first, second));
    }

    private static Holder<Attribute> categoryAttribute(
            DamageNexusContext context
    ) {
        if (context.isProjectileDamage()) {
            return ModAttributes.RESISTANCE_PROJECTILE;
        }
        if (context.isMeleeDamage()) {
            return ModAttributes.RESISTANCE_MELEE;
        }
        return null;
    }
}

