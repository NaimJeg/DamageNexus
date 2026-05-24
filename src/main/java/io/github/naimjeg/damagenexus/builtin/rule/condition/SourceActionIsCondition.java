package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.resources.Identifier;

import java.util.Objects;

public record SourceActionIsCondition(
        Identifier action
) implements DamageRuleCondition {
    public static final MapCodec<SourceActionIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC.fieldOf("action")
                            .forGetter(SourceActionIsCondition::action)
            ).apply(instance, SourceActionIsCondition::new));

    public SourceActionIsCondition {
        Objects.requireNonNull(action, "action");
    }

    @Override public Identifier type() {
        return DamageNexusConditionIds.SOURCE_ACTION_IS;
    }

    @Override public boolean test(DamageRuleContext ctx) {
        return ctx.actionId().filter(action::equals).isPresent();
    }
}
