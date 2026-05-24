package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.damage.DamageAttribution;
import io.github.naimjeg.damagenexus.api.damage.DamageAttributionProvenance;
import io.github.naimjeg.damagenexus.api.damage.DamageLineage;
import io.github.naimjeg.damagenexus.api.damage.DamageMetadata;
import io.github.naimjeg.damagenexus.api.damage.DamageOrigin;
import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;
import io.github.naimjeg.damagenexus.api.damage.DamageSourceDescriptor;
import io.github.naimjeg.damagenexus.api.damage.DamageTriggerPolicy;
import io.github.naimjeg.damagenexus.bridge.vanilla.*;
import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import io.github.naimjeg.damagenexus.diagnostics.logging.VanillaBridgeDiagnosticsLog;
import io.github.naimjeg.damagenexus.core.attribution.DamageAttributionResolvers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Optional;
import java.util.Set;

/**
 * Factory for constructing DamageNexusContext instances from NeoForge damage events.
 *
 * <p>This class owns the vanilla bridge setup required before the pipeline can run:
 * attacker/victim extraction, vanilla offensive snapshot consumption, source profile
 * creation, mob-effect bridge analysis, bridge-plan construction, and diagnostics
 * logging.</p>
 *
 * <p>The event handler remains responsible for recursion protection and final
 * VanillaDamageCapture cleanup.</p>
 */
public final class DamageNexusContextFactory {

    private DamageNexusContextFactory() {
    }

    /**
     * Creates a context for a server-side LivingIncomingDamageEvent.
     *
     * @return a DamageNexusContext, or {@code null} when the event should not be
     * processed by DamageNexus.
     */
    public static DamageNexusContext tryCreate(
            LivingIncomingDamageEvent event,
            DamageOrigin origin
    ) {
        if (event == null) {
            return null;
        }

        LivingEntity victim = event.getEntity();
        if (victim == null || victim.level().isClientSide()) {
            return null;
        }

        if (!DamageSourcePolicy.shouldManage(event.getSource())) {
            return null;
        }

        if (DamageNexusSettings.fullTraceEnabled()) {
            VanillaBridgeDiagnosticsLog.incomingCaught(
                    event.getSource().type().msgId()
            );
        }

        LivingEntity attacker = origin.attribution().logicalAttacker();

        return createServerContext(
                event,
                attacker,
                victim,
                origin
        );
    }

    private static DamageNexusContext createServerContext(
            LivingIncomingDamageEvent event,
            LivingEntity attacker,
            LivingEntity victim,
            DamageOrigin origin
    ) {
        VanillaDamageCapture.OffensiveSnapshot vanillaSnapshot =
                VanillaDamageCapture.consumeOffensiveSnapshot(
                        event.getSource(),
                        victim,
                        event.getOriginalAmount()
                );

        if (DamageNexusSettings.fullTraceEnabled()) {
            VanillaBridgeLogger.logSnapshot(vanillaSnapshot);
        }

        VanillaDamageSourceProfile sourceProfile =
                VanillaDamageSourceProfile.create(
                        event.getSource(),
                        attacker,
                        victim
                );

        VanillaMobEffectBridge.OffensiveMobEffectBreakdown mobEffectBreakdown =
                VanillaMobEffectBridge.computeOffensiveBreakdown(sourceProfile);

        VanillaBridgePlan bridgePlan =
                VanillaBridgePlan.from(
                        event.getOriginalAmount(),
                        sourceProfile,
                        vanillaSnapshot,
                        mobEffectBreakdown.observedDelta(),
                        mobEffectBreakdown.enabledDelta()
                );

        if (DamageNexusSettings.fullTraceEnabled()) {
            VanillaBridgeDiagnosticsLog.bridgePlan(
                    event.getOriginalAmount(),
                    vanillaSnapshot,
                    mobEffectBreakdown,
                    bridgePlan
            );
        }

        return createFromBridgePlan(
                event,
                attacker,
                victim,
                origin,
                sourceProfile,
                vanillaSnapshot,
                bridgePlan
        );
    }

    private static DamageNexusContext createFromBridgePlan(
            LivingIncomingDamageEvent event,
            LivingEntity attacker,
            LivingEntity victim,
            DamageOrigin origin,
            VanillaDamageSourceProfile sourceProfile,
            VanillaDamageCapture.OffensiveSnapshot vanillaSnapshot,
            VanillaBridgePlan bridgePlan
    ) {
        return new DamageNexusContext(DamageNexusContextSpec.of(
                event,
                attacker,
                victim,
                origin,
                sourceProfile,
                bridgePlan.initialBaseAmount(),
                vanillaSnapshot,
                bridgePlan.rebuildOffensiveMobEffects(),
                bridgePlan.rebuildOffensiveEnchantment(),
                bridgePlan.rebuildPreEventDelta(),
                bridgePlan.offensiveMobEffectDelta(),
                bridgePlan.initialBaseBucket(),
                bridgePlan.offensiveMobEffectBucket(),
                bridgePlan.offensiveEnchantmentBucket()
        ));
    }

    private static DamageRequestKind nativeRequestKind(
            DamageAttribution attribution
    ) {
        return attribution.directEntity() == null
                && attribution.logicalAttacker() == null
                && attribution.effectOwner() == null
                ? DamageRequestKind.ENVIRONMENTAL
                : DamageRequestKind.PRIMARY;
    }

    public static DamageOrigin nativeOrigin(
            LivingIncomingDamageEvent event
    ) {
        Entity rawAttacker = event.getSource().getEntity();
        LivingEntity vanillaAttacker = rawAttacker
                instanceof LivingEntity livingAttacker
                ? livingAttacker
                : null;
        DamageAttribution vanillaDefault = DamageAttribution.defaults(
                event.getSource().getDirectEntity(),
                vanillaAttacker,
                vanillaAttacker,
                vanillaAttacker
        );
        DamageAttribution attribution =
                DamageAttributionResolvers.normalizeVanillaDefault(
                        (ServerLevel) event.getEntity().level(),
                        vanillaDefault
                );
        DamageRequestKind kind = nativeRequestKind(attribution);
        float originalAmount = event.getOriginalAmount();
        float safeAmount = Float.isFinite(originalAmount)
                ? Math.max(0.0f, originalAmount)
                : 0.0f;

        DamageOrigin origin = new DamageOrigin(
                DamageLineage.newRoot(),
                kind,
                attribution,
                DamageSourceDescriptor.from(event.getSource()),
                safeAmount,
                Optional.empty(),
                Set.of(),
                DamageTriggerPolicy.defaultsFor(kind),
                DamageMetadata.empty()
        );
        return origin.withResolvedAttribution(
                attribution,
                DamageAttributionProvenance.vanillaDefault()
        );
    }
}


