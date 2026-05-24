package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.DamageNexusPreMultiplierBuckets;

import io.github.naimjeg.damagenexus.core.DamageComponent;
import io.github.naimjeg.damagenexus.core.registry.PreMultiplierBucketRegistry;
import io.github.naimjeg.damagenexus.registry.ModAttributes;
import io.github.naimjeg.damagenexus.registry.PreMultiplierBuckets;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Private one-shot handoff after all TYPE_SCALING callbacks have completed. */
final class DamageAttributeScalingEngine {
    private static final String TRACE_PREFIX = "attribute:";

    private DamageAttributeScalingEngine() {
    }

    static void execute(DamageNexusContext context) {
        if (!context.claimAttributeScaling()) {
            throw new IllegalStateException(
                    "Damage attribute scaling already executed for this transaction");
        }

        int componentCount = context.getActiveComponentCount();
        for (int index = 0; index < componentCount; index++) {
            DamageComponent component = context.getActiveComponent(index);
            DamageAttributeMappings.ChannelMapping mapping =
                    DamageAttributeMappings.forChannel(component.channel);
            if (mapping == null) {
                continue;
            }
            float value = finiteOrZero(
                    context.getLogicalAttackerAttrOrZero(mapping.attribute()));
            if (value == 0.0f) {
                continue;
            }
            int bucket = PreMultiplierBucketRegistry.getPreMultiplierBucketId(
                    mapping.bucketId());
            context.tryAddChannelPreMultiplier(
                    component.channel,
                    bucket,
                    value,
                    traceId(mapping.attribute())
            );
            context.trace().calculation().attributeScaling(
                    "channel", component.channel.id().toString(),
                    mapping.attributeId().toString(),
                    mapping.bucketId().toString(), value);
        }

        Holder<Attribute> categoryAttribute = switch (context.attackCategory()) {
            case MELEE -> ModAttributes.MELEE_DAMAGE_ADDITIVE;
            case PROJECTILE -> ModAttributes.PROJECTILE_DAMAGE_ADDITIVE;
            case NONE -> null;
        };
        if (categoryAttribute == null) {
            return;
        }
        float categoryValue = finiteOrZero(
                context.getLogicalAttackerAttrOrZero(categoryAttribute));
        if (categoryValue == 0.0f) {
            return;
        }
        context.tryAddGlobalPreMultiplier(
                PreMultiplierBuckets.genericDamage(),
                categoryValue,
                traceId(categoryAttribute)
        );
        context.trace().calculation().attributeScaling(
                "category", context.attackCategory().name().toLowerCase(),
                categoryAttribute.getKey().identifier().toString(),
                DamageNexusPreMultiplierBuckets.GENERIC_DAMAGE.toString(),
                categoryValue);
    }

    static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static String traceId(Holder<Attribute> attribute) {
        return TRACE_PREFIX + attribute.getKey().identifier();
    }
}
