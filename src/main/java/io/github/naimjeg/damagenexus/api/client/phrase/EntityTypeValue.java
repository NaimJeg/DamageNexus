package io.github.naimjeg.damagenexus.api.client.phrase;

import net.minecraft.resources.Identifier;
import java.util.Objects;

public record EntityTypeValue(Identifier entityTypeId) implements PhraseValue {
    public EntityTypeValue {
        Objects.requireNonNull(entityTypeId, "entityTypeId");
    }
}
