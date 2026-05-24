package io.github.naimjeg.damagenexus.event.neoforge;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaDamageCapture;
import io.github.naimjeg.damagenexus.api.damage.DamageAttributionEntryPoint;
import io.github.naimjeg.damagenexus.api.damage.DamageAttributionQuery;
import io.github.naimjeg.damagenexus.api.damage.DamageOrigin;
import io.github.naimjeg.damagenexus.core.attribution.DamageAttributionResolvers;
import io.github.naimjeg.damagenexus.core.pipeline.DamageSourcePolicy;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContextFactory;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusPipeline;
import io.github.naimjeg.damagenexus.core.pipeline.DamageExecutionSummary;
import io.github.naimjeg.damagenexus.core.request.DamageRequestSubmissionTracker;
import io.github.naimjeg.damagenexus.core.request.DamageAdmissionController;
import io.github.naimjeg.damagenexus.core.request.DamageAdmissionResult;
import io.github.naimjeg.damagenexus.core.request.DamageTransactionActivity;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementTracker;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageAdmissionDiagnosticsLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = DamageNexus.MODID)
public class IncomingDamageHandler {

    private static final ThreadLocal<Integer> EVENT_REENTRANCY_DEPTH =
            ThreadLocal.withInitial(() -> 0);

    /* Stack-safety fuse only; explicit lineage limits are enforced elsewhere. */
    private static final int MAX_EVENT_REENTRANCY_DEPTH = 32;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        int eventDepth = EVENT_REENTRANCY_DEPTH.get();

        try {
            EVENT_REENTRANCY_DEPTH.set(eventDepth + 1);

            if (!DamageSourcePolicy.shouldManage(event.getSource())
                    || event.getEntity().level().isClientSide()) {
                return;
            }

            DamageRequestSubmissionTracker.Claim claim =
                    DamageRequestSubmissionTracker.claimIncoming(event);
            DamageOrigin origin;
            if (claim == null) {
                ServerLevel level = (ServerLevel) event.getEntity().level();
                DamageOrigin base = DamageNexusContextFactory.nativeOrigin(event);
                origin = DamageAttributionResolvers.resolve(
                        new DamageAttributionQuery(
                                level,
                                event.getEntity(),
                                base.source(),
                                java.util.Optional.of(event.getSource()),
                                base.requestKind(),
                                base.attribution(),
                                base.actionId(),
                                base.sourceTags(),
                                base.metadata(),
                                DamageAttributionEntryPoint.NATIVE
                        ),
                        base
                );
            } else {
                origin = claim.origin();
            }
            MinecraftServer server = ((ServerLevel) event.getEntity().level())
                    .getServer();
            DamageAdmissionResult admission;
            boolean nativeDamage = claim == null;

            if (eventDepth >= MAX_EVENT_REENTRANCY_DEPTH) {
                admission = DamageAdmissionController.rejectEventReentrancy(
                        origin,
                        server
                );
            } else if (nativeDamage) {
                admission = DamageAdmissionController.admitNative(
                        origin,
                        server
                );
            } else {
                admission = claim.admission();
            }

            if (!admission.admitted()) {
                if (!DamageSettlementTracker.rejectAdmission(
                        event,
                        origin,
                        admission.reason()
                )) {
                    throw new IllegalStateException(
                            "Managed admission rejection had no hurt scope"
                    );
                }
                event.setCanceled(true);
                if (nativeDamage) {
                    DamageAdmissionDiagnosticsLog.nativeRejected(
                            origin,
                            admission
                    );
                }
                return;
            }

            if (nativeDamage) {
                DamageAdmissionDiagnosticsLog.accepted(origin, admission);
            }

            try (DamageTransactionActivity.Scope ignored =
                         DamageTransactionActivity.enter()) {
                DamageNexusContext ctx =
                        DamageNexusContextFactory.tryCreate(event, origin);

                if (ctx == null) {
                    return;
                }

                boolean settlementTracked =
                        DamageSettlementTracker.beginIncoming(
                        event,
                        ctx.origin()
                );
                DamageExecutionSummary summary =
                        DamageNexusPipeline.execute(ctx);
                if (settlementTracked) {
                    DamageSettlementTracker.recordCalculated(event, summary);
                }
                DamageRequestSubmissionTracker.recordPipelineResult(
                        event,
                        summary
                );
            }

        } finally {
            VanillaDamageCapture.clear();
            if (eventDepth == 0) {
                EVENT_REENTRANCY_DEPTH.remove();
            } else {
                EVENT_REENTRANCY_DEPTH.set(eventDepth);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onIncomingDispatchComplete(
            LivingIncomingDamageEvent event
    ) {
        DamageSettlementTracker.enforceAdmissionRejection(event);
        DamageSettlementTracker.markIncomingDispatchComplete(event);
    }
}

