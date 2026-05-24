package io.github.naimjeg.damagenexus.api.rule.source;

import io.github.naimjeg.damagenexus.api.damage.DamageOrigin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

/** Read-only, owner-bound query for one transaction and one direction. */
public record EquippedItemRuleSourceQuery(
        ServerLevel level,
        LivingEntity owner,
        LivingEntity target,
        EquippedItemRuleSourceDirection direction,
        DamageOrigin origin,
        boolean projectileDamage
) {
    public EquippedItemRuleSourceQuery {
        level = Objects.requireNonNull(level, "level");
        owner = Objects.requireNonNull(owner, "owner");
        target = Objects.requireNonNull(target, "target");
        direction = Objects.requireNonNull(direction, "direction");
        origin = Objects.requireNonNull(origin, "origin");
    }
}
