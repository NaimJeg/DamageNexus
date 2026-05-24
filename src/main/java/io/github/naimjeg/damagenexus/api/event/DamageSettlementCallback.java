package io.github.naimjeg.damagenexus.api.event;

import io.github.naimjeg.damagenexus.api.damage.DamageParentRef;
import io.github.naimjeg.damagenexus.api.damage.DamageSettlementSnapshot;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementCallbacks;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementDispatchScope;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Read-only invocation object for a registered
 * {@link DamageSettlementListener}.
 *
 * <p>The child authority is visible only while this exact invocation is
 * running on its owning server. Every registered listener invocation receives
 * a distinct authority object even when all listeners observe the same parent
 * settlement. Saving this object or its authority does not extend that
 * lifetime.</p>
 */
public final class DamageSettlementCallback {

    private final DamageSettlementSnapshot snapshot;
    private final @Nullable DamageParentRef authority;

    private DamageSettlementCallback(
            DamageSettlementSnapshot snapshot,
            @Nullable DamageParentRef authority
    ) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.authority = authority;
    }

    /** Framework-only construction boundary. */
    @ApiStatus.Internal
    public static DamageSettlementCallback createInternal(
            DamageSettlementSnapshot snapshot,
            @Nullable DamageParentRef authority
    ) {
        if (StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .getCallerClass() != DamageSettlementCallbacks.class) {
            throw new SecurityException(
                    "Only the settlement callback dispatcher may create an invocation"
            );
        }
        return new DamageSettlementCallback(snapshot, authority);
    }

    public DamageSettlementSnapshot snapshot() {
        return snapshot;
    }

    /**
     * Returns authority for independent derived requests only during this
     * exact callback invocation. NOT_APPLIED settlements return empty.
     */
    public Optional<DamageParentRef> childAuthority() {
        DamageParentRef candidate = authority;
        if (candidate == null || !DamageSettlementDispatchScope.exposesAuthority(
                this,
                candidate,
                snapshot.level().getServer()
        )) {
            return Optional.empty();
        }
        return Optional.of(candidate);
    }
}
