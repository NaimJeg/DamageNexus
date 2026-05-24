package io.github.naimjeg.damagenexus.api.client.phrase;

public record NumberValue(double value) implements PhraseValue {
    public NumberValue {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Number phrase values must be finite");
        }
    }
}
