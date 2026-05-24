package io.github.naimjeg.damagenexus.builtin.rule.operation;

import io.github.naimjeg.damagenexus.core.registry.PreMultiplierBucketRegistry;
import io.github.naimjeg.damagenexus.registry.PreMultiplierBuckets;
import net.minecraft.resources.Identifier;

import java.util.Optional;

final class DamageOperationPreMultiplierBuckets {

    private DamageOperationPreMultiplierBuckets() {
    }

    static int resolveOrGeneric(Optional<Identifier> bucketId) {
        PreMultiplierBucketRegistry.requireFrozen();

        if (bucketId == null || bucketId.isEmpty()) {
            return PreMultiplierBuckets.genericDamage();
        }

        return PreMultiplierBucketRegistry.getPreMultiplierBucketIdOrUnknown(
                bucketId.get()
        );
    }
}
