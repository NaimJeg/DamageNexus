package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestTargetSelector;
import io.github.naimjeg.damagenexus.registry.ModAttributes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
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
                                        "crit chance = 0"
                                )))
                        .then(Commands.literal("crit_100")
                                .executes(ctx -> setSelfAttribute(
                                        ctx.getSource(),
                                        ModAttributes.CRIT_CHANCE,
                                        1.0D,
                                        "crit chance = 100%"
                                )))
                        .then(Commands.literal("crit_damage_20")
                                .executes(ctx -> setSelfAttribute(
                                        ctx.getSource(),
                                        ModAttributes.CRIT_DAMAGE_ADDITIVE,
                                        0.20D,
                                        "crit damage additive = 20%"
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
                                        "thorns damage = 0"
                                )))
                        .then(Commands.literal("thorns_5")
                                .executes(ctx -> setSelfAttribute(
                                        ctx.getSource(),
                                        ModAttributes.THORNS,
                                        5.0D,
                                        "thorns damage = 5"
                                )))
                        .then(Commands.literal("thorns_20")
                                .executes(ctx -> setSelfAttribute(
                                        ctx.getSource(),
                                        ModAttributes.THORNS,
                                        20.0D,
                                        "thorns damage = 20"
                                ))))

                .then(Commands.literal("target")
                        .then(Commands.literal("armor_0")
                                .executes(ctx -> setTargetAttribute(
                                        ctx.getSource(),
                                        Attributes.ARMOR,
                                        0.0D,
                                        "armor = 0"
                                )))
                        .then(Commands.literal("armor_20")
                                .executes(ctx -> setTargetAttribute(
                                        ctx.getSource(),
                                        Attributes.ARMOR,
                                        20.0D,
                                        "armor = 20"
                                )))
                        .then(Commands.literal("toughness_0")
                                .executes(ctx -> setTargetAttribute(
                                        ctx.getSource(),
                                        Attributes.ARMOR_TOUGHNESS,
                                        0.0D,
                                        "toughness = 0"
                                )))
                        .then(Commands.literal("toughness_12")
                                .executes(ctx -> setTargetAttribute(
                                        ctx.getSource(),
                                        Attributes.ARMOR_TOUGHNESS,
                                        12.0D,
                                        "toughness = 12"
                                )))
                        .then(Commands.literal("fire_res_50")
                                .executes(ctx -> setTargetAttribute(
                                        ctx.getSource(),
                                        ModAttributes.RESISTANCE_FIRE,
                                        50.0D,
                                        "fire resistance = 50"
                                )))
                        .then(Commands.literal("physical_res_50")
                                .executes(ctx -> setTargetAttribute(
                                        ctx.getSource(),
                                        ModAttributes.RESISTANCE_PHYSICAL,
                                        50.0D,
                                        "physical resistance = 50"
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
                    "this command must be run by a living entity."
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
                    "no nearby [DN-Test] living target found."
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
                    "target does not have attribute: " + label
            );
        }

        instance.setBaseValue(value);

        return CommandFeedback.success(
                source,
                "attribute set: " + label
        );
    }
}
