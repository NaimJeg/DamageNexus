package io.github.naimjeg.damagenexus.command.test;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class TestTargetSelector {

    private static final String TEST_NAME_MARKER = "[DN-Test]";
    private static final double DEFAULT_RANGE = 24.0D;

    private TestTargetSelector() {
    }

    public static LivingEntity nearestTestLiving(CommandSourceStack source) {
        return nearestTestLiving(source, DEFAULT_RANGE);
    }

    public static LivingEntity nearestTestLiving(
            CommandSourceStack source,
            double range
    ) {
        Vec3 center = source.getPosition();

        return source.getLevel()
                .getEntitiesOfClass(
                        LivingEntity.class,
                        AABB.ofSize(
                                center,
                                range * 2.0D,
                                range * 2.0D,
                                range * 2.0D
                        ),
                        TestTargetSelector::isTestLiving
                )
                .stream()
                .min(Comparator.comparingDouble(
                        entity -> entity.distanceToSqr(center)
                ))
                .orElse(null);
    }

    public static boolean isTestEntityName(Component name) {
        return name != null
                && name.getString().contains(TEST_NAME_MARKER);
    }

    public static boolean isTestLiving(LivingEntity entity) {
        return entity != null
                && entity.isAlive()
                && isTestEntityName(entity.getCustomName());
    }
}
