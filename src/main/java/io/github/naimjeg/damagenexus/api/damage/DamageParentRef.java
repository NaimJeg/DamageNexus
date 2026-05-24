package io.github.naimjeg.damagenexus.api.damage;

import net.minecraft.resources.Identifier;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementCallbacks;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable, callback-scoped authority for constructing a child request.
 *
 * <p>Valid instances are exposed only by the official synchronous
 * registered DamageNexus settlement callback. Raw lineage values, snapshots,
 * results, unsubmitted requests, and saved references outside that callback
 * cannot be used as child authority.</p>
 */
public final class DamageParentRef {

    private final DamageOrigin parentOrigin;

    DamageParentRef(DamageOrigin parentOrigin) {
        this.parentOrigin = Objects.requireNonNull(
                parentOrigin,
                "parentOrigin"
        );
    }

    /**
     * Framework-only authority factory owned by the settlement callback
     * dispatcher. Each registered callback invocation receives a fresh
     * instance wrapping the same completed parent origin.
     */
    @ApiStatus.Internal
    public static DamageParentRef createInternal(DamageOrigin parentOrigin) {
        if (StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .getCallerClass() != DamageSettlementCallbacks.class) {
            throw new SecurityException(
                    "Only the settlement callback dispatcher may create child authority"
            );
        }
        return new DamageParentRef(parentOrigin);
    }

    public DamageLineage lineage() {
        return parentOrigin.lineage();
    }

    /** Permissions this completed parent grants to immediate child kinds. */
    public DamageTriggerPolicy triggerPolicy() {
        return parentOrigin.triggerPolicy();
    }

    public DamageAttribution attribution() {
        return parentOrigin.attribution();
    }

    public DamageSourceDescriptor source() {
        return parentOrigin.source();
    }

    public Optional<Identifier> actionId() {
        return parentOrigin.actionId();
    }

    public Set<Identifier> sourceTags() {
        return parentOrigin.sourceTags();
    }

    public DamageMetadata metadata() {
        return parentOrigin.metadata();
    }
}
