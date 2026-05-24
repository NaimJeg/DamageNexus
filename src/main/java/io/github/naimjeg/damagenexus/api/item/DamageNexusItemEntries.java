package io.github.naimjeg.damagenexus.api.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSelectionResolver;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySelectionResolver;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record DamageNexusItemEntries(
        List<DamageEntryDefinition> entries,
        List<DamageAffixDefinition> affixes
) {
    public static final DamageNexusItemEntries EMPTY =
            new DamageNexusItemEntries(List.of(), List.of());

    public static final Codec<List<DamageEntryDefinition>>
            ENTRY_STORAGE_CODEC =
            createEntryComponentCodec(
                    DamageEntryDefinition.STORAGE_CODEC
            );

    public static final Codec<List<DamageAffixDefinition>>
            AFFIX_STORAGE_CODEC =
            createAffixComponentCodec(
                    DamageAffixDefinition.STORAGE_CODEC
            );

    public static final StreamCodec<ByteBuf, List<DamageEntryDefinition>>
            ENTRY_NETWORK_CODEC =
            ByteBufCodecs.fromCodec(ENTRY_STORAGE_CODEC);

    public static final StreamCodec<ByteBuf, List<DamageAffixDefinition>>
            AFFIX_NETWORK_CODEC =
            ByteBufCodecs.fromCodec(AFFIX_STORAGE_CODEC);

    /**
     * Strict codec for authoring and external imports.
     */
    public static final Codec<DamageNexusItemEntries> CODEC =
            createCodec(
                    ENTRY_STORAGE_CODEC,
                    AFFIX_STORAGE_CODEC
            );

    /** Persistent ItemStack bundle codec. */
    public static final Codec<DamageNexusItemEntries> STORAGE_CODEC = CODEC;

    private static Codec<DamageNexusItemEntries> createCodec(
            Codec<List<DamageEntryDefinition>> entryCodec,
            Codec<List<DamageAffixDefinition>> affixCodec
    ) {
        Codec<DamageNexusItemEntries> structural =
                RecordCodecBuilder.create(instance -> instance.group(
                    entryCodec
                            .optionalFieldOf("entries", List.of())
                            .forGetter(DamageNexusItemEntries::entries),

                    affixCodec
                            .optionalFieldOf("affixes", List.of())
                            .forGetter(DamageNexusItemEntries::affixes)
            ).apply(instance, DamageNexusItemEntries::new));

        return structural.flatXmap(
                DamageNexusItemEntries::validateBundleCodec,
                DamageNexusItemEntries::validateBundleCodec
        );
    }

    private static Codec<List<DamageEntryDefinition>>
    createEntryComponentCodec(
            Codec<DamageEntryDefinition> entryCodec
    ) {
        Codec<List<DamageEntryDefinition>> typed =
                DamageRuleLimits.boundedList(
                                entryCodec,
                                DamageRuleLimits.MAX_ITEM_ENTRIES,
                                "item damage entries"
                        )
                        .flatXmap(
                                DamageRuleLimits
                                        ::validateEntryComponentCodec,
                                DamageRuleLimits
                                        ::validateEntryComponentCodec
                        );

        return DamageRuleLimits.guardRawStructure(
                typed,
                "DamageNexus entry component",
                DamageRuleLimits.MAX_RAW_CODEC_DEPTH,
                DamageRuleLimits.MAX_COMPONENT_RAW_NODES
        );
    }

    private static Codec<List<DamageAffixDefinition>>
    createAffixComponentCodec(
            Codec<DamageAffixDefinition> affixCodec
    ) {
        Codec<List<DamageAffixDefinition>> typed =
                DamageRuleLimits.boundedList(
                                affixCodec,
                                DamageRuleLimits.MAX_ITEM_AFFIXES,
                                "item damage affixes"
                        )
                        .flatXmap(
                                DamageRuleLimits
                                        ::validateAffixComponentCodec,
                                DamageRuleLimits
                                        ::validateAffixComponentCodec
                        );

        return DamageRuleLimits.guardRawStructure(
                typed,
                "DamageNexus affix component",
                DamageRuleLimits.MAX_RAW_CODEC_DEPTH,
                DamageRuleLimits.MAX_COMPONENT_RAW_NODES
        );
    }

    private static com.mojang.serialization.DataResult
    <DamageNexusItemEntries> validateBundleCodec(
            DamageNexusItemEntries value
    ) {
        java.util.Optional<String> problem =
                DamageRuleLimits.findItemProblem(
                        value.entries(),
                        value.affixes()
                );

        return problem
                .<com.mojang.serialization.DataResult
                        <DamageNexusItemEntries>>map(reason ->
                        com.mojang.serialization.DataResult.error(() ->
                                "Invalid DamageNexus item bundle: "
                                        + reason
                        ))
                .orElseGet(() ->
                        com.mojang.serialization.DataResult.success(value)
                );
    }

    public DamageNexusItemEntries {
        entries = copyNonNull(entries, "entries");
        affixes = copyNonNull(affixes, "affixes");
    }

    public boolean isEmpty() {
        return entries.isEmpty() && affixes.isEmpty();
    }

    public List<DamageEntryDefinition> resolvedEntries() {
        return DamageEntrySelectionResolver.resolve(entries);
    }

    public List<DamageAffixDefinition> resolvedAffixes() {
        return DamageAffixSelectionResolver.resolve(affixes);
    }

    public DamageNexusItemEntries withEntries(
            List<DamageEntryDefinition> entries
    ) {
        return new DamageNexusItemEntries(entries, this.affixes);
    }

    public DamageNexusItemEntries withAffixes(
            List<DamageAffixDefinition> affixes
    ) {
        return new DamageNexusItemEntries(this.entries, affixes);
    }

    public DamageNexusItemEntries withAddedEntry(
            DamageEntryDefinition entry
    ) {
        Objects.requireNonNull(entry, "entry must not be null");

        List<DamageEntryDefinition> next =
                new ArrayList<>(this.entries.size() + 1);

        next.addAll(this.entries);
        next.add(entry);

        return new DamageNexusItemEntries(next, this.affixes);
    }

    public DamageNexusItemEntries withAddedAffix(
            DamageAffixDefinition affix
    ) {
        Objects.requireNonNull(affix, "affix must not be null");

        List<DamageAffixDefinition> next =
                new ArrayList<>(this.affixes.size() + 1);

        next.addAll(this.affixes);
        next.add(affix);

        return new DamageNexusItemEntries(this.entries, next);
    }

    private static <T> List<T> copyNonNull(
            List<T> input,
            String name
    ) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }

        List<T> result = new ArrayList<>(input.size());

        for (T value : input) {
            result.add(Objects.requireNonNull(
                    value,
                    name + " must not contain null elements"
            ));
        }

        return List.copyOf(result);
    }
}
