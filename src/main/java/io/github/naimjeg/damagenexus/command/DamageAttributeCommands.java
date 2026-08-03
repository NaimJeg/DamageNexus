package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestTargetSelector;
import io.github.naimjeg.damagenexus.registry.ModAttributes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class DamageAttributeCommands {

    private DamageAttributeCommands() {
    }

    public static void register(
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        root.then(Commands.literal("attribute")
                .requires(DamageCommandSecurity.adminPermission())
                .then(Commands.literal("self")
                        .then(Commands.literal("crit_0")
                                .executes(ctx -> setSelfAttribute(
                                        ctx.getSource(),
                                        ModAttributes.CRIT_CHANCE,
                                        0.0D,
                                        "command.damagenexus.attribute.crit_chance_zero"
                                )))
                        .then(Commands.literal("crit_100")
                                .executes(ctx -> setSelfAttribute(
                                        ctx.getSource(),
                                        ModAttributes.CRIT_CHANCE,
                                        1.0D,
                                        "command.damagenexus.attribute.crit_chance_full"
                                )))
                        .then(Commands.literal("crit_damage_20")
                                .executes(ctx -> setSelfAttribute(
                                        ctx.getSource(),
                                        ModAttributes.CRIT_DAMAGE_ADDITIVE,
                                        0.20D,
                                        "command.damagenexus.attribute.crit_damage_twenty"
                                )))
                        /*
                         * Manual check: wear vanilla Thorns armor, set self
                         * thorns_5 or thorns_20, then compare reflected damage
                         * while normal outgoing attacks remain unchanged.
                         */
                        .then(Commands.literal("thorns_0")
                                .executes(ctx -> setSelfAttribute(
                                        ctx.getSource(),
                                        ModAttributes.THORNS,
                                        0.0D,
                                        "command.damagenexus.attribute.thorns_zero"
                                )))
                        .then(Commands.literal("thorns_5")
                                .executes(ctx -> setSelfAttribute(
                                        ctx.getSource(),
                                        ModAttributes.THORNS,
                                        5.0D,
                                        "command.damagenexus.attribute.thorns_five"
                                )))
                        .then(Commands.literal("thorns_20")
                                .executes(ctx -> setSelfAttribute(
                                        ctx.getSource(),
                                        ModAttributes.THORNS,
                                        20.0D,
                                        "command.damagenexus.attribute.thorns_twenty"
                                ))))

                .then(Commands.literal("target")
                        .then(Commands.literal("armor_0")
                                .executes(ctx -> setTargetAttribute(
                                        ctx.getSource(),
                                        Attributes.ARMOR,
                                        0.0D,
                                        "command.damagenexus.attribute.armor_zero"
                                )))
                        .then(Commands.literal("armor_20")
                                .executes(ctx -> setTargetAttribute(
                                        ctx.getSource(),
                                        Attributes.ARMOR,
                                        20.0D,
                                        "command.damagenexus.attribute.armor_twenty"
                                )))
                        .then(Commands.literal("toughness_0")
                                .executes(ctx -> setTargetAttribute(
                                        ctx.getSource(),
                                        Attributes.ARMOR_TOUGHNESS,
                                        0.0D,
                                        "command.damagenexus.attribute.toughness_zero"
                                )))
                        .then(Commands.literal("toughness_12")
                                .executes(ctx -> setTargetAttribute(
                                        ctx.getSource(),
                                        Attributes.ARMOR_TOUGHNESS,
                                        12.0D,
                                        "command.damagenexus.attribute.toughness_twelve"
                                )))
                        .then(Commands.literal("fire_res_50")
                                .executes(ctx -> setTargetAttribute(
                                        ctx.getSource(),
                                        ModAttributes.RESISTANCE_FIRE,
                                        50.0D,
                                        "command.damagenexus.attribute.fire_resistance_fifty"
                                )))
                        .then(Commands.literal("physical_res_50")
                                .executes(ctx -> setTargetAttribute(
                                        ctx.getSource(),
                                        ModAttributes.RESISTANCE_PHYSICAL,
                                        50.0D,
                                        "command.damagenexus.attribute.physical_resistance_fifty"
                                )))));
    }

    private static int setSelfAttribute(
            CommandSourceStack source,
            Holder<Attribute> attribute,
            double value,
            String label
    ) {
        LivingEntity self = source.getEntity() instanceof LivingEntity living
                ? living
                : null;

        if (self == null) {
            return CommandFeedback.fail(
                    source,
                    "command.damagenexus.living_entity_required"
            );
        }

        return setAttribute(
                source,
                self,
                attribute,
                value,
                label
        );
    }

    private static int setTargetAttribute(
            CommandSourceStack source,
            Holder<Attribute> attribute,
            double value,
            String label
    ) {
        LivingEntity target = TestTargetSelector.nearestTestLiving(source);

        if (target == null) {
            return CommandFeedback.fail(
                    source,
                    "command.damagenexus.target_not_found"
            );
        }

        return setAttribute(
                source,
                target,
                attribute,
                value,
                label
        );
    }

    private static int setAttribute(
            CommandSourceStack source,
            LivingEntity entity,
            Holder<Attribute> attribute,
            double value,
            String label
    ) {
        AttributeInstance instance = entity.getAttribute(attribute);

        if (instance == null) {
            return CommandFeedback.fail(
                    source,
                    "command.damagenexus.attribute_missing",
                    Component.translatable(label)
            );
        }

        instance.setBaseValue(value);

        return CommandFeedback.success(
                source,
                "command.damagenexus.attribute_set",
                Component.translatable(label)
        );
    }
}
