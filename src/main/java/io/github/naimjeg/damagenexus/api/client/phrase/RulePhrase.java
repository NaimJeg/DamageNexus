package io.github.naimjeg.damagenexus.api.client.phrase;

import java.util.Objects;

public record RulePhrase(
        RulePhraseType type,
        PhraseVariant variant,
        PhraseArguments arguments
) {
    public RulePhrase {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(variant, "variant");
        arguments = arguments == null ? PhraseArguments.EMPTY : arguments;
    }
}
