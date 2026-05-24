package io.github.naimjeg.damagenexus.api.client.phrase;

import java.util.Objects;

public record PhraseArgument(PhraseSlot<?> slot, PhraseValue value) {
    public PhraseArgument {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(value, "value");
    }
}
