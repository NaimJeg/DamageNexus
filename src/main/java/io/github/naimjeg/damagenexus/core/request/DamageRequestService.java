package io.github.naimjeg.damagenexus.core.request;

import io.github.naimjeg.damagenexus.api.damage.*;
import io.github.naimjeg.damagenexus.core.attribution.DamageAttributionResolvers;
import io.github.naimjeg.damagenexus.core.pipeline.DamageSourcePolicy;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementCoordinator;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementDispatchScope;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementTracker;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageAdmissionDiagnosticsLog;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

/** Internal implementation behind the stable DamageNexusApi facade. */
public final class DamageRequestService {

    private static final Map<DamageRequest, Boolean> SUBMITTED_REQUESTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private DamageRequestService() {
    }

    public static DamageResult submit(DamageRequest request) {
        Objects.requireNonNull(request, "Damage request must not be null");

        if (DamageRequestSubmissionTracker.hasActiveSubmission()
                || DamageTransactionActivity.isActive()
                || DamageSettlementTracker.hasActiveHurt()) {
            return DamageResult.rejected(
                    request,
                    DamageFailureReason.ACTIVE_TRANSACTION,
                    "A damage request cannot be submitted while a mutable "
                            + "damage scope is active: submission="
                            + DamageRequestSubmissionTracker.hasActiveSubmission()
                            + ", transaction="
                            + DamageTransactionActivity.isActive()
                            + ", hurt="
                            + DamageSettlementTracker.hasActiveHurt()
            );
        }

        if (!markSubmitted(request)) {
            return DamageResult.rejected(
                    request,
                    DamageFailureReason.DUPLICATE_REQUEST,
                    "DamageRequest instances are single-submission commands"
            );
        }

        DamageResult validationFailure = validateRequestBase(request);
        if (validationFailure != null) {
            return validationFailure;
        }

        validationFailure = validateEventAuthority(request);
        if (validationFailure != null) {
            return validationFailure;
        }

        Optional<Holder.Reference<DamageType>> damageType =
                request.level()
                        .registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .get(request.source().damageType());

        if (damageType.isEmpty()) {
            return DamageResult.rejected(
                    request,
                    DamageFailureReason.UNKNOWN_DAMAGE_TYPE,
                    "Unknown damage type: "
                            + request.source().damageType().identifier()
            );
        }

        validationFailure = validateAttributionStructure(
                request,
                request.attribution()
        );
        if (validationFailure != null) {
            return validationFailure;
        }

        DamageOrigin authoritativeOrigin = resolveAttribution(request);
        validationFailure = validateAuthoritativeAttribution(
                request,
                authoritativeOrigin
        );
        if (validationFailure != null) {
            return validationFailure;
        }

        DamageSource source = createDamageSource(
                authoritativeOrigin,
                damageType.orElseThrow()
        );

        if (!DamageSourcePolicy.shouldManage(source)) {
            return DamageResult.rejected(
                    request,
                    DamageFailureReason.SOURCE_NOT_MANAGED,
                    "Damage source is excluded by DamageNexus policy: "
                            + request.source().damageType().identifier()
            );
        }

        DamageAdmissionResult admission =
                DamageAdmissionController.admitPublic(request);
        if (!admission.admitted()) {
            DamageFailureReason reason = Objects.requireNonNull(
                    admission.reason(),
                    "Rejected admission reason"
            );
            return DamageResult.rejected(
                    request,
                    reason,
                    admissionDiagnostic(request, admission)
            );
        }
        DamageAdmissionDiagnosticsLog.accepted(
                authoritativeOrigin,
                admission
        );

        DamageRequestSubmissionTracker.Submission submission =
                DamageRequestSubmissionTracker.open(
                        request,
                        source,
                        admission,
                        authoritativeOrigin
                );
        DamageResult result;
        boolean finished = false;

        try {
            boolean vanillaAccepted = request.target().hurtServer(
                    request.level(),
                    source,
                    request.baseDamage()
            );
            result = submission.finish(vanillaAccepted);
            finished = true;
        } finally {
            submission.close();
            if (!finished) {
                DamageSettlementCoordinator.drainIfSafe();
            }
        }

        DamageSettlementCoordinator.drainIfSafe();
        return result;
    }

    private static @Nullable DamageResult validateEventAuthority(
            DamageRequest request
    ) {
        DamageParentRef parent = request.parentRefInternal();
        if (parent == null) {
            if (DamageSettlementDispatchScope.isActive()) {
                return rejected(
                        request,
                        DamageFailureReason.ROOT_REQUEST_DURING_SETTLEMENT,
                        "A parentless root request cannot be submitted during "
                                + "settlement observation/callback dispatch"
                );
            }
            return null;
        }

        if (!DamageSettlementDispatchScope.accepts(
                parent,
                request.level().getServer()
        )) {
            return rejected(
                    request,
                    DamageFailureReason.PARENT_AUTHORITY_INACTIVE,
                    "The child parent authority is not active for the current "
                            + "registered settlement callback and server lifecycle"
            );
        }
        return null;
    }

    private static boolean markSubmitted(DamageRequest request) {
        synchronized (SUBMITTED_REQUESTS) {
            return SUBMITTED_REQUESTS.put(request, Boolean.TRUE) == null;
        }
    }

    private static @Nullable DamageResult validateRequestBase(
            DamageRequest request
    ) {
        ServerLevel level = request.level();
        LivingEntity target = request.target();

        if (!level.getServer().isSameThread()) {
            return rejected(
                    request,
                    DamageFailureReason.WRONG_THREAD,
                    "Damage requests must run on the owning server thread"
            );
        }

        if (target.level() != level) {
            return rejected(
                    request,
                    DamageFailureReason.TARGET_WRONG_LEVEL,
                    "Damage target is not in the request ServerLevel"
            );
        }

        if (target.isRemoved()) {
            return rejected(
                    request,
                    DamageFailureReason.TARGET_REMOVED,
                    "Damage target has been removed"
            );
        }


        if (!target.isAddedToLevel()) {
            return rejected(
                    request,
                    DamageFailureReason.TARGET_NOT_ADDED,
                    "Damage target is not added to the request ServerLevel"
            );
        }

        if (!target.isAlive() || target.isDeadOrDying()) {
            return rejected(
                    request,
                    DamageFailureReason.TARGET_DEAD,
                    "Damage target is dead or dying"
            );
        }

        return null;
    }

    private static DamageOrigin resolveAttribution(DamageRequest request) {
        DamageOrigin base = request.origin().withResolvedAttribution(
                request.attribution(),
                DamageAttributionProvenance.publicRequest()
        );
        DamageAttributionQuery query = new DamageAttributionQuery(
                request.level(),
                request.target(),
                request.source(),
                Optional.empty(),
                request.kind(),
                request.attribution(),
                request.actionId(),
                request.sourceTags(),
                request.metadata(),
                DamageAttributionEntryPoint.PUBLIC_REQUEST
        );
        return DamageAttributionResolvers.resolve(query, base);
    }

    private static @Nullable DamageResult validateAuthoritativeAttribution(
            DamageRequest request,
            DamageOrigin origin
    ) {
        DamageAttribution attribution = origin.attribution();
        DamageResult roleFailure = validateAttributionStructure(
                request,
                attribution
        );
        if (roleFailure != null) {
            return roleFailure;
        }

        LivingEntity equipmentOwner = attribution.equipmentOwner();
        if (equipmentOwner != null
                && equipmentOwner != attribution.logicalAttacker()
                && origin.attributionSource()
                != DamageAttributionSource.REGISTERED_RESOLVER) {
            return rejected(
                    request,
                    DamageFailureReason.EQUIPMENT_OWNER_UNAUTHORIZED,
                    "Equipment owner is not the logical attacker; direct "
                            + "entities, effect owners, targets, and proxy "
                            + "entities are not trusted equipment owners "
                            + "without a registered server-side attribution resolver"
            );
        }

        return null;
    }

    /**
     * Validates only entity structure and lifecycle. Equipment authorization
     * is intentionally performed after resolver selection.
     */
    private static @Nullable DamageResult validateAttributionStructure(
            DamageRequest request,
            DamageAttribution attribution
    ) {
        DamageResult roleFailure = validateEntityRole(
                request,
                attribution.directEntity(),
                DamageFailureReason.DIRECT_ENTITY_INVALID,
                "direct entity"
        );
        if (roleFailure != null) {
            return roleFailure;
        }

        roleFailure = validateEntityRole(
                request,
                attribution.logicalAttacker(),
                DamageFailureReason.LOGICAL_ATTACKER_INVALID,
                "logical attacker"
        );
        if (roleFailure != null) {
            return roleFailure;
        }

        roleFailure = validateEntityRole(
                request,
                attribution.effectOwner(),
                DamageFailureReason.EFFECT_OWNER_INVALID,
                "effect owner"
        );
        if (roleFailure != null) {
            return roleFailure;
        }

        roleFailure = validateEntityRole(
                request,
                attribution.equipmentOwner(),
                DamageFailureReason.EQUIPMENT_OWNER_INVALID,
                "equipment owner"
        );
        if (roleFailure != null) {
            return roleFailure;
        }

        return null;
    }

    private static @Nullable DamageResult validateEntityRole(
            DamageRequest request,
            @Nullable Entity entity,
            DamageFailureReason reason,
            String roleName
    ) {
        if (entity == null) {
            return null;
        }

        if (entity.level() != request.level()) {
            return rejected(
                    request,
                    reason,
                    "Damage " + roleName + " belongs to another level"
            );
        }

        if (entity.isRemoved()) {
            return rejected(
                    request,
                    reason,
                    "Damage " + roleName + " is removed"
            );
        }

        if (!entity.isAddedToLevel()) {
            return rejected(
                    request,
                    reason,
                    "Damage " + roleName
                            + " is not added to the request ServerLevel"
            );
        }

        return null;
    }

    private static DamageResult rejected(
            DamageRequest request,
            DamageFailureReason reason,
            String diagnostic
    ) {
        return DamageResult.rejected(request, reason, diagnostic);
    }

    private static DamageSource createDamageSource(
            DamageOrigin origin,
            Holder<DamageType> type
    ) {
        Vec3 sourcePosition = origin.source()
                .sourcePosition()
                .orElse(null);

        return new DamageSource(
                type,
                origin.attribution().directEntity(),
                origin.attribution().logicalAttacker(),
                sourcePosition
        );
    }

    private static String admissionDiagnostic(
            DamageRequest request,
            DamageAdmissionResult admission
    ) {
        return "Managed damage admission rejected: reason="
                + admission.reason()
                + ", damageId=" + request.lineage().damageId()
                + ", rootDamageId=" + request.rootDamageId()
                + ", parentDamageId=" + request.parentDamageId()
                + ", depth=" + request.recursionDepth()
                + ", rootDerivedCount="
                + admission.rootDerivedCount()
                + ", serverTickCount="
                + admission.serverTickCount();
    }
}
