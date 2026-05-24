package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCodecs;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.MobCategory;

public record TargetMobCategoryIsCondition(
        MobCategory category
) implements DamageRuleCondition {

    public static final MapCodec<TargetMobCategoryIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.MOB_CATEGORY
                            .fieldOf("category")
                            .forGetter(TargetMobCategoryIsCondition::category)
            ).apply(instance, TargetMobCategoryIsCondition::new));

    @Override
    public Identifier type() {
        return DamageNexusConditionIds.TARGET_MOB_CATEGORY_IS;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return ctx.victim() != null
                && ctx.victim().getType().getCategory() == category;
    }
}
