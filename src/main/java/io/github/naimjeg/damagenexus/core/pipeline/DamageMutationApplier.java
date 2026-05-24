package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.core.DamageComponent;

/**
 * Applies already-validated damage mutations to the mutable pipeline state.
 *
 * <p>This class deliberately does not perform phase/lock/value validation and
 * does not emit diagnostic logs. {@link DamageContextMutations} owns mutation
 * policy and trace emission; this class owns only state writes.</p>
 */
final class DamageMutationApplier {

    private final DamagePacketState packet;
    private final DamageCombatState combat;
    private final DamagePipelineResult result;

    DamageMutationApplier(
            DamagePacketState packet,
            DamageCombatState combat,
            DamagePipelineResult result
    ) {
        this.packet = packet;
        this.combat = combat;
        this.result = result;
    }

    void addBaseDamage(
            DamageChannel channel,
            DamageApplicationBucket bucket,
            float value
    ) {
        packet.getOrCreateComponent(channel).addBase(bucket, value);
    }

    void addChannelPreMultiplier(
            DamageChannel channel,
            int modifierId,
            float value
    ) {
        packet.getOrCreateComponent(channel).addPreMultiplier(
                modifierId,
                value
        );
    }

    void addApplicationPreMultiplier(
            DamageApplicationBucket bucket,
            int modifierId,
            float value
    ) {
        for (int i = 0; i < packet.activeComponentCount(); i++) {
            packet.activeComponent(i).addApplicationPreMultiplier(
                    bucket,
                    modifierId,
                    value
            );
        }
    }

    void addGlobalPreMultiplier(
            int modifierId,
            float value
    ) {
        packet.addGlobalPreMultiplier(modifierId, value);
    }

    void addChannelPostMultiplier(
            DamageChannel channel,
            float value
    ) {
        packet.getOrCreateComponent(channel).addPostMultiplier(value);
    }

    void addGlobalPostMultiplier(float value) {
        packet.addGlobalPostMultiplier(value);
    }

    float convertDamage(
            DamageChannel from,
            DamageChannel to,
            float ratio
    ) {
        DamageComponent sourceComponent = packet.findActiveComponent(from);

        if (sourceComponent == null) {
            return 0.0f;
        }

        DamageComponent targetComponent = packet.getOrCreateComponent(to);

        return sourceComponent.convertBaseTo(
                targetComponent,
                ratio
        );
    }

    float gainExtraDamage(
            DamageChannel from,
            DamageChannel to,
            float ratio
    ) {
        DamageComponent sourceComponent = packet.findActiveComponent(from);

        if (sourceComponent == null) {
            return 0.0f;
        }

        float extraAmount = sourceComponent.getBaseAmount() * ratio;

        if (extraAmount <= 0.0f) {
            return 0.0f;
        }

        packet.getOrCreateComponent(to).addBase(
                DamageApplicationBucket.DN_RULE_BASE,
                extraAmount
        );

        return extraAmount;
    }

    void multiplyArmorEffectiveness(float multiplier) {
        combat.multiplyArmorEffectiveness(multiplier);
    }

    boolean addTemporaryResistance(
            DamageChannel channel,
            float rating
    ) {
        DamageComponent component = packet.findActiveComponent(channel);

        if (component == null) {
            return false;
        }

        component.addTemporaryResistance(rating);
        return true;
    }

    boolean addChannelMitigation(
            DamageChannel channel,
            float reductionPercent
    ) {
        DamageComponent component = packet.findActiveComponent(channel);

        if (component == null) {
            return false;
        }

        component.addMitigation(reductionPercent);
        return true;
    }

    void addGlobalMitigation(float reductionPercent) {
        packet.addGlobalMitigation(reductionPercent);
    }

    void overrideFinalDamage(float amount) {
        result.setFinalEventDamage(amount);
    }

    void cancelDamage(String sourceId) {
        result.cancel(sourceId);
    }
}
