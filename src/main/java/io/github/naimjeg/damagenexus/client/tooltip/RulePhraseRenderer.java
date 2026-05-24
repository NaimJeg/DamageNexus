package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.client.phrase.*;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static io.github.naimjeg.damagenexus.api.client.phrase.DamageNexusRulePhrases.UNKNOWN_CONDITION;
import static io.github.naimjeg.damagenexus.api.client.phrase.DamageNexusRulePhrases.UNKNOWN_EFFECT;

public final class RulePhraseRenderer {
    private final RulePhraseRegistry registry;
    private final RulePhraseValueRenderer values = new RulePhraseValueRenderer();
    private final Locale localeOverride;

    public RulePhraseRenderer(RulePhraseRegistry registry) {
        this(registry, null);
    }

    public RulePhraseRenderer(RulePhraseRegistry registry, Locale localeOverride) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.localeOverride = localeOverride;
    }

    public Optional<Component> render(
            RulePhrase phrase,
            PhraseForm form,
            Locale locale
    ) {
        Objects.requireNonNull(phrase, "phrase");
        Objects.requireNonNull(form, "form");
        Locale safeLocale = locale == null ? Locale.ROOT : locale;
        if (form == PhraseForm.COMPACT && isUnknown(phrase.type())) {
            return Optional.empty();
        }

        RulePhraseSchema schema = registry.schemas().stream()
                .filter(candidate -> candidate.type().equals(phrase.type()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown phrase schema: " + phrase.type().id()
                ));
        schema.validate(phrase.variant(), phrase.arguments());

        String key = phrase.type().translationKey(phrase.variant(), form);
        if (!Language.getInstance().has(key)) {
            return Optional.of(Component.translatable(
                    phrase.type().equals(UNKNOWN_CONDITION)
                            ? "tooltip.damagenexus.unknown_condition"
                            : phrase.type().equals(UNKNOWN_EFFECT)
                                    ? "tooltip.damagenexus.unknown_effect"
                                    : "tooltip.damagenexus.unknown_phrase"
            ));
        }

        List<Object> arguments = new ArrayList<>();
        for (PhraseSlot<?> slot : schema.slots()) {
            arguments.add(valueFor(phrase, slot, form, safeLocale));
        }
        return Optional.of(Component.translatable(key, arguments.toArray()));
    }

    public Optional<Component> render(RulePhrase phrase, PhraseForm form) {
        return render(
                phrase,
                form,
                localeOverride == null ? currentLocale() : localeOverride
        );
    }

    Component renderValue(PhraseValue value, PhraseForm form) {
        Locale locale = localeOverride == null ? currentLocale() : localeOverride;
        if (value instanceof NestedPhraseValue nested) {
            return render(nested.phrase(), form, locale).orElseGet(Component::empty);
        }
        return values.render(value, form, locale);
    }

    private Component valueFor(
            RulePhrase phrase,
            PhraseSlot<?> slot,
            PhraseForm form,
            Locale locale
    ) {
        for (PhraseArgument argument : phrase.arguments().entries()) {
            if (argument.slot() == slot) {
                if (argument.value() instanceof NestedPhraseValue nested) {
                    return render(nested.phrase(), form, locale)
                            .orElseGet(Component::empty);
                }
                return values.render(argument.value(), form, locale);
            }
        }
        return Component.empty();
    }

    private static boolean isUnknown(RulePhraseType type) {
        return type.equals(UNKNOWN_CONDITION) || type.equals(UNKNOWN_EFFECT);
    }

    private static Locale currentLocale() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getLanguageManager() == null) {
            return Locale.getDefault(Locale.Category.FORMAT);
        }
        String selected = minecraft.getLanguageManager().getSelected();
        Locale locale = Locale.forLanguageTag(selected.replace('_', '-'));
        return locale.getLanguage().isEmpty()
                ? Locale.getDefault(Locale.Category.FORMAT)
                : locale;
    }
}
