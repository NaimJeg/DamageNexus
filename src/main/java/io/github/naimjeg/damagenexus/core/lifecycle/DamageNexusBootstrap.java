package io.github.naimjeg.damagenexus.core.lifecycle;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.event.DamageNexusRegisterEvent;
import io.github.naimjeg.damagenexus.config.DamageNexusConfig;
import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusPipeline;
import io.github.naimjeg.damagenexus.core.attribution.DamageAttributionResolvers;
import io.github.naimjeg.damagenexus.core.critical.CriticalDecisionProviders;
import io.github.naimjeg.damagenexus.core.rule.ExternalItemRuleSources;
import io.github.naimjeg.damagenexus.core.registry.PreMultiplierBucketRegistry;
import io.github.naimjeg.damagenexus.core.template.DamageTemplateRegistry;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementCallbacks;
import io.github.naimjeg.damagenexus.core.util.JvmFatalErrors;
import io.github.naimjeg.damagenexus.diagnostics.DamageNexusStartupSelfCheck;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusLifecycleLog;
import io.github.naimjeg.damagenexus.registry.PreMultiplierBuckets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Framework-owned lifecycle coordinator.
 *
 * <p>NeoForge discovers this package-private MOD-bus subscriber directly.
 * There is no public install/progression entry point that another mod can call
 * before the real DamageNexus bootstrap.</p>
 */
@EventBusSubscriber(modid = DamageNexus.MODID)
final class DamageNexusBootstrap {

    private static final AtomicBoolean COMMON_SETUP_CLAIMED =
            new AtomicBoolean();

    private DamageNexusBootstrap() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!claimCommonSetup()) {
            return;
        }

        event.enqueueWork(DamageNexusBootstrap::runCommonSetup);
    }

    static boolean claimCommonSetup() {
        return COMMON_SETUP_CLAIMED.compareAndSet(false, true);
    }

    private static void runCommonSetup() {
        DamageNexusConfig.bakeConfig();

        DamageNexusRegistrationAccess access =
                DamageNexusLifecycle.beginRegistering();
        DamageNexusRegistrationSession session = null;

        try {
            PreMultiplierBuckets.register(access);

            session = new DamageNexusRegistrationSession(access);

            try {
                DamageNexus.LOGGER.info(
                        "[DamageNexus] Posting DamageNexusRegisterEvent"
                );
                NeoForge.EVENT_BUS.post(
                        new DamageNexusRegisterEvent(session)
                );
            } finally {
                session.close();
            }

            PreMultiplierBucketRegistry.freeze(access);
            DamageAttributionResolvers.freeze(access);
            ExternalItemRuleSources.freeze(access);
            CriticalDecisionProviders.freeze(access);
            DamageSettlementCallbacks.freeze(access);
            DamageTemplateRegistry.freeze(access);
            DamageNexusLifecycle.freezeRegistration(access);

            DamageNexusStartupSelfCheck.run();

            DamageNexusLifecycleLog.safetyConfig(
                    DamageNexusSettings.maxRecursionDepth(),
                    DamageNexusSettings.maxDerivedRequestsPerRoot(),
                    DamageNexusSettings.maxManagedRequestsPerServerTick()
            );
            DamageNexusLifecycleLog.attributeConsumptionStatus();

            DamageNexusPipeline.clearCache();
            DamageNexusLifecycle.running();
        } catch (Throwable throwable) {
            JvmFatalErrors.rethrowIfFatal(throwable);

            if (session != null) {
                session.close();
            }

            DamageNexusLifecycle.failBootstrap(access);
            throw throwable;
        }

        DamageNexusLifecycleLog.commonSetupComplete(
                DamageNexusSettings.diagnosticMode(),
                DamageNexusSettings.testCommandsEnabled(),
                PreMultiplierBucketRegistry.bucketCount()
        );
    }

}
