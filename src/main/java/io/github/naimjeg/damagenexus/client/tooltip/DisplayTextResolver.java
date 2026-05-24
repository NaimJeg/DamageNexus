package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.display.DisplayText;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public final class DisplayTextResolver {
    private DisplayTextResolver() {
    }

    public static Component resolve(DisplayText text) {
        if (text == null) {
            return Component.empty();
        }
        return switch (text) {
            case DisplayText.Literal literal -> Component.literal(literal.text());
            case DisplayText.Translatable translatable -> {
                Object[] arguments = translatable.args().stream()
                        .map(Component::literal)
                        .toArray(Object[]::new);
                if (!Language.getInstance().has(translatable.key())
                        && translatable.fallback().isEmpty()) {
                    yield Component.translatable(
                            "tooltip.damagenexus.unlocalized_display_text"
                    );
                }
                yield translatable.fallback().isPresent()
                        ? Component.translatableWithFallback(
                                translatable.key(),
                                translatable.fallback().get(),
                                arguments
                        )
                        : Component.translatable(translatable.key(), arguments);
            }
        };
    }

    public static Optional<Component> resolveOptional(Optional<DisplayText> text) {
        return text == null ? Optional.empty() : text.map(DisplayTextResolver::resolve);
    }

    public static String debugString(DisplayText text) {
        return text == null ? "" : text.debugString();
    }
}
