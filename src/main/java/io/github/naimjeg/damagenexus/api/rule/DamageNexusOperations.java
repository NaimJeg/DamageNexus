package io.github.naimjeg.damagenexus.api.rule;

import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.builtin.rule.operation.*;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class DamageNexusOperations {

    private DamageNexusOperations() {
    }

    public static DamageRuleOperation addBaseDamage(
            Identifier channel,
            float value
    ) {
        return new AddBaseDamageOperation(channel, value);
    }

    public static DamageRuleOperation addBaseDamage(
            Identifier channel,
            DamageApplicationBucket applicationBucket,
            float value
    ) {
        return new AddBaseDamageOperation(channel, applicationBucket, value);
    }

    public static DamageRuleOperation addChannelPreMultiplier(
            Identifier channel,
            float value
    ) {
        return new AddChannelPreMultiplierOperation(
                channel,
                Optional.empty(),
                value
        );
    }

    public static DamageRuleOperation addChannelPreMultiplier(
            Identifier channel,
            Identifier preMultiplierBucket,
            float value
    ) {
        return new AddChannelPreMultiplierOperation(
                channel,
                Optional.of(preMultiplierBucket),
                value
        );
    }

    public static DamageRuleOperation addChannelPostMultiplier(
            Identifier channel,
            float value
    ) {
        return new AddChannelPostMultiplierOperation(channel, value);
    }

    public static DamageRuleOperation addGlobalPreMultiplier(float value) {
        return new AddGlobalPreMultiplierOperation(
                Optional.empty(),
                value
        );
    }

    public static DamageRuleOperation addGlobalPreMultiplier(
            Identifier preMultiplierBucket,
            float value
    ) {
        return new AddGlobalPreMultiplierOperation(
                Optional.of(preMultiplierBucket),
                value
        );
    }

    public static DamageRuleOperation addGlobalPostMultiplier(float value) {
        return new AddGlobalPostMultiplierOperation(value);
    }

    public static DamageRuleOperation convertDamage(
            Identifier from,
            Identifier to,
            float ratio
    ) {
        return new ConvertDamageOperation(from, to, ratio);
    }

    public static DamageRuleOperation gainExtraDamage(
            Identifier basedOn,
            Identifier to,
            float ratio
    ) {
        return new GainExtraDamageOperation(basedOn, to, ratio);
    }

    public static DamageRuleOperation addTemporaryResistance(
            Identifier channel,
            float value
    ) {
        return new AddTemporaryResistanceOperation(channel, value);
    }

    public static DamageRuleOperation addChannelMitigation(
            Identifier channel,
            float value
    ) {
        return new AddChannelMitigationOperation(channel, value);
    }

    public static DamageRuleOperation addGlobalMitigation(float value) {
        return new AddGlobalMitigationOperation(value);
    }

    public static DamageRuleOperation multiplyArmorEffectiveness(float value) {
        return new MultiplyArmorEffectivenessOperation(value);
    }

    public static DamageRuleOperation addTrueDamage(
            Identifier channel,
            float value
    ) {
        return new AddTrueDamageOperation(channel, value);
    }

    public static DamageRuleOperation overrideFinalDamage(float value) {
        return new OverrideFinalDamageOperation(value);
    }

    public static DamageRuleOperation cancelDamage() {
        return new CancelDamageOperation();
    }

    public static DamageRuleOperation cancelDamage(String sourceId) {
        return new CancelDamageOperation(sourceId);
    }
}
