package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixRarity;
import io.github.naimjeg.damagenexus.util.EnchantmentStackUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class VanillaEnchantmentTooltipAdapter {

    private static final String DEBUG_SOURCE =
            "VANILLA_ENCHANTMENT_TOOLTIP";

    private VanillaEnchantmentTooltipAdapter() {
    }

    public static boolean hasEntries(ItemStack stack) {
        return !collectEntries(stack).isEmpty();
    }

    public static List<DamageTooltipView> collectTooltipViews(ItemStack stack) {
        return collectEntries(stack)
                .stream()
                .map(VanillaEnchantmentTooltipAdapter::toTooltipView)
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<DamageTooltipView> toTooltipView(
            EnchantmentEntry entry
    ) {
        return VanillaEnchantmentTooltipCatalog
                .create(entry.source(), entry.level())
                .map(spec -> new DamageTooltipView(
                        spec.source(),
                        spec.displayName(),
                        tooltipLines(spec),
                        Optional.empty(),
                        DamageAffixRarity.COMMON,
                        List.of(spec.source()),
                        DEBUG_SOURCE,
                        false
                ));
    }

    private static List<EnchantmentEntry> collectEntries(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }

        List<EnchantmentEntry> entries = new ArrayList<>();

        EnchantmentStackUtil.forEachEnchantment(
                stack,
                (ignoredStack, enchantment, level) -> {
                    if (level <= 0) {
                        return;
                    }

                    entries.add(new EnchantmentEntry(
                            sourceId(enchantment),
                            level
                    ));
                }
        );

        return List.copyOf(entries);
    }

    private static Identifier sourceId(
            Holder<Enchantment> enchantment
    ) {
        if (enchantment == null) {
            return unknownEnchantmentId();
        }

        return enchantment.unwrapKey()
                .map(key -> key.identifier())
                .orElseGet(VanillaEnchantmentTooltipAdapter::unknownEnchantmentId);
    }

    private static Identifier unknownEnchantmentId() {
        return Identifier.fromNamespaceAndPath(
                "minecraft",
                "unknown_enchantment"
        );
    }

    private static List<Component> tooltipLines(
            VanillaEnchantmentTooltipSpec spec
    ) {
        List<Component> lines = new ArrayList<>();

        for (DamageRuleOperation operation : spec.operations()) {
            lines.add(operationLine(
                    operation,
                    spec.conditions()
            ));
        }

        lines.addAll(spec.extraLines());

        return List.copyOf(lines);
    }

    private static MutableComponent operationLine(
            DamageRuleOperation operation,
            List<DamageRuleCondition> conditions
    ) {
        MutableComponent line = Component.empty()
                .append(RuleTooltipDescriptions.describeOperation(
                        operation,
                        RuleTooltipMode.NORMAL
                ));

        if (conditions == null || conditions.isEmpty()) {
            return line;
        }

        return line.append(Component.literal(" "))
                .append(Component.translatable(
                        "tooltip.damagenexus.condition_suffix",
                        joinConditions(conditions)
                ));
    }

    private static MutableComponent joinConditions(
            List<DamageRuleCondition> conditions
    ) {
        MutableComponent result = Component.empty();

        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                result.append(Component.translatable(
                        "tooltip.damagenexus.separator.comma"
                ));
            }

            result.append(RuleTooltipDescriptions.describeCondition(
                    conditions.get(i),
                    RuleTooltipMode.NORMAL
            ));
        }

        return result;
    }

    private record EnchantmentEntry(
            Identifier source,
            int level
    ) {
    }
}