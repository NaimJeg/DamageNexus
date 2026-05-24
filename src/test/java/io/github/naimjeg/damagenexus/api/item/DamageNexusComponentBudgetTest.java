package io.github.naimjeg.damagenexus.api.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperations;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleStacking;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDisplay;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixRarity;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSlot;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixStacking;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AllOfCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AlwaysCondition;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageNexusComponentBudgetTest {

    @Test
    void entryRuleAggregateAccepts128AndRejects129InJsonNbtAndNetwork() {
        List<DamageEntryDefinition> atLimit = entriesWithRules(
                4,
                DamageRuleLimits.MAX_ENTRY_RULES
        );
        List<DamageEntryDefinition> overLimit =
                new ArrayList<>(atLimit);
        overLimit.add(entry("over_rule", 1));

        assertComponentRoundTrips(
                DamageNexusItemEntries.ENTRY_STORAGE_CODEC,
                DamageNexusItemEntries.ENTRY_NETWORK_CODEC,
                atLimit
        );
        assertJsonDecodeRejected(
                DamageNexusItemEntries.ENTRY_STORAGE_CODEC,
                rawJsonList(
                        DamageEntryDefinition.STORAGE_CODEC,
                        overLimit
                )
        );
        assertNbtDecodeRejected(
                DamageNexusItemEntries.ENTRY_STORAGE_CODEC,
                DamageEntryDefinition.STORAGE_CODEC
                        .listOf()
                        .encodeStart(NbtOps.INSTANCE, overLimit)
                        .result()
                        .orElseThrow()
        );
        assertNetworkEncodeRejected(
                DamageNexusItemEntries.ENTRY_NETWORK_CODEC,
                overLimit
        );
        assertNetworkDecodeRejected(
                DamageNexusItemEntries.ENTRY_NETWORK_CODEC,
                DamageEntryDefinition.STORAGE_CODEC.listOf(),
                overLimit
        );
    }

    @Test
    void affixNestedEntryProductHasAnAggregateBoundary() {
        List<DamageAffixDefinition> atLimit = affixes(
                8,
                DamageRuleLimits.MAX_AFFIX_ENTRIES
        );
        List<DamageAffixDefinition> overLimit =
                new ArrayList<>(atLimit);
        overLimit.add(affix("over_affix", 1));

        assertComponentRoundTrips(
                DamageNexusItemEntries.AFFIX_STORAGE_CODEC,
                DamageNexusItemEntries.AFFIX_NETWORK_CODEC,
                atLimit
        );
        assertJsonDecodeRejected(
                DamageNexusItemEntries.AFFIX_STORAGE_CODEC,
                rawJsonList(
                        DamageAffixDefinition.STORAGE_CODEC,
                        overLimit
                )
        );
        assertNbtDecodeRejected(
                DamageNexusItemEntries.AFFIX_STORAGE_CODEC,
                DamageAffixDefinition.STORAGE_CODEC
                        .listOf()
                        .encodeStart(NbtOps.INSTANCE, overLimit)
                        .result()
                        .orElseThrow()
        );
        assertNetworkEncodeRejected(
                DamageNexusItemEntries.AFFIX_NETWORK_CODEC,
                overLimit
        );
        assertNetworkDecodeRejected(
                DamageNexusItemEntries.AFFIX_NETWORK_CODEC,
                DamageAffixDefinition.STORAGE_CODEC.listOf(),
                overLimit
        );
    }

    @Test
    void locallyLegalListsCannotExceedAggregateOperationOrConditionCost() {
        List<DamageEntryDefinition> operationsAtLimit =
                entriesWithOperationCost(64, 32);
        List<DamageEntryDefinition> operationsOver =
                new ArrayList<>(operationsAtLimit);
        operationsOver.add(entryWithRules(
                "operation_over",
                List.of(rule(
                        "operation_over_rule",
                        List.of(),
                        operations(1)
                ))
        ));

        assertComponentRoundTrips(
                DamageNexusItemEntries.ENTRY_STORAGE_CODEC,
                DamageNexusItemEntries.ENTRY_NETWORK_CODEC,
                operationsAtLimit
        );
        assertTrue(DamageNexusItemEntries.ENTRY_STORAGE_CODEC
                .encodeStart(JsonOps.INSTANCE, operationsOver)
                .error()
                .isPresent());

        List<DamageEntryDefinition> conditionAtLimit =
                entriesWithConditionCost(16, 256);
        List<DamageEntryDefinition> conditionOver =
                new ArrayList<>(conditionAtLimit);
        conditionOver.add(entryWithRules(
                "condition_over",
                List.of(rule(
                        "condition_over_rule",
                        List.of(new AlwaysCondition()),
                        operations(1)
                ))
        ));

        assertComponentRoundTrips(
                DamageNexusItemEntries.ENTRY_STORAGE_CODEC,
                DamageNexusItemEntries.ENTRY_NETWORK_CODEC,
                conditionAtLimit
        );
        assertTrue(DamageNexusItemEntries.ENTRY_STORAGE_CODEC
                .encodeStart(JsonOps.INSTANCE, conditionOver)
                .error()
                .isPresent());
    }

    @Test
    void componentDisplayBudgetAndLocalCountLimitsAreExact() {
        List<DamageEntryDefinition> displayAtLimit =
                new ArrayList<>();

        for (int index = 0; index < 32; index++) {
            displayAtLimit.add(entryWithDisplay(
                    "display_" + index,
                    512
            ));
        }

        assertComponentRoundTrips(
                DamageNexusItemEntries.ENTRY_STORAGE_CODEC,
                DamageNexusItemEntries.ENTRY_NETWORK_CODEC,
                displayAtLimit
        );

        List<DamageEntryDefinition> displayOver =
                new ArrayList<>(displayAtLimit);
        displayOver.set(
                0,
                entryWithDisplay("display_over", 513)
        );
        assertTrue(DamageNexusItemEntries.ENTRY_STORAGE_CODEC
                .encodeStart(JsonOps.INSTANCE, displayOver)
                .error()
                .isPresent());

        assertCodecSuccess(
                DamageNexusItemEntries.ENTRY_STORAGE_CODEC,
                entriesWithRules(
                        DamageRuleLimits.MAX_ITEM_ENTRIES,
                        1
                )
        );
        assertCodecSuccess(
                DamageNexusItemEntries.AFFIX_STORAGE_CODEC,
                affixes(
                        DamageRuleLimits.MAX_ITEM_AFFIXES,
                        1
                )
        );
    }

    @Test
    void unknownWideFieldsAreRejectedBeforeTypedDecodeWithoutPartialValue() {
        JsonObject entry = encode(
                DamageEntryDefinition.STORAGE_CODEC,
                entry("unknown_width", 1)
        ).getAsJsonObject();
        JsonArray unknown = new JsonArray();

        for (int index = 0;
                index < DamageRuleLimits.MAX_COMPONENT_RAW_NODES;
                index++) {
            unknown.add(index);
        }

        entry.add("unknown_attacker_tree", unknown);
        JsonArray component = new JsonArray();
        component.add(entry);

        var result = DamageNexusItemEntries.ENTRY_STORAGE_CODEC
                .parse(JsonOps.INSTANCE, component);

        assertTrue(result.error().isPresent());
        assertTrue(result.result().isEmpty());
    }

    private static List<DamageEntryDefinition> entriesWithRules(
            int entryCount,
            int rulesPerEntry
    ) {
        List<DamageEntryDefinition> entries = new ArrayList<>();

        for (int index = 0; index < entryCount; index++) {
            entries.add(entry("entry_" + index, rulesPerEntry));
        }

        return List.copyOf(entries);
    }

    private static List<DamageAffixDefinition> affixes(
            int affixCount,
            int entriesPerAffix
    ) {
        List<DamageAffixDefinition> affixes = new ArrayList<>();

        for (int index = 0; index < affixCount; index++) {
            affixes.add(affix(
                    "affix_" + index,
                    entriesPerAffix
            ));
        }

        return List.copyOf(affixes);
    }

    private static List<DamageEntryDefinition> entriesWithOperationCost(
            int ruleCount,
            int operationCount
    ) {
        List<DamageEntryDefinition> entries = new ArrayList<>();

        int remaining = ruleCount;
        int entryIndex = 0;

        while (remaining > 0) {
            int rulesInEntry = Math.min(
                    remaining,
                    DamageRuleLimits.MAX_ENTRY_RULES
            );
            List<DamageRuleDefinition> rules = new ArrayList<>();

            for (int ruleIndex = 0;
                    ruleIndex < rulesInEntry;
                    ruleIndex++) {
                rules.add(rule(
                        "operation_rule_"
                                + entryIndex
                                + "_"
                                + ruleIndex,
                        List.of(),
                        operations(operationCount)
                ));
            }

            entries.add(entryWithRules(
                    "operation_entry_" + entryIndex,
                    rules
            ));
            remaining -= rulesInEntry;
            entryIndex++;
        }

        return entries;
    }

    private static List<DamageEntryDefinition> entriesWithConditionCost(
            int ruleCount,
            int conditionNodes
    ) {
        List<DamageEntryDefinition> entries = new ArrayList<>();

        for (int index = 0; index < ruleCount; index++) {
            entries.add(entryWithRules(
                    "condition_entry_" + index,
                    List.of(rule(
                            "condition_rule_" + index,
                            List.of(conditionTree(conditionNodes)),
                            operations(1)
                    ))
            ));
        }

        return entries;
    }

    private static DamageRuleCondition conditionTree(int nodes) {
        if (nodes == 1) {
            return new AlwaysCondition();
        }

        int groupCount = 15;
        int leaves = (nodes - 1 - groupCount) / groupCount;
        int remainder = (nodes - 1 - groupCount) % groupCount;
        List<DamageRuleCondition> groups = new ArrayList<>();

        for (int group = 0; group < groupCount; group++) {
            int size = leaves + (group < remainder ? 1 : 0);
            groups.add(new AllOfCondition(
                    java.util.Collections.nCopies(
                            size,
                            new AlwaysCondition()
                    )
            ));
        }

        return new AllOfCondition(groups);
    }

    private static DamageEntryDefinition entry(
            String path,
            int ruleCount
    ) {
        List<DamageRuleDefinition> rules = new ArrayList<>();

        for (int index = 0; index < ruleCount; index++) {
            rules.add(rule(
                    path + "_rule_" + index,
                    List.of(),
                    operations(1)
            ));
        }

        return entryWithRules(path, rules);
    }

    private static DamageEntryDefinition entryWithRules(
            String path,
            List<DamageRuleDefinition> rules
    ) {
        return new DamageEntryDefinition(
                id(path),
                DamageEntryDisplay.EMPTY,
                DamageEntrySlot.ITEM,
                rules,
                DamageEntryStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageEntryDefinition entryWithDisplay(
            String path,
            int codePoints
    ) {
        List<DisplayText> lines = new ArrayList<>();
        int remaining = codePoints;

        while (remaining > 0) {
            int length = Math.min(
                    remaining,
                    DamageRuleLimits.MAX_DISPLAY_CODE_POINTS
            );
            lines.add(DisplayText.literal("x".repeat(length)));
            remaining -= length;
        }

        return new DamageEntryDefinition(
                id(path),
                new DamageEntryDisplay(
                        DisplayText.EMPTY,
                        lines,
                        Optional.empty(),
                        false
                ),
                DamageEntrySlot.ITEM,
                List.of(rule(
                        path + "_rule",
                        List.of(),
                        operations(1)
                )),
                DamageEntryStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageAffixDefinition affix(
            String path,
            int entryCount
    ) {
        List<DamageEntryDefinition> entries = new ArrayList<>();

        for (int index = 0; index < entryCount; index++) {
            entries.add(entry(path + "_entry_" + index, 1));
        }

        return new DamageAffixDefinition(
                id(path),
                DamageAffixDisplay.EMPTY,
                DamageAffixSlot.ITEM,
                DamageAffixRarity.COMMON,
                entries,
                DamageAffixStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageRuleDefinition rule(
            String path,
            List<DamageRuleCondition> conditions,
            List<DamageRuleOperation> operations
    ) {
        return new DamageRuleDefinition(
                id(path),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                conditions,
                operations,
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );
    }

    private static List<DamageRuleOperation> operations(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index ->
                        DamageNexusOperations.addBaseDamage(
                                DamageChannel.UNTYPED_ID,
                                1.0f
                        ))
                .map(DamageRuleOperation.class::cast)
                .toList();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("test", path);
    }

    private static <T> void assertCodecSuccess(
            Codec<T> codec,
            T value
    ) {
        assertEquals(
                value,
                codec.parse(
                                JsonOps.INSTANCE,
                                encode(codec, value)
                        )
                        .result()
                        .orElseThrow()
        );
    }

    private static <T> void assertComponentRoundTrips(
            Codec<T> codec,
            net.minecraft.network.codec.StreamCodec<ByteBuf, T> streamCodec,
            T value
    ) {
        assertCodecSuccess(codec, value);

        net.minecraft.nbt.Tag encodedNbt = codec
                .encodeStart(NbtOps.INSTANCE, value)
                .result()
                .orElseThrow();
        assertEquals(
                value,
                codec.parse(NbtOps.INSTANCE, encodedNbt)
                        .result()
                        .orElseThrow()
        );

        ByteBuf buffer = Unpooled.buffer();

        try {
            streamCodec.encode(buffer, value);
            assertEquals(value, streamCodec.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    private static <T> JsonArray rawJsonList(
            Codec<T> elementCodec,
            List<T> values
    ) {
        JsonArray array = new JsonArray();

        for (T value : values) {
            array.add(encode(elementCodec, value));
        }

        return array;
    }

    private static <T> JsonElement encode(Codec<T> codec, T value) {
        return codec.encodeStart(JsonOps.INSTANCE, value)
                .result()
                .orElseThrow();
    }

    private static <T> void assertJsonDecodeRejected(
            Codec<T> codec,
            JsonElement input
    ) {
        var result = codec.parse(JsonOps.INSTANCE, input);
        assertTrue(result.error().isPresent());
        assertTrue(result.result().isEmpty());
    }

    private static <T> void assertNbtDecodeRejected(
            Codec<T> codec,
            net.minecraft.nbt.Tag input
    ) {
        var result = codec.parse(NbtOps.INSTANCE, input);
        assertTrue(result.error().isPresent());
        assertTrue(result.result().isEmpty());
    }

    private static <T> void assertNetworkEncodeRejected(
            net.minecraft.network.codec.StreamCodec<ByteBuf, T> codec,
            T value
    ) {
        ByteBuf buffer = Unpooled.buffer();

        try {
            assertThrows(
                    RuntimeException.class,
                    () -> codec.encode(buffer, value)
            );
        } finally {
            buffer.release();
        }
    }

    private static <T> void assertNetworkDecodeRejected(
            net.minecraft.network.codec.StreamCodec<ByteBuf, T> guarded,
            Codec<T> unguardedStorageCodec,
            T value
    ) {
        ByteBuf buffer = Unpooled.buffer();

        try {
            ByteBufCodecs.fromCodec(unguardedStorageCodec)
                    .encode(buffer, value);
            assertThrows(RuntimeException.class, () -> guarded.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
