package net.minecraft.core;

import net.minecraft.tags.TagKey;

import java.util.Collection;

/** Test-only package fixture for simulating a registry tag reload. */
public final class HolderReferenceTagFixture {
    private HolderReferenceTagFixture() {
    }

    public static <T> void bind(
            Holder.Reference<T> holder,
            Collection<TagKey<T>> tags
    ) {
        holder.bindTags(tags);
    }
}
