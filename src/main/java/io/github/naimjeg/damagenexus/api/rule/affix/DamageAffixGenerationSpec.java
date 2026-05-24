package io.github.naimjeg.damagenexus.api.rule.affix;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Experimental authoring-only generation metadata for an affix blueprint.
 *
 * <p>Keep generation concerns out of DamageAffixDefinition so the runtime affix
 * component can remain a materialized, executable form. Phase 10 does not
 * consume this model or define item-level, pool, rarity, or roll semantics.</p>
 */
public record DamageAffixGenerationSpec(
        AffixFamily family,
        AffixPlacement placement,
        int minItemLevel,
        int maxItemLevel,
        int weight,
        Optional<Identifier> pool,
        List<Identifier> requiredItemTags,
        List<Identifier> blockedItemTags
) {
    public static final DamageAffixGenerationSpec DEFAULT =
            new DamageAffixGenerationSpec(
                    AffixFamily.GENERIC,
                    AffixPlacement.IMPLICIT,
                    0,
                    Integer.MAX_VALUE,
                    1,
                    Optional.empty(),
                    List.of(),
                    List.of()
            );

    public DamageAffixGenerationSpec {
        if (family == null) {
            family = AffixFamily.GENERIC;
        }

        if (placement == null) {
            placement = AffixPlacement.IMPLICIT;
        }

        minItemLevel = Math.max(0, minItemLevel);
        maxItemLevel = Math.max(minItemLevel, maxItemLevel);
        weight = Math.max(0, weight);
        pool = pool == null ? Optional.empty() : pool;
        requiredItemTags = requiredItemTags == null
                ? List.of()
                : List.copyOf(requiredItemTags);
        blockedItemTags = blockedItemTags == null
                ? List.of()
                : List.copyOf(blockedItemTags);
    }

    public enum AffixFamily {
        GENERIC,
        OFFENSIVE,
        DEFENSIVE,
        UTILITY,
        UNIQUE
    }

    public enum AffixPlacement {
        IMPLICIT,
        PREFIX,
        SUFFIX,
        UNIQUE,
        ENCHANTMENT_BRIDGE
    }
}

