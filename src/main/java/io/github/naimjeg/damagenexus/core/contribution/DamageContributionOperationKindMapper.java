package io.github.naimjeg.damagenexus.core.contribution;

import io.github.naimjeg.damagenexus.api.display.DamageContributionOperationKind;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperationIds;
import net.minecraft.resources.Identifier;

public final class DamageContributionOperationKindMapper {

    private DamageContributionOperationKindMapper() {
    }

    public static DamageContributionOperationKind fromRuleOperationType(
            Identifier operationType
    ) {
        if (DamageNexusOperationIds.ADD_BASE_DAMAGE.equals(operationType)) {
            return DamageContributionOperationKind.ADD_BASE_DAMAGE;
        }

        if (DamageNexusOperationIds.ADD_TRUE_DAMAGE.equals(operationType)) {
            return DamageContributionOperationKind.ADD_TRUE_DAMAGE;
        }

        if (DamageNexusOperationIds.ADD_CHANNEL_PRE_MULTIPLIER.equals(operationType)) {
            return DamageContributionOperationKind.ADD_CHANNEL_PRE_MULTIPLIER;
        }

        if (DamageNexusOperationIds.ADD_GLOBAL_PRE_MULTIPLIER.equals(operationType)) {
            return DamageContributionOperationKind.ADD_GLOBAL_PRE_MULTIPLIER;
        }

        if (DamageNexusOperationIds.ADD_CHANNEL_POST_MULTIPLIER.equals(operationType)) {
            return DamageContributionOperationKind.ADD_CHANNEL_POST_MULTIPLIER;
        }

        if (DamageNexusOperationIds.ADD_GLOBAL_POST_MULTIPLIER.equals(operationType)) {
            return DamageContributionOperationKind.ADD_GLOBAL_POST_MULTIPLIER;
        }

        if (DamageNexusOperationIds.ADD_TEMPORARY_RESISTANCE.equals(operationType)) {
            return DamageContributionOperationKind.ADD_TEMPORARY_RESISTANCE;
        }

        if (DamageNexusOperationIds.ADD_CHANNEL_MITIGATION.equals(operationType)) {
            return DamageContributionOperationKind.ADD_CHANNEL_MITIGATION;
        }

        if (DamageNexusOperationIds.ADD_GLOBAL_MITIGATION.equals(operationType)) {
            return DamageContributionOperationKind.ADD_GLOBAL_MITIGATION;
        }

        if (DamageNexusOperationIds.MULTIPLY_ARMOR_EFFECTIVENESS.equals(operationType)) {
            return DamageContributionOperationKind.MULTIPLY_ARMOR_EFFECTIVENESS;
        }

        if (DamageNexusOperationIds.CONVERT_DAMAGE.equals(operationType)) {
            return DamageContributionOperationKind.CONVERT_DAMAGE;
        }

        if (DamageNexusOperationIds.GAIN_EXTRA_DAMAGE.equals(operationType)) {
            return DamageContributionOperationKind.GAIN_EXTRA_DAMAGE;
        }

        if (DamageNexusOperationIds.OVERRIDE_FINAL_DAMAGE.equals(operationType)) {
            return DamageContributionOperationKind.OVERRIDE_FINAL_DAMAGE;
        }

        if (DamageNexusOperationIds.CANCEL_DAMAGE.equals(operationType)) {
            return DamageContributionOperationKind.CANCEL_DAMAGE;
        }

        return DamageContributionOperationKind.UNKNOWN;
    }
}