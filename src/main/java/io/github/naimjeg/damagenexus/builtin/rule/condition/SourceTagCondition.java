package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.resources.Identifier;

import java.util.Objects;

public record SourceTagCondition(
        Identifier tag
) implements DamageRuleCondition {
    public static final MapCodec<SourceTagCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC.fieldOf("tag")
                            .forGetter(SourceTagCondition::tag)
            ).apply(instance, SourceTagCondition::new));

    public SourceTagCondition {
        Objects.requireNonNull(tag, "tag");
    }

    @Override public Identifier type() {
        return DamageNexusConditionIds.SOURCE_TAG;
    }

    @Override public boolean test(DamageRuleContext ctx) {
        return ctx.sourceTags().contains(tag);
    }
}
