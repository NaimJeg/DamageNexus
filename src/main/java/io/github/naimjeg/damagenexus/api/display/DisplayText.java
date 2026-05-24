package io.github.naimjeg.damagenexus.api.display;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record DisplayText(
        Optional<String> translate,
        Optional<String> text,
        List<String> args,
        Optional<String> fallback
) {
    public static final DisplayText EMPTY =
            new DisplayText(
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    Optional.empty()
            );
    public static final Codec<DisplayText> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    DamageRuleLimits.boundedString(
                                    DamageRuleLimits.MAX_DISPLAY_CODE_POINTS,
                                    "translation key"
                            )
                            .optionalFieldOf("translate")
                            .forGetter(DisplayText::translate),

                    DamageRuleLimits.boundedString(
                                    DamageRuleLimits.MAX_DISPLAY_CODE_POINTS,
                                    "display text"
                            )
                            .optionalFieldOf("text")
                            .forGetter(DisplayText::text),

                    DamageRuleLimits.boundedList(
                                    DamageRuleLimits.boundedString(
                                            DamageRuleLimits.MAX_DISPLAY_CODE_POINTS,
                                            "translation argument"
                                    ),
                                    DamageRuleLimits.MAX_TRANSLATION_ARGS,
                                    "translation arguments"
                            )
                            .optionalFieldOf("args", List.of())
                            .forGetter(DisplayText::args),

                    DamageRuleLimits.boundedString(
                                    DamageRuleLimits.MAX_DISPLAY_CODE_POINTS,
                                    "display fallback"
                            )
                            .optionalFieldOf("fallback")
                            .forGetter(DisplayText::fallback)
            ).apply(instance, DisplayText::new));

    public DisplayText {
        translate = normalize(translate);
        text = normalize(text);
        fallback = normalize(fallback);
        args = args == null ? List.of() : List.copyOf(args);
    }

    public static DisplayText literal(String text) {
        return new DisplayText(
                Optional.empty(),
                Optional.ofNullable(text),
                List.of(),
                Optional.empty()
        );
    }

    public static DisplayText translatable(String key, String... args) {
        return new DisplayText(
                Optional.ofNullable(key),
                Optional.empty(),
                args == null ? List.of() : Arrays.asList(args),
                Optional.empty()
        );
    }

    public static DisplayText translatableWithFallback(
            String key,
            String fallback,
            String... args
    ) {
        return new DisplayText(
                Optional.ofNullable(key),
                Optional.empty(),
                args == null ? List.of() : Arrays.asList(args),
                Optional.ofNullable(fallback)
        );
    }

    private static Optional<String> normalize(Optional<String> input) {
        if (input == null || input.isEmpty()) {
            return Optional.empty();
        }

        String value = input.get();

        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(value);
    }

    public boolean isBlank() {
        return translate.isEmpty()
                && text.map(String::isBlank).orElse(true)
                && fallback.map(String::isBlank).orElse(true);
    }

    public String debugString() {
        if (translate.isPresent()) {
            return translate.get();
        }

        return text.or(() -> fallback).orElse("");
    }
}
