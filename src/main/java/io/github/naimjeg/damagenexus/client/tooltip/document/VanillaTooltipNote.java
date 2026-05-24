package io.github.naimjeg.damagenexus.client.tooltip.document;

import io.github.naimjeg.damagenexus.api.client.phrase.PhraseValue;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** A complete localized technical note with typed arguments. */
public record VanillaTooltipNote(
        String translationKey,
        List<PhraseValue> arguments
) {
    private static final Pattern KEY = Pattern.compile("[a-z0-9][a-z0-9_.:/-]{0,255}");

    public VanillaTooltipNote {
        Objects.requireNonNull(translationKey, "translationKey");
        if (!KEY.matcher(translationKey).matches()) {
            throw new IllegalArgumentException("Invalid vanilla tooltip note key");
        }
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
        if (arguments.size() > 16 || arguments.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Invalid vanilla tooltip note arguments");
        }
    }
}
