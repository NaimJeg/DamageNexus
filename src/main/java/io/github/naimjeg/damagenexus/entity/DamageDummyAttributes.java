package io.github.naimjeg.damagenexus.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

/**
 * Server/common-side attribute catalog for {@link DamageDummyEntity}.
 *
 * <p>The attribute service and GUI use this catalog to enumerate the dummy's
 * real {@link AttributeInstance AttributeInstances}, read their base and
 * effective values, and edit base values. The entity's own
 * {@code AttributeMap} stays authoritative; this class never duplicates
 * attribute state and never mutates AttributeSupplier internals.</p>
 *
 * <p>Presence of an AttributeInstance does not guarantee that the dummy's
 * entity implementation consumes that attribute semantically. Semantics are
 * driven by entity code that reads attributes; attachability and semantic
 * usefulness are deliberately separate concerns for a training dummy.</p>
 *
 * <p><strong>Threading / ownership contract.</strong> Every
 * {@link AttributeInstance} returned by this catalog is live state on the
 * dummy's authoritative server/common {@code AttributeMap}. Reads are safe
 * wherever the entity state is read, but mutation must always happen through
 * the authoritative server-side entity (server code, commands, or a
 * server-authoritative request). A future client {@code Screen} must never
 * mutate a client-side {@code AttributeInstance} and treat that as
 * authoritative: the client copy is presentation state and will be overwritten
 * by server synchronization. The attribute GUI therefore sends requests and
 * displays only snapshots returned by the server.</p>
 */
public final class DamageDummyAttributes {

    /**
     * One attached attribute on a specific dummy. Registry identity, the
     * attribute holder, and the live instance are all exposed so a GUI can
     * display {@code baseValue()}/{@code value()} or edit the instance
     * directly through supported APIs.
     */
    public record AvailableAttribute(
            Identifier id,
            Holder<Attribute> attribute,
            AttributeInstance instance
    ) {
        public double baseValue() {
            return this.instance.getBaseValue();
        }

        public double value() {
            return this.instance.getValue();
        }
    }

    private DamageDummyAttributes() {
    }

    /**
     * All attributes actually attached to this dummy, queried through its real
     * AttributeMap (an instance is materialized through the normal lazy
     * {@code LivingEntity#getAttribute} path). The finalized ATTRIBUTE
     * registry is enumerated on demand and sorted by registry identifier, so
     * ordering is deterministic and independent of registry iteration order.
     * Attribute counts are small and this path is only used by commands,
     * debugging, and GUI snapshots, never per tick, so no cached registry
     * snapshot is needed and there is no registration-lifecycle ordering
     * hazard.
     */
    public static List<AvailableAttribute> availableAttributes(
            DamageDummyEntity dummy
    ) {
        Objects.requireNonNull(dummy, "dummy");
        List<AvailableAttribute> available = new ArrayList<>();
        for (Holder.Reference<Attribute> holder
                : BuiltInRegistries.ATTRIBUTE.listElements()
                .sorted((a, b) -> a.key().identifier()
                        .compareNamespaced(b.key().identifier()))
                .toList()) {
            AttributeInstance instance = dummy.getAttribute(holder);
            if (instance != null) {
                available.add(new AvailableAttribute(
                        holder.key().identifier(),
                        holder,
                        instance
                ));
            }
        }
        return List.copyOf(available);
    }

    /**
     * Finds the attached attribute entry for a registry key, if present.
     */
    public static Optional<AvailableAttribute> find(
            DamageDummyEntity dummy,
            ResourceKey<Attribute> key
    ) {
        Objects.requireNonNull(dummy, "dummy");
        Objects.requireNonNull(key, "key");
        Optional<Holder.Reference<Attribute>> holder =
                BuiltInRegistries.ATTRIBUTE.get(key);
        if (holder.isEmpty()) {
            return Optional.empty();
        }
        AttributeInstance instance = dummy.getAttribute(holder.get());
        return instance == null
                ? Optional.empty()
                : Optional.of(new AvailableAttribute(
                        key.identifier(),
                        holder.get(),
                        instance
                ));
    }

    /** Returns the live AttributeInstance for a registry key, if attached. */
    public static Optional<AttributeInstance> findInstance(
            DamageDummyEntity dummy,
            ResourceKey<Attribute> key
    ) {
        return find(dummy, key).map(AvailableAttribute::instance);
    }

    /**
     * Whether the dummy's AttributeMap supports the given registry attribute
     * (checks the actual supplier/map without materializing an instance).
     */
    public static boolean has(
            DamageDummyEntity dummy,
            ResourceKey<Attribute> key
    ) {
        Objects.requireNonNull(dummy, "dummy");
        Objects.requireNonNull(key, "key");
        return BuiltInRegistries.ATTRIBUTE.get(key)
                .map(holder -> dummy.getAttributes().hasAttribute(holder))
                .orElse(false);
    }

    /** Current base value of the attached instance, if present. */
    public static OptionalDouble getBaseValue(
            DamageDummyEntity dummy,
            ResourceKey<Attribute> key
    ) {
        return find(dummy, key)
                .map(entry -> OptionalDouble.of(entry.baseValue()))
                .orElseGet(OptionalDouble::empty);
    }

    /**
     * Sets the base value on the real AttributeInstance, using its normal
     * validation path (values are sanitized when the effective value is
     * calculated). Returns {@code false} if the attribute is not attached.
     */
    public static boolean setBaseValue(
            DamageDummyEntity dummy,
            ResourceKey<Attribute> key,
            double value
    ) {
        Optional<AttributeInstance> instance = findInstance(dummy, key);
        instance.ifPresent(attributeInstance ->
                attributeInstance.setBaseValue(value));
        return instance.isPresent();
    }

    /**
     * Resets the attached instance to the AttributeSupplier's default base
     * value through the vanilla {@code AttributeMap} API. Returns
     * {@code false} if the attribute is not supported.
     */
    public static boolean resetBaseValue(
            DamageDummyEntity dummy,
            ResourceKey<Attribute> key
    ) {
        Objects.requireNonNull(dummy, "dummy");
        Objects.requireNonNull(key, "key");
        return BuiltInRegistries.ATTRIBUTE.get(key)
                .map(holder -> dummy.getAttributes()
                        .resetBaseValue(holder))
                .orElse(false);
    }
}
