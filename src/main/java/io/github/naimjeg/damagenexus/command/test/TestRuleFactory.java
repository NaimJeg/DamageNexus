package io.github.naimjeg.damagenexus.command.test;

import io.github.naimjeg.damagenexus.api.DamageNexusPreMultiplierBuckets;

import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleStacking;
import io.github.naimjeg.damagenexus.api.rule.affix.*;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AlwaysCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.IsCriticalCondition;
import io.github.naimjeg.damagenexus.builtin.rule.operation.*;
import io.github.naimjeg.damagenexus.registry.PreMultiplierBuckets;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class TestRuleFactory {

    private static final String TEST_RULE_LANG_PREFIX =
            "test.damagenexus.rule.";
    private static final String TEST_AFFIX_LANG_PREFIX =
            "test.damagenexus.affix.";

    private TestRuleFactory() {
    }

    public static DamageRuleDefinition convertPhysicalToFire() {
        return new DamageRuleDefinition(
                id("test_ops_convert_physical_to_fire"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.TYPE_SCALING,
                400,
                List.of(new AlwaysCondition()),
                List.of(new ConvertDamageOperation(
                        DamageChannel.PHYSICAL_ID,
                        DamageChannel.FIRE_ID,
                        0.50f
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.of("Ops Convert 50% Physical to Fire")
        );
    }

    public static DamageRuleDefinition gainLightningFromPhysical() {
        return new DamageRuleDefinition(
                id("test_ops_gain_lightning_from_physical"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.TYPE_SCALING,
                401,
                List.of(new AlwaysCondition()),
                List.of(new GainExtraDamageOperation(
                        DamageChannel.PHYSICAL_ID,
                        DamageChannel.LIGHTNING_ID,
                        0.25f
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.of("Ops Gain 25% Physical as Lightning")
        );
    }

    public static DamageRuleDefinition temporaryFireResistance() {
        return new DamageRuleDefinition(
                id("test_ops_temp_fire_resistance"),
                DamageRuleRole.DEFENSIVE,
                DamagePhase.MITIGATION_SETUP,
                500,
                List.of(new AlwaysCondition()),
                List.of(new AddTemporaryResistanceOperation(
                        DamageChannel.FIRE_ID,
                        25.0f
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.of("Ops Temp Fire Resistance +25")
        );
    }

    private static Identifier id(String path) {
        return DamageNexusIds.id(path);
    }

    public static DamageRuleDefinition physicalScaling25() {
        return new DamageRuleDefinition(
                id("test_physical_scaling_25"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.TYPE_SCALING,
                500,
                List.of(new AlwaysCondition()),
                List.of(new AddChannelPreMultiplierOperation(
                        DamageChannel.PHYSICAL_ID,
                        Optional.empty(),
                        0.25f
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.of("Physical Scaling +25%")
        );
    }

    public static DamageRuleDefinition flatFire4() {
        return new DamageRuleDefinition(
                id("test_flat_fire_4"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                List.of(new AlwaysCondition()),
                List.of(new AddBaseDamageOperation(
                        DamageChannel.FIRE_ID,
                        4.0f
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.of("Flat Fire +4")
        );
    }

    public static DamageRuleDefinition physicalMitigation20() {
        return new DamageRuleDefinition(
                id("test_ops_physical_mitigation"),
                DamageRuleRole.DEFENSIVE,
                DamagePhase.MITIGATION_SETUP,
                501,
                List.of(new AlwaysCondition()),
                List.of(new AddChannelMitigationOperation(
                        DamageChannel.PHYSICAL_ID,
                        0.20f
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.of("Ops Physical Mitigation +20%")
        );
    }

    public static DamageRuleDefinition overrideFinalDamage7() {
        return new DamageRuleDefinition(
                id("test_ops_override_final_7"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.FINAL_OVERRIDE,
                999,
                List.of(new AlwaysCondition()),
                List.of(new OverrideFinalDamageOperation(
                        7.0f
                )),
                DamageRuleStacking.REPLACE,
                Optional.of(id("test_ops_override_group")),
                Optional.of("Ops Override Final 7")
        );
    }

    public static DamageRuleDefinition globalPreMultiplier15() {
        return new DamageRuleDefinition(
                id("test_ops_global_pre_15"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.GLOBAL_ADJUSTMENT,
                777,
                List.of(new AlwaysCondition()),
                List.of(new AddGlobalPreMultiplierOperation(
                        Optional.empty(),
                        0.15f
                )),
                DamageRuleStacking.UNIQUE_SOURCE,
                Optional.of(id("test_ops_global_group")),
                Optional.of("Ops Global +15%")
        );
    }

    public static DamageRuleDefinition firePostMultiplierNegative10() {
        return new DamageRuleDefinition(
                id("test_ops_fire_post_negative"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.GLOBAL_ADJUSTMENT,
                778,
                List.of(new AlwaysCondition()),
                List.of(new AddChannelPostMultiplierOperation(
                        DamageChannel.FIRE_ID,
                        -0.10f
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.of("Ops Fire Post -10%")
        );
    }

    public static DamageRuleDefinition projectileFire3() {
        return new DamageRuleDefinition(
                id("test_projectile_source_fire_3"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                520,
                List.of(new AlwaysCondition()),
                List.of(new AddBaseDamageOperation(
                        DamageChannel.FIRE_ID,
                        3.0f
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.of("Projectile Source +3 Fire")
        );
    }

    public static DamageRuleDefinition projectileKinetic3() {
        return new DamageRuleDefinition(
                id("test_projectile_source_kinetic_3"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                520,
                List.of(new AlwaysCondition()),
                List.of(new AddBaseDamageOperation(
                        DamageChannel.KINETIC_ID,
                        3.0f
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.of("Projectile Source +3 Kinetic")
        );
    }

    public static DamageRuleDefinition critPhysicalPreMultiplier20() {
        return new DamageRuleDefinition(
                id("test_crit_damage_20"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.CRITICAL_HIT,
                500,
                List.of(new IsCriticalCondition()),
                List.of(new AddChannelPreMultiplierOperation(
                        DamageChannel.PHYSICAL_ID,
                        Optional.of(DamageNexusPreMultiplierBuckets.CRIT_DAMAGE),
                        0.20f
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.of("Critical Damage +20%")
        );
    }

    public static DamageAffixDefinition blazingEdgeAffix() {
        return new DamageAffixDefinition(
                id("test_affix_blazing_edge"),
                testAffixDisplay(
                        "test_affix_blazing_edge",
                        List.of(
                                testAffixText(
                                        "test_affix_blazing_edge",
                                        "tooltip.1"
                                ),
                                testAffixText(
                                        "test_affix_blazing_edge",
                                        "tooltip.2"
                                )
                        ),
                        Optional.of(testAffixText(
                                "test_affix_blazing_edge",
                                "flavor"
                        )),
                        false
                ),
                DamageAffixSlot.WEAPON,
                DamageAffixRarity.RARE,
                List.of(
                        entryFromRule(blazingEdgeFireDamageRule()),
                        entryFromRule(blazingEdgeFireScalingRule())
                ),
                DamageAffixStacking.UNIQUE_AFFIX,
                Optional.empty()
        );
    }

    private static DamageRuleDefinition blazingEdgeFireDamageRule() {
        return new DamageRuleDefinition(
                id("test_affix_blazing_edge/fire_damage"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                520,
                List.of(new AlwaysCondition()),
                List.of(new AddBaseDamageOperation(
                        DamageChannel.FIRE_ID,
                        4.0f
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.of("Blazing Edge +4 Fire")
        );
    }

    private static DamageRuleDefinition blazingEdgeFireScalingRule() {
        return new DamageRuleDefinition(
                id("test_affix_blazing_edge/fire_scaling"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.TYPE_SCALING,
                510,
                List.of(new AlwaysCondition()),
                List.of(new AddChannelPreMultiplierOperation(
                        DamageChannel.FIRE_ID,
                        Optional.of(DamageNexusPreMultiplierBuckets.FIRE_DAMAGE),
                        0.15f
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.of("Blazing Edge Fire +15%")
        );
    }

    private static DisplayText testRuleText(
            String rulePath,
            String field
    ) {
        return DisplayText.translatable(
                TEST_RULE_LANG_PREFIX + rulePath + "." + field
        );
    }

    private static DisplayText testAffixText(
            String affixPath,
            String field
    ) {
        return DisplayText.translatable(
                TEST_AFFIX_LANG_PREFIX + affixPath + "." + field
        );
    }

    private static DamageEntryDefinition entryFromRule(
            DamageRuleDefinition rule
    ) {
        Identifier entryId = DamageNexusIds.id(
                "test_entry_" + sanitizePath(rule.id().getPath())
        );

        return new DamageEntryDefinition(
                entryId,
                new DamageEntryDisplay(
                        DisplayText.literal(rule.id().getPath()),
                        List.of(DisplayText.literal("Test rule entry")),
                        Optional.empty(),
                        true
                ),
                DamageEntrySlot.WEAPON,
                List.of(rule),
                DamageEntryStacking.STACK,
                Optional.empty()
        );
    }

    private static String sanitizePath(String path) {
        if (path == null || path.isBlank()) {
            return "unknown";
        }

        return path
                .replace(':', '_')
                .replace('/', '_')
                .replace(' ', '_')
                .toLowerCase(Locale.ROOT);
    }

    private static DamageAffixDisplay testAffixDisplay(
            String affixPath,
            List<DisplayText> tooltip,
            Optional<DisplayText> flavorText,
            boolean showRuleBreakdown
    ) {
        return new DamageAffixDisplay(
                testAffixText(affixPath, "name"),
                tooltip,
                flavorText,
                showRuleBreakdown
        );
    }
}
