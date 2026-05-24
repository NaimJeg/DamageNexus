package io.github.naimjeg.damagenexus.api.rule;

import com.mojang.serialization.Codec;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;

import java.util.Locale;

/**
 * Shared codecs for DamageNexus rule JSON.
 * <p>
 * Important:
 * Damage channels must be stored as Identifier in serialized/data-driven rules.
 * Do not eagerly decode them into DamageChannel during JSON parsing, because
 * datapack channel definitions may be reloaded independently from rule data.
 */
public final class DamageRuleCodecs {

    /*
     * ---------------------------------------------------------------------
     * DamageNexus ids
     * ---------------------------------------------------------------------
     */

    /**
     * Serialized damage channel reference.
     * <p>
     * This intentionally stays as Identifier.
     * Runtime code should resolve it through DamageChannelRegistry only when
     * applying the rule.
     */
    public static final Codec<Identifier> DAMAGE_CHANNEL_ID =
            Identifier.CODEC;

    /**
     * Serialized pre-multiplier bucket reference.
     * <p>
     * This also stays as Identifier because external API users may register
     * pre-multiplier buckets during setup before the registry is frozen.
     */
    public static final Codec<Identifier> PRE_MULTIPLIER_BUCKET_ID =
            Identifier.CODEC;

    /*
     * ---------------------------------------------------------------------
     * Minecraft registry ids
     * ---------------------------------------------------------------------
     */

    public static final Codec<Identifier> DAMAGE_TYPE_ID =
            Identifier.CODEC;

    public static final Codec<Identifier> ENTITY_TYPE_ID =
            Identifier.CODEC;

    public static final Codec<Identifier> MOB_EFFECT_ID =
            Identifier.CODEC;

    public static final Codec<Identifier> ATTRIBUTE_ID =
            Identifier.CODEC;

    public static final Codec<Identifier> ITEM_ID =
            Identifier.CODEC;

    /*
     * ---------------------------------------------------------------------
     * Tags
     * ---------------------------------------------------------------------
     */

    public static final Codec<TagKey<DamageType>> DAMAGE_TYPE_TAG =
            TagKey.codec(Registries.DAMAGE_TYPE);

    public static final Codec<TagKey<EntityType<?>>> ENTITY_TYPE_TAG =
            TagKey.codec(Registries.ENTITY_TYPE);

    public static final Codec<TagKey<Item>> ITEM_TAG =
            TagKey.codec(Registries.ITEM);

    public static final Codec<TagKey<MobEffect>> MOB_EFFECT_TAG =
            TagKey.codec(Registries.MOB_EFFECT);

    /*
     * ---------------------------------------------------------------------
     * Enums
     * ---------------------------------------------------------------------
     */

    public static final Codec<MobCategory> MOB_CATEGORY =
            Codec.STRING.xmap(
                    name -> MobCategory.valueOf(name.toUpperCase(Locale.ROOT)),
                    category -> category.name().toLowerCase(Locale.ROOT)
            );

    public static final Codec<DamageApplicationBucket> DAMAGE_APPLICATION_BUCKET =
            DamageApplicationBucket.CODEC;

    /*
     * ---------------------------------------------------------------------
     * Numeric helpers
     * ---------------------------------------------------------------------
     */

    public static final Codec<Float> RATIO_0_TO_1 =
            Codec.floatRange(0.0f, 1.0f);

    public static final Codec<Float> NON_NEGATIVE_FLOAT =
            Codec.floatRange(0.0f, Float.MAX_VALUE);

    /**
     * Additive multiplier value.
     * <p>
     * Example:
     * 0.25 means +25%.
     * -0.20 means -20%.
     */
    public static final Codec<Float> ADDITIVE_MULTIPLIER =
            Codec.FLOAT;


    private DamageRuleCodecs() {
    }
}
