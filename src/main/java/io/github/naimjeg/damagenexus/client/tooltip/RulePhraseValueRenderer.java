package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.client.phrase.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.text.NumberFormat;
import java.util.Locale;

final class RulePhraseValueRenderer {
    Component render(PhraseValue value, PhraseForm form, Locale locale) {
        return switch (value) {
            case NumberValue number -> Component.literal(number(number.value(), form, locale));
            case PercentValue percent -> Component.literal(percent(percent.ratio(), form, locale));
            case ChannelValue channel -> named(
                    "damage_channel.", channel.channelId(), humanize(channel.channelId())
            );
            case EntityRoleValue role -> Component.translatable(
                    "entity_role.damagenexus." + role.role().serializedName()
            );
            case EffectValue effect -> named(
                    "effect.", effect.effectId(), humanize(effect.effectId())
            );
            case EntityTypeValue entity -> named(
                    "entity.", entity.entityTypeId(), humanize(entity.entityTypeId())
            );
            case TagValue tag -> named(
                    "tag." + tag.kind().serializedName() + ".",
                    tag.tagId(),
                    humanize(tag.tagId())
            );
            case RequestKindValue request -> Component.translatable(
                    request.kind().translationKey()
            );
            case IdentifierValue identifier -> named(
                    "identifier.", identifier.identifier(), humanize(identifier.identifier())
            );
            case MobCategoryValue category -> Component.translatableWithFallback(
                    "mob_category.damagenexus."
                            + category.category().getSerializedName(),
                    humanize(category.category().name())
            );
            case NestedPhraseValue ignored -> Component.empty();
        };
    }

    private static Component named(String prefix, Identifier id, String fallback) {
        return Component.translatableWithFallback(
                prefix + id.getNamespace() + "." + id.getPath().replace('/', '.'),
                fallback
        );
    }

    private static String number(double value, PhraseForm form, Locale locale) {
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setGroupingUsed(false);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(form == PhraseForm.COMPACT ? 2 : 4);
        return format.format(value == -0.0d ? 0.0d : value);
    }

    private static String percent(double ratio, PhraseForm form, Locale locale) {
        NumberFormat format = NumberFormat.getPercentInstance(locale);
        format.setGroupingUsed(false);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(form == PhraseForm.COMPACT ? 1 : 2);
        return format.format(ratio == -0.0d ? 0.0d : ratio);
    }

    private static String humanize(Identifier id) {
        return humanize(id.getPath());
    }

    private static String humanize(String raw) {
        String[] words = raw.replace('/', '_').split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }
}
