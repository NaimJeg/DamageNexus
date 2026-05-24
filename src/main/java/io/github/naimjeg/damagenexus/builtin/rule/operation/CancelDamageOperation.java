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

public record CancelDamageOperation(
        String sourceId
) implements DamageRuleOperation {

    public static final MapCodec<CancelDamageOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING
                            .optionalFieldOf(
                                    "source",
                                    RuleTraceIds.CANCEL_DAMAGE
                            )
                            .forGetter(CancelDamageOperation::sourceId)
            ).apply(instance, CancelDamageOperation::new));

    public CancelDamageOperation() {
        this(RuleTraceIds.CANCEL_DAMAGE);
    }

    @Override
    public Identifier type() {
        return DamageNexusOperationIds.CANCEL_DAMAGE;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryCancelDamage(sourceId);
    }

    @Override
    public Set<DamagePhase> supportedPhases() {
        return Set.of(DamagePhase.FINAL_OVERRIDE);
    }

    @Override
    public float stackingValue() {
        return 1.0f;
    }
}
