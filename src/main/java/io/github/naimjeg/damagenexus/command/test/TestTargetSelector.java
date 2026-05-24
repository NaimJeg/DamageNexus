package io.github.naimjeg.damagenexus.command.test;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class TestTargetSelector {

    private static final String LEGACY_TEST_NAME_MARKER = "[DN-Test]";
    private static final String ENGLISH_TEST_NAME_MARKER =
            "[DamageNexus Test]";
    private static final String CHINESE_TEST_NAME_MARKER =
            "[伤害枢纽测试]";
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
        if (name == null) {
            return false;
        }

        String value = name.getString();
        return value.contains(LEGACY_TEST_NAME_MARKER)
                || value.contains(ENGLISH_TEST_NAME_MARKER)
                || value.contains(CHINESE_TEST_NAME_MARKER);
    }

    public static boolean isTestLiving(LivingEntity entity) {
        return entity != null
                && entity.isAlive()
                && isTestEntityName(entity.getCustomName());
    }
}
