package io.github.naimjeg.damagenexus.api.client.phrase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record PhraseArguments(List<PhraseArgument> entries) {
    public static final PhraseArguments EMPTY = new PhraseArguments(List.of());

    public PhraseArguments {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T extends PhraseValue> Optional<T> get(PhraseSlot<T> slot) {
        for (PhraseArgument entry : entries) {
            if (entry.slot() == slot && slot.valueType().isInstance(entry.value())) {
                return Optional.of(slot.valueType().cast(entry.value()));
            }
        }
        return Optional.empty();
    }

    public static final class Builder {
        private final List<PhraseArgument> entries = new ArrayList<>();

        public <T extends PhraseValue> Builder put(PhraseSlot<T> slot, T value) {
            entries.add(new PhraseArgument(slot, value));
            return this;
        }

        public PhraseArguments build() {
            return new PhraseArguments(entries);
        }
    }
}
