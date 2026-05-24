package io.github.naimjeg.damagenexus.builtin.processor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResistanceMitigationProcessorTest {
    private static final float EPSILON = 0.0001f;

    @Test
    void ratingsCombineBeforeOneFormulaAndAllowCancellation() {
        assertEquals(25.0f,
                ResistanceMitigationProcessor.totalRating(25, 0, 0), EPSILON);
        assertEquals(25.0f,
                ResistanceMitigationProcessor.totalRating(0, 0, 25), EPSILON);
        assertEquals(25.0f,
                ResistanceMitigationProcessor.totalRating(0, 25, 0), EPSILON);
        assertEquals(50.0f,
                ResistanceMitigationProcessor.totalRating(10, 15, 25), EPSILON);
        assertEquals(0.5f,
                ResistanceMitigationProcessor.reductionFor(50, 50), EPSILON);
        assertEquals(0.0f,
                ResistanceMitigationProcessor.totalRating(25, 0, -25), EPSILON);
        assertEquals(0.0f,
                ResistanceMitigationProcessor.reductionFor(0, 50), EPSILON);
    }

    @Test
    void positiveAndNegativeCapsAndNonFiniteValuesAreSafe() {
        assertEquals(0.95f,
                ResistanceMitigationProcessor.reductionFor(100000, 1), EPSILON);
        assertEquals(-1.0f,
                ResistanceMitigationProcessor.reductionFor(-100000, 1), EPSILON);
        assertEquals(0.0f,
                ResistanceMitigationProcessor.reductionFor(Float.NaN, 50), EPSILON);
        assertEquals(0.0f,
                ResistanceMitigationProcessor.totalRating(
                        Float.POSITIVE_INFINITY, Float.NaN, 0), EPSILON);
        assertEquals(Float.MAX_VALUE,
                ResistanceMitigationProcessor.totalRating(
                        Float.MAX_VALUE, Float.MAX_VALUE, 0));
        assertEquals(0.95f,
                ResistanceMitigationProcessor.reductionFor(
                        Float.MAX_VALUE, 1), EPSILON);
        assertEquals(-Float.MAX_VALUE,
                ResistanceMitigationProcessor.totalRating(
                        -Float.MAX_VALUE, -Float.MAX_VALUE, 0));
        assertEquals(-1.0f,
                ResistanceMitigationProcessor.reductionFor(
                        -Float.MAX_VALUE, Float.MIN_VALUE), EPSILON);
    }

    @Test
    void identicalChannelAndCategoryHolderCanBeDeduplicated() {
        Object first = new Object();
        Object second = new Object();
        assertTrue(ResistanceMitigationProcessor.sameReference(first, first));
        assertFalse(ResistanceMitigationProcessor.sameReference(first, second));
    }

    @Test
    void bypassSignalsDisableMitigationWithoutChangingTheFormula() {
        assertTrue(ResistanceMitigationProcessor.shouldMitigate(
                true, false, false));
        assertFalse(ResistanceMitigationProcessor.shouldMitigate(
                true, true, false));
        assertFalse(ResistanceMitigationProcessor.shouldMitigate(
                true, false, true));
        assertFalse(ResistanceMitigationProcessor.shouldMitigate(
                false, false, false));
    }
}
