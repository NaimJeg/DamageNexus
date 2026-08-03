package io.github.naimjeg.damagenexus.api.client.phrase;

import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;
import java.util.Objects;

public record RequestKindValue(DamageRequestKind kind) implements PhraseValue {
    public RequestKindValue {
        Objects.requireNonNull(kind, "kind");
    }
}
