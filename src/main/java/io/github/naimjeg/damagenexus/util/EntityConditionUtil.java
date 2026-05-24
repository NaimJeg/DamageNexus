package io.github.naimjeg.damagenexus.util;

import io.github.naimjeg.damagenexus.api.DamageNexusTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class EntityConditionUtil {

    private static final Identifier ENDER_DRAGON_ID =
            Identifier.fromNamespaceAndPath("minecraft", "ender_dragon");

    private static final Identifier WITHER_ID =
            Identifier.fromNamespaceAndPath("minecraft", "wither");

    private EntityConditionUtil() {
    }

    public static boolean isEntityType(
            Entity entity,
            Identifier expected
    ) {
        if (entity == null) {
            return false;
        }

        Identifier actual =
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        return expected.equals(actual);
    }

    public static boolean isEntityTypeTag(
            Entity entity,
            TagKey<EntityType<?>> tag
    ) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()).is(tag);
    }

    public static boolean isBoss(Entity entity) {
        if (entity == null) {
            return false;
        }

        if (isEntityTypeTag(
                entity,
                DamageNexusTags.EntityTypes.BOSSES
        )) {
            return true;
        }

        Identifier id =
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        /*
         * Vanilla fallback.
         * Datapacks can add more boss-like entities through:
         * data/damagenexus/tags/entity_type/bosses.json
         */
        return ENDER_DRAGON_ID.equals(id)
                || WITHER_ID.equals(id);
    }
}
