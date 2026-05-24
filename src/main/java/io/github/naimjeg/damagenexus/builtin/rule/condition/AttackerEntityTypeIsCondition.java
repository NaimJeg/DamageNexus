package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import io.github.naimjeg.damagenexus.util.EntityConditionUtil;
import net.minecraft.resources.Identifier;

public record AttackerEntityTypeIsCondition(
        Identifier entityType
) implements DamageRuleCondition {

    public static final MapCodec<AttackerEntityTypeIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC
                            .fieldOf("entity_type")
                            .forGetter(AttackerEntityTypeIsCondition::entityType)
            ).apply(instance, AttackerEntityTypeIsCondition::new));

    @Override
    public Identifier type() {
        return DamageNexusConditionIds.ATTACKER_ENTITY_TYPE_IS;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return EntityConditionUtil.isEntityType(
                ctx.logicalAttacker(),
                entityType
        );
    }
}
