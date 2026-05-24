package io.github.naimjeg.damagenexus.api.client.phrase;

import net.minecraft.resources.Identifier;
import java.util.Objects;

public record IdentifierValue(Identifier identifier) implements PhraseValue {
    public IdentifierValue {
        Objects.requireNonNull(identifier, "identifier");
    }
}
