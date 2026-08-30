package io.github.naimjeg.damagenexus.command.test;

import net.minecraft.world.entity.Mob;

/**
 * A synchronous mutation applied to a freshly constructed test mob before it is
 * added to its server level.
 *
 * <p>This is a developer/test facility, not a formal datapack mob modifier and
 * not an entity-generation event system. Mutations must run before
 * {@code level.addFreshEntity(mob)} so the constructed entity already carries
 * its final attribute state when it enters the world.</p>
 */
public sealed interface TestMobMutation permits ResistanceMutation {

    /**
     * Applies this mutation to the test mob.
     *
     * @return {@code null} on success, or a {@link TestMobFactory.SpawnFailure}
     *         that must prevent the mob from being added to the level
     */
    TestMobFactory.SpawnFailure apply(Mob mob);
}
