package io.github.naimjeg.damagenexus.entity;

import io.github.naimjeg.damagenexus.DamageNexus;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * GameTest-only sentinel Attribute that proves the damage dummy's universal,
 * registry-driven attribute attachment.
 *
 * <p>This attribute is deliberately unknown to {@link DamageDummyEntity},
 * {@code ModEntityAttributes}, and {@code ModEntityTypes}. It is registered
 * only when NeoForge's GameTest facility is enabled, through the ATTRIBUTE
 * registry's {@link RegisterEvent}. NeoForge posts every registry's
 * RegisterEvent (ATTRIBUTE first) before firing
 * {@code EntityAttributeModificationEvent}, so by the time the dummy's
 * attribute lifecycle runs this sentinel is a normal member of the ATTRIBUTE
 * registry and is attached generically like any third-party attribute.</p>
 *
 * <p>In production (GameTest disabled) the sentinel is never registered and is
 * not part of DamageNexus content.</p>
 */
@EventBusSubscriber(modid = DamageNexus.MODID)
public final class DamageDummyTestAttribute {

    /** Distinctive nonzero default, deliberately unlike any production value. */
    public static final double DEFAULT_BASE = 7.0D;
    public static final double TEST_MIN = 0.0D;
    public static final double TEST_MAX = 1024.0D;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(
            DamageNexus.MODID,
            "damage_dummy_test_attribute"
    );

    public static final ResourceKey<Attribute> KEY =
            ResourceKey.create(Registries.ATTRIBUTE, ID);

    private DamageDummyTestAttribute() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        event.register(
                Registries.ATTRIBUTE,
                ID,
                () -> new RangedAttribute(
                        "attribute.name.damagenexus.damage_dummy_test_attribute",
                        DEFAULT_BASE,
                        TEST_MIN,
                        TEST_MAX
                )
        );
    }

    /** The sentinel's holder once registered (empty when GameTest is off). */
    public static Optional<Holder.Reference<Attribute>> holder() {
        return BuiltInRegistries.ATTRIBUTE.get(KEY);
    }
}
