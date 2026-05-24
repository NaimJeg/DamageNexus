package io.github.naimjeg.damagenexus.diagnostics.logging;

import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDisplay;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixRarity;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSlot;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixStacking;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixValidator;
import io.github.naimjeg.damagenexus.api.rule.builder.DamageRuleBuilder;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryValidator;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticSecurityTest {

    @BeforeEach
    @AfterEach
    void clearDiagnosticState() {
        DamageNexusDiagnosticState.clearAll();
    }

    @Test
    void boundedLimiterUsesLruEvictionAndTtl() {
        AtomicLong clock = new AtomicLong();
        BoundedDiagnosticLimiter<String> limiter =
                new BoundedDiagnosticLimiter<>(
                        2,
                        Duration.ofNanos(10),
                        clock::get
                );

        assertTrue(limiter.shouldLog("a"));
        assertTrue(limiter.shouldLog("b"));
        assertFalse(limiter.shouldLog("a"));
        assertTrue(limiter.shouldLog("c"));
        assertEquals(2, limiter.size());

        assertTrue(limiter.shouldLog("b"));
        assertEquals(2, limiter.size());

        clock.set(11);
        assertEquals(0, limiter.size());
        assertTrue(limiter.shouldLog("a"));
        limiter.clear();
        assertEquals(0, limiter.size());
    }

    @Test
    void boundedLimiterCapacityRemainsExactUnderConcurrency()
            throws Exception {
        int capacity = 32;
        BoundedDiagnosticLimiter<String> limiter =
                new BoundedDiagnosticLimiter<>(
                        capacity,
                        Duration.ofMinutes(1)
                );
        var executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);

        try {
            for (int worker = 0; worker < 16; worker++) {
                int workerId = worker;
                executor.submit(() -> {
                    start.await();

                    for (int index = 0; index < 100; index++) {
                        limiter.shouldLog(
                                workerId + ":" + index
                        );
                    }

                    return null;
                });
            }

            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(
                    10,
                    TimeUnit.SECONDS
            ));
            assertTrue(limiter.size() <= capacity);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void sanitizerRemovesLineAndUnicodeLogSpoofingControls() {
        String sanitized = DiagnosticTextSanitizer.sanitizeLine(
                "trusted\r\n\u001B[31m[FAKE]\u0000"
                        + "\u202Espoof\u2066\uD83D\uDE03"
        );

        assertFalse(sanitized.contains("\r"));
        assertFalse(sanitized.contains("\n"));
        assertFalse(sanitized.contains("\u202E"));
        assertFalse(sanitized.contains("\u2066"));
        assertFalse(sanitized.contains("\u001B"));
        assertFalse(sanitized.contains("\u0000"));
        assertTrue(sanitized.contains("\uD83D\uDE03"));
    }

    @Test
    void sanitizerTruncatesByCodePointWithoutBreakingSurrogatePair() {
        String sanitized = DiagnosticTextSanitizer.sanitizeLine(
                "\uD83D\uDE03\uD83D\uDE03\uD83D\uDE03\uD83D\uDE03",
                3
        );

        assertEquals(3, sanitized.codePointCount(0, sanitized.length()));
        assertTrue(sanitized.endsWith("\u2026"));
        assertFalse(sanitized.contains("\uFFFD"));
    }

    @Test
    void throwableArgumentsKeepTheirStackTraceObject() {
        RuntimeException throwable = new RuntimeException("boom");
        Object[] sanitized =
                DiagnosticTextSanitizer.sanitizeArguments(
                        "line\r\ninjection",
                        throwable
                );

        assertEquals("line  injection", sanitized[0]);
        assertSame(throwable, sanitized[1]);
    }

    @Test
    void entryAndAffixWarningsIgnoreHighCardinalitySourceText() {
        List<DamageEntryDefinition> invalidEntries =
                java.util.Collections.singletonList(null);
        assertTrue(DamageEntryValidator.filterValid(
                invalidEntries,
                "attacker/head/Player supplied item name A"
        ).isEmpty());
        assertTrue(DamageEntryValidator.filterValid(
                invalidEntries,
                "victim/offhand/Player supplied item name B"
        ).isEmpty());
        assertEquals(
                1,
                DamageNexusDiagnosticState.domainSize(
                        DamageNexusDiagnosticState.Domain.ENTRY_VALIDATION
                )
        );

        List<DamageAffixDefinition> invalidAffixes =
                java.util.Collections.singletonList(null);
        assertTrue(DamageAffixValidator.filterValid(
                invalidAffixes,
                "attacker/chest/First name"
        ).isEmpty());
        assertTrue(DamageAffixValidator.filterValid(
                invalidAffixes,
                "victim/feet/Second name"
        ).isEmpty());
        assertEquals(
                1,
                DamageNexusDiagnosticState.domainSize(
                        DamageNexusDiagnosticState.Domain.AFFIX_VALIDATION
                )
        );
    }

    @Test
    void differentDefinitionOrReasonReceivesItsOwnDiagnostic() {
        var domain =
                DamageNexusDiagnosticState.Domain.ENTRY_VALIDATION;

        assertTrue(DamageNexusDiagnosticState.shouldLog(
                domain,
                "test:first",
                "ENTITY",
                "unsupported"
        ));
        assertFalse(DamageNexusDiagnosticState.shouldLog(
                domain,
                "test:first",
                "ENTITY",
                "unsupported"
        ));
        assertTrue(DamageNexusDiagnosticState.shouldLog(
                domain,
                "test:second",
                "ENTITY",
                "unsupported"
        ));
        assertTrue(DamageNexusDiagnosticState.shouldLog(
                domain,
                "test:first",
                "ENTITY",
                "different_reason"
        ));
        assertEquals(3, DamageNexusDiagnosticState.domainSize(domain));
    }

    private static DamageEntryDefinition entry(
            String path,
            DamageEntrySlot slot
    ) {
        DamageRuleDefinition rule = DamageRuleBuilder
                .offensive(id(path + "_rule"))
                .addBaseDamage(DamageChannel.UNTYPED_ID, 1.0f)
                .build();

        return new DamageEntryDefinition(
                id(path),
                DamageEntryDisplay.EMPTY,
                slot,
                List.of(rule),
                DamageEntryStacking.STACK,
                Optional.empty()
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("test", path);
    }
}
