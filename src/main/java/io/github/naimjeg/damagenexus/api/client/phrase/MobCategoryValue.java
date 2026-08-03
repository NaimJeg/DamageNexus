package io.github.naimjeg.damagenexus.api.client.phrase;

import net.minecraft.world.entity.MobCategory;
import java.util.Objects;

public record MobCategoryValue(MobCategory category) implements PhraseValue {
    public MobCategoryValue {
        Objects.requireNonNull(category, "category");
    }
}
