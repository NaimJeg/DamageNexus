package io.github.naimjeg.damagenexus.api.client.phrase;

public enum PhraseForm {
    COMPACT("compact"),
    DETAIL("detail");

    private final String serializedName;

    PhraseForm(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
