package io.github.naimjeg.damagenexus.api.client.phrase;

/** Stores a ratio: 0.25 is rendered as 25%. */
public record PercentValue(double ratio) implements PhraseValue {
    public PercentValue {
        if (!Double.isFinite(ratio)) {
            throw new IllegalArgumentException("Percent phrase values must be finite");
        }
    }
}
