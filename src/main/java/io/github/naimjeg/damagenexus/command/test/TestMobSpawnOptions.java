package io.github.naimjeg.damagenexus.command.test;

import io.github.naimjeg.damagenexus.DamageNexus;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Configuration applied before a test mob is added to its server level. */
public enum TestMobSpawnOptions {
    DEFAULT(false),
    IMMORTAL_EXHIBIT(true);

    private static final Identifier EXHIBIT_KNOCKBACK_RESISTANCE_ID =
            Identifier.fromNamespaceAndPath(
                    DamageNexus.MODID,
                    "test_exhibit_knockback_resistance"
            );

    private final boolean immortal;

    TestMobSpawnOptions(boolean immortal) {
        this.immortal = immortal;
    }

    public boolean immortal() {
        return immortal;
    }

    void apply(Mob mob) {
        mob.addTag(TestMobTags.TEST_ENTITY);
        if (!immortal) {
            return;
        }

        mob.addTag(TestMobTags.IMMORTAL);
        mob.setPersistenceRequired();
        mob.setNoAi(true);
        mob.setDeltaMovement(Vec3.ZERO);

        AttributeInstance knockback = mob.getAttribute(
                Attributes.KNOCKBACK_RESISTANCE
        );
        if (knockback != null) {
            knockback.addOrReplacePermanentModifier(new AttributeModifier(
                    EXHIBIT_KNOCKBACK_RESISTANCE_ID,
                    1.0D,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }
}
