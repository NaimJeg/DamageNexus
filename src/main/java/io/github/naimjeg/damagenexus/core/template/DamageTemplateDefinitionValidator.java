package io.github.naimjeg.damagenexus.core.template;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleReferenceValidator;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleValidator;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixValidator;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryValidator;

import java.util.List;

/** Strict validation shared by Java registration and datapack publication. */
final class DamageTemplateDefinitionValidator {
    private DamageTemplateDefinitionValidator() {}

    static void requireEntry(
            DamageEntryDefinition definition,
            String source,
            boolean datapackReferences
    ) {
        if (definition == null) {
            throw new IllegalArgumentException("Entry template is null");
        }
        DamageRuleLimits.findItemProblem(List.of(definition), List.of())
                .ifPresent(reason -> {
                    throw new IllegalArgumentException(
                            "Invalid entry template: " + reason);
                });
        List<DamageEntryDefinition> validated =
                DamageEntryValidator.filterValid(List.of(definition), source);
        if (validated.size() != 1 || !validated.getFirst().equals(definition)) {
            throw new IllegalArgumentException(
                    "Entry template did not pass strict validation: "
                            + definition.id());
        }
        for (var rule : definition.rules()) {
            boolean valid = datapackReferences
                    ? DamageRuleReferenceValidator.validateDatapackReferences(
                    rule, source, DamageRuleValidator.Policy.WARN)
                    : DamageRuleReferenceValidator
                    .validateJavaRegistrationReferences(
                            rule, source, DamageRuleValidator.Policy.WARN);
            if (!valid) {
                throw new IllegalArgumentException(
                        "Entry template has invalid rule references: "
                                + definition.id());
            }
        }
    }

    static void requireAffix(
            DamageAffixDefinition definition,
            String source,
            boolean datapackReferences
    ) {
        if (definition == null) {
            throw new IllegalArgumentException("Affix template is null");
        }
        DamageRuleLimits.findItemProblem(List.of(), List.of(definition))
                .ifPresent(reason -> {
                    throw new IllegalArgumentException(
                            "Invalid affix template: " + reason);
                });
        List<DamageAffixDefinition> validated =
                DamageAffixValidator.filterValid(List.of(definition), source);
        if (validated.size() != 1 || !validated.getFirst().equals(definition)) {
            throw new IllegalArgumentException(
                    "Affix template did not pass strict validation: "
                            + definition.id());
        }
        for (var entry : definition.entries()) {
            for (var rule : entry.rules()) {
                boolean valid = datapackReferences
                        ? DamageRuleReferenceValidator
                        .validateDatapackReferences(
                                rule, source, DamageRuleValidator.Policy.WARN)
                        : DamageRuleReferenceValidator
                        .validateJavaRegistrationReferences(
                                rule, source, DamageRuleValidator.Policy.WARN);
                if (!valid) {
                    throw new IllegalArgumentException(
                            "Affix template has invalid rule references: "
                                    + definition.id());
                }
            }
        }
    }
}
