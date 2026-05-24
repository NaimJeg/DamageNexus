package io.github.naimjeg.damagenexus.api.client.phrase;

@FunctionalInterface
public interface RulePhraseFactory {
    RulePhrase create(
            RulePhraseType type,
            PhraseVariant variant,
            PhraseArguments arguments
    );
}
