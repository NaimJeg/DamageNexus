package io.github.naimjeg.damagenexus.client.tooltip.document;

import net.minecraft.resources.Identifier;
import java.util.Objects;

public record TemplateReferenceTooltipView(Kind kind, Identifier id) {
    public TemplateReferenceTooltipView {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
    }

    public enum Kind {
        ENTRY,
        AFFIX
    }
}
