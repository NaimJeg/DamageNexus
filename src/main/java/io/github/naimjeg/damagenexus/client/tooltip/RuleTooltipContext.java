package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.util.IdentifierText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffect;
import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;

import java.text.DecimalFormat;

public final class RuleTooltipContext {

    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    private static String idToLangPath(Identifier id) {
        return id.toString().replace(':', '.');
    }

    public String number(float value) {
        return FORMAT.format(value);
    }

    public String percent(float value) {
        return FORMAT.format(value * 100.0f);
    }

    public String signedNumber(float value) {
        if (value > 0.0f) {
            return "+" + number(value);
        }

        return number(value);
    }

    public String signedPercent(float value) {
        if (value > 0.0f) {
            return "+" + percent(value) + "%";
        }

        return percent(value) + "%";
    }

    public String percentWithSymbol(float value) {
        return percent(value) + "%";
    }

    public String phaseNamePlain(Enum<?> phase) {
        return phase.name();
    }

    public String enumNamePlain(Enum<?> value) {
        return value.name();
    }

    public Component channelName(Identifier id) {
        return Component.translatable("damage_channel." + idToLangPath(id));
    }

    public String channelNamePlain(Identifier id) {
        return id.toString();
    }

    public Component damageTypeName(Identifier id) {
        return Component.translatable("damage_type." + idToLangPath(id));
    }

    public String rawId(Identifier id) {
        return id.toString();
    }

    public MutableComponent effectName(Identifier effectId) {
        return Component.translatable(
                "effect." + IdentifierText.namespace(effectId) + "." + IdentifierText.path(effectId)
        );
    }

    public MutableComponent entityTypeName(Identifier entityTypeId) {
        return Component.translatable(
                "entity." + IdentifierText.namespace(entityTypeId) + "." + IdentifierText.path(entityTypeId)
        );
    }

    public String entityTypeNamePlain(Identifier entityTypeId) {
        return entityTypeId.toString();
    }

    public MutableComponent entityTypeTagName(TagKey<EntityType<?>> tag) {
        return tagName(
                "tag.entity_type.",
                tag
        );
    }

    public MutableComponent damageTypeTagName(TagKey<DamageType> tag) {
        return tagName(
                "tag.damage_type.",
                tag
        );
    }

    public MutableComponent mobEffectTagName(TagKey<MobEffect> tag) {
        return tagName(
                "tag.mob_effect.",
                tag
        );
    }

    public MutableComponent requestKindName(DamageRequestKind kind) {
        return Component.translatable(
                "damage_request_kind.damagenexus."
                        + kind.serializedName()
        );
    }

    private MutableComponent tagName(
            String prefix,
            TagKey<?> tag
    ) {
        if (tag == null) {
            return Component.literal("#<null>");
        }

        Identifier id = tag.location();
        String fallback = "#" + id;

        return Component.translatableWithFallback(
                prefix + IdentifierText.langPath(id),
                fallback
        );
    }
}

