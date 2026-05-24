package io.github.naimjeg.damagenexus.diagnostics;

import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.core.registry.PreMultiplierBucketRegistry;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementMixinStatus;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusLifecycleLog;
import io.github.naimjeg.damagenexus.registry.PreMultiplierBuckets;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import io.github.naimjeg.damagenexus.registry.rule.DamageRuleConditionTypes;
import net.minecraft.resources.Identifier;

public final class DamageNexusStartupSelfCheck {

    private DamageNexusStartupSelfCheck() {
    }

    public static void run() {
        requireConditionCodec(
                DamageNexusConditionIds.ATTACKER_ENTITY_TYPE_TAG
        );

        requirePreMultiplierRegistryFrozen();
        requireGenericDamageBucket();
        requireTrueDamageBucketFlags();
        requireSettlementMixinApplied();

        DamageNexusLifecycleLog.startupSelfCheckPassed();
    }

    private static void requireConditionCodec(Identifier id) {
        try {
            DamageRuleConditionTypes.codec(id);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Missing DamageNexus condition codec registration: " + id,
                    e
            );
        }
    }

    private static void requireTrueDamageBucketFlags() {
        DamageApplicationBucket bucket = DamageApplicationBucket.DN_TRUE_DAMAGE;

        if (bucket.affectedByChannelPreMultiplier()) {
            throw new IllegalStateException(
                    "DN_TRUE_DAMAGE must not be affected by channel pre multipliers."
            );
        }

        if (bucket.affectedByGlobalPreMultiplier()) {
            throw new IllegalStateException(
                    "DN_TRUE_DAMAGE must not be affected by global pre multipliers."
            );
        }

        if (bucket.affectedByPostMultiplier()) {
            throw new IllegalStateException(
                    "DN_TRUE_DAMAGE must not be affected by post multipliers."
            );
        }

        if (bucket.affectedByMitigation()) {
            throw new IllegalStateException(
                    "DN_TRUE_DAMAGE must not be affected by mitigation."
            );
        }
    }

    private static void requirePreMultiplierRegistryFrozen() {
        PreMultiplierBucketRegistry.requireFrozen();
    }

    private static void requireGenericDamageBucket() {
        if (PreMultiplierBuckets.genericDamage() < 0) {
            throw new IllegalStateException(
                    "GENERIC_DAMAGE pre-multiplier bucket was not registered."
            );
        }

        if (PreMultiplierBuckets.genericDamage()
                >= PreMultiplierBucketRegistry.bucketCount()) {
            throw new IllegalStateException(
                    "GENERIC_DAMAGE pre-multiplier bucket id is outside registry bounds: "
                            + PreMultiplierBuckets.genericDamage()
            );
        }
    }

    private static void requireSettlementMixinApplied() {
        if (!DamageSettlementMixinStatus.isApplied()) {
            throw new IllegalStateException(
                    "LivingEntity damage settlement mixin was not applied"
            );
        }
    }
}

