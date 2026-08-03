package io.github.naimjeg.damagenexus.api.client.phrase;

import java.util.Objects;

public record NestedPhraseValue(RulePhrase phrase) implements PhraseValue {
    public NestedPhraseValue {
        Objects.requireNonNull(phrase, "phrase");
    }
}
