package io.github.naimjeg.damagenexus.core.request;

import io.github.naimjeg.damagenexus.api.damage.DamageFailureReason;
import io.github.naimjeg.damagenexus.api.damage.DamageOrigin;
import io.github.naimjeg.damagenexus.api.damage.DamageParentRef;
import io.github.naimjeg.damagenexus.api.damage.DamageRequest;
import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;
import io.github.naimjeg.damagenexus.api.damage.DamageTriggerPolicy;
import io.github.naimjeg.damagenexus.config.DamageSafetySettings;
import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/** Atomic Phase 4 trigger, lineage, root, and server-tick admission. */
public final class DamageAdmissionController {

    private static final Map<MinecraftServer, DamageServerTickBudget> SERVERS =
            new WeakHashMap<>();

    private DamageAdmissionController() {
    }

    public static DamageAdmissionResult admitPublic(DamageRequest request) {
        Objects.requireNonNull(request, "request");
        DamageSafetySettings settings = DamageNexusSettings.safety();
        DamageParentRef parent = request.parentRefInternal();

        DamageFailureReason triggerFailure = triggerFailure(
                request.kind(),
                parent == null ? null : parent.triggerPolicy()
        );
        if (triggerFailure != null) {
            return rejectedWithoutConsumption(
                    request.origin(),
                    request.level().getServer(),
                    triggerFailure
            );
        }

        DamageFailureReason depthFailure = depthFailure(
                request.recursionDepth(),
                settings.maxRecursionDepth()
        );
        if (depthFailure != null) {
            return rejectedWithoutConsumption(
                    request.origin(),
                    request.level().getServer(),
                    depthFailure
            );
        }

        return budget(request.origin(), request.level().getServer())
                .tryAdmit(
                        request.lineage(),
                        request.level().getServer().getTickCount(),
                        settings
                );
    }

    public static DamageAdmissionResult admitNative(
            DamageOrigin origin,
            MinecraftServer server
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(server, "server");
        DamageSafetySettings settings = DamageNexusSettings.safety();

        DamageFailureReason depthFailure = depthFailure(
                origin.lineage().recursionDepth(),
                settings.maxRecursionDepth()
        );
        if (depthFailure != null) {
            return rejectedWithoutConsumption(
                    origin,
                    server,
                    depthFailure
            );
        }

        return budget(origin, server).tryAdmit(
                origin.lineage(),
                server.getTickCount(),
                settings
        );
    }

    static @Nullable DamageFailureReason triggerFailure(
            DamageRequestKind kind,
            @Nullable DamageTriggerPolicy parentPolicy
    ) {
        if (parentPolicy == null) {
            return null;
        }
        return switch (kind) {
            case PROC -> parentPolicy.procAllowed()
                    ? null
                    : DamageFailureReason.PROC_SUPPRESSED;
            case REFLECTED -> parentPolicy.reflectionAllowed()
                    ? null
                    : DamageFailureReason.REFLECTION_SUPPRESSED;
            case THORNS -> parentPolicy.thornsAllowed()
                    ? null
                    : DamageFailureReason.THORNS_SUPPRESSED;
            case PRIMARY, DOT, ENVIRONMENTAL, CUSTOM -> null;
        };
    }

    static @Nullable DamageFailureReason depthFailure(
            int depth,
            int maximum
    ) {
        if (depth < 0 || maximum < 1) {
            throw new IllegalArgumentException(
                    "Depth must be non-negative and maximum must be positive"
            );
        }
        return depth > maximum
                ? DamageFailureReason.MAX_RECURSION_DEPTH
                : null;
    }

    public static void clearServer(MinecraftServer server) {
        synchronized (SERVERS) {
            SERVERS.remove(server);
        }
    }

    public static DamageAdmissionResult rejectEventReentrancy(
            DamageOrigin origin,
            MinecraftServer server
    ) {
        return rejectedWithoutConsumption(
                Objects.requireNonNull(origin, "origin"),
                Objects.requireNonNull(server, "server"),
                DamageFailureReason.EVENT_REENTRANCY_LIMIT
        );
    }

    static int currentTickCount(MinecraftServer server) {
        synchronized (SERVERS) {
            DamageServerTickBudget state = SERVERS.get(server);
            return state == null ? 0 : state.count(server.getTickCount());
        }
    }

    private static DamageAdmissionResult rejectedWithoutConsumption(
            DamageOrigin origin,
            MinecraftServer server,
            DamageFailureReason reason
    ) {
        return DamageAdmissionResult.rejected(
                reason,
                origin.lineage().derivedRequestCountInternal(),
                currentTickCount(server)
        );
    }

    private static DamageServerTickBudget budget(
            DamageOrigin origin,
            MinecraftServer server
    ) {
        Objects.requireNonNull(origin, "origin");
        synchronized (SERVERS) {
            return SERVERS.computeIfAbsent(
                    Objects.requireNonNull(server, "server"),
                    ignored -> new DamageServerTickBudget()
            );
        }
    }
}
