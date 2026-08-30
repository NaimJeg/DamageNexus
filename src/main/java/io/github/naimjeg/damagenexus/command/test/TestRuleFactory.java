package io.github.naimjeg.damagenexus.command.test;

import io.github.naimjeg.damagenexus.api.DamageNexusPreMultiplierBuckets;

import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.builder.DamageRuleBuilder;
import io.github.naimjeg.damagenexus.api.rule.affix.*;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
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
        return DamageRuleBuilder
                .offensive(id("test_ops_convert_physical_to_fire"))
                .typeScaling()
                .priority(400)
                .always()
                .convertDamage(
                        DamageChannel.PHYSICAL_ID,
                        DamageChannel.FIRE_ID,
                        0.50f)
                .trace("测试：50% 物理伤害转换为火焰伤害")
                .build();
    }

    public static DamageRuleDefinition gainLightningFromPhysical() {
        return DamageRuleBuilder
                .offensive(id("test_ops_gain_lightning_from_physical"))
                .typeScaling()
                .priority(401)
                .always()
                .gainExtraDamage(
                        DamageChannel.PHYSICAL_ID,
                        DamageChannel.LIGHTNING_ID,
                        0.25f)
                .trace("测试：获得物理伤害 25% 的额外闪电伤害")
                .build();
    }

    public static DamageRuleDefinition temporaryFireResistance() {
        return DamageRuleBuilder
                .defensive(id("test_ops_temp_fire_resistance"))
                .mitigationSetup()
                .always()
                .addTemporaryResistance(DamageChannel.FIRE_ID, 25.0f)
                .trace("测试：临时火焰抗性 +25")
                .build();
    }

    private static Identifier id(String path) {
        return DamageNexusIds.id(path);
    }

    public static DamageRuleDefinition physicalScaling25() {
        return DamageRuleBuilder
                .offensive(id("test_physical_scaling_25"))
                .typeScaling()
                .always()
                .addChannelPreMultiplier(DamageChannel.PHYSICAL_ID, 0.25f)
                .trace("测试：物理伤害 +25%")
                .build();
    }

    public static DamageRuleDefinition flatFire4() {
        return DamageRuleBuilder
                .offensive(id("test_flat_fire_4"))
                .baseModification()
                .always()
                .addBaseDamage(DamageChannel.FIRE_ID, 4.0f)
                .trace("测试：固定火焰伤害 +4")
                .build();
    }

    public static DamageRuleDefinition physicalMitigation20() {
        return DamageRuleBuilder
                .defensive(id("test_ops_physical_mitigation"))
                .mitigationSetup()
                .priority(501)
                .always()
                .addChannelMitigation(DamageChannel.PHYSICAL_ID, 0.20f)
                .trace("测试：物理伤害减免 20%")
                .build();
    }

    public static DamageRuleDefinition overrideFinalDamage7() {
        return DamageRuleBuilder
                .offensive(id("test_ops_override_final_7"))
                .finalOverride()
                .priority(999)
                .always()
                .overrideFinalDamage(7.0f)
                .replace()
                .stackingGroup(id("test_ops_override_group"))
                .trace("测试：最终伤害覆盖为 7")
                .build();
    }

    public static DamageRuleDefinition globalPreMultiplier15() {
        return DamageRuleBuilder
                .offensive(id("test_ops_global_pre_15"))
                .globalAdjustment()
                .priority(777)
                .always()
                .addGlobalPreMultiplier(0.15f)
                .uniqueSource()
                .stackingGroup(id("test_ops_global_group"))
                .trace("测试：全局前乘伤害 +15%")
                .build();
    }

    public static DamageRuleDefinition firePostMultiplierNegative10() {
        return DamageRuleBuilder
                .offensive(id("test_ops_fire_post_negative"))
                .globalAdjustment()
                .priority(778)
                .always()
                .addChannelPostMultiplier(DamageChannel.FIRE_ID, -0.10f)
                .trace("测试：火焰伤害后乘 -10%")
                .build();
    }

    public static DamageRuleDefinition projectileFire3() {
        return DamageRuleBuilder
                .offensive(id("test_projectile_source_fire_3"))
                .baseModification()
                .priority(520)
                .always()
                .addBaseDamage(DamageChannel.FIRE_ID, 3.0f)
                .trace("测试：投射物来源火焰伤害 +3")
                .build();
    }

    public static DamageRuleDefinition projectileKinetic3() {
        return DamageRuleBuilder
                .offensive(id("test_projectile_source_kinetic_3"))
                .baseModification()
                .priority(520)
                .always()
                .addBaseDamage(DamageChannel.KINETIC_ID, 3.0f)
                .trace("测试：投射物来源动能伤害 +3")
                .build();
    }

    public static DamageRuleDefinition criticalPhysicalScaling20() {
        return DamageRuleBuilder
                .offensive(id("test_critical_physical_scaling_20"))
                .criticalHit()
                .critical()
                .addChannelPreMultiplier(
                        DamageChannel.PHYSICAL_ID,
                        DamageNexusPreMultiplierBuckets.CRIT_DAMAGE,
                        0.20f)
                .trace("测试：暴击时物理伤害额外提高 20%")
                .build();
    }

    /**
     * CONDITIONAL_MULTI test rule: grants the target a final/post multiplier
     * of +25% (1.25x) whenever the victim's current health is strictly above
     * 80% of its maximum health. Exactly 80% does not qualify.
     */
    public static DamageRuleDefinition targetHighHealthGlobalPost25() {
        return DamageRuleBuilder
                .offensive(id("test_target_health_above_80_global_post_25"))
                .conditionalMultiplier()
                .targetHealthAbove(0.80f)
                .addGlobalPostMultiplier(0.25f)
                .trace("测试：目标生命值高于 80% 时最终乘区 +25%")
                .build();
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
        return DamageRuleBuilder
                .offensive(id("test_affix_blazing_edge/fire_damage"))
                .baseModification()
                .priority(520)
                .always()
                .addBaseDamage(DamageChannel.FIRE_ID, 4.0f)
                .trace("测试词缀：炽焰锋刃火焰伤害 +4")
                .build();
    }

    private static DamageRuleDefinition blazingEdgeFireScalingRule() {
        return DamageRuleBuilder
                .offensive(id("test_affix_blazing_edge/fire_scaling"))
                .typeScaling()
                .priority(510)
                .always()
                .addChannelPreMultiplier(
                        DamageChannel.FIRE_ID,
                        DamageNexusPreMultiplierBuckets.FIRE_DAMAGE,
                        0.15f)
                .trace("测试词缀：炽焰锋刃火焰伤害 +15%")
                .build();
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
                        testRuleText(rule.id().getPath(), "name"),
                        List.of(testRuleText(
                                rule.id().getPath(),
                                "description"
                        )),
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
