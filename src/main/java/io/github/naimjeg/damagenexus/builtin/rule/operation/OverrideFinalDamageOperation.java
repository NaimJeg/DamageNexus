package io.github.naimjeg.damagenexus.builtin.rule.operation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.RuleTraceIds;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperationIds;
import net.minecraft.resources.Identifier;

public record OverrideFinalDamageOperation(
        float value
) implements DamageRuleOperation {

    public static final MapCodec<OverrideFinalDamageOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.FLOAT
                            .fieldOf("value")
                            .forGetter(OverrideFinalDamageOperation::value)
            ).apply(instance, OverrideFinalDamageOperation::new));

    @Override
    public Identifier type() {
        return DamageNexusOperationIds.OVERRIDE_FINAL_DAMAGE;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryOverrideFinalDamage(
                value,
                RuleTraceIds.OVERRIDE_FINAL_DAMAGE
        );
    }

    @Override
    public java.util.Set<DamagePhase> supportedPhases() {
        return java.util.Set.of(DamagePhase.FINAL_OVERRIDE);
    }

    @Override
    public float stackingValue() {
        return value;
    }
}
