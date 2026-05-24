package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.builtin.rule.operation.*;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperationIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class DefaultOperationTooltips {

    private DefaultOperationTooltips() {
    }

    public static void register() {
        RuleTooltipDescriptions.registerOperation(
                DamageNexusOperationIds.ADD_BASE_DAMAGE,
                (AddBaseDamageOperation operation, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        prefix(additiveMarker(operation.value()))
                                .append(Component.translatable(
                                        operationKey(mode, "add_base_damage"),
                                        ctx.signedNumber(operation.value()),
                                        ctx.channelName(operation.channelId())
                                ))
        );

        RuleTooltipDescriptions.registerOperation(
                DamageNexusOperationIds.ADD_GLOBAL_MITIGATION,
                (AddGlobalMitigationOperation operation, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        prefix(mitigationMarker(operation.value()))
                                .append(Component.translatable(
                                        operationKey(mode, "add_global_mitigation"),
                                        ctx.signedPercent(operation.value())
                                ))
        );

        RuleTooltipDescriptions.registerOperation(
                DamageNexusOperationIds.MULTIPLY_ARMOR_EFFECTIVENESS,
                (MultiplyArmorEffectivenessOperation operation, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        prefix(mitigationMarker(1.0f - operation.value()))
                                .append(Component.translatable(
                                        operationKey(mode, "multiply_armor_effectiveness"),
                                        ctx.number(operation.value())
                                ))
        );

        RuleTooltipDescriptions.registerOperation(
                DamageNexusOperationIds.CANCEL_DAMAGE,
                (CancelDamageOperation operation, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        prefix(overrideMarker())
                                .append(Component.translatable(
                                        operationKey(mode, "cancel_damage")
                                ))
        );

        RuleTooltipDescriptions.registerOperation(
                DamageNexusOperationIds.ADD_CHANNEL_PRE_MULTIPLIER,
                (AddChannelPreMultiplierOperation operation, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        prefix(preMultiplierMarker())
                                .append(Component.translatable(
                                        operationKey(mode, "add_channel_pre_multiplier"),
                                        ctx.signedPercent(operation.value()),
                                        ctx.channelName(operation.channelId())
                                ))
        );

        RuleTooltipDescriptions.registerOperation(
                DamageNexusOperationIds.ADD_CHANNEL_POST_MULTIPLIER,
                (AddChannelPostMultiplierOperation operation, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.empty()
                                .append(Component.translatable(
                                        operationKey(mode, "add_channel_post_multiplier"),
                                        ctx.signedPercent(operation.value()),
                                        ctx.channelName(operation.channelId())
                                ))
                                .append(postMultiplierSuffix())
        );

        RuleTooltipDescriptions.registerOperation(
                DamageNexusOperationIds.ADD_GLOBAL_PRE_MULTIPLIER,
                (AddGlobalPreMultiplierOperation operation, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        prefix(preMultiplierMarker())
                                .append(Component.translatable(
                                        operationKey(mode, "add_global_pre_multiplier"),
                                        ctx.signedPercent(operation.value())
                                ))
        );

        RuleTooltipDescriptions.registerOperation(
                DamageNexusOperationIds.ADD_GLOBAL_POST_MULTIPLIER,
                (AddGlobalPostMultiplierOperation operation, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        Component.empty()
                                .append(Component.translatable(
                                        operationKey(mode, "add_global_post_multiplier"),
                                        ctx.signedPercent(operation.value())
                                ))
                                .append(postMultiplierSuffix())
        );

        RuleTooltipDescriptions.registerOperation(
                DamageNexusOperationIds.ADD_TEMPORARY_RESISTANCE,
                (AddTemporaryResistanceOperation operation, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        prefix(mitigationMarker(operation.value()))
                                .append(Component.translatable(
                                        operationKey(mode, "add_temporary_resistance"),
                                        ctx.signedNumber(operation.value()),
                                        ctx.channelName(operation.channelId())
                                ))
        );

        RuleTooltipDescriptions.registerOperation(
                DamageNexusOperationIds.CONVERT_DAMAGE,
                (ConvertDamageOperation operation, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        prefix(convertMarker())
                                .append(Component.translatable(
                                        operationKey(mode, "convert_damage"),
                                        ctx.percentWithSymbol(operation.ratio()),
                                        ctx.channelName(operation.fromChannel()),
                                        ctx.channelName(operation.toChannel())
                                ))
        );

        RuleTooltipDescriptions.registerOperation(
                DamageNexusOperationIds.GAIN_EXTRA_DAMAGE,
                (GainExtraDamageOperation operation, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        prefix(additiveMarker(operation.ratio()))
                                .append(Component.translatable(
                                        operationKey(mode, "gain_extra_damage"),
                                        ctx.percentWithSymbol(operation.ratio()),
                                        ctx.channelName(operation.basedOnChannel()),
                                        ctx.channelName(operation.toChannel())
                                ))
        );

        RuleTooltipDescriptions.registerOperation(
                DamageNexusOperationIds.ADD_CHANNEL_MITIGATION,
                (AddChannelMitigationOperation operation, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        prefix(mitigationMarker(operation.value()))
                                .append(Component.translatable(
                                        operationKey(mode, "add_channel_mitigation"),
                                        ctx.signedPercent(operation.value()),
                                        ctx.channelName(operation.channelId())
                                ))
        );

        RuleTooltipDescriptions.registerOperation(
                DamageNexusOperationIds.ADD_TRUE_DAMAGE,
                (AddTrueDamageOperation operation, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        prefix(additiveMarker(operation.value()))
                                .append(Component.translatable(
                                        operationKey(mode, "add_true_damage"),
                                        ctx.signedNumber(operation.value()),
                                        ctx.channelName(operation.channelId())
                                ))
        );

        RuleTooltipDescriptions.registerOperation(
                DamageNexusOperationIds.OVERRIDE_FINAL_DAMAGE,
                (OverrideFinalDamageOperation operation, RuleTooltipContext ctx, RuleTooltipMode mode) ->
                        prefix(overrideMarker())
                                .append(Component.translatable(
                                        operationKey(mode, "override_final_damage"),
                                        ctx.number(operation.value())
                                ))
        );
    }

    private static String operationKey(
            RuleTooltipMode mode,
            String operationName
    ) {
        return "operation.damagenexus."
                + (mode == RuleTooltipMode.DETAIL ? "detail" : "normal")
                + "."
                + operationName;
    }

    private static MutableComponent prefix(MutableComponent marker) {
        return Component.empty().append(marker);
    }

    private static MutableComponent additiveMarker(float value) {
        return marker(value < 0.0f
                ? "tooltip.damagenexus.marker.remove"
                : "tooltip.damagenexus.marker.add");
    }

    private static MutableComponent mitigationMarker(float value) {
        return marker(value < 0.0f
                ? "tooltip.damagenexus.marker.add"
                : "tooltip.damagenexus.marker.remove");
    }

    private static MutableComponent preMultiplierMarker() {
        return marker("tooltip.damagenexus.marker.multiply_prefix");
    }

    private static MutableComponent postMultiplierSuffix() {
        return marker("tooltip.damagenexus.marker.multiply_suffix");
    }

    private static MutableComponent convertMarker() {
        return marker("tooltip.damagenexus.marker.convert");
    }

    private static MutableComponent overrideMarker() {
        return marker("tooltip.damagenexus.marker.override");
    }

    private static MutableComponent marker(String key) {
        return Component.translatable(key);
    }
}

