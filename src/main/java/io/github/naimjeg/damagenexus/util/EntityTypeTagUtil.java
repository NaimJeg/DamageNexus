package io.github.naimjeg.damagenexus.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class EntityTypeTagUtil {

    private EntityTypeTagUtil() {
    }

    public static boolean is(EntityType<?> type, TagKey<EntityType<?>> tag) {
        return type != null
                && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(tag);
    }
}
