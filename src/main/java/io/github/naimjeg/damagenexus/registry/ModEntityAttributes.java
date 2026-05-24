package io.github.naimjeg.damagenexus.registry;

import io.github.naimjeg.damagenexus.entity.DamageDummyEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

/**
 * Attribute lifecycle wiring for {@link DamageDummyEntity}.
 *
 * <p>Lifecycle: NeoForge posts every registry's RegisterEvent (the ATTRIBUTE
 * registry is registered first), then {@code EntityAttributeCreationEvent},
 * then {@code EntityAttributeModificationEvent}, before registries freeze.
 * Both events therefore observe the finalized registered ATTRIBUTE registry,
 * including vanilla, NeoForge, DamageNexus, and third-party attributes.</p>
 *
 * <p>There is no "applicable to entity type" predicate on the Attribute API:
 * an attribute is attachable to any LivingEntity AttributeMap. Whether the
 * dummy's code consumes a given attribute semantically is a separate concern;
 * {@code DamageDummyAttributes} enumerates whatever the lifecycle attached.</p>
 *
 * <p>This class only wires the entity attribute lifecycle. The catalog itself
 * enumerates the finalized ATTRIBUTE registry on demand, so nothing here needs
 * to snapshot or cache registry state.</p>
 */
public final class ModEntityAttributes {

    private ModEntityAttributes() {
    }

    /**
     * Establishes a valid initial supplier so the dummy behaves as a normal
     * LivingEntity from construction onward.
     */
    public static void onCreateAttributes(EntityAttributeCreationEvent event) {
        event.put(
                ModEntityTypes.DAMAGE_DUMMY.get(),
                DamageDummyEntity.createAttributes().build()
        );
    }

    /**
     * Attaches every registered entity Attribute not already present, using
     * each attribute's declared default base value. This is deliberately
     * registry-driven: no namespace filter, no allowlist, no manual
     * enumeration of vanilla or DamageNexus attributes. A newly registered
     * third-party Attribute becomes available to the dummy with zero changes
     * to this source file.
     */
    public static void onModifyAttributes(
            EntityAttributeModificationEvent event
    ) {
        EntityType<DamageDummyEntity> type =
                ModEntityTypes.DAMAGE_DUMMY.get();
        for (Holder.Reference<Attribute> holder
                : BuiltInRegistries.ATTRIBUTE.listElements().toList()) {
            if (!event.has(type, holder)) {
                event.add(type, holder);
            }
        }
    }
}
