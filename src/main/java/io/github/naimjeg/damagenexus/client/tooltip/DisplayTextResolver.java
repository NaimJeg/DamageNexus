package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.display.DisplayText;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public final class DisplayTextResolver {

    private DisplayTextResolver() {
    }

    public static Component resolve(DisplayText text) {
        if (text == null || text.isBlank()) {
            return Component.empty();
        }

        if (text.translate().isPresent()) {
            String key = text.translate().get();

            Object[] args = text.args()
                    .stream()
                    .map(Component::literal)
                    .toArray(Object[]::new);

            if (text.fallback().isPresent()) {
                return Component.translatableWithFallback(
                        key,
                        text.fallback().get(),
                        args
                );
            }

            return Component.translatable(key, args);
        }

        return Component.literal(
                text.text()
                        .or(text::fallback)
                        .orElse("")
        );
    }

    public static Optional<Component> resolveOptional(
            Optional<DisplayText> text
    ) {
        if (text == null || text.isEmpty() || text.get().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(resolve(text.get()));
    }

    public static String debugString(DisplayText text) {
        if (text == null) {
            return "";
        }

        return text.text()
                .or(text::fallback)
                .or(text::translate)
                .orElse("");
    }
}
