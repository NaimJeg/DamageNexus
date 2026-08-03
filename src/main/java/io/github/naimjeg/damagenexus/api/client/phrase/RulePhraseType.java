package io.github.naimjeg.damagenexus.api.client.phrase;

import net.minecraft.resources.Identifier;

import java.util.Objects;

public record RulePhraseType(Identifier id) {
    public RulePhraseType {
        Objects.requireNonNull(id, "id");
    }

    public static RulePhraseType of(String namespace, String path) {
        return new RulePhraseType(
                Identifier.fromNamespaceAndPath(namespace, path)
        );
    }

    public String translationKey(
            PhraseVariant variant,
            PhraseForm form
    ) {
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(form, "form");
        return "rule_phrase."
                + id.getNamespace()
                + "."
                + id.getPath().replace('/', '.')
                + "."
                + variant.serializedName()
                + "."
                + form.serializedName();
    }
}
