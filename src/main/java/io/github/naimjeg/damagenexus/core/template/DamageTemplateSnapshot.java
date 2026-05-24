package io.github.naimjeg.damagenexus.core.template;

import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Optional;

/** Immutable transaction-pinnable view of one template registry revision. */
public record DamageTemplateSnapshot(
        long revision,
        long validatedChannelRevision,
        boolean serverAuthoritative,
        Map<Identifier, DamageEntryDefinition> entries,
        Map<Identifier, DamageAffixDefinition> affixes
) {
    public DamageTemplateSnapshot {
        entries = entries == null ? Map.of() : Map.copyOf(entries);
        affixes = affixes == null ? Map.of() : Map.copyOf(affixes);
    }

    public Optional<DamageEntryDefinition> entry(Identifier id) {
        return Optional.ofNullable(entries.get(id));
    }

    public Optional<DamageAffixDefinition> affix(Identifier id) {
        return Optional.ofNullable(affixes.get(id));
    }

    /** True only for execution snapshots validated against this channel revision. */
    public boolean isCompatibleWith(long channelRevision) {
        return serverAuthoritative
                && validatedChannelRevision == channelRevision;
    }

    DamageTemplateSnapshot failClosed() {
        if (!serverAuthoritative) {
            return this;
        }
        return new DamageTemplateSnapshot(
                revision,
                validatedChannelRevision,
                false,
                entries,
                affixes
        );
    }
}
