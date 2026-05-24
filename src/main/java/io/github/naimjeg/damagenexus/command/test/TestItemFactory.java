package io.github.naimjeg.damagenexus.command.test;

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
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddBaseDamageOperation;
import io.github.naimjeg.damagenexus.registry.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class TestItemFactory {

    private TestItemFactory() {
    }

    private static final String TEST_ENTRY_LANG_PREFIX =
            "test.damagenexus.entry.";

    private static final String TEST_AFFIX_LANG_PREFIX =
            "test.damagenexus.affix.";

    public static ItemStack physicalScalingSword() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.IRON_SWORD),
                        "[DN-Test] +25% Physical"
                ),
                List.of(TestRuleFactory.physicalScaling25())
        );
    }

    public static ItemStack flatFireSword() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.DIAMOND_SWORD),
                        "[DN-Test] +4 Fire Damage"
                ),
                List.of(TestRuleFactory.flatFire4())
        );
    }

    public static ItemStack convertGainOpsItem() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.GOLDEN_SWORD),
                        "[DN-Test] Ops / Convert + Gain"
                ),
                List.of(
                        TestRuleFactory.convertPhysicalToFire(),
                        TestRuleFactory.gainLightningFromPhysical()
                )
        );
    }

    public static ItemStack defensiveOpsItem() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.SHIELD),
                        "[DN-Test] Ops / Defensive Mitigation"
                ),
                List.of(
                        TestRuleFactory.temporaryFireResistance(),
                        TestRuleFactory.physicalMitigation20()
                )
        );
    }

    public static ItemStack finalOverrideOpsItem() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.STICK),
                        "[DN-Test] Ops / Final Override 7"
                ),
                List.of(TestRuleFactory.overrideFinalDamage7())
        );
    }

    public static ItemStack multiplierOpsItem() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.DIAMOND_SWORD),
                        "[DN-Test] Ops / Global + Post Multipliers"
                ),
                List.of(
                        TestRuleFactory.globalPreMultiplier15(),
                        TestRuleFactory.firePostMultiplierNegative10()
                )
        );
    }

    public static ItemStack arrows64() {
        return named(
                new ItemStack(Items.ARROW, 64),
                "[DN-Test] Arrows"
        );
    }

    public static ItemStack powerBow(ServerLevel level) {
        return enchantedItem(
                level,
                Items.BOW,
                "[DN-Test] Power V Bow",
                Enchantments.POWER,
                5
        );
    }

    public static ItemStack ruleBow() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.BOW),
                        "[DN-Test] Projectile Rule Bow / +3 Fire"
                ),
                List.of(TestRuleFactory.projectileFire3())
        );
    }

    public static ItemStack plainCrossbow() {
        return named(
                new ItemStack(Items.CROSSBOW),
                "[DN-Test] Plain Crossbow"
        );
    }

    public static ItemStack piercingCrossbow(ServerLevel level) {
        return enchantedItem(
                level,
                Items.CROSSBOW,
                "[DN-Test] Piercing IV Crossbow",
                Enchantments.PIERCING,
                4
        );
    }

    public static ItemStack ruleCrossbow() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.CROSSBOW),
                        "[DN-Test] Projectile Rule Crossbow / +3 Fire"
                ),
                List.of(TestRuleFactory.projectileFire3())
        );
    }

    public static ItemStack plainTrident() {
        return named(
                new ItemStack(Items.TRIDENT),
                "[DN-Test] Plain Trident"
        );
    }

    public static ItemStack impalingTrident(ServerLevel level) {
        return enchantedItem(
                level,
                Items.TRIDENT,
                "[DN-Test] Impaling V Trident",
                Enchantments.IMPALING,
                5
        );
    }

    public static ItemStack ruleTrident() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.TRIDENT),
                        "[DN-Test] Projectile Rule Trident / +3 Kinetic"
                ),
                List.of(TestRuleFactory.projectileKinetic3())
        );
    }

    public static ItemStack plainIronSword() {
        return named(
                new ItemStack(Items.IRON_SWORD),
                "[DN-Test] Plain Iron Sword"
        );
    }

    public static ItemStack plainDiamondSword() {
        return named(
                new ItemStack(Items.DIAMOND_SWORD),
                "[DN-Test] Plain Diamond Sword"
        );
    }

    public static ItemStack sharpnessSword(ServerLevel level) {
        return enchantedItem(
                level,
                Items.IRON_SWORD,
                "[DN-Test] Sharpness V",
                Enchantments.SHARPNESS,
                5
        );
    }

    public static ItemStack smiteSword(ServerLevel level) {
        return enchantedItem(
                level,
                Items.IRON_SWORD,
                "[DN-Test] Smite V",
                Enchantments.SMITE,
                5
        );
    }

    public static ItemStack baneSword(ServerLevel level) {
        return enchantedItem(
                level,
                Items.IRON_SWORD,
                "[DN-Test] Bane V",
                Enchantments.BANE_OF_ARTHROPODS,
                5
        );
    }

    public static ItemStack critDamageSword() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.IRON_SWORD),
                        "[DN-Test] +20% Crit Damage"
                ),
                List.of(TestRuleFactory.critPhysicalPreMultiplier20())
        );
    }

    public static ItemStack blazingEdgeSword() {
        return withAffixes(
                named(
                        new ItemStack(Items.IRON_SWORD),
                        "[DN-Test] Affix: Blazing Edge"
                ),
                List.of(TestRuleFactory.blazingEdgeAffix())
        );
    }

    public static ItemStack entryFireSword() {
        return withEntries(
                named(
                        new ItemStack(Items.IRON_SWORD),
                        "[DN-Test] Entry: Fire Edge"
                ),
                List.of(new DamageEntryDefinition(
                        id("test_entry_fire_edge"),
                        testEntryDisplay(
                                "test_entry_fire_edge",
                                true
                        ),
                        DamageEntrySlot.WEAPON,
                        List.of(TestRuleFactory.flatFire4()),
                        DamageEntryStacking.STACK,
                        Optional.empty()
                ))
        );
    }

    public static ItemStack entryUniqueGroupProbe() {
        Identifier group = id("test_entry_group_fire_probe");

        return withEntries(
                named(
                        new ItemStack(Items.IRON_SWORD),
                        "[DN-Test] Entry Unique Group Probe"
                ),
                List.of(
                        fireEntry(
                                "test_entry_unique_group_a",
                                1.0f,
                                DamageEntryStacking.UNIQUE_GROUP,
                                group
                        ),
                        fireEntry(
                                "test_entry_unique_group_b",
                                2.0f,
                                DamageEntryStacking.UNIQUE_GROUP,
                                group
                        )
                )
        );
    }

    public static ItemStack entryReplaceProbe() {
        Identifier group = id("test_entry_group_fire_replace_probe");

        return withEntries(
                named(
                        new ItemStack(Items.IRON_SWORD),
                        "[DN-Test] Entry Replace Probe"
                ),
                List.of(
                        fireEntry(
                                "test_entry_replace_a",
                                1.0f,
                                DamageEntryStacking.REPLACE,
                                group
                        ),
                        fireEntry(
                                "test_entry_replace_b",
                                2.0f,
                                DamageEntryStacking.REPLACE,
                                group
                        )
                )
        );
    }

    public static ItemStack affixUniqueGroupProbe() {
        Identifier group = id("test_affix_group_fire_probe");

        return withAffixes(
                named(
                        new ItemStack(Items.DIAMOND_SWORD),
                        "[DN-Test] Affix Unique Group Probe"
                ),
                List.of(
                        fireAffix(
                                "test_affix_unique_group_a",
                                DamageAffixRarity.COMMON,
                                1.0f,
                                DamageAffixStacking.UNIQUE_GROUP,
                                group
                        ),
                        fireAffix(
                                "test_affix_unique_group_b",
                                DamageAffixRarity.RARE,
                                2.0f,
                                DamageAffixStacking.UNIQUE_GROUP,
                                group
                        )
                )
        );
    }

    public static ItemStack affixReplaceProbe() {
        Identifier group = id("test_affix_group_fire_replace_probe");

        return withAffixes(
                named(
                        new ItemStack(Items.DIAMOND_SWORD),
                        "[DN-Test] Affix Replace Probe"
                ),
                List.of(
                        fireAffix(
                                "test_affix_replace_a",
                                DamageAffixRarity.COMMON,
                                1.0f,
                                DamageAffixStacking.REPLACE,
                                group
                        ),
                        fireAffix(
                                "test_affix_replace_b",
                                DamageAffixRarity.RARE,
                                2.0f,
                                DamageAffixStacking.REPLACE,
                                group
                        )
                )
        );
    }

    public static ItemStack affixHighestLevelProbe() {
        Identifier group = id("test_affix_group_fire_highest_probe");

        return withAffixes(
                named(
                        new ItemStack(Items.DIAMOND_SWORD),
                        "[DN-Test] Affix Highest Level Probe"
                ),
                List.of(
                        fireAffix(
                                "test_affix_highest_common",
                                DamageAffixRarity.COMMON,
                                1.0f,
                                DamageAffixStacking.HIGHEST_LEVEL,
                                group
                        ),
                        fireAffix(
                                "test_affix_highest_epic",
                                DamageAffixRarity.EPIC,
                                3.0f,
                                DamageAffixStacking.HIGHEST_LEVEL,
                                group
                        )
                )
        );
    }

    private static DamageRuleDefinition fireBaseRule(
            String path,
            float value
    ) {
        return new DamageRuleDefinition(
                id(path),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                List.of(new AlwaysCondition()),
                List.of(new AddBaseDamageOperation(
                        DamageChannel.FIRE_ID,
                        value
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.of(path + " fire +" + value)
        );
    }

    private static DamageEntryDefinition fireEntry(
            String path,
            float value,
            DamageEntryStacking stacking,
            Identifier stackingGroup
    ) {
        return new DamageEntryDefinition(
                id(path),
                testEntryDisplay(path, true),
                DamageEntrySlot.WEAPON,
                List.of(fireBaseRule(path + "_rule", value)),
                stacking,
                Optional.ofNullable(stackingGroup)
        );
    }

    private static DamageAffixDefinition fireAffix(
            String path,
            DamageAffixRarity rarity,
            float value,
            DamageAffixStacking stacking,
            Identifier stackingGroup
    ) {
        return new DamageAffixDefinition(
                id(path),
                testAffixDisplay(path, true),
                DamageAffixSlot.WEAPON,
                rarity,
                List.of(fireEntry(
                        path + "_entry",
                        value,
                        DamageEntryStacking.STACK,
                        null
                )),
                stacking,
                Optional.ofNullable(stackingGroup)
        );
    }

    private static DamageEntryDisplay testEntryDisplay(
            String entryPath,
            boolean showRuleBreakdown
    ) {
        return new DamageEntryDisplay(
                testEntryText(entryPath, "name"),
                List.of(testEntryText(entryPath, "tooltip.1")),
                Optional.empty(),
                showRuleBreakdown
        );
    }

    private static DamageAffixDisplay testAffixDisplay(
            String affixPath,
            boolean showRuleBreakdown
    ) {
        return new DamageAffixDisplay(
                testAffixText(affixPath, "name"),
                List.of(testAffixText(affixPath, "tooltip.1")),
                Optional.empty(),
                showRuleBreakdown
        );
    }

    private static DisplayText testEntryText(
            String entryPath,
            String field
    ) {
        return DisplayText.translatable(
                TEST_ENTRY_LANG_PREFIX + entryPath + "." + field
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

    private static ItemStack withEntries(
            ItemStack stack,
            List<DamageEntryDefinition> entries
    ) {
        stack.set(
                ModDataComponents.DAMAGE_ENTRIES.get(),
                List.copyOf(entries)
        );

        return stack;
    }

    private static ItemStack withAffixes(
            ItemStack stack,
            List<DamageAffixDefinition> affixes
    ) {
        stack.set(
                ModDataComponents.DAMAGE_AFFIXES.get(),
                List.copyOf(affixes)
        );

        return stack;
    }

    public static ItemStack enchantedItem(
            ServerLevel level,
            Item item,
            String name,
            ResourceKey<Enchantment> enchantmentKey,
            int levelValue
    ) {
        ItemStack stack = named(new ItemStack(item), name);

        Holder<Enchantment> enchantment =
                level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .get(enchantmentKey)
                        .orElse(null);

        if (enchantment == null) {
            return stack;
        }

        ItemEnchantments.Mutable mutableEnchantments =
                new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

        mutableEnchantments.set(enchantment, levelValue);

        stack.set(
                DataComponents.ENCHANTMENTS,
                mutableEnchantments.toImmutable()
        );

        return stack;
    }

    private static ItemStack withRuleEntries(
            ItemStack stack,
            List<DamageRuleDefinition> rules
    ) {
        if (rules == null || rules.isEmpty()) {
            return stack;
        }

        List<DamageEntryDefinition> entries = rules.stream()
                .map(TestItemFactory::ruleEntry)
                .toList();

        stack.set(
                ModDataComponents.DAMAGE_ENTRIES.get(),
                entries
        );

        return stack;
    }

    private static DamageEntryDefinition ruleEntry(
            DamageRuleDefinition rule
    ) {
        Identifier entryId = DamageNexusIds.id(
                "test_entry_" + sanitizePath(rule.id().getPath())
        );

        return new DamageEntryDefinition(
                entryId,
                new DamageEntryDisplay(
                        DisplayText.literal(rule.id().getPath()),
                        List.of(DisplayText.literal("Rule-backed test entry")),
                        Optional.empty(),
                        true
                ),
                DamageEntrySlot.WEAPON,
                List.of(rule),
                DamageEntryStacking.STACK,
                Optional.empty()
        );
    }

    private static Identifier id(String path) {
        return DamageNexusIds.id(path);
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

    private static ItemStack named(
            ItemStack stack,
            String name
    ) {
        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.literal(name)
        );

        return stack;
    }
}
