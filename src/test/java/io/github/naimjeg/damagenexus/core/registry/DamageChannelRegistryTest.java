package io.github.naimjeg.damagenexus.core.registry;

import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageChannelRegistryTest {

    private static final Identifier ALPHA =
            Identifier.fromNamespaceAndPath("test", "alpha");

    private static final Identifier BETA =
            Identifier.fromNamespaceAndPath("test", "beta");

    @AfterEach
    void resetRegistry() {
        DamageChannelRegistry.resetStateForTesting();
    }

    @Test
    void staleChannelWithSameIdResolvesToCurrentIndexAfterReload() {
        DamageChannelRegistry.replaceStateForTesting(definitions(BETA));
        DamageChannel staleBeta =
                DamageChannelRegistry.getChannelOrUntyped(BETA);

        assertEquals(1, staleBeta.index());
        assertTrue(DamageChannelRegistry.isCurrentRuntimeChannel(staleBeta));

        DamageChannelRegistry.replaceStateForTesting(definitions(ALPHA, BETA));

        DamageChannel resolved =
                DamageChannelRegistry.resolve(staleBeta);

        assertEquals(BETA, resolved.id());
        assertEquals(2, resolved.index());
        assertFalse(DamageChannelRegistry.isCurrentRuntimeChannel(staleBeta));
        assertTrue(DamageChannelRegistry.isKnownRuntimeChannel(staleBeta));
    }

    @Test
    void reloadAcceptsChannelBoundaryAndRejectsOneOverAtomically() {
        Map<Identifier, DamageChannelRegistry.ChannelDefinition> atLimit =
                numberedDefinitions(
                        DamageChannelRegistry.MAX_CUSTOM_CHANNELS,
                        0
                );

        DamageChannelRegistry.replaceStateForTesting(atLimit);
        assertEquals(
                DamageChannelRegistry.MAX_CUSTOM_CHANNELS + 1,
                DamageChannelRegistry.channelCount()
        );

        Map<Identifier, DamageChannelRegistry.ChannelDefinition> overLimit =
                numberedDefinitions(
                        DamageChannelRegistry.MAX_CUSTOM_CHANNELS + 1,
                        0
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> DamageChannelRegistry.replaceStateForTesting(
                        overLimit
                )
        );
        assertEquals(
                DamageChannelRegistry.MAX_CUSTOM_CHANNELS + 1,
                DamageChannelRegistry.channelCount()
        );
        assertTrue(DamageChannelRegistry.containsChannel(
                Identifier.fromNamespaceAndPath("test", "channel_127")
        ));
    }

    @Test
    void triggerTagLimitRejectsReloadAndKeepsOldSnapshot() {
        DamageChannelRegistry.replaceStateForTesting(definitions(ALPHA));

        DamageChannelRegistry.ChannelDefinition oversized =
                new DamageChannelRegistry.ChannelDefinition(
                        BETA,
                        java.util.stream.IntStream.range(
                                        0,
                                        DamageChannelRegistry
                                                .MAX_TRIGGER_TAGS_PER_CHANNEL
                                                + 1
                                )
                                .mapToObj(index ->
                                        net.minecraft.tags.TagKey.create(
                                                net.minecraft.core.registries
                                                        .Registries.DAMAGE_TYPE,
                                                Identifier.fromNamespaceAndPath(
                                                        "test",
                                                        "tag_" + index
                                                )
                                        ))
                                .toList(),
                        Optional.empty(),
                        true,
                        0
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> DamageChannelRegistry.replaceStateForTesting(
                        Map.of(BETA, oversized)
                )
        );
        assertTrue(DamageChannelRegistry.containsChannel(ALPHA));
        assertFalse(DamageChannelRegistry.containsChannel(BETA));
    }

    @Test
    void matchOrderPreservesPriorityThenIdentifierDeterminism() {
        Map<Identifier, DamageChannelRegistry.ChannelDefinition> values =
                new LinkedHashMap<>();
        values.put(
                ALPHA,
                definition(ALPHA, 10)
        );
        values.put(
                BETA,
                definition(BETA, 20)
        );
        Identifier gamma =
                Identifier.fromNamespaceAndPath("test", "gamma");
        values.put(
                gamma,
                definition(gamma, 20)
        );

        DamageChannelRegistry.replaceStateForTesting(values);

        assertEquals(
                List.of(BETA, gamma, ALPHA),
                DamageChannelRegistry.matchOrderIdsForTesting()
        );
    }

    @Test
    void duplicateChannelWinnerUsesResourceFileIdNotMapOrder() {
        Identifier duplicate = Identifier.fromNamespaceAndPath(
                "test",
                "duplicate"
        );
        Identifier firstFile = Identifier.fromNamespaceAndPath(
                "test",
                "a_first_file"
        );
        Identifier secondFile = Identifier.fromNamespaceAndPath(
                "test",
                "z_second_file"
        );
        DamageChannelRegistry.ChannelDefinition first =
                definition(duplicate, 11);
        DamageChannelRegistry.ChannelDefinition second =
                definition(duplicate, 99);

        Map<Identifier, DamageChannelRegistry.ChannelDefinition> forward =
                new LinkedHashMap<>();
        forward.put(firstFile, first);
        forward.put(secondFile, second);
        DamageChannelRegistry.replaceStateForTesting(forward);
        DamageChannel forwardChannel =
                DamageChannelRegistry.getChannelOrUntyped(duplicate);
        int forwardIndex = forwardChannel.index();
        int forwardPriority = DamageChannelRegistry
                .getData(forwardChannel)
                .priority();

        Map<Identifier, DamageChannelRegistry.ChannelDefinition> reverse =
                new LinkedHashMap<>();
        reverse.put(secondFile, second);
        reverse.put(firstFile, first);
        DamageChannelRegistry.replaceStateForTesting(reverse);
        DamageChannel reverseChannel =
                DamageChannelRegistry.getChannelOrUntyped(duplicate);

        assertEquals(11, forwardPriority);
        assertEquals(11, DamageChannelRegistry
                .getData(reverseChannel)
                .priority());
        assertEquals(forwardIndex, reverseChannel.index());
        assertEquals(duplicate, reverseChannel.id());
        assertEquals(2, DamageChannelRegistry.channelCount());
        assertEquals(0, DamageChannelRegistry.getUntyped().index());
    }

    @Test
    void concurrentReadersObserveOnlyCompleteSnapshots()
            throws Exception {
        Map<Identifier, DamageChannelRegistry.ChannelDefinition> first =
                numberedDefinitions(64, 0);
        Map<Identifier, DamageChannelRegistry.ChannelDefinition> second =
                numberedDefinitions(96, 1000);
        DamageChannelRegistry.replaceStateForTesting(first);

        try (var executor = Executors.newFixedThreadPool(3)) {
            CountDownLatch start = new CountDownLatch(1);
            var writer = executor.submit(() -> {
                start.await();

                for (int index = 0; index < 200; index++) {
                    DamageChannelRegistry.replaceStateForTesting(
                            index % 2 == 0 ? first : second
                    );
                }

                return null;
            });
            var reader = executor.submit(() -> {
                start.await();

                for (int index = 0; index < 2_000; index++) {
                    int count = DamageChannelRegistry.channelCount();

                    if (count != 65 && count != 97) {
                        throw new AssertionError(
                                "partial channel snapshot count=" + count
                        );
                    }

                    assertEquals(
                            DamageChannel.UNTYPED_ID,
                            DamageChannelRegistry.getUntyped().id()
                    );
                }

                return null;
            });

            start.countDown();
            writer.get(10, TimeUnit.SECONDS);
            reader.get(10, TimeUnit.SECONDS);
        }
    }

    private static Map<Identifier, DamageChannelRegistry.ChannelDefinition> definitions(
            Identifier... ids
    ) {
        Map<Identifier, DamageChannelRegistry.ChannelDefinition> definitions =
                new LinkedHashMap<>();

        for (Identifier id : ids) {
            definitions.put(
                    id,
                    new DamageChannelRegistry.ChannelDefinition(
                            id,
                            List.of(),
                            Optional.empty(),
                            true,
                            0
                    )
            );
        }

        return definitions;
    }

    private static Map<Identifier, DamageChannelRegistry.ChannelDefinition>
    numberedDefinitions(int count, int offset) {
        Map<Identifier, DamageChannelRegistry.ChannelDefinition> definitions =
                new LinkedHashMap<>();

        for (int index = 0; index < count; index++) {
            Identifier id = Identifier.fromNamespaceAndPath(
                    "test",
                    "channel_" + (index + offset)
            );
            definitions.put(id, definition(id, index));
        }

        return definitions;
    }

    private static DamageChannelRegistry.ChannelDefinition definition(
            Identifier id,
            int priority
    ) {
        return new DamageChannelRegistry.ChannelDefinition(
                id,
                List.of(),
                Optional.empty(),
                true,
                priority
        );
    }
}
