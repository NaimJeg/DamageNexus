package io.github.naimjeg.damagenexus.command.test;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.Objects;

/**
 * Sets a single DamageNexus resistance attribute base value on a test mob.
 *
 * <p>Semantics are SET, not ADD: the command sets the attribute base value,
 * rather than stacking an {@code AttributeModifier}. The mutation API itself
 * rejects non-finite and out-of-range values so GameTests and Java callers
 * cannot bypass the Brigadier value range.</p>
 *
 * <p>Example: {@code mutation resistance fire 50} must leave
 * {@code mob.getAttribute(RESISTANCE_FIRE).getBaseValue() == 50.0}, not
 * {@code old + 50}.</p>
 */
public record ResistanceMutation(
        TestResistance resistance,
        double value
) implements TestMobMutation {

    public ResistanceMutation {
        Objects.requireNonNull(
                resistance,
                "resistance must not be null"
        );
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Resistance mutation value must be finite: " + value
            );
        }
        if (value < TestResistance.MIN_ATTRIBUTE_VALUE
                || value > TestResistance.MAX_ATTRIBUTE_VALUE) {
            throw new IllegalArgumentException(
                    "Resistance mutation value out of legal range: "
                            + value
            );
        }
    }

    @Override
    public TestMobFactory.SpawnFailure apply(Mob mob) {
        AttributeInstance instance = mob.getAttribute(resistance.attribute());
        if (instance == null) {
            return TestMobFactory.SpawnFailure.MUTATION_ATTRIBUTE_MISSING;
        }
        instance.setBaseValue(value);
        return null;
    }
}
