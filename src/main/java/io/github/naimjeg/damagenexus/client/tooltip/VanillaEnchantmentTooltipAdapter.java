package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddBaseDamageOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddChannelPreMultiplierOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddGlobalPreMultiplierOperation;
import io.github.naimjeg.damagenexus.client.tooltip.document.VanillaTooltipAugmentation;
import io.github.naimjeg.damagenexus.client.tooltip.narrative.RuleNarrativePlanner;
import io.github.naimjeg.damagenexus.util.EnchantmentStackUtil;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

public final class VanillaEnchantmentTooltipAdapter {
    private VanillaEnchantmentTooltipAdapter() {
    }

    public static List<VanillaTooltipAugmentation> collect(
            ItemStack stack,
            RuleNarrativePlanner narratives
    ) {
        if (narratives == null) {
            return List.of();
        }
        return collectEntries(stack).stream()
                .map(entry -> VanillaEnchantmentTooltipCatalog
                        .create(entry.source(), entry.level()))
                .flatMap(java.util.Optional::stream)
                .map(spec -> new VanillaTooltipAugmentation(
                        spec.source(),
                        spec.displayName(),
                        narratives.plan(spec.conditions(), spec.operations()),
                        spec.extraLines(),
                        applicationBuckets(spec.operations()),
                        spec.conditions().stream().map(condition -> condition.type()).toList(),
                        spec.operations().stream().map(operation -> operation.type()).toList()
                ))
                .toList();
    }

    private static List<String> applicationBuckets(
            List<DamageRuleOperation> operations
    ) {
        List<String> buckets = new ArrayList<>();
        for (DamageRuleOperation operation : operations) {
            if (operation instanceof AddBaseDamageOperation base) {
                buckets.add(base.applicationBucket().name());
            } else if (operation instanceof AddChannelPreMultiplierOperation channel) {
                channel.preMultiplierBucketId().map(Object::toString).ifPresent(buckets::add);
            } else if (operation instanceof AddGlobalPreMultiplierOperation global) {
                global.preMultiplierBucketId().map(Object::toString).ifPresent(buckets::add);
            }
        }
        return List.copyOf(buckets);
    }

    private static List<EnchantmentEntry> collectEntries(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        List<EnchantmentEntry> entries = new ArrayList<>();
        EnchantmentStackUtil.forEachEnchantment(
                stack,
                (ignoredStack, enchantment, level) -> {
                    if (level > 0) {
                        entries.add(new EnchantmentEntry(sourceId(enchantment), level));
                    }
                }
        );
        return List.copyOf(entries);
    }

    private static Identifier sourceId(Holder<Enchantment> enchantment) {
        if (enchantment == null) {
            return unknownEnchantmentId();
        }
        return enchantment.unwrapKey()
                .map(key -> key.identifier())
                .orElseGet(VanillaEnchantmentTooltipAdapter::unknownEnchantmentId);
    }

    private static Identifier unknownEnchantmentId() {
        return Identifier.fromNamespaceAndPath("minecraft", "unknown_enchantment");
    }

    private record EnchantmentEntry(Identifier source, int level) {
    }
}
