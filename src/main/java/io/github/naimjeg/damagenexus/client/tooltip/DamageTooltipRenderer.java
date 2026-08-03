package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.client.phrase.PhraseForm;
import io.github.naimjeg.damagenexus.api.client.phrase.RulePhrase;
import io.github.naimjeg.damagenexus.client.tooltip.document.*;
import io.github.naimjeg.damagenexus.client.tooltip.narrative.ConditionExpression;
import io.github.naimjeg.damagenexus.client.tooltip.narrative.NarrativeLayout;
import io.github.naimjeg.damagenexus.client.tooltip.narrative.RuleNarrative;
import io.github.naimjeg.damagenexus.client.tooltip.narrative.RuleNarrativePlanner;
import io.github.naimjeg.damagenexus.config.TooltipDebugLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Renders an already planned semantic document. It never inspects rule objects. */
public final class DamageTooltipRenderer {
    private final RuleNarrativePlanner narratives;
    private final RulePhraseRenderer phrases;

    public DamageTooltipRenderer(
            RuleNarrativePlanner narratives,
            RulePhraseRenderer phrases
    ) {
        this.narratives = Objects.requireNonNull(narratives, "narratives");
        this.phrases = Objects.requireNonNull(phrases, "phrases");
    }

    public void render(
            List<Component> tooltip,
            DamageTooltipDocument document,
            TooltipPresentationPolicy policy
    ) {
        Objects.requireNonNull(tooltip, "tooltip");
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(policy, "policy");

        for (EntryTooltipView entry : document.standaloneEntries()) {
            renderEntry(tooltip, entry, policy, 0, false);
        }
        for (AffixTooltipView affix : document.affixes()) {
            renderAffix(tooltip, affix, policy);
        }
        renderVanilla(tooltip, document.vanillaAugmentations(), policy);
        if (document.debugSection().isPresent()
                && policy.debugLevel() != TooltipDebugLevel.OFF) {
            renderDebug(tooltip, document, policy.debugLevel());
        }
    }

    private void renderAffix(
            List<Component> tooltip,
            AffixTooltipView affix,
            TooltipPresentationPolicy policy
    ) {
        affix.name().map(DisplayTextResolver::resolve).ifPresent(name ->
                tooltip.add(name.copy().withStyle(styleForRarity(affix.rarity().name())))
        );
        renderAuthored(tooltip, affix.authoredLines(), affix.flavorText(), 1);

        boolean breakdown = policy.detailLevel() == TooltipDetailLevel.EXPANDED
                ? affix.breakdownPolicy().visibleInDetail()
                : affix.breakdownPolicy().visibleInCompact();
        List<EntryTooltipView> visibleEntries = affix.entries().stream()
                .filter(entry -> breakdown || hasVisibleEntryContent(entry))
                .toList();
        if (!visibleEntries.isEmpty()) {
            tooltip.add(indent(
                    Component.translatable("tooltip.damagenexus.entries"),
                    1,
                    ChatFormatting.DARK_AQUA
            ));
            for (EntryTooltipView entry : visibleEntries) {
                renderEntry(tooltip, entry, policy, 2, breakdown);
            }
        }
        if (policy.detailLevel() == TooltipDetailLevel.COMPACT
                && affix.breakdownPolicy().visibleInDetail()) {
            tooltip.add(indent(
                    Component.translatable("tooltip.damagenexus.hold_shift"),
                    1,
                    ChatFormatting.DARK_GRAY
            ));
        }
    }

    private void renderEntry(
            List<Component> tooltip,
            EntryTooltipView entry,
            TooltipPresentationPolicy policy,
            int depth,
            boolean forceBreakdown
    ) {
        entry.name().map(DisplayTextResolver::resolve).ifPresent(name ->
                tooltip.add(indent(name, depth, ChatFormatting.GRAY))
        );
        renderAuthored(tooltip, entry.authoredLines(), entry.flavorText(), depth + 1);

        boolean breakdown = forceBreakdown || (policy.detailLevel() == TooltipDetailLevel.EXPANDED
                ? entry.breakdownPolicy().visibleInDetail()
                : entry.breakdownPolicy().visibleInCompact());
        if (breakdown && !entry.rules().isEmpty()) {
            if (policy.detailLevel() == TooltipDetailLevel.EXPANDED) {
                tooltip.add(indent(
                        Component.translatable("tooltip.damagenexus.rules"),
                        depth + 1,
                        ChatFormatting.DARK_AQUA
                ));
            }
            for (RuleTooltipView rule : entry.rules()) {
                renderNarrative(tooltip, rule.narrative(), policy.detailLevel(), depth + 1);
            }
        }
        if (policy.detailLevel() == TooltipDetailLevel.COMPACT
                && entry.breakdownPolicy().visibleInDetail()) {
            tooltip.add(indent(
                    Component.translatable("tooltip.damagenexus.hold_shift"),
                    depth + 1,
                    ChatFormatting.DARK_GRAY
            ));
        }
    }

    private void renderNarrative(
            List<Component> tooltip,
            RuleNarrative narrative,
            TooltipDetailLevel detailLevel,
            int depth
    ) {
        NarrativeLayout layout = narratives.layout(narrative, detailLevel);
        PhraseForm form = detailLevel == TooltipDetailLevel.COMPACT
                ? PhraseForm.COMPACT : PhraseForm.DETAIL;
        if (layout == NarrativeLayout.SINGLE_SENTENCE) {
            Optional<Component> condition = renderSingleCondition(narrative.condition(), form);
            Optional<Component> effect = narrative.effects().isEmpty()
                    ? Optional.empty()
                    : phrases.render(narrative.effects().getFirst(), form);
            if (effect.isPresent()) {
                Component sentence = condition.isPresent()
                        ? Component.translatable(
                                "rule_sentence.damagenexus.conditional.single",
                                condition.get(),
                                effect.get()
                        )
                        : effect.get();
                tooltip.add(indent(sentence, depth, ChatFormatting.GRAY));
            }
            return;
        }

        boolean showConditions = !(narrative.condition() instanceof ConditionExpression.Always);
        if (showConditions) {
            tooltip.add(indent(
                    Component.translatable("tooltip.damagenexus.conditions"),
                    depth,
                    ChatFormatting.DARK_AQUA
            ));
            renderConditionTree(tooltip, narrative.condition(), form, depth + 1);
        }
        if (!narrative.effects().isEmpty()) {
            tooltip.add(indent(
                    Component.translatable("tooltip.damagenexus.effects"),
                    depth,
                    ChatFormatting.DARK_AQUA
            ));
            for (RulePhrase effect : narrative.effects()) {
                phrases.render(effect, form).ifPresent(line ->
                        tooltip.add(indent(line, depth + 1, ChatFormatting.GRAY))
                );
            }
        }
    }

    private void renderConditionTree(
            List<Component> tooltip,
            ConditionExpression expression,
            PhraseForm form,
            int depth
    ) {
        switch (expression) {
            case ConditionExpression.Always ignored -> tooltip.add(indent(
                    Component.translatable("rule_phrase.damagenexus.always.default." + form.serializedName()),
                    depth,
                    ChatFormatting.GRAY
            ));
            case ConditionExpression.Phrase phrase -> phrases.render(phrase.phrase(), form)
                    .ifPresent(line -> tooltip.add(indent(line, depth, ChatFormatting.GRAY)));
            case ConditionExpression.AllOf all -> {
                if (all.implicit() && all.children().size() == 1) {
                    renderConditionTree(tooltip, all.children().getFirst(), form, depth);
                    break;
                }
                tooltip.add(indent(
                        Component.translatable("rule_condition_group.damagenexus.all_of"),
                        depth,
                        ChatFormatting.GRAY
                ));
                for (ConditionExpression child : all.children()) {
                    renderConditionTree(tooltip, child, form, depth + 1);
                }
            }
            case ConditionExpression.AnyOf any -> {
                tooltip.add(indent(
                        Component.translatable("rule_condition_group.damagenexus.any_of"),
                        depth,
                        ChatFormatting.GRAY
                ));
                for (ConditionExpression child : any.children()) {
                    renderConditionTree(tooltip, child, form, depth + 1);
                }
            }
            case ConditionExpression.Not not -> {
                tooltip.add(indent(
                        Component.translatable("rule_condition_group.damagenexus.not"),
                        depth,
                        ChatFormatting.GRAY
                ));
                renderConditionTree(tooltip, not.child(), form, depth + 1);
            }
        }
    }

    private Optional<Component> renderSingleCondition(
            ConditionExpression expression,
            PhraseForm form
    ) {
        return switch (expression) {
            case ConditionExpression.Phrase phrase -> phrases.render(phrase.phrase(), form);
            case ConditionExpression.AllOf all when all.children().size() == 1 ->
                    renderSingleCondition(all.children().getFirst(), form);
            default -> Optional.empty();
        };
    }

    private void renderVanilla(
            List<Component> tooltip,
            List<VanillaTooltipAugmentation> augmentations,
            TooltipPresentationPolicy policy
    ) {
        List<Component> fallback = new ArrayList<>();
        for (VanillaTooltipAugmentation augmentation : augmentations) {
            List<Component> lines = new ArrayList<>();
            renderNarrative(lines, augmentation.narrative(), policy.detailLevel(), 1);
            if (policy.detailLevel() == TooltipDetailLevel.EXPANDED) {
                for (VanillaTooltipNote note : augmentation.detailNotes()) {
                    Object[] arguments = note.arguments().stream()
                            .map(value -> phrases.renderValue(value, PhraseForm.DETAIL))
                            .toArray(Object[]::new);
                    lines.add(indent(
                            Component.translatable(note.translationKey(), arguments),
                            1,
                            ChatFormatting.DARK_GRAY
                    ));
                }
            }
            if (lines.isEmpty()) {
                continue;
            }
            int anchor = findAnchor(tooltip, augmentation.originalLineAnchor());
            if (anchor >= 0) {
                tooltip.addAll(anchor + 1, lines);
            } else {
                fallback.addAll(lines);
            }
        }
        if (!fallback.isEmpty()) {
            tooltip.add(Component.translatable(
                    "tooltip.damagenexus.vanilla_augmentation"
            ).withStyle(ChatFormatting.DARK_AQUA));
            tooltip.addAll(fallback);
        }
    }

    private void renderDebug(
            List<Component> tooltip,
            DamageTooltipDocument document,
            TooltipDebugLevel level
    ) {
        tooltip.add(Component.translatable("tooltip.damagenexus.debug.header")
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable(
                "tooltip.damagenexus.debug.summary",
                document.affixes().size(),
                document.standaloneEntries().size(),
                document.vanillaAugmentations().size(),
                document.templateReferences().size()
        ).withStyle(ChatFormatting.DARK_AQUA));
        if (level == TooltipDebugLevel.SUMMARY) {
            return;
        }
        for (AffixTooltipView affix : document.affixes()) {
            tooltip.add(debug("tooltip.damagenexus.debug.affix", 1, affix.id()));
            if (level == TooltipDebugLevel.FULL) {
                tooltip.add(debug("tooltip.damagenexus.debug.affix_metadata", 2,
                        affix.slot(), affix.rarity(), affix.stacking(),
                        affix.stackingGroup().map(Object::toString).orElse("-")));
            }
            for (EntryTooltipView entry : affix.entries()) {
                renderEntryDebug(tooltip, entry, level, 2);
            }
        }
        for (EntryTooltipView entry : document.standaloneEntries()) {
            renderEntryDebug(tooltip, entry, level, 1);
        }
        for (VanillaTooltipAugmentation vanilla : document.vanillaAugmentations()) {
            tooltip.add(debug("tooltip.damagenexus.debug.vanilla", 1, vanilla.enchantmentId()));
            if (level == TooltipDebugLevel.FULL) {
                for (var bucket : vanilla.applicationBuckets()) {
                    tooltip.add(debug("tooltip.damagenexus.debug.application_bucket", 2, bucket));
                }
                for (var type : vanilla.conditionTypeIds()) {
                    tooltip.add(debug("tooltip.damagenexus.debug.condition_type", 2, type));
                }
                for (var type : vanilla.operationTypeIds()) {
                    tooltip.add(debug("tooltip.damagenexus.debug.operation_type", 2, type));
                }
            }
        }
        for (TemplateReferenceTooltipView template : document.templateReferences()) {
            tooltip.add(debug("tooltip.damagenexus.debug.template", 1,
                    Component.translatable(template.kind()
                            == TemplateReferenceTooltipView.Kind.ENTRY
                            ? "source_kind.damagenexus.entry"
                            : "source_kind.damagenexus.affix"),
                    template.id()));
            if (level == TooltipDebugLevel.FULL) {
                tooltip.add(debug(
                        "tooltip.damagenexus.debug.template_resolution",
                        2
                ));
            }
        }
    }

    private void renderEntryDebug(
            List<Component> tooltip,
            EntryTooltipView entry,
            TooltipDebugLevel level,
            int depth
    ) {
        tooltip.add(debug("tooltip.damagenexus.debug.entry", depth, entry.id()));
        if (level == TooltipDebugLevel.FULL) {
            tooltip.add(debug("tooltip.damagenexus.debug.entry_metadata", depth + 1,
                    entry.slot(), entry.stacking(),
                    entry.stackingGroup().map(Object::toString).orElse("-")));
        }
        for (RuleTooltipView rule : entry.rules()) {
            tooltip.add(debug("tooltip.damagenexus.debug.rule", depth + 1, rule.id()));
            if (level == TooltipDebugLevel.FULL) {
                tooltip.add(debug("tooltip.damagenexus.debug.rule_metadata", depth + 2,
                        rule.phase(), rule.role(), rule.priority(), rule.stacking(),
                        rule.stackingGroup().map(Object::toString).orElse("-")));
                for (var type : rule.conditionTypeIds()) {
                    tooltip.add(debug("tooltip.damagenexus.debug.condition_type", depth + 2, type));
                }
                for (var type : rule.operationTypeIds()) {
                    tooltip.add(debug("tooltip.damagenexus.debug.operation_type", depth + 2, type));
                }
                rule.traceLabel().ifPresent(trace -> tooltip.add(
                        debug("tooltip.damagenexus.debug.trace_label", depth + 2, trace)
                ));
            }
        }
    }

    private static void renderAuthored(
            List<Component> tooltip,
            List<io.github.naimjeg.damagenexus.api.display.DisplayText> lines,
            Optional<io.github.naimjeg.damagenexus.api.display.DisplayText> flavor,
            int depth
    ) {
        for (var line : lines) {
            tooltip.add(indent(DisplayTextResolver.resolve(line), depth, ChatFormatting.GRAY));
        }
        flavor.map(DisplayTextResolver::resolve).ifPresent(line ->
                tooltip.add(indent(line, depth, ChatFormatting.DARK_GRAY)
                        .copy().withStyle(ChatFormatting.ITALIC))
        );
    }

    private static boolean hasVisibleEntryContent(EntryTooltipView entry) {
        return entry.name().isPresent()
                || !entry.authoredLines().isEmpty()
                || entry.flavorText().isPresent()
                || entry.breakdownPolicy() != RuleBreakdownPolicy.NONE;
    }

    private static int findAnchor(List<Component> tooltip, Component anchor) {
        for (int index = 0; index < tooltip.size(); index++) {
            Component candidate = tooltip.get(index);
            if (candidate.equals(anchor)
                    || candidate.getString().equals(anchor.getString())) {
                return index;
            }
        }
        return -1;
    }

    private static Component debug(String key, int depth, Object... arguments) {
        return indent(Component.translatable(key, arguments), depth, ChatFormatting.DARK_AQUA);
    }

    private static Component indent(Component component, int depth, ChatFormatting color) {
        return Component.literal("  ".repeat(Math.max(0, depth)))
                .append(component)
                .withStyle(color);
    }

    private static ChatFormatting styleForRarity(String rarity) {
        return switch (rarity) {
            case "UNCOMMON" -> ChatFormatting.GREEN;
            case "RARE" -> ChatFormatting.BLUE;
            case "EPIC" -> ChatFormatting.DARK_PURPLE;
            case "LEGENDARY" -> ChatFormatting.GOLD;
            case "UNIQUE" -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.GRAY;
        };
    }
}
