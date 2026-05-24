package io.github.naimjeg.damagenexus.api.display;

import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class DamageContributionLogFormatter {

    private DamageContributionLogFormatter() {
    }

    public static String statusLabel(DamageContributionSummary summary) {
        return summary == null || summary.status() == null
                ? "unknown"
                : summary.status().name().toLowerCase(Locale.ROOT);
    }

    public static String compact(
            DamageContributionSummary summary
    ) {
        if (summary == null) {
            return "<null contribution>";
        }

        return sourceLabel(summary)
                + " "
                + operationLabel(summary)
                + " "
                + valueLabel(summary)
                + targetLabel(summary)
                + countLabel(summary);
    }

    public static String sourceLabel(
            DamageContributionSummary summary
    ) {
        if (summary.displayName().isPresent()) {
            return summary.displayName().get();
        }

        if (summary.displayGroup().isPresent()) {
            String group = shortId(summary.displayGroup().get());

            if (summary.displaySubgroup().isPresent()) {
                return group + " / " + shortId(summary.displaySubgroup().get());
            }

            return group;
        }

        return switch (summary.sourceKind()) {
            case AFFIX -> "Affix";
            case ENTRY -> "Entry";
            case RULE -> "Rule";
            case VANILLA_BRIDGE -> "Vanilla";
            case VANILLA_ENCHANTMENT -> "Vanilla Enchantment";
            case VANILLA_MOB_EFFECT -> "Vanilla Effect";
            case VANILLA_DAMAGE_TYPE -> "Vanilla Damage Type";
            case JAVA_API -> "Java API";
            case UNKNOWN -> "Unknown";
        };
    }

    public static String operationLabel(
            DamageContributionSummary summary
    ) {
        return switch (summary.operationKind()) {
            case ADD_BASE_DAMAGE -> "adds";
            case ADD_TRUE_DAMAGE -> "adds true damage";

            case ADD_APPLICATION_PRE_MULTIPLIER -> "scales application";
            case ADD_CHANNEL_PRE_MULTIPLIER -> "scales channel";
            case ADD_GLOBAL_PRE_MULTIPLIER -> "scales global damage";

            case ADD_CHANNEL_POST_MULTIPLIER -> "post-scales channel";
            case ADD_GLOBAL_POST_MULTIPLIER -> "post-scales global damage";

            case ADD_TEMPORARY_RESISTANCE -> "adds resistance rating";
            case ADD_CHANNEL_MITIGATION -> "adds mitigation";
            case ADD_GLOBAL_MITIGATION -> "adds global mitigation";
            case MULTIPLY_ARMOR_EFFECTIVENESS -> "changes armor effectiveness";

            case CONVERT_DAMAGE -> "converts";
            case GAIN_EXTRA_DAMAGE -> "gains extra damage";

            case OVERRIDE_FINAL_DAMAGE -> "overrides final damage";
            case CANCEL_DAMAGE -> "cancels damage";

            case UNKNOWN -> "applies";
        };
    }

    public static String valueLabel(
            DamageContributionSummary summary
    ) {
        float value = summary.totalValue();

        if (!Float.isFinite(value)) {
            value = 0.0f;
        }

        return switch (summary.operationKind()) {
            case ADD_BASE_DAMAGE,
                 ADD_TRUE_DAMAGE,
                 ADD_TEMPORARY_RESISTANCE,
                 OVERRIDE_FINAL_DAMAGE -> signedNumber(value);

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

            case UNKNOWN -> signedNumber(value);
        };
    }

    public static String targetLabel(
            DamageContributionSummary summary
    ) {
        StringBuilder builder = new StringBuilder();

        summary.channel().ifPresent(channel ->
                builder.append(" ")
                        .append(shortId(channel))
        );

        summary.applicationBucket().ifPresent(bucket ->
                builder.append(" ")
                        .append(bucketLabel(bucket))
        );

        summary.preMultiplierBucket().ifPresent(bucket ->
                builder.append(" [")
                        .append(shortId(bucket))
                        .append("]")
        );

        return builder.toString();
    }

    private static String countLabel(
            DamageContributionSummary summary
    ) {
        return summary.count() > 1
                ? " x" + summary.count()
                : "";
    }

    private static String bucketLabel(
            DamageApplicationBucket bucket
    ) {
        if (bucket == null) {
            return "";
        }

        return bucket.name()
                .toLowerCase(Locale.ROOT)
                .replace("vanilla_", "")
                .replace("dn_", "")
                .replace('_', ' ');
    }

    private static String shortId(
            Identifier id
    ) {
        if (id == null) {
            return "-";
        }

        String namespace = id.getNamespace();
        String path = id.getPath();

        if ("minecraft".equals(namespace)) {
            return path;
        }

        return namespace + ":" + path;
    }

    private static String signedNumber(float value) {
        return (value >= 0.0f ? "+" : "") + number(value);
    }

    private static String signedPercent(float value) {
        return (value >= 0.0f ? "+" : "") + number(value * 100.0f) + "%";
    }

    private static String number(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
