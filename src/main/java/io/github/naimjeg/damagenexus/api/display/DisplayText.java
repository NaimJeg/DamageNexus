package io.github.naimjeg.damagenexus.api.display;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Authored display text. Literal and translatable forms are mutually exclusive. */
public sealed interface DisplayText permits DisplayText.Literal, DisplayText.Translatable {
    Pattern TRANSLATION_KEY = Pattern.compile("[a-z0-9][a-z0-9_.:/-]{0,255}");

    Codec<Encoded> ENCODED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DamageRuleLimits.boundedString(
                            DamageRuleLimits.MAX_DISPLAY_CODE_POINTS,
                            "translation key"
                    )
                    .optionalFieldOf("translate")
                    .forGetter(Encoded::translate),
            DamageRuleLimits.boundedString(
                            DamageRuleLimits.MAX_DISPLAY_CODE_POINTS,
                            "display text"
                    )
                    .optionalFieldOf("text")
                    .forGetter(Encoded::text),
            DamageRuleLimits.boundedList(
                            DamageRuleLimits.boundedString(
                                    DamageRuleLimits.MAX_DISPLAY_CODE_POINTS,
                                    "translation argument"
                            ),
                            DamageRuleLimits.MAX_TRANSLATION_ARGS,
                            "translation arguments"
                    )
                    .optionalFieldOf("args", List.of())
                    .forGetter(Encoded::args),
            DamageRuleLimits.boundedString(
                            DamageRuleLimits.MAX_DISPLAY_CODE_POINTS,
                            "display fallback"
                    )
                    .optionalFieldOf("fallback")
                    .forGetter(Encoded::fallback)
    ).apply(instance, Encoded::new));

    Codec<DisplayText> CODEC = ENCODED_CODEC.flatXmap(
            DisplayText::decode,
            DisplayText::encode
    );

    static DisplayText literal(String text) {
        return new Literal(text);
    }

    static DisplayText translatable(String key, String... args) {
        return new Translatable(
                key,
                args == null ? List.of() : Arrays.asList(args),
                Optional.empty()
        );
    }

    static DisplayText translatableWithFallback(
            String key,
            String fallback,
            String... args
    ) {
        return new Translatable(
                key,
                args == null ? List.of() : Arrays.asList(args),
                Optional.ofNullable(fallback)
        );
    }

    default boolean isBlank() {
        return false;
    }

    default String debugString() {
        return switch (this) {
            case Literal literal -> literal.text();
            case Translatable translatable -> translatable.fallback()
                    .orElse(translatable.key());
        };
    }

    static Optional<String> validationProblem(DisplayText text) {
        if (text == null) {
            return Optional.of("display_text_is_null");
        }
        return switch (text) {
            case Literal literal -> invalidText(literal.text(), "literal");
            case Translatable translatable -> {
                if (!TRANSLATION_KEY.matcher(translatable.key()).matches()) {
                    yield Optional.of("invalid_translation_key");
                }
                Optional<String> problem = invalidText(translatable.key(), "translation_key");
                if (problem.isPresent()) {
                    yield problem;
                }
                if (translatable.args().size() > DamageRuleLimits.MAX_TRANSLATION_ARGS) {
                    yield Optional.of("too_many_translation_arguments");
                }
                for (String argument : translatable.args()) {
                    problem = invalidText(argument, "translation_argument");
                    if (problem.isPresent()) {
                        yield problem;
                    }
                }
                if (translatable.fallback().isPresent()) {
                    yield invalidText(translatable.fallback().get(), "fallback");
                }
                yield Optional.empty();
            }
        };
    }

    private static DataResult<DisplayText> decode(Encoded encoded) {
        boolean hasTranslate = encoded.translate().isPresent();
        boolean hasText = encoded.text().isPresent();
        if (hasTranslate == hasText) {
            return DataResult.error(() ->
                    "DisplayText requires exactly one of 'translate' or 'text'"
            );
        }
        if (!hasTranslate && (!encoded.args().isEmpty() || encoded.fallback().isPresent())) {
            return DataResult.error(() ->
                    "DisplayText 'args' and 'fallback' require 'translate'"
            );
        }
        DisplayText value;
        try {
            value = hasTranslate
                    ? new Translatable(
                            encoded.translate().orElseThrow(),
                            encoded.args(),
                            encoded.fallback()
                    )
                    : new Literal(encoded.text().orElseThrow());
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() ->
                    "Invalid DisplayText: " + exception.getMessage()
            );
        }
        Optional<String> problem = validationProblem(value);
        return problem.<DataResult<DisplayText>>map(reason ->
                DataResult.error(() -> "Invalid DisplayText: " + reason)
        ).orElseGet(() -> DataResult.success(value));
    }

    private static DataResult<Encoded> encode(DisplayText value) {
        Optional<String> problem = validationProblem(value);
        if (problem.isPresent()) {
            return DataResult.error(() -> "Invalid DisplayText: " + problem.get());
        }
        return DataResult.success(switch (value) {
            case Literal literal -> new Encoded(
                    Optional.empty(), Optional.of(literal.text()),
                    List.of(), Optional.empty()
            );
            case Translatable translatable -> new Encoded(
                    Optional.of(translatable.key()), Optional.empty(),
                    translatable.args(), translatable.fallback()
            );
        });
    }

    private static Optional<String> invalidText(String value, String field) {
        if (value == null || value.isBlank()) {
            return Optional.of(field + "_is_blank");
        }
        if (value.codePointCount(0, value.length())
                > DamageRuleLimits.MAX_DISPLAY_CODE_POINTS) {
            return Optional.of(field + "_is_too_long");
        }
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            if (Character.isISOControl(codePoint)
                    || Character.getType(codePoint) == Character.FORMAT
                    || codePoint == 0x2028
                    || codePoint == 0x2029
                    || codePoint == 0x00A7) {
                return Optional.of(field + "_contains_layout_control");
            }
            offset += Character.charCount(codePoint);
        }
        return Optional.empty();
    }

    record Literal(String text) implements DisplayText {
        public Literal {
            Objects.requireNonNull(text, "text");
            Optional<String> problem = invalidText(text, "literal");
            if (problem.isPresent()) {
                throw new IllegalArgumentException(problem.get());
            }
        }
    }

    record Translatable(
            String key,
            List<String> args,
            Optional<String> fallback
    ) implements DisplayText {
        public Translatable {
            Objects.requireNonNull(key, "key");
            args = args == null ? List.of() : List.copyOf(args);
            fallback = fallback == null ? Optional.empty() : fallback;
            Optional<String> problem = validationProblemRaw(key, args, fallback);
            if (problem.isPresent()) {
                throw new IllegalArgumentException(problem.get());
            }
        }

        private static Optional<String> validationProblemRaw(
                String key,
                List<String> args,
                Optional<String> fallback
        ) {
            if (!TRANSLATION_KEY.matcher(key).matches()) {
                return Optional.of("invalid_translation_key");
            }
            Optional<String> problem = invalidText(key, "translation_key");
            if (problem.isPresent()) {
                return problem;
            }
            if (args.size() > DamageRuleLimits.MAX_TRANSLATION_ARGS) {
                return Optional.of("too_many_translation_arguments");
            }
            for (String argument : args) {
                problem = invalidText(argument, "translation_argument");
                if (problem.isPresent()) {
                    return problem;
                }
            }
            return fallback.isPresent()
                    ? invalidText(fallback.get(), "fallback")
                    : Optional.empty();
        }
    }

    record Encoded(
            Optional<String> translate,
            Optional<String> text,
            List<String> args,
            Optional<String> fallback
    ) {
        public Encoded {
            translate = translate == null ? Optional.empty() : translate;
            text = text == null ? Optional.empty() : text;
            args = args == null ? List.of() : List.copyOf(args);
            fallback = fallback == null ? Optional.empty() : fallback;
        }
    }
}
