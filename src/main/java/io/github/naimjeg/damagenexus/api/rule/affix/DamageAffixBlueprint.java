package io.github.naimjeg.damagenexus.api.rule.affix;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Experimental authoring-only model for a future generated-affix pipeline.
 *
 * <p>This intentionally does not replace {@link DamageAffixDefinition} yet.
 * The current component stores materialized affixes; this blueprint is the
 * authoring/generation-side shape that may later produce a rolled
 * {@link DamageAffixInstance}. It is not a Phase 10 static template: roll
 * keys have no declared operation-parameter binding, and this type has no
 * stable codec, version, or migration contract.</p>
 */
public record DamageAffixBlueprint(
        Identifier id,
        DamageAffixDisplay display,
        DamageAffixSlot slot,
        DamageAffixGenerationSpec generation,
        DamageAffixStacking stacking,
        Optional<Identifier> stackingGroup,
        List<DamageAffixTier> tiers,
        List<DamageRuleDefinition> staticRules,
        List<Identifier> tags
) {
    public DamageAffixBlueprint {
        if (id == null) {
            throw new IllegalArgumentException("Damage affix blueprint id cannot be null");
        }

        if (display == null) {
            throw new IllegalArgumentException("Damage affix blueprint display cannot be null");
        }

        if (slot == null) {
            throw new IllegalArgumentException("Damage affix blueprint slot cannot be null");
        }

        if (generation == null) {
            generation = DamageAffixGenerationSpec.DEFAULT;
        }

        if (stacking == null) {
            stacking = DamageAffixStacking.UNIQUE_AFFIX;
        }

        stackingGroup = stackingGroup == null ? Optional.empty() : stackingGroup;
        tiers = tiers == null ? List.of() : List.copyOf(tiers);
        staticRules = staticRules == null ? List.of() : List.copyOf(staticRules);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}

