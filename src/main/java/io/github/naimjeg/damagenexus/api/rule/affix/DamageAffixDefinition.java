package io.github.naimjeg.damagenexus.api.rule.affix;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DamageAffixDefinition(
        Identifier id,
        DamageAffixDisplay display,
        DamageAffixSlot slot,
        DamageAffixRarity rarity,
        List<DamageEntryDefinition> entries,
        DamageAffixStacking stacking,
        Optional<Identifier> stackingGroup
) {
    public static final Codec<DamageAffixDefinition> CODEC =
            createCodecFromEntryList(withoutPartialDecode(
                    DamageRuleLimits.boundedList(
                            DamageEntryDefinition.CODEC,
                            DamageRuleLimits.MAX_AFFIX_ENTRIES,
                            "affix entries"
                    )
            ));

    /** Codec used for ItemStack persistence and network synchronization. */
    public static final Codec<DamageAffixDefinition> STORAGE_CODEC = CODEC;

    private static Codec<DamageAffixDefinition> createCodecFromEntryList(
            Codec<List<DamageEntryDefinition>> entriesCodec
    ) {
        return RecordCodecBuilder.<DamageAffixDefinition>create(
                instance -> instance.group(
                    Identifier.CODEC
                            .fieldOf("id")
                            .forGetter(DamageAffixDefinition::id),

                    DamageAffixDisplay.CODEC
                            .fieldOf("display")
                            .forGetter(DamageAffixDefinition::display),

                    DamageAffixSlot.CODEC
                            .optionalFieldOf("slot", DamageAffixSlot.ITEM)
                            .forGetter(DamageAffixDefinition::slot),

                    DamageAffixRarity.CODEC
                            .optionalFieldOf("rarity", DamageAffixRarity.COMMON)
                            .forGetter(DamageAffixDefinition::rarity),

                    entriesCodec
                            .fieldOf("entries")
                            .forGetter(DamageAffixDefinition::entries),

                    DamageAffixStacking.CODEC
                            .optionalFieldOf("stacking", DamageAffixStacking.STACK)
                            .forGetter(DamageAffixDefinition::stacking),

                    Identifier.CODEC
                            .optionalFieldOf("stacking_group")
                            .forGetter(DamageAffixDefinition::stackingGroup)
                ).apply(instance, DamageAffixDefinition::new)
        );
    }

    /**
     * RecordCodecBuilder applies constructors to partial list results. An
     * invalid nested entry would therefore look like an empty affix and throw
     * from the record constructor before the original DataResult diagnostic
     * could be returned. Strip partial decode values at this strict boundary
     * so callers receive the nested entry's authoring error instead.
     */
    private static <T> Codec<T> withoutPartialDecode(Codec<T> codec) {
        return Codec.of(codec, new Decoder<>() {
            @Override
            public <R> DataResult<Pair<T, R>> decode(
                    DynamicOps<R> ops,
                    R input
            ) {
                DataResult<Pair<T, R>> result =
                        codec.decode(ops, input);

                return result.error()
                        .<DataResult<Pair<T, R>>>map(error ->
                                DataResult.error(error::message)
                        )
                        .orElse(result);
            }
        });
    }

    public DamageAffixDefinition {
        id = Objects.requireNonNull(id, "Damage affix id must not be null");
        display = Objects.requireNonNull(display, "Damage affix display must not be null");
        slot = slot != null ? slot : DamageAffixSlot.ITEM;
        rarity = rarity != null ? rarity : DamageAffixRarity.COMMON;
        stacking = stacking != null ? stacking : DamageAffixStacking.STACK;
        stackingGroup = stackingGroup != null ? stackingGroup : Optional.empty();

        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException(
                    "Damage affix must contain at least one entry: " + id
            );
        }

        List<DamageEntryDefinition> normalizedEntries =
                new ArrayList<>(entries.size());

        for (DamageEntryDefinition entry : entries) {
            normalizedEntries.add(Objects.requireNonNull(
                    entry,
                    "Damage affix entry must not be null: " + id
            ));
        }

        entries = List.copyOf(normalizedEntries);
    }
    
    public Identifier stackingKey() {
        return stackingGroup.orElse(id);
    }

}
