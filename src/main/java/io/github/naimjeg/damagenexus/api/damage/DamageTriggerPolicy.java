package io.github.naimjeg.damagenexus.api.damage;

/**
 * Monotonic upper bounds for downstream damage-trigger categories.
 *
 * <p>A request's final policy is the intersection of the final request-kind
 * default, all caller restrictions, and its parent settlement's downstream
 * policy. Builder setters therefore tighten an upper bound; they never force
 * a disabled trigger back on.</p>
 */
public record DamageTriggerPolicy(
        boolean procAllowed,
        boolean reflectionAllowed,
        boolean thornsAllowed
) {
    public static final DamageTriggerPolicy ALL_ALLOWED =
            new DamageTriggerPolicy(true, true, true);

    public static final DamageTriggerPolicy NONE_ALLOWED =
            new DamageTriggerPolicy(false, false, false);

    public static final DamageTriggerPolicy PROC_SUPPRESSED =
            new DamageTriggerPolicy(false, true, true);

    public static final DamageTriggerPolicy REFLECTION_SUPPRESSED =
            new DamageTriggerPolicy(true, false, true);

    public static final DamageTriggerPolicy THORNS_SUPPRESSED =
            new DamageTriggerPolicy(true, true, false);

    public boolean procSuppressed() {
        return !procAllowed;
    }

    /** Returns the framework default for a new root or child kind. */
    public static DamageTriggerPolicy defaultsFor(DamageRequestKind kind) {
        if (kind == null) {
            throw new IllegalArgumentException(
                    "Damage request kind must not be null"
            );
        }

        return switch (kind) {
            case PROC -> PROC_SUPPRESSED;
            case REFLECTED -> REFLECTION_SUPPRESSED;
            case THORNS -> THORNS_SUPPRESSED;
            default -> ALL_ALLOWED;
        };
    }

    public DamageTriggerPolicy intersect(DamageTriggerPolicy other) {
        if (other == null) {
            return this;
        }

        return new DamageTriggerPolicy(
                procAllowed && other.procAllowed,
                reflectionAllowed && other.reflectionAllowed,
                thornsAllowed && other.thornsAllowed
        );
    }
}
