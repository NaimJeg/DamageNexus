package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.DamageNexusPreMultiplierBuckets;

import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import io.github.naimjeg.damagenexus.registry.ModAttributes;
import io.github.naimjeg.damagenexus.registry.PreMultiplierBuckets;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Single authoritative mapping for the seven built-in channel attributes. */
final class DamageAttributeMappings {
    private static final List<ChannelMapping> CHANNEL_MAPPINGS = List.of(
            new ChannelMapping(DamageChannel.FIRE_ID,
                    DamageNexusIds.id("fire_damage_additive"),
                    () -> ModAttributes.FIRE_DAMAGE_ADDITIVE,
                    DamageNexusPreMultiplierBuckets.FIRE_DAMAGE),
            new ChannelMapping(DamageChannel.COLD_ID,
                    DamageNexusIds.id("cold_damage_additive"),
                    () -> ModAttributes.COLD_DAMAGE_ADDITIVE,
                    DamageNexusPreMultiplierBuckets.COLD_DAMAGE),
            new ChannelMapping(DamageChannel.LIGHTNING_ID,
                    DamageNexusIds.id("lightning_damage_additive"),
                    () -> ModAttributes.LIGHTNING_DAMAGE_ADDITIVE,
                    DamageNexusPreMultiplierBuckets.LIGHTNING_DAMAGE),
            new ChannelMapping(DamageChannel.MAGIC_ID,
                    DamageNexusIds.id("magic_damage_additive"),
                    () -> ModAttributes.MAGIC_DAMAGE_ADDITIVE,
                    DamageNexusPreMultiplierBuckets.MAGIC_DAMAGE),
            new ChannelMapping(DamageChannel.WITHER_ID,
                    DamageNexusIds.id("wither_damage_additive"),
                    () -> ModAttributes.WITHER_DAMAGE_ADDITIVE,
                    DamageNexusPreMultiplierBuckets.WITHER_DAMAGE),
            new ChannelMapping(DamageChannel.POISON_ID,
                    DamageNexusIds.id("poison_damage_additive"),
                    () -> ModAttributes.POISON_DAMAGE_ADDITIVE,
                    DamageNexusPreMultiplierBuckets.POISON_DAMAGE),
            new ChannelMapping(DamageChannel.KINETIC_ID,
                    DamageNexusIds.id("kinetic_damage_additive"),
                    () -> ModAttributes.KINETIC_DAMAGE_ADDITIVE,
                    DamageNexusPreMultiplierBuckets.KINETIC_DAMAGE)
    );
    private static final Map<Identifier, ChannelMapping> BY_CHANNEL = index();

    private DamageAttributeMappings() {
    }

    static List<ChannelMapping> channelMappings() {
        return CHANNEL_MAPPINGS;
    }

    static ChannelMapping forChannel(DamageChannel channel) {
        return channel == null ? null : BY_CHANNEL.get(channel.id());
    }

    private static Map<Identifier, ChannelMapping> index() {
        HashMap<Identifier, ChannelMapping> mappings = new HashMap<>();
        Set<Identifier> attributes = new HashSet<>();
        Set<Identifier> buckets = new HashSet<>();
        for (ChannelMapping mapping : CHANNEL_MAPPINGS) {
            if (mappings.put(mapping.channelId(), mapping) != null
                    || !attributes.add(mapping.attributeId())
                    || !buckets.add(mapping.bucketId())) {
                throw new ExceptionInInitializerError(
                        "Duplicate built-in damage attribute mapping: " + mapping);
            }
        }
        return Map.copyOf(mappings);
    }

    record ChannelMapping(
            Identifier channelId,
            Identifier attributeId,
            Supplier<Holder<Attribute>> attributeSupplier,
            Identifier bucketId
    ) {
        ChannelMapping {
            if (channelId == null || attributeId == null
                    || attributeSupplier == null || bucketId == null) {
                throw new NullPointerException("Damage attribute mapping fields");
            }
        }

        Holder<Attribute> attribute() {
            return attributeSupplier.get();
        }
    }
}
