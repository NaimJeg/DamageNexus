package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.display.DamageContributionOperationKind;
import io.github.naimjeg.damagenexus.api.display.DamageContributionSourceKind;
import io.github.naimjeg.damagenexus.api.display.DamageContributionSummary;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.util.IdentifierText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.text.DecimalFormat;
import java.util.Locale;

public final class CombatContributionComponentFormatter {

    private static final DecimalFormat FORMAT =
            new DecimalFormat("0.##");

    private CombatContributionComponentFormatter() {
    }

    public static MutableComponent compact(
            DamageContributionSummary summary
    ) {
        if (summary == null) {
            return Component.translatable(
                    "tooltip.damagenexus.contribution.unknown"
            );
        }

        return Component.translatable(
                "tooltip.damagenexus.contribution.line",
                source(summary),
                operation(summary),
                target(summary),
                count(summary)
        );
    }

    private static MutableComponent source(
            DamageContributionSummary summary
    ) {
        if (summary.displayName().isPresent()) {
            return Component.literal(summary.displayName().get());
        }

        if (summary.displayGroup().isPresent()) {
            MutableComponent result =
                    Component.literal(shortId(summary.displayGroup().get()));

            summary.displaySubgroup().ifPresent(subgroup ->
                    result.append(Component.literal(" / "))
                            .append(Component.literal(shortId(subgroup)))
            );

            return result;
        }

        return Component.translatable(sourceKey(summary.sourceKind()));
    }

    private static MutableComponent operation(
            DamageContributionSummary summary
    ) {
        return Component.translatable(
                operationKey(summary.operationKind()),
                value(summary)
        );
    }

    private static MutableComponent target(
            DamageContributionSummary summary
    ) {
        MutableComponent result = Component.empty();

        summary.channel().ifPresent(channel ->
                result.append(Component.literal(" "))
                        .append(channelName(channel))
        );

        summary.applicationBucket().ifPresent(bucket ->
                result.append(Component.literal(" "))
                        .append(bucketName(bucket))
        );

        summary.preMultiplierBucket().ifPresent(bucket ->
                result.append(Component.literal(" ["))
                        .append(Component.literal(shortId(bucket)))
                        .append(Component.literal("]"))
        );

        return result;
    }

    private static MutableComponent count(
            DamageContributionSummary summary
    ) {
        if (summary.count() <= 1) {
            return Component.empty();
        }

        return Component.literal(" x" + summary.count());
    }

    private static String value(
            DamageContributionSummary summary
    ) {
        float value = summary.totalValue();

        if (!Float.isFinite(value)) {
            value = 0.0f;
        }

        return switch (summary.operationKind()) {
            case ADD_APPLICATION_PRE_MULTIPLIER,
                 ADD_CHANNEL_PRE_MULTIPLIER,
                 ADD_GLOBAL_PRE_MULTIPLIER,
                 ADD_CHANNEL_POST_MULTIPLIER,
                 ADD_GLOBAL_POST_MULTIPLIER,
                 ADD_CHANNEL_MITIGATION,
                 ADD_GLOBAL_MITIGATION,
                 CONVERT_DAMAGE,
                 GAIN_EXTRA_DAMAGE -> signedPercent(value);

            case MULTIPLY_ARMOR_EFFECTIVENESS -> "x" + number(value);

            case CANCEL_DAMAGE -> "";

            default -> signedNumber(value);
        };
    }

    private static MutableComponent channelName(Identifier channel) {
        return Component.translatableWithFallback(
                "channel." + IdentifierText.langPath(channel),
                shortId(channel)
        );
    }

    private static MutableComponent bucketName(DamageApplicationBucket bucket) {
        return Component.translatableWithFallback(
                "bucket.damagenexus."
                        + bucket.name().toLowerCase(Locale.ROOT),
                bucket.name()
                        .toLowerCase(Locale.ROOT)
                        .replace("vanilla_", "")
                        .replace("dn_", "")
                        .replace('_', ' ')
        );
    }

    private static String sourceKey(DamageContributionSourceKind kind) {
        String path = switch (kind) {
            case RULE -> "rule";
            case ENTRY -> "entry";
            case AFFIX -> "affix";
            case VANILLA_BRIDGE -> "vanilla_bridge";
            case VANILLA_ENCHANTMENT -> "vanilla_enchantment";
            case VANILLA_MOB_EFFECT -> "vanilla_mob_effect";
            case VANILLA_DAMAGE_TYPE -> "vanilla_damage_type";
            case JAVA_API -> "java_api";
            case UNKNOWN -> "unknown";
        };

        return "source_kind.damagenexus." + path;
    }

    private static String operationKey(DamageContributionOperationKind kind) {
        String path = kind.name().toLowerCase(Locale.ROOT);
        return "operation_kind.damagenexus." + path;
    }

    private static String signedNumber(float value) {
        return (value >= 0.0f ? "+" : "") + number(value);
    }

    private static String signedPercent(float value) {
        return (value >= 0.0f ? "+" : "") + number(value * 100.0f) + "%";
    }

    private static String number(float value) {
        return FORMAT.format(value);
    }

    private static String shortId(Identifier id) {
        if (id == null) {
            return "-";
        }

        if ("minecraft".equals(id.getNamespace())) {
            return id.getPath();
        }

        return id.toString();
    }
}