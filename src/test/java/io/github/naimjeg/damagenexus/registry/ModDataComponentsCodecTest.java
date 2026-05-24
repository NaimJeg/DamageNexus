package io.github.naimjeg.damagenexus.registry;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.item.DamageNexusItemEntries;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDisplay;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixRarity;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSlot;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixStacking;
import io.github.naimjeg.damagenexus.api.rule.builder.DamageRuleBuilder;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModDataComponentsCodecTest {

    @ParameterizedTest
    @EnumSource(DamageEntrySlot.class)
    void codecsAcceptEverySupportedSlot(DamageEntrySlot entrySlot) {
        DamageEntryDefinition entry = entry(
                "supported_" + entrySlot.name().toLowerCase(),
                entrySlot
        );
        DamageAffixDefinition affix = affix(
                "supported_" + entrySlot.name().toLowerCase() + "_affix",
                DamageAffixSlot.valueOf(entrySlot.name()),
                entry
        );

        assertEquals(entry, roundTrip(DamageEntryDefinition.CODEC, entry));
        assertEquals(entry,
                roundTrip(DamageEntryDefinition.STORAGE_CODEC, entry));
        assertEquals(affix, roundTrip(DamageAffixDefinition.CODEC, affix));
        assertEquals(affix,
                roundTrip(DamageAffixDefinition.STORAGE_CODEC, affix));
    }

    @Test
    void persistentAndNetworkCodecsUseTheCanonicalSchema() {
        DamageEntryDefinition weapon =
                entry("weapon_entry", DamageEntrySlot.WEAPON);
        DamageEntryDefinition projectile =
                entry("projectile_entry", DamageEntrySlot.PROJECTILE);
        DamageAffixDefinition armor = affix(
                "armor_affix",
                DamageAffixSlot.ARMOR,
                entry("armor_nested", DamageEntrySlot.ARMOR)
        );
        DamageNexusItemEntries bundle = new DamageNexusItemEntries(
                List.of(weapon, projectile),
                List.of(armor)
        );

        assertEquals(bundle,
                roundTrip(DamageNexusItemEntries.CODEC, bundle));
        assertEquals(bundle,
                roundTrip(DamageNexusItemEntries.STORAGE_CODEC, bundle));
        assertEquals(bundle,
                nbtRoundTrip(DamageNexusItemEntries.STORAGE_CODEC, bundle));
        assertEquals(bundle.entries(), networkRoundTrip(
                DamageNexusItemEntries.ENTRY_NETWORK_CODEC,
                bundle.entries()
        ));
        assertEquals(bundle.affixes(), networkRoundTrip(
                DamageNexusItemEntries.AFFIX_NETWORK_CODEC,
                bundle.affixes()
        ));
    }

    @Test
    void removedSlotNamesFailInPersistentCodecs() {
        DamageEntryDefinition entry =
                entry("canonical", DamageEntrySlot.ITEM);
        var entryJson = encode(DamageEntryDefinition.STORAGE_CODEC, entry)
                .getAsJsonObject();
        DamageAffixDefinition affix = affix(
                "canonical_affix",
                DamageAffixSlot.ITEM,
                entry
        );
        var affixJson = encode(DamageAffixDefinition.STORAGE_CODEC, affix)
                .getAsJsonObject();

        for (String removed : List.of("entity", "global")) {
            var removedEntrySlot = entryJson.deepCopy();
            removedEntrySlot.addProperty("slot", removed);
            assertTrue(DamageEntryDefinition.STORAGE_CODEC
                    .parse(JsonOps.INSTANCE, removedEntrySlot)
                    .error()
                    .isPresent());

            var removedAffixSlot = affixJson.deepCopy();
            removedAffixSlot.addProperty("slot", removed);
            assertTrue(DamageAffixDefinition.STORAGE_CODEC
                    .parse(JsonOps.INSTANCE, removedAffixSlot)
                    .error()
                    .isPresent());

            var removedNestedSlot = affixJson.deepCopy();
            removedNestedSlot.getAsJsonArray("entries")
                    .get(0)
                    .getAsJsonObject()
                    .addProperty("slot", removed);
            assertTrue(DamageAffixDefinition.STORAGE_CODEC
                    .parse(JsonOps.INSTANCE, removedNestedSlot)
                    .error()
                    .isPresent());
        }
    }

    private static DamageEntryDefinition entry(
            String path,
            DamageEntrySlot slot
    ) {
        return new DamageEntryDefinition(
                id(path),
                DamageEntryDisplay.EMPTY,
                slot,
                List.of(rule(path + "_rule")),
                DamageEntryStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageAffixDefinition affix(
            String path,
            DamageAffixSlot slot,
            DamageEntryDefinition entry
    ) {
        return new DamageAffixDefinition(
                id(path),
                DamageAffixDisplay.EMPTY,
                slot,
                DamageAffixRarity.COMMON,
                List.of(entry),
                DamageAffixStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageRuleDefinition rule(String path) {
        return DamageRuleBuilder
                .offensive(id(path))
                .addBaseDamage(DamageChannel.UNTYPED_ID, 1.0f)
                .build();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("test", path);
    }

    private static <T> T roundTrip(Codec<T> codec, T value) {
        return codec.parse(JsonOps.INSTANCE, encode(codec, value))
                .getOrThrow();
    }

    private static <T> JsonElement encode(Codec<T> codec, T value) {
        return codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
    }

    private static <T> T nbtRoundTrip(Codec<T> codec, T value) {
        var encoded = codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow();
        return codec.parse(NbtOps.INSTANCE, encoded).getOrThrow();
    }

    private static <T> T networkRoundTrip(
            StreamCodec<ByteBuf, T> codec,
            T value
    ) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            codec.encode(buffer, value);
            return codec.decode(buffer);
        } finally {
            buffer.release();
        }
    }
}
