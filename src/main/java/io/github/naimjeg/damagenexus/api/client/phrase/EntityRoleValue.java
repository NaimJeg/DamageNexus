package io.github.naimjeg.damagenexus.api.client.phrase;

import java.util.Objects;

public record EntityRoleValue(Role role) implements PhraseValue {
    public EntityRoleValue {
        Objects.requireNonNull(role, "role");
    }

    public enum Role {
        ATTACKER("attacker"),
        TARGET("target");

        private final String serializedName;

        Role(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }
    }
}
