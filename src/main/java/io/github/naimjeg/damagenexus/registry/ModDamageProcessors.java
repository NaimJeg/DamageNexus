package io.github.naimjeg.damagenexus.registry;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.builtin.bridge.*;
import io.github.naimjeg.damagenexus.builtin.processor.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

public class ModDamageProcessors {

    private static final ResourceKey<Registry<DamagePhaseProcessor>> PROCESSORS_KEY =
            ResourceKey.createRegistryKey(
                    DamageNexusIds.id("damage_phase_processors")
            );

    private static final DeferredRegister<DamagePhaseProcessor> PROCESSORS =
            DeferredRegister.create(PROCESSORS_KEY, DamageNexus.MODID);

    private static final Registry<DamagePhaseProcessor> PROCESSOR_REGISTRY =
            PROCESSORS.makeRegistry(builder -> builder.sync(false));

    public static final DeferredHolder<DamagePhaseProcessor, RuleExecutionProcessor> RULE_BASE =
            PROCESSORS.register(
                    "rule_base",
                    () -> new RuleExecutionProcessor(DamagePhase.BASE_MODIFICATION)
            );

    public static final DeferredHolder<DamagePhaseProcessor, RuleExecutionProcessor> RULE_TYPE_SCALING =
            PROCESSORS.register(
                    "rule_type_scaling",
                    () -> new RuleExecutionProcessor(DamagePhase.TYPE_SCALING)
            );

    public static final DeferredHolder<DamagePhaseProcessor, RuleExecutionProcessor> RULE_MITIGATION =
            PROCESSORS.register(
                    "rule_mitigation",
                    () -> new RuleExecutionProcessor(DamagePhase.MITIGATION_SETUP)
            );

    public static final DeferredHolder<DamagePhaseProcessor, RuleExecutionProcessor> RULE_CRITICAL =
            PROCESSORS.register(
                    "rule_critical",
                    () -> new RuleExecutionProcessor(DamagePhase.CRITICAL_HIT)
            );

    public static final DeferredHolder<DamagePhaseProcessor, RuleExecutionProcessor> RULE_CONDITIONAL =
            PROCESSORS.register(
                    "rule_conditional",
                    () -> new RuleExecutionProcessor(DamagePhase.CONDITIONAL_MULTI)
            );

    public static final DeferredHolder<DamagePhaseProcessor, RuleExecutionProcessor> RULE_GLOBAL =
            PROCESSORS.register(
                    "rule_global",
                    () -> new RuleExecutionProcessor(DamagePhase.GLOBAL_ADJUSTMENT)
            );

    public static final DeferredHolder<DamagePhaseProcessor, RuleExecutionProcessor> RULE_FINAL_OVERRIDE =
            PROCESSORS.register(
                    "rule_final_override",
                    () -> new RuleExecutionProcessor(DamagePhase.FINAL_OVERRIDE)
            );

    public static final DeferredHolder<DamagePhaseProcessor, VanillaArmorEffectivenessProcessor> VANILLA_ARMOR_EFFECTIVENESS =
            PROCESSORS.register("vanilla_armor_effectiveness", VanillaArmorEffectivenessProcessor::new);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaInitialBaseDamageProcessor> VANILLA_INITIAL_BASE_DAMAGE =
            PROCESSORS.register("vanilla_initial_base_damage", VanillaInitialBaseDamageProcessor::new);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaThornsAttributeBridgeProcessor> VANILLA_THORNS_ATTRIBUTE =
            PROCESSORS.register("vanilla_thorns_attribute", VanillaThornsAttributeBridgeProcessor::new);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaOffensiveEnchantmentProcessor> VANILLA_OFFENSIVE_ENCHANTMENT =
            PROCESSORS.register("vanilla_offensive_enchantment", VanillaOffensiveEnchantmentProcessor::new);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaOffensiveMobEffectProcessor> VANILLA_OFFENSIVE_MOB_EFFECT =
            PROCESSORS.register("vanilla_offensive_mob_effect", VanillaOffensiveMobEffectProcessor::new);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaDamageProtectionProcessor> VANILLA_DAMAGE_PROTECTION =
            PROCESSORS.register("vanilla_damage_protection", VanillaDamageProtectionProcessor::new);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaResistanceEffectProcessor> VANILLA_RESISTANCE_EFFECT =
            PROCESSORS.register("vanilla_resistance_effect", VanillaResistanceEffectProcessor::new);

    public static final DeferredHolder<DamagePhaseProcessor, ArmorMitigationProcessor> ARMOR =
            PROCESSORS.register("armor", ArmorMitigationProcessor::new);

    public static final DeferredHolder<DamagePhaseProcessor, ResistanceMitigationProcessor> RESISTANCE =
            PROCESSORS.register("resistance", ResistanceMitigationProcessor::new);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaDifficultyScalingProcessor> VANILLA_DIFFICULTY_SCALING =
            PROCESSORS.register("vanilla_difficulty_scaling", VanillaDifficultyScalingProcessor::new);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaSpecialAttackScalingProcessor> VANILLA_SPECIAL_ATTACK_SCALING =
            PROCESSORS.register("vanilla_special_attack_scaling", VanillaSpecialAttackScalingProcessor::new);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaSpearBonusProcessor> VANILLA_SPEAR_BONUS =
            PROCESSORS.register("vanilla_spear_bonus", VanillaSpearBonusProcessor::new);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaPlayerAttackScalingProcessor> VANILLA_PLAYER_ATTACK_SCALING =
            PROCESSORS.register("vanilla_player_attack_scaling", VanillaPlayerAttackScalingProcessor::new);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaProjectileScalingProcessor> VANILLA_PROJECTILE_SCALING =
            PROCESSORS.register("vanilla_projectile_scaling", VanillaProjectileScalingProcessor::new);

    private static final List<Supplier<? extends DamagePhaseProcessor>>
            BUILTIN_PROCESSORS = List.of(
            RULE_BASE,
            RULE_TYPE_SCALING,
            RULE_MITIGATION,
            RULE_CRITICAL,
            RULE_CONDITIONAL,
            RULE_GLOBAL,
            RULE_FINAL_OVERRIDE,
            VANILLA_ARMOR_EFFECTIVENESS,
            VANILLA_INITIAL_BASE_DAMAGE,
            VANILLA_THORNS_ATTRIBUTE,
            VANILLA_OFFENSIVE_ENCHANTMENT,
            VANILLA_OFFENSIVE_MOB_EFFECT,
            VANILLA_DAMAGE_PROTECTION,
            VANILLA_RESISTANCE_EFFECT,
            ARMOR,
            RESISTANCE,
            VANILLA_DIFFICULTY_SCALING,
            VANILLA_SPECIAL_ATTACK_SCALING,
            VANILLA_SPEAR_BONUS,
            VANILLA_PLAYER_ATTACK_SCALING,
            VANILLA_PROJECTILE_SCALING
    );

    public static void register(IEventBus modBus) {
        PROCESSORS.register(modBus);
    }

    public static List<DamagePhaseProcessor> builtInProcessors() {
        return BUILTIN_PROCESSORS.stream()
                .map(Supplier::get)
                .map(DamagePhaseProcessor.class::cast)
                .toList();
    }

    public static Identifier builtInProcessorId(
            DamagePhaseProcessor processor
    ) {
        return processor == null
                ? null
                : PROCESSOR_REGISTRY.getKey(processor);
    }
}

