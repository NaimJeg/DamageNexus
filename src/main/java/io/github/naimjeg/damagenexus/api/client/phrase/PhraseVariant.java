package io.github.naimjeg.damagenexus.api.client.phrase;

public enum PhraseVariant {
    DEFAULT("default"),
    INCREASE("increase"),
    DECREASE("decrease"),
    ABOVE("above"),
    BELOW("below"),
    BASE("base"),
    TRUE("true"),
    PRIMARY("primary"),
    PROC("proc");

    private final String serializedName;

    PhraseVariant(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
