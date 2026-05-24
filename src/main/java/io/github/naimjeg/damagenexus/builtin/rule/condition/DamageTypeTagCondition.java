package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public record DamageTypeTagCondition(
        TagKey<DamageType> tag
) implements DamageRuleCondition {

    public static final MapCodec<DamageTypeTagCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC
                            .fieldOf("tag")
                            .xmap(
                                    id -> TagKey.create(Registries.DAMAGE_TYPE, id),
                                    TagKey::location
                            )
                            .forGetter(DamageTypeTagCondition::tag)
            ).apply(instance, DamageTypeTagCondition::new));

    @Override
    public Identifier type() {
        return DamageNexusConditionIds.DAMAGE_TYPE_TAG;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return ctx.source().is(tag);
    }
}
