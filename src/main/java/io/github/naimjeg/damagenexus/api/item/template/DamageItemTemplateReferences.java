package io.github.naimjeg.damagenexus.api.item.template;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Ordered ItemStack references to server-authoritative static templates.
 * References contain no executable definition or client authority.
 */
public record DamageItemTemplateReferences(
        List<DamageEntryTemplateReference> entries,
        List<DamageAffixTemplateReference> affixes
) {
    public static final int MAX_ENTRY_REFERENCES =
            DamageRuleLimits.MAX_ITEM_ENTRIES;
    public static final int MAX_AFFIX_REFERENCES =
            DamageRuleLimits.MAX_ITEM_AFFIXES;
    public static final DamageItemTemplateReferences EMPTY =
            new DamageItemTemplateReferences(List.of(), List.of());

    public static final Codec<DamageItemTemplateReferences> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    DamageRuleLimits.boundedList(
                                    DamageEntryTemplateReference.CODEC,
                                    MAX_ENTRY_REFERENCES,
                                    "entry template references"
                            )
                            .optionalFieldOf("entries", List.of())
                            .forGetter(DamageItemTemplateReferences::entries),
                    DamageRuleLimits.boundedList(
                                    DamageAffixTemplateReference.CODEC,
                                    MAX_AFFIX_REFERENCES,
                                    "affix template references"
                            )
                            .optionalFieldOf("affixes", List.of())
                            .forGetter(DamageItemTemplateReferences::affixes)
            ).apply(instance, DamageItemTemplateReferences::new));

    public static final StreamCodec<ByteBuf, DamageItemTemplateReferences>
            NETWORK_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public DamageItemTemplateReferences {
        entries = copy(entries, "entry template references");
        affixes = copy(affixes, "affix template references");
        if (entries.size() > MAX_ENTRY_REFERENCES) {
            throw new IllegalArgumentException(
                    "Too many entry template references: " + entries.size());
        }
        if (affixes.size() > MAX_AFFIX_REFERENCES) {
            throw new IllegalArgumentException(
                    "Too many affix template references: " + affixes.size());
        }
    }

    public boolean isEmpty() {
        return entries.isEmpty() && affixes.isEmpty();
    }

    public DamageItemTemplateReferences withAddedEntry(
            DamageEntryTemplateReference reference
    ) {
        Objects.requireNonNull(reference, "reference");
        ArrayList<DamageEntryTemplateReference> next =
                new ArrayList<>(entries);
        next.add(reference);
        return new DamageItemTemplateReferences(next, affixes);
    }

    public DamageItemTemplateReferences withAddedAffix(
            DamageAffixTemplateReference reference
    ) {
        Objects.requireNonNull(reference, "reference");
        ArrayList<DamageAffixTemplateReference> next =
                new ArrayList<>(affixes);
        next.add(reference);
        return new DamageItemTemplateReferences(entries, next);
    }

    private static <T> List<T> copy(List<T> input, String name) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        ArrayList<T> result = new ArrayList<>(input.size());
        for (T value : input) {
            result.add(Objects.requireNonNull(
                    value, name + " must not contain null"));
        }
        return List.copyOf(result);
    }
}
