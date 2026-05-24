package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.TargetEntityTypeTagCondition;
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddBaseDamageOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.MultiplyArmorEffectivenessOperation;
import io.github.naimjeg.damagenexus.config.DamageNexusConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Optional;

public final class VanillaEnchantmentTooltipCatalog {

    private static final Identifier SHARPNESS =
            minecraft("sharpness");

    private static final Identifier SMITE =
            minecraft("smite");

    private static final Identifier BANE_OF_ARTHROPODS =
            minecraft("bane_of_arthropods");

    private static final Identifier FEATHER_FALLING =
            minecraft("feather_falling");

    private static final Identifier BREACH =
            minecraft("breach");

    private static final Identifier POWER =
            minecraft("power");

    private static final Identifier DENSITY =
            minecraft("density");

    private static final TagKey<EntityType<?>> SENSITIVE_TO_SMITE =
            entityTypeTag("sensitive_to_smite");

    private static final TagKey<EntityType<?>> SENSITIVE_TO_BANE_OF_ARTHROPODS =
            entityTypeTag("sensitive_to_bane_of_arthropods");

    private VanillaEnchantmentTooltipCatalog() {
    }

    public static Optional<VanillaEnchantmentTooltipSpec> create(
            Identifier enchantmentId,
            int level
    ) {
        if (level <= 0 || enchantmentId == null) {
            return Optional.empty();
        }

        if (SHARPNESS.equals(enchantmentId)) {
            return Optional.of(sharpness(enchantmentId, level));
        }

        if (SMITE.equals(enchantmentId)) {
            return Optional.of(smite(enchantmentId, level));
        }

        if (BANE_OF_ARTHROPODS.equals(enchantmentId)) {
            return Optional.of(baneOfArthropods(enchantmentId, level));
        }

        if (FEATHER_FALLING.equals(enchantmentId)) {
            return Optional.of(featherFalling(enchantmentId, level));
        }

        if (BREACH.equals(enchantmentId)) {
            return Optional.of(breach(enchantmentId, level));
        }

        if (POWER.equals(enchantmentId)) {
            return Optional.of(power(enchantmentId, level));
        }

        if (DENSITY.equals(enchantmentId)) {
            return Optional.of(density(enchantmentId, level));
        }

        return Optional.empty();
    }

    private static VanillaEnchantmentTooltipSpec sharpness(
            Identifier source,
            int level
    ) {
        return meleeBase(
                source,
                level,
                sharpnessValue(level),
                List.of()
        );
    }

    private static VanillaEnchantmentTooltipSpec smite(
            Identifier source,
            int level
    ) {
        return meleeBase(
                source,
                level,
                2.5f * level,
                List.of(new TargetEntityTypeTagCondition(SENSITIVE_TO_SMITE))
        );
    }

    private static VanillaEnchantmentTooltipSpec baneOfArthropods(
            Identifier source,
            int level
    ) {
        return meleeBase(
                source,
                level,
                2.5f * level,
                List.of(new TargetEntityTypeTagCondition(
                        SENSITIVE_TO_BANE_OF_ARTHROPODS
                ))
        );
    }

    private static VanillaEnchantmentTooltipSpec featherFalling(
            Identifier source,
            int level
    ) {
        float epf = 3.0f * level;
        float rating = epf * DamageNexusConfig.current()
                .formulas()
                .ratingPerProtScore();

        return new VanillaEnchantmentTooltipSpec(
                source,
                enchantmentName(source, level),
                List.of(),
                List.of(),
                List.of(
                        VanillaEnchantmentTooltipLines.featherFallingEpf(epf),
                        VanillaEnchantmentTooltipLines.featherFallingResistance(rating)
                )
        );
    }

    private static VanillaEnchantmentTooltipSpec breach(
            Identifier source,
            int level
    ) {
        float reduction = 0.15f * level;
        float multiplier = Math.max(0.0f, 1.0f - reduction);

        return new VanillaEnchantmentTooltipSpec(
                source,
                enchantmentName(source, level),
                List.of(new MultiplyArmorEffectivenessOperation(multiplier)),
                List.of(),
                List.of(VanillaEnchantmentTooltipLines.breachReduction(reduction))
        );
    }

    private static VanillaEnchantmentTooltipSpec power(
            Identifier source,
            int level
    ) {
        float value = 0.5f + 0.5f * level;

        return new VanillaEnchantmentTooltipSpec(
                source,
                enchantmentName(source, level),
                List.of(new AddBaseDamageOperation(
                        DamageChannel.PHYSICAL_ID,
                        DamageApplicationBucket.VANILLA_PROJECTILE_ENCHANTMENT,
                        value
                )),
                List.of(),
                List.of(VanillaEnchantmentTooltipLines.powerFormula())
        );
    }

    private static VanillaEnchantmentTooltipSpec density(
            Identifier source,
            int level
    ) {
        float damagePerBlock = 0.5f * level;

        return new VanillaEnchantmentTooltipSpec(
                source,
                enchantmentName(source, level),
                List.of(),
                List.of(),
                List.of(
                        VanillaEnchantmentTooltipLines.densityPerBlock(damagePerBlock),
                        VanillaEnchantmentTooltipLines.densityFormula()
                )
        );
    }

    private static VanillaEnchantmentTooltipSpec meleeBase(
            Identifier source,
            int level,
            float value,
            List<DamageRuleCondition> conditions
    ) {
        return new VanillaEnchantmentTooltipSpec(
                source,
                enchantmentName(source, level),
                List.of(new AddBaseDamageOperation(
                        DamageChannel.PHYSICAL_ID,
                        DamageApplicationBucket.VANILLA_MELEE_ENCHANTMENT,
                        value
                )),
                conditions
        );
    }

    private static float sharpnessValue(int level) {
        return 0.5f * level + 0.5f;
    }

    private static Component enchantmentName(
            Identifier source,
            int level
    ) {
        return Component.translatable(
                        "enchantment."
                                + source.getNamespace()
                                + "."
                                + source.getPath()
                )
                .append(" ")
                .append(Component.translatable("enchantment.level." + level));
    }

    private static Identifier minecraft(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    private static TagKey<EntityType<?>> entityTypeTag(String path) {
        return TagKey.create(
                Registries.ENTITY_TYPE,
                minecraft(path)
        );
    }
}