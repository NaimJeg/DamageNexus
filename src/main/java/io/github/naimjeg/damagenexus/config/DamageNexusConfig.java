package io.github.naimjeg.damagenexus.config;

import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusLifecycleLog;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class DamageNexusConfig {
    private static final ModConfigSpec.Builder BUILDER =
            new ModConfigSpec.Builder();

    public static final ModConfigSpec SPEC;

    private static volatile DamageNexusConfigValues CURRENT =
            DamageNexusConfigValues.defaults();
    private static EffectiveConfigLogSnapshot lastLoggedConfig;

    static {
        DeveloperConfigSpec.define(BUILDER);
        DiagnosticsConfigSpec.define(BUILDER);
        TooltipConfigSpec.define(BUILDER);
        CombatFormulaConfigSpec.define(BUILDER);
        VanillaCompatibilityConfigSpec.define(BUILDER);
        DamageSafetyConfigSpec.define(BUILDER);

        SPEC = BUILDER.build();
    }

    private DamageNexusConfig() {
    }

    public static DamageNexusConfigValues current() {
        return CURRENT;
    }

    public static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            bakeConfig();
        }
    }

    public static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            bakeConfig();
        }
    }

    public static synchronized void bakeConfig() {
        DamageNexusConfigValues values = new DamageNexusConfigValues(
                DeveloperConfigSpec.bake(),
                DiagnosticsConfigSpec.bake(),
                TooltipConfigSpec.bake(),
                CombatFormulaConfigSpec.bake(),
                VanillaCompatibilityConfigSpec.bake(),
                DamageSafetyConfigSpec.bake()
        );

        CURRENT = values;

        if (!shouldLogBakedConfig(values)) {
            return;
        }

        DamageNexusLifecycleLog.configBaked(
                values.diagnostics().diagnosticMode(),
                values.developer().testCommandsEnabled(),
                values.developer().strictProcessorErrors(),
                values.developer().strictRuleErrors(),
                values.vanillaCompatibility().mode(),
                values.vanillaCompatibility().shouldSuppressArmor(),
                values.vanillaCompatibility().shouldSuppressEnchantments(),
                values.vanillaCompatibility().shouldSuppressMobEffects(),
                values.vanillaCompatibility().shouldSuppressInnateResistance(),
                values.formulas().asymptoticKValue(),
                values.formulas().resistanceKValue(),
                values.formulas().ratingPerProtScore()
        );
    }

    static synchronized boolean shouldLogBakedConfig(
            DamageNexusConfigValues values
    ) {
        EffectiveConfigLogSnapshot snapshot =
                EffectiveConfigLogSnapshot.from(values);
        if (snapshot.equals(lastLoggedConfig)) {
            return false;
        }
        lastLoggedConfig = snapshot;
        return true;
    }

    static synchronized void resetConfigLogGateForTesting() {
        lastLoggedConfig = null;
    }

    private record EffectiveConfigLogSnapshot(
            DeveloperSettings developer,
            DiagnosticsSettings diagnostics,
            TooltipSettings tooltips,
            CombatFormulaSettings formulas,
            DamageSafetySettings safety,
            VanillaReductionCompatibilityMode vanillaMode,
            boolean suppressArmor,
            boolean suppressEnchantments,
            boolean suppressMobEffects,
            boolean suppressInnateResistance
    ) {
        private static EffectiveConfigLogSnapshot from(
                DamageNexusConfigValues values
        ) {
            VanillaCompatibilitySettings vanilla =
                    values.vanillaCompatibility();
            return new EffectiveConfigLogSnapshot(
                    values.developer(),
                    values.diagnostics(),
                    values.tooltips(),
                    values.formulas(),
                    values.safety(),
                    vanilla.mode(),
                    vanilla.shouldSuppressArmor(),
                    vanilla.shouldSuppressEnchantments(),
                    vanilla.shouldSuppressMobEffects(),
                    vanilla.shouldSuppressInnateResistance()
            );
        }
    }
}
