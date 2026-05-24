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

import java.util.Set;

public record MultiplyArmorEffectivenessOperation(
        float value
) implements DamageRuleOperation {

    public static final MapCodec<MultiplyArmorEffectivenessOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.FLOAT
                            .fieldOf("value")
                            .forGetter(MultiplyArmorEffectivenessOperation::value)
            ).apply(instance, MultiplyArmorEffectivenessOperation::new));

    @Override
    public Identifier type() {
        return DamageNexusOperationIds.MULTIPLY_ARMOR_EFFECTIVENESS;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryMultiplyArmorEffectiveness(
                value,
                RuleTraceIds.MULTIPLY_ARMOR_EFFECTIVENESS
        );
    }

    @Override
    public Set<DamagePhase> supportedPhases() {
        return Set.of(DamagePhase.MITIGATION_SETUP);
    }

    @Override
    public float stackingValue() {
        return value;
    }
}
