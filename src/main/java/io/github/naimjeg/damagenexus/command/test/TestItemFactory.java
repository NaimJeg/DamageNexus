package io.github.naimjeg.damagenexus.command.test;

import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import io.github.naimjeg.damagenexus.api.DamageNexusAttributes;
import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.item.DamageNexusItemApi;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.builder.DamageRuleBuilder;
import io.github.naimjeg.damagenexus.api.rule.affix.*;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
import io.github.naimjeg.damagenexus.registry.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
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

    private static final String TEST_RULE_LANG_PREFIX =
            "test.damagenexus.rule.";

    private static final String TEST_ITEM_LANG_PREFIX =
            "test.damagenexus.item.";

    public static boolean isTestItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return Boolean.TRUE.equals(
                stack.get(ModDataComponents.TEST_ITEM.get())
        );
    }

    public static ItemStack physicalScalingSword() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.IRON_SWORD),
                        itemName("physical_scaling_sword")
                ),
                List.of(TestRuleFactory.physicalScaling25())
        );
    }

    public static ItemStack flatFireSword() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.DIAMOND_SWORD),
                        itemName("flat_fire_sword")
                ),
                List.of(TestRuleFactory.flatFire4())
        );
    }

    public static ItemStack convertGainOpsItem() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.GOLDEN_SWORD),
                        itemName("ops_convert_gain")
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
                        itemName("ops_defensive")
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
                        itemName("ops_final_override")
                ),
                List.of(TestRuleFactory.overrideFinalDamage7())
        );
    }

    public static ItemStack multiplierOpsItem() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.DIAMOND_SWORD),
                        itemName("ops_multipliers")
                ),
                List.of(
                        TestRuleFactory.flatFire4(),
                        TestRuleFactory.globalPreMultiplier15(),
                        TestRuleFactory.firePostMultiplierNegative10()
                )
        );
    }

    public static ItemStack arrows64() {
        return named(
                new ItemStack(Items.ARROW, 64),
                itemName("arrows")
        );
    }

    public static ItemStack powerBow(ServerLevel level) {
        return enchantedItem(
                level,
                Items.BOW,
                itemName("power_bow"),
                Enchantments.POWER,
                5
        );
    }

    public static ItemStack ruleBow() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.BOW),
                        itemName("rule_bow")
                ),
                List.of(TestRuleFactory.projectileFire3())
        );
    }

    public static ItemStack plainCrossbow() {
        return named(
                new ItemStack(Items.CROSSBOW),
                itemName("plain_crossbow")
        );
    }

    public static ItemStack piercingCrossbow(ServerLevel level) {
        return enchantedItem(
                level,
                Items.CROSSBOW,
                itemName("piercing_crossbow"),
                Enchantments.PIERCING,
                4
        );
    }

    public static ItemStack ruleCrossbow() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.CROSSBOW),
                        itemName("rule_crossbow")
                ),
                List.of(TestRuleFactory.projectileFire3())
        );
    }

    public static ItemStack plainTrident() {
        return named(
                new ItemStack(Items.TRIDENT),
                itemName("plain_trident")
        );
    }

    public static ItemStack impalingTrident(ServerLevel level) {
        return enchantedItem(
                level,
                Items.TRIDENT,
                itemName("impaling_trident"),
                Enchantments.IMPALING,
                5
        );
    }

    public static ItemStack ruleTrident() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.TRIDENT),
                        itemName("rule_trident")
                ),
                List.of(TestRuleFactory.projectileKinetic3())
        );
    }

    public static ItemStack plainIronSword() {
        return named(
                new ItemStack(Items.IRON_SWORD),
                itemName("plain_iron_sword")
        );
    }

    public static ItemStack plainDiamondSword() {
        return named(
                new ItemStack(Items.DIAMOND_SWORD),
                itemName("plain_diamond_sword")
        );
    }

    public static ItemStack sharpnessSword(ServerLevel level) {
        return enchantedItem(
                level,
                Items.IRON_SWORD,
                itemName("sharpness_sword"),
                Enchantments.SHARPNESS,
                5
        );
    }

    public static ItemStack smiteSword(ServerLevel level) {
        return enchantedItem(
                level,
                Items.IRON_SWORD,
                itemName("smite_sword"),
                Enchantments.SMITE,
                5
        );
    }

    public static ItemStack baneSword(ServerLevel level) {
        return enchantedItem(
                level,
                Items.IRON_SWORD,
                itemName("bane_sword"),
                Enchantments.BANE_OF_ARTHROPODS,
                5
        );
    }

    public static ItemStack criticalPhysicalScalingSword() {
        return withRuleEntries(
                named(
                        new ItemStack(Items.IRON_SWORD),
                        itemName("critical_physical_sword")
                ),
                List.of(TestRuleFactory.criticalPhysicalScaling20())
        );
    }

    /** A real +20 percentage-point critical multiplier attribute probe. */
    public static ItemStack critDamageAdditiveSword() {
        ItemStack stack = named(
                new ItemStack(Items.IRON_SWORD),
                itemName("crit_damage_additive_sword")
        );

        ItemAttributeModifiers current = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY
        );
        List<ItemAttributeModifiers.Entry> modifiers =
                new ArrayList<>(current.modifiers());
        modifiers.add(new ItemAttributeModifiers.Entry(
                DamageNexusAttributes.critDamageAdditive(),
                new AttributeModifier(
                        id("test_crit_damage_additive_20"),
                        0.20D,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND,
                ItemAttributeModifiers.Display.override(
                        Component.translatable(
                                TEST_ITEM_LANG_PREFIX
                                        + "crit_damage_additive_sword.attribute"
                        )
                )
        ));
        stack.set(
                DataComponents.ATTRIBUTE_MODIFIERS,
                new ItemAttributeModifiers(modifiers)
        );
        return stack;
    }

    public static ItemStack blazingEdgeSword() {
        return withAffixes(
                named(
                        new ItemStack(Items.IRON_SWORD),
                        itemName("blazing_edge_sword")
                ),
                List.of(TestRuleFactory.blazingEdgeAffix())
        );
    }

    public static ItemStack entryFireSword() {
        return withEntries(
                named(
                        new ItemStack(Items.IRON_SWORD),
                        itemName("entry_fire_sword")
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
                        itemName("entry_unique_group_probe")
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
                        itemName("entry_replace_probe")
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
                        itemName("affix_unique_group_probe")
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
                        itemName("affix_replace_probe")
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

    public static ItemStack affixHighestRarityProbe() {
        Identifier group = id("test_affix_group_fire_highest_probe");

        return withAffixes(
                named(
                        new ItemStack(Items.DIAMOND_SWORD),
                        itemName("affix_highest_rarity_probe")
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
        return DamageRuleBuilder
                .offensive(id(path))
                .baseModification()
                .always()
                .addBaseDamage(DamageChannel.FIRE_ID, value)
                .trace("测试：火焰伤害 +" + value)
                .build();
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
        if (!DamageNexusItemApi.setEntries(stack, entries)) {
            throw new IllegalStateException(
                    "无法向测试物品写入伤害条目"
            );
        }

        return stack;
    }

    private static ItemStack withAffixes(
            ItemStack stack,
            List<DamageAffixDefinition> affixes
    ) {
        if (!DamageNexusItemApi.setAffixes(stack, affixes)) {
            throw new IllegalStateException(
                    "无法向测试物品写入伤害词缀"
            );
        }

        return stack;
    }

    public static ItemStack enchantedItem(
            ServerLevel level,
            Item item,
            Component name,
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

        return withEntries(stack, entries);
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
                        DisplayText.translatable(
                                TEST_RULE_LANG_PREFIX
                                        + rule.id().getPath()
                                        + ".name"
                        ),
                        List.of(DisplayText.translatable(
                                TEST_RULE_LANG_PREFIX
                                        + rule.id().getPath()
                                        + ".description"
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

    private static Component itemName(String path) {
        return Component.translatable(
                TEST_ITEM_LANG_PREFIX + path + ".name"
        );
    }

    private static ItemStack named(ItemStack stack, Component name) {
        stack.set(
                DataComponents.CUSTOM_NAME,
                name
        );
        markTestItem(stack);

        return stack;
    }

    private static void markTestItem(ItemStack stack) {
        stack.set(
                ModDataComponents.TEST_ITEM.get(),
                Boolean.TRUE
        );
    }
}
