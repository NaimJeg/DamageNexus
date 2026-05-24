package io.github.naimjeg.damagenexus.api.rule;

import com.mojang.serialization.Codec;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.registry.rule.DamageRuleOperationTypes;
import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * A rule operation supplied through the Java API.
 *
 * <p>Implementations must remain immutable after validation and must return
 * stable, immutable phase/reference metadata. DamageNexus cannot defensively
 * copy arbitrary state owned by third-party executable code.</p>
 */
public interface DamageRuleOperation {

    Codec<DamageRuleOperation> CODEC =
            Identifier.CODEC.dispatch(
                    "type",
                    DamageRuleOperation::type,
                    DamageRuleOperationTypes::codec
            );

    Identifier type();

    DamageMutationResult apply(DamageRuleContext ctx);

    default float stackingValue() {
        return 0.0f;
    }

    default Set<DamagePhase> supportedPhases() {
        return Set.of();
    }

    default boolean supportsPhase(DamagePhase phase) {
        Set<DamagePhase> phases = supportedPhases();
        return phases.isEmpty() || phases.contains(phase);
    }
}
