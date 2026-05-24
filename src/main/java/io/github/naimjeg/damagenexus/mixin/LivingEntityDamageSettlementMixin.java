package io.github.naimjeg.damagenexus.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.naimjeg.damagenexus.core.pipeline.DamageSourcePolicy;
import io.github.naimjeg.damagenexus.core.request.DamageRequestSubmissionTracker;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementCompletion;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementCoordinator;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementDispatchScope;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementMixinStatus;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Ensures every LivingEntity hurt scope has a finally-backed completion. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageSettlementMixin
        implements DamageSettlementMixinStatus.Marker {

    @WrapMethod(method = "hurtServer(Lnet/minecraft/server/level/"
            + "ServerLevel;Lnet/minecraft/world/damagesource/"
            + "DamageSource;F)Z")
    private boolean damagenexus$trackSettlementLifecycle(
            ServerLevel level,
            DamageSource source,
            float amount,
            Operation<Boolean> original
    ) {
        LivingEntity target = (LivingEntity) (Object) this;

        /*
         * Settlement delivery is an immutable authority boundary. A managed
         * hurt may cross it only through DamageRequestService, which has
         * already opened the exact public submission for this target and the
         * DamageSource instance it created. Reject direct native roots before
         * opening any hurt/settlement state or consuming native admission.
         */
        if (DamageSourcePolicy.shouldManage(source)
                && DamageSettlementDispatchScope.isActive()
                && !DamageRequestSubmissionTracker.matchesActiveSubmission(
                        target,
                        source
                )) {
            return false;
        }

        DamageSettlementTracker.HurtScope scope =
                DamageSettlementTracker.openHurt(
                        target,
                        level,
                        source
                );
        DamageSettlementCompletion completion;
        boolean accepted;

        try {
            accepted = original.call(level, source, amount);
            completion = scope.complete(accepted);
        } catch (RuntimeException | Error throwable) {
            scope.abort();
            throw throwable;
        } finally {
            scope.close();
        }

        DamageSettlementCoordinator.handoff(
                completion,
                target,
                source
        );
        return accepted;
    }
}
