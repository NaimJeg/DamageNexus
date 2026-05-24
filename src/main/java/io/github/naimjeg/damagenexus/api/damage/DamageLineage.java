package io.github.naimjeg.damagenexus.api.damage;

import org.jetbrains.annotations.ApiStatus;

import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Immutable identity and ancestry of one independent damage transaction.
 *
 * <p>Raw identifiers cannot be supplied by callers. Use a
 * {@link DamageRequest.Builder} and its {@code parent(...)} methods.</p>
 */
public final class DamageLineage {

    private static final AtomicLong NEXT_DAMAGE_ID = new AtomicLong();

    private final long damageId;
    private final long rootDamageId;
    private final OptionalLong parentDamageId;
    private final int recursionDepth;
    /* Not part of value equality, diagnostics, or public serialization. */
    private final DamageRootDerivationBudget rootDerivationBudget;

    private DamageLineage(
            long damageId,
            long rootDamageId,
            OptionalLong parentDamageId,
            int recursionDepth,
            DamageRootDerivationBudget rootDerivationBudget
    ) {
        if (damageId <= 0L || rootDamageId <= 0L) {
            throw new IllegalArgumentException(
                    "Damage lineage identifiers must be positive"
            );
        }

        if (recursionDepth < 0) {
            throw new IllegalArgumentException(
                    "Damage recursion depth must not be negative"
            );
        }

        this.damageId = damageId;
        this.rootDamageId = rootDamageId;
        this.parentDamageId = parentDamageId == null
                ? OptionalLong.empty()
                : parentDamageId;
        this.recursionDepth = recursionDepth;
        this.rootDerivationBudget = java.util.Objects.requireNonNull(
                rootDerivationBudget,
                "rootDerivationBudget"
        );
    }

    /**
     * Allocates a new root lineage.
     *
     * <p>Most callers should let {@link DamageRequest.Builder} allocate this
     * value. The factory is public so framework adapters can create native
     * root requests without exposing a raw-id constructor.</p>
     */
    public static DamageLineage newRoot() {
        long id = nextId();
        return new DamageLineage(
                id,
                id,
                OptionalLong.empty(),
                0,
                new DamageRootDerivationBudget()
        );
    }

    /** Creates a new child with this lineage as its direct parent. */
    public DamageLineage newChild() {
        int childDepth;

        try {
            childDepth = Math.addExact(recursionDepth, 1);
        } catch (ArithmeticException exception) {
            throw new IllegalStateException(
                    "Damage recursion depth overflow",
                    exception
            );
        }

        return new DamageLineage(
                nextId(),
                rootDamageId,
                OptionalLong.of(damageId),
                childDepth,
                rootDerivationBudget
        );
    }

    public long damageId() {
        return damageId;
    }

    public long rootDamageId() {
        return rootDamageId;
    }

    public OptionalLong parentDamageId() {
        return parentDamageId;
    }

    public int recursionDepth() {
        return recursionDepth;
    }

    public boolean hasParent() {
        return parentDamageId.isPresent();
    }

    /** Framework-only atomic reservation against this root's shared budget. */
    @ApiStatus.Internal
    public boolean reserveDerivedRequestInternal(int maximum) {
        if (!hasParent()) {
            throw new IllegalStateException(
                    "Root damage does not consume a derived-request slot"
            );
        }
        return rootDerivationBudget.tryReserve(maximum);
    }

    /** Framework-only diagnostic count for this root's shared budget. */
    @ApiStatus.Internal
    public int derivedRequestCountInternal() {
        return rootDerivationBudget.count();
    }

    private static long nextId() {
        long id = NEXT_DAMAGE_ID.incrementAndGet();

        if (id <= 0L) {
            throw new IllegalStateException(
                    "Damage identifier space has been exhausted"
            );
        }

        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof DamageLineage lineage)) {
            return false;
        }

        return damageId == lineage.damageId
                && rootDamageId == lineage.rootDamageId
                && recursionDepth == lineage.recursionDepth
                && parentDamageId.equals(lineage.parentDamageId);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(damageId);
        result = 31 * result + Long.hashCode(rootDamageId);
        result = 31 * result + parentDamageId.hashCode();
        result = 31 * result + recursionDepth;
        return result;
    }

    @Override
    public String toString() {
        return "DamageLineage[damageId="
                + damageId
                + ", rootDamageId="
                + rootDamageId
                + ", parentDamageId="
                + parentDamageId
                + ", recursionDepth="
                + recursionDepth
                + ']';
    }
}
