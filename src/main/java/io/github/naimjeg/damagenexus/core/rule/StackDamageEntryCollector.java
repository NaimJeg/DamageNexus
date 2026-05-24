package io.github.naimjeg.damagenexus.core.rule;

import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.RuleExecutionContext;
import io.github.naimjeg.damagenexus.api.rule.RuntimeDamageRule;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSelectionResolver;
import io.github.naimjeg.damagenexus.api.rule.affix.RuntimeDamageAffix;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySelectionResolver;
import io.github.naimjeg.damagenexus.api.rule.entry.RuntimeDamageEntry;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import io.github.naimjeg.damagenexus.core.security.DamageNexusItemSecurity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class StackDamageEntryCollector {

    private StackDamageEntryCollector() {
    }

    public static void collectStackEntries(
            DamageNexusContext ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out,
            ItemStack stack,
            RuleExecutionContext exec,
            String source
    ) {
        collectStackEntries(
                ctx, phase, out, stack, exec, source, true, true
        );
    }

    public static void collectStackEntries(
            DamageNexusContext ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out,
            ItemStack stack,
            RuleExecutionContext exec,
            String source,
            boolean readEntries,
            boolean readAffixes
    ) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        DamageNexusItemSecurity.ValidatedItemRules validated =
                DamageNexusItemSecurity.validateForExecution(
                        stack,
                        source,
                        ctx.templateSnapshot()
                );

        if (validated.isEmpty()) {
            return;
        }

        if (readEntries) {
            collectEntryRules(
                    ctx,
                    phase,
                    out,
                    validated.entries(),
                    exec,
                    source
            );
        }

        if (readAffixes) {
            collectAffixRules(
                    ctx,
                    phase,
                    out,
                    validated.affixes(),
                    exec,
                    source
            );
        }
    }

    private static void collectEntryRules(
            DamageNexusContext ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out,
            List<DamageEntryDefinition> entries,
            RuleExecutionContext exec,
            String source
    ) {
        if (entries.isEmpty()) {
            return;
        }

        List<DamageEntryDefinition> selectedEntries =
                resolveApplicableEntries(entries, exec);

        for (DamageEntryDefinition entry : selectedEntries) {
            RuntimeDamageEntry runtimeEntry =
                    new RuntimeDamageEntry(entry, exec);

            for (RuntimeDamageRule runtimeRule : runtimeEntry.expandRules()) {
                DamageRuleDefinition rule = runtimeRule.definition();

                if (rule.phase() != phase) {
                    continue;
                }

                if (!rule.role().canRunAs(exec.role())) {
                    continue;
                }

                ctx.trace().rules().collected(
                        phase,
                        rule,
                        exec
                );

                out.add(runtimeRule);
            }
        }
    }

    private static void collectAffixRules(
            DamageNexusContext ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out,
            List<DamageAffixDefinition> affixes,
            RuleExecutionContext exec,
            String source
    ) {
        if (affixes.isEmpty()) {
            return;
        }

        List<DamageAffixDefinition> selectedAffixes =
                resolveApplicableAffixes(affixes, exec);

        for (DamageAffixDefinition affix : selectedAffixes) {
            RuntimeDamageAffix runtimeAffix =
                    new RuntimeDamageAffix(affix, exec);

            for (RuntimeDamageRule runtimeRule : runtimeAffix.expandRules()) {
                DamageRuleDefinition rule = runtimeRule.definition();

                if (rule.phase() != phase) {
                    continue;
                }

                if (!rule.role().canRunAs(exec.role())) {
                    continue;
                }

                ctx.trace().rules().collected(
                        phase,
                        rule,
                        exec
                );

                out.add(runtimeRule);
            }
        }
    }

    static List<DamageEntryDefinition> resolveApplicableEntries(
            List<DamageEntryDefinition> entries,
            RuleExecutionContext executionContext
    ) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        return DamageEntrySelectionResolver.resolve(
                entries.stream()
                        .filter(entry -> entry != null
                                && DamageRuleSlotMatcher.matches(
                                entry.slot(),
                                executionContext
                        ))
                        .toList()
        );
    }

    static List<DamageAffixDefinition> resolveApplicableAffixes(
            List<DamageAffixDefinition> affixes,
            RuleExecutionContext executionContext
    ) {
        if (affixes == null || affixes.isEmpty()) {
            return List.of();
        }

        return DamageAffixSelectionResolver.resolve(
                affixes.stream()
                        .filter(affix -> affix != null
                                && DamageRuleSlotMatcher.matches(
                                affix.slot(),
                                executionContext
                        ))
                        .filter(affix -> affix.entries().stream()
                                .anyMatch(entry -> entry != null
                                        && DamageRuleSlotMatcher.matches(
                                        entry.slot(),
                                        executionContext
                                )))
                        .toList()
        );
    }
}
