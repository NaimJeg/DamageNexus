package io.github.naimjeg.damagenexus.api.client.phrase;

@FunctionalInterface
public interface RulePhraseProvider<T> {
    RulePhrase provide(T value, RulePhraseFactory phrases);
}
