package io.github.naimjeg.damagenexus.api.client.phrase;

import net.minecraft.resources.Identifier;
import java.util.Objects;

public record TagValue(Kind kind, Identifier tagId) implements PhraseValue {
    public TagValue {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(tagId, "tagId");
    }

    public enum Kind {
        MOB_EFFECT("mob_effect"),
        ENTITY_TYPE("entity_type"),
        DAMAGE_TYPE("damage_type"),
        SOURCE("source");

        private final String serializedName;

        Kind(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }
    }
}
